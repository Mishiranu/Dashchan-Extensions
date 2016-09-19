package com.mishiranu.dashchan.chan.nulltirech;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;
import android.util.Base64;

import chan.content.ApiException;
import chan.content.ChanPerformer;
import chan.content.InvalidResponseException;
import chan.content.ThreadRedirectException;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.http.HttpException;
import chan.http.HttpHolder;
import chan.http.HttpRequest;
import chan.http.MultipartEntity;
import chan.http.UrlEncodedEntity;
import chan.text.ParseException;
import chan.util.CommonUtils;
import chan.util.StringUtils;

public class NulltirechChanPerformer extends ChanPerformer
{
	@Override
	public ReadThreadsResult onReadThreads(ReadThreadsData data) throws HttpException, InvalidResponseException
	{
		NulltirechChanLocator locator = NulltirechChanLocator.get(this);
		if (data.isCatalog())
		{
			Uri uri = locator.buildPath(data.boardName, "catalog.json");
			JSONArray jsonArray = new HttpRequest(uri, data.holder, data).read().getJsonArray();
			if (jsonArray == null) throw new InvalidResponseException();
			if (jsonArray.length() == 1)
			{
				try
				{
					JSONObject jsonObject = jsonArray.getJSONObject(0);
					if (!jsonObject.has("threads")) return null;
				}
				catch (JSONException e)
				{
					throw new InvalidResponseException(e);
				}
			}
			try
			{
				ArrayList<Posts> threads = new ArrayList<>();
				for (int i = 0; i < jsonArray.length(); i++)
				{
					JSONArray threadsArray = jsonArray.getJSONObject(i).getJSONArray("threads");
					for (int j = 0; j < threadsArray.length(); j++)
					{
						threads.add(NulltirechModelMapper.createThread(threadsArray.getJSONObject(j),
								locator, data.boardName, true));
					}
				}
				return new ReadThreadsResult(threads);
			}
			catch (JSONException e)
			{
				throw new InvalidResponseException(e);
			}
		}
		else
		{
			Uri uri = locator.buildPath(data.boardName, data.pageNumber + ".json");
			JSONObject jsonObject = new HttpRequest(uri, data.holder, data).setValidator(data.validator)
					.read().getJsonObject();
			if (jsonObject == null) throw new InvalidResponseException();
			// TODO Uncomment after fix 400 response code
			/*if (data.pageNumber == 0)
			{
				uri = locator.buildQuery("settings.php", "board", data.boardName);
				JSONObject boardConfigObject = new HttpRequest(uri, data.holder, data).read().getJsonObject();
				if (boardConfigObject != null)
				{
					NulltirechChanConfiguration configuration = NulltirechChanConfiguration.get(this);
					configuration.updateFromBoardJson(data.boardName, boardConfigObject, true);
				}
			}*/
			try
			{
				JSONArray threadsArray = jsonObject.getJSONArray("threads");
				Posts[] threads = new Posts[threadsArray.length()];
				for (int i = 0; i < threads.length; i++)
				{
					threads[i] = NulltirechModelMapper.createThread(threadsArray.getJSONObject(i),
							locator, data.boardName, false);
				}
				return new ReadThreadsResult(threads);
			}
			catch (JSONException e)
			{
				throw new InvalidResponseException(e);
			}
		}
	}
	
	@Override
	public ReadPostsResult onReadPosts(ReadPostsData data) throws HttpException, ThreadRedirectException,
			InvalidResponseException
	{
		NulltirechChanLocator locator = NulltirechChanLocator.get(this);
		Uri uri = locator.buildPath(data.boardName, "res", data.threadNumber + ".json");
		JSONObject jsonObject = new HttpRequest(uri, data.holder, data).setValidator(data.validator)
				.read().getJsonObject();
		if (jsonObject != null)
		{
			try
			{
				JSONArray jsonArray = jsonObject.getJSONArray("posts");
				if (jsonArray.length() > 0)
				{
					Post[] posts = new Post[jsonArray.length()];
					for (int i = 0; i < posts.length; i++)
					{
						posts[i] = NulltirechModelMapper.createPost(jsonArray.getJSONObject(i),
								locator, data.boardName);
					}
					return new ReadPostsResult(posts);
				}
				return null;
			}
			catch (JSONException e)
			{
				throw new InvalidResponseException(e);
			}
		}
		throw new InvalidResponseException();
	}
	
	@Override
	public ReadSearchPostsResult onReadSearchPosts(ReadSearchPostsData data) throws HttpException,
			InvalidResponseException
	{
		NulltirechChanLocator locator = NulltirechChanLocator.get(this);
		Uri uri = locator.buildQuery("search.php", "board", data.boardName, "search", data.searchQuery);
		String responseText = new HttpRequest(uri, data.holder, data).read().getString();
		try
		{
			return new ReadSearchPostsResult(new NulltirechSearchParser(responseText, this).convertPosts());
		}
		catch (ParseException e)
		{
			throw new InvalidResponseException(e);
		}
	}
	
	@Override
	public ReadPostsCountResult onReadPostsCount(ReadPostsCountData data) throws HttpException, InvalidResponseException
	{
		NulltirechChanLocator locator = NulltirechChanLocator.get(this);
		Uri uri = locator.buildPath(data.boardName, "res", data.threadNumber + ".json");
		JSONObject jsonObject = new HttpRequest(uri, data.holder, data).setValidator(data.validator)
				.read().getJsonObject();
		if (jsonObject != null)
		{
			try
			{
				return new ReadPostsCountResult(jsonObject.getJSONArray("posts").length());
			}
			catch (JSONException e)
			{
				throw new InvalidResponseException(e);
			}
		}
		throw new InvalidResponseException();
	}private boolean mRequireCaptcha = false;
	
	private static final Pattern PATTERN_CAPTCHA = Pattern.compile("<image src=\"data:image/png;base64,(.*?)\">" +
			"(?:.*?value=['\"]([^'\"]+?)['\"])?");
	
	private static final String DNSBLS_CAPTCHA_CHALLENGE = "dnsbls";
	
	private static final String REQUIRE_REPORT = "report";
	
	@Override
	public ReadCaptchaResult onReadCaptcha(ReadCaptchaData data) throws HttpException, InvalidResponseException
	{
		NulltirechChanLocator locator = NulltirechChanLocator.get(this);
		if (!mRequireCaptcha)
		{
			try
			{
				// on fake post request to check DNSBLS bypass necessity
				UrlEncodedEntity entity = new UrlEncodedEntity("post", "1", "board", "b", "body", "",
						"json_response", "1");
				new HttpRequest(locator.buildPath("post.php"), data.holder, data).setPostMethod(entity)
						.setRedirectHandler(HttpRequest.RedirectHandler.STRICT).setSuccessOnly(false).execute();
				int responseCode = data.holder.getResponseCode();
				if (responseCode == HttpURLConnection.HTTP_BAD_REQUEST || responseCode == HttpURLConnection.HTTP_OK)
				{
					JSONObject jsonObject = data.holder.read().getJsonObject();
					if (jsonObject != null)
					{
						String message = CommonUtils.optJsonString(jsonObject, "error");
						if (message != null && message.contains("dnsbls_bypass"))
						{
							mRequireCaptcha = true;
						}
					}
				}
				else data.holder.checkResponseCode();
			}
			finally
			{
				data.holder.disconnect();
			}
		}
		if (mRequireCaptcha && data.mayShowLoadButton) return new ReadCaptchaResult(CaptchaState.NEED_LOAD, null);
		
		String dnsblsCaptchaChallenge = null;
		String sendingCaptchaChallenge = null;
		ArrayList<Bitmap> images = new ArrayList<>();
		NulltirechChanConfiguration.Captcha.Validity validity = null;
		
		if (data.requirement != null && data.requirement.startsWith(REQUIRE_REPORT))
		{
			String postNumber = data.requirement.substring(REQUIRE_REPORT.length());
			Uri uri = locator.buildQuery("report.php", "board", data.boardName, "post", "delete_" + postNumber);
			String responseText = new HttpRequest(uri, data.holder, data).read().getString();
			Matcher matcher = PATTERN_CAPTCHA.matcher(responseText);
			boolean success = false;
			if (matcher.find())
			{
				String base64 = matcher.group(1);
				String captchaChallenge = matcher.group(2);
				byte[] imageArray = Base64.decode(base64, Base64.DEFAULT);
				Bitmap image = BitmapFactory.decodeByteArray(imageArray, 0, imageArray.length);
				if (image != null)
				{
					Bitmap trimmed = CommonUtils.trimBitmap(image, 0xffffffff);
					if (trimmed != null && image != trimmed)
					{
						image.recycle();
						image = trimmed;
					}
					sendingCaptchaChallenge = captchaChallenge;
					images.add(image);
					success = true;
					validity = NulltirechChanConfiguration.Captcha.Validity.SHORT_LIFETIME;
				}
			}
			if (!success) throw new InvalidResponseException();
		}
		// TODO Uncomment after fix 400 response code
		/*else
		{
			Uri uri = locator.buildQuery("settings.php", "board", data.boardName);
			JSONObject jsonObject = new HttpRequest(uri, data.holder, data).read().getJsonObject();
			if (jsonObject == null) throw new InvalidResponseException();
			boolean success = false;
			try
			{
				jsonObject = jsonObject.getJSONObject("captcha");
				if (jsonObject.getBoolean("enabled"))
				{
					if (data.mayShowLoadButton) return new ReadCaptchaResult(CaptchaState.NEED_LOAD, null);
					String extra = CommonUtils.getJsonString(jsonObject, "extra");
					Uri providerUri = Uri.parse(CommonUtils.getJsonString(jsonObject, "provider_get"));
					uri = providerUri.buildUpon().scheme(uri.getScheme()).authority(uri.getAuthority())
							.appendQueryParameter("mode", "get").appendQueryParameter("extra", extra).build();
					jsonObject = new HttpRequest(uri, data.holder, data).read().getJsonObject();
					Matcher matcher = PATTERN_CAPTCHA.matcher(CommonUtils.getJsonString(jsonObject, "captchahtml"));
					if (matcher.matches())
					{
						String base64 = matcher.group(1);
						byte[] imageArray = Base64.decode(base64, Base64.DEFAULT);
						Bitmap image = BitmapFactory.decodeByteArray(imageArray, 0, imageArray.length);
						if (image != null)
						{
							Bitmap trimmed = CommonUtils.trimBitmap(image, 0xffffffff);
							if (trimmed != null && image != trimmed)
							{
								image.recycle();
								image = trimmed;
							}
							sendingCaptchaChallenge = CommonUtils.getJsonString(jsonObject, "cookie");
							images.add(image);
							validity = NulltirechChanConfiguration.Captcha.Validity.SHORT_LIFETIME;
							success = true;
						}
					}
					if (!success) throw new InvalidResponseException();
				}
			}
			catch (JSONException e)
			{
				throw new InvalidResponseException(e);
			}
		}*/
		
		if (mRequireCaptcha)
		{
			String responseText = new HttpRequest(locator.buildPath("dnsbls_bypass.php"), data.holder, data)
					.read().getString();
			Matcher matcher = PATTERN_CAPTCHA.matcher(responseText);
			boolean success = false;
			if (matcher.find())
			{
				String base64 = matcher.group(1);
				String captchaChallenge = matcher.group(2);
				byte[] imageArray = Base64.decode(base64, Base64.DEFAULT);
				Bitmap image = BitmapFactory.decodeByteArray(imageArray, 0, imageArray.length);
				if (image != null)
				{
					dnsblsCaptchaChallenge = captchaChallenge;
					images.add(image);
					success = true;
				}
			}
			if (!success) throw new InvalidResponseException();
		}
		
		if (images.isEmpty()) return new ReadCaptchaResult(CaptchaState.SKIP, null);
		else if (data.mayShowLoadButton) return new ReadCaptchaResult(CaptchaState.NEED_LOAD, null);
		
		int width = 0, height = 0;
		for (Bitmap bitmap : images)
		{
			width += bitmap.getWidth();
			height = Math.max(height, bitmap.getHeight());
		}
		Bitmap image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		Paint paint = new Paint();
		ColorMatrix colorMatrix = new ColorMatrix();
		colorMatrix.setSaturation(0f);
		paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
		Canvas canvas = new Canvas(image);
		int left = 0;
		for (Bitmap bitmap : images)
		{
			canvas.drawBitmap(bitmap, left, 0f, paint);
			left += bitmap.getWidth();
			bitmap.recycle();
		}
		
		CaptchaData captchaData = new CaptchaData();
		if (sendingCaptchaChallenge != null) captchaData.put(CaptchaData.CHALLENGE, sendingCaptchaChallenge);
		if (dnsblsCaptchaChallenge != null) captchaData.put(DNSBLS_CAPTCHA_CHALLENGE, dnsblsCaptchaChallenge);
		return new ReadCaptchaResult(CaptchaState.CAPTCHA, captchaData).setImage(image).setValidity(validity);
	}
	
	private boolean checkCaptcha(CaptchaData captchaData, HttpHolder holder) throws HttpException
	{
		String captchaInput = captchaData.get(CaptchaData.INPUT);
		int index = captchaInput.indexOf(' ');
		if (index >= 0)
		{
			String first = captchaInput.substring(0, index);
			captchaInput = captchaInput.substring(index + 1);
			captchaData.put(CaptchaData.INPUT, first);
		}
		UrlEncodedEntity entity = new UrlEncodedEntity("captcha_cookie", captchaData.get(DNSBLS_CAPTCHA_CHALLENGE),
				"captcha_text", captchaInput);
		NulltirechChanLocator locator = NulltirechChanLocator.get(this);
		Uri uri = locator.buildPath("dnsbls_bypass.php");
		String responseText = new HttpRequest(uri, holder).setPostMethod(entity)
				.setSuccessOnly(false).read().getString();
		if (holder.getResponseCode() != HttpURLConnection.HTTP_BAD_REQUEST) holder.checkResponseCode();
		if (responseText == null || !responseText.contains("<h1>Success!</h1>"))
		{
			return false;
		}
		mRequireCaptcha = false;
		return true;
	}
	
	@Override
	public SendPostResult onSendPost(SendPostData data) throws HttpException, ApiException, InvalidResponseException
	{
		if (mRequireCaptcha && data.captchaData != null && data.captchaData.get(DNSBLS_CAPTCHA_CHALLENGE) != null)
		{
			checkCaptcha(data.captchaData, data.holder);
		}
		MultipartEntity entity = new MultipartEntity();
		entity.add("post", "on");
		entity.add("board", data.boardName);
		entity.add("thread", data.threadNumber);
		entity.add("subject", data.subject);
		entity.add("body", StringUtils.emptyIfNull(data.comment));
		entity.add("name", data.name);
		entity.add("email", data.email);
		entity.add("password", data.password);
		if (data.optionSage) entity.add("no-bump", "on");
		entity.add("user_flag", data.userIcon);
		boolean hasSpoilers = false;
		if (data.attachments != null)
		{
			for (int i = 0; i < data.attachments.length; i++)
			{
				SendPostData.Attachment attachment = data.attachments[i];
				attachment.addToEntity(entity, "file" + (i > 0 ? i + 1 : ""));
				hasSpoilers |= attachment.optionSpoiler;
			}
		}
		if (hasSpoilers) entity.add("spoiler", "on");
		if (data.captchaData != null && data.captchaData.get(CaptchaData.CHALLENGE) != null)
		{
			entity.add("captcha_cookie", StringUtils.emptyIfNull(data.captchaData.get(CaptchaData.CHALLENGE)));
			entity.add("captcha_text", StringUtils.emptyIfNull(data.captchaData.get(CaptchaData.INPUT)));
		}
		entity.add("json_response", "1");
		
		NulltirechChanLocator locator = NulltirechChanLocator.get(this);
		Uri uri = locator.buildPath("post.php");
		JSONObject jsonObject = new HttpRequest(uri, data.holder, data).setPostMethod(entity)
				.addHeader("Referer", locator.buildPath().toString())
				.setRedirectHandler(HttpRequest.RedirectHandler.STRICT).read().getJsonObject();
		if (jsonObject == null) throw new InvalidResponseException();
		
		String redirect = jsonObject.optString("redirect");
		if (!StringUtils.isEmpty(redirect))
		{
			uri = locator.buildPath(redirect);
			String threadNumber = locator.getThreadNumber(uri);
			String postNumber = locator.getPostNumber(uri);
			return new SendPostResult(threadNumber, postNumber);
		}
		String errorMessage = jsonObject.optString("error");
		if (errorMessage != null)
		{
			int errorType = 0;
			if (errorMessage.contains("dnsbls_bypass"))
			{
				mRequireCaptcha = true;
				errorType = ApiException.SEND_ERROR_CAPTCHA;
			}
			else if (errorMessage.contains("CAPTCHA expired"))
			{
				errorType = ApiException.SEND_ERROR_CAPTCHA;
			}
			else if (errorMessage.contains("The body was") || errorMessage.contains("must be at least"))
			{
				errorType = ApiException.SEND_ERROR_EMPTY_COMMENT;
			}
			else if (errorMessage.contains("You must upload an image"))
			{
				errorType = ApiException.SEND_ERROR_EMPTY_FILE;
			}
			else if (errorMessage.contains("The file was too big") || errorMessage.contains("is longer than"))
			{
				errorType = ApiException.SEND_ERROR_FILE_TOO_BIG;
			}
			else if (errorMessage.contains("You have attempted to upload too many"))
			{
				errorType = ApiException.SEND_ERROR_FILES_TOO_MANY;
			}
			else if (errorMessage.contains("was too long"))
			{
				errorType = ApiException.SEND_ERROR_FIELD_TOO_LONG;
			}
			else if (errorMessage.contains("Thread locked"))
			{
				errorType = ApiException.SEND_ERROR_CLOSED;
			}
			else if (errorMessage.contains("Invalid board"))
			{
				errorType = ApiException.SEND_ERROR_NO_BOARD;
			}
			else if (errorMessage.contains("Thread specified does not exist"))
			{
				errorType = ApiException.SEND_ERROR_NO_THREAD;
			}
			else if (errorMessage.contains("Unsupported image format"))
			{
				errorType = ApiException.SEND_ERROR_FILE_NOT_SUPPORTED;
			}
			else if (errorMessage.contains("That file"))
			{
				errorType = ApiException.SEND_ERROR_FILE_EXISTS;
			}
			else if (errorMessage.contains("Flood detected"))
			{
				errorType = ApiException.SEND_ERROR_TOO_FAST;
			}
			if (errorType != 0) throw new ApiException(errorType);
			CommonUtils.writeLog("8chan send message", errorMessage);
			throw new ApiException(errorMessage);
		}
		throw new InvalidResponseException();
	}
	
	@Override
	public SendDeletePostsResult onSendDeletePosts(SendDeletePostsData data) throws HttpException, ApiException,
			InvalidResponseException
	{
		NulltirechChanLocator locator = NulltirechChanLocator.get(this);
		UrlEncodedEntity entity = new UrlEncodedEntity("delete", "on", "board", data.boardName,
				"password", data.password, "json_response", "1");
		for (String postNumber : data.postNumbers) entity.add("delete_" + postNumber, "on");
		if (data.optionFilesOnly) entity.add("file", "on");
		Uri uri = locator.buildPath("post.php");
		JSONObject jsonObject = new HttpRequest(uri, data.holder, data).setPostMethod(entity)
				.setRedirectHandler(HttpRequest.RedirectHandler.STRICT).setSuccessOnly(false)
				.read().getJsonObject();
		if (jsonObject == null) throw new InvalidResponseException();
		if (jsonObject.optBoolean("success")) return null;
		String errorMessage = jsonObject.optString("error");
		if (errorMessage != null)
		{
			int errorType = 0;
			if (errorMessage.contains("dnsbls_bypass"))
			{
				mRequireCaptcha = true;
				boolean first = true;
				while (true)
				{
					CaptchaData captchaData = requireUserCaptcha("delete", data.boardName, data.threadNumber, !first);
					if (captchaData == null) throw new ApiException(ApiException.DELETE_ERROR_NO_ACCESS);
					if (Thread.currentThread().isInterrupted()) return null;
					boolean success = checkCaptcha(captchaData, data.holder);
					if (success) break;
					first = false;
				}
				onSendDeletePosts(data);
				return null;
			}
			else if (errorMessage.contains("Wrong password"))
			{
				errorType = ApiException.DELETE_ERROR_PASSWORD;
			}
			else if (errorMessage.contains("before deleting that"))
			{
				errorType = ApiException.DELETE_ERROR_TOO_NEW;
			}
			else if (errorMessage.contains("That post has no files"))
			{
				return null;
			}
			if (errorType != 0) throw new ApiException(errorType);
			CommonUtils.writeLog("8chan delete message", errorMessage);
			throw new ApiException(errorMessage);
		}
		throw new InvalidResponseException();
	}
	
	private static final Pattern PATTERN_REPORT = Pattern.compile("<strong>(.*?)</strong>");
	
	@Override
	public SendReportPostsResult onSendReportPosts(SendReportPostsData data) throws HttpException, ApiException,
			InvalidResponseException
	{
		String postNumber = data.postNumbers.get(0);
		boolean first = true;
		while (true)
		{
			CaptchaData captchaData = requireUserCaptcha(REQUIRE_REPORT + postNumber,
					data.boardName, data.threadNumber, !first);
			if (captchaData == null) throw new ApiException(ApiException.REPORT_ERROR_NO_ACCESS);
			if (Thread.currentThread().isInterrupted()) return null;
			if (captchaData.get(DNSBLS_CAPTCHA_CHALLENGE) != null)
			{
				boolean success = checkCaptcha(captchaData, data.holder);
				if (!success)
				{
					first = false;
					continue;
				}
			}
			NulltirechChanLocator locator = NulltirechChanLocator.get(this);
			UrlEncodedEntity entity = new UrlEncodedEntity("report", "1", "board", data.boardName);
			entity.add("delete_" + postNumber, "1");
			entity.add("reason", StringUtils.emptyIfNull(data.comment));
			entity.add("captcha_cookie", StringUtils.emptyIfNull(captchaData.get(CaptchaData.CHALLENGE)));
			entity.add("captcha_text", StringUtils.emptyIfNull(captchaData.get(CaptchaData.INPUT)));
			Uri uri = locator.buildPath("post.php");
			String responseText = new HttpRequest(uri, data.holder, data).setPostMethod(entity)
					.setRedirectHandler(HttpRequest.RedirectHandler.STRICT).setSuccessOnly(false)
					.read().getString();
			Matcher matcher = PATTERN_REPORT.matcher(responseText);
			if (matcher.find())
			{
				String errorMessage = matcher.group(1);
				if (errorMessage != null)
				{
					int errorType = 0;
					if (errorMessage.contains("CAPTCHA expired"))
					{
						first = false;
						continue;
					}
					if (errorType != 0) throw new ApiException(errorType);
					CommonUtils.writeLog("8chan report message", errorMessage);
					throw new ApiException(errorMessage);
				}
				throw new InvalidResponseException();
			}
			break;
		}
		return null;
	}
}