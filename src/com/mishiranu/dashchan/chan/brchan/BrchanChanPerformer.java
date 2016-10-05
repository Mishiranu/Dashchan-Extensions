package com.mishiranu.dashchan.chan.brchan;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;
import android.util.Base64;

import chan.content.ApiException;
import chan.content.ChanPerformer;
import chan.content.InvalidResponseException;
import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.http.HttpException;
import chan.http.HttpRequest;
import chan.http.MultipartEntity;
import chan.http.UrlEncodedEntity;
import chan.text.ParseException;
import chan.util.CommonUtils;
import chan.util.StringUtils;

public class BrchanChanPerformer extends ChanPerformer
{
	@Override
	public ReadThreadsResult onReadThreads(ReadThreadsData data) throws HttpException, InvalidResponseException
	{
		BrchanChanLocator locator = BrchanChanLocator.get(this);
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
						threads.add(BrchanModelMapper.createThread(threadsArray.getJSONObject(j),
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
			if (data.pageNumber == 0)
			{
				uri = locator.buildQuery("settings.php", "board", data.boardName);
				JSONObject boardConfigObject = new HttpRequest(uri, data.holder, data).read().getJsonObject();
				if (boardConfigObject != null)
				{
					BrchanChanConfiguration configuration = BrchanChanConfiguration.get(this);
					configuration.updateFromBoardJson(data.boardName, boardConfigObject, true);
				}
			}
			try
			{
				JSONArray threadsArray = jsonObject.getJSONArray("threads");
				Posts[] threads = new Posts[threadsArray.length()];
				for (int i = 0; i < threads.length; i++)
				{
					threads[i] = BrchanModelMapper.createThread(threadsArray.getJSONObject(i),
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
	public ReadPostsResult onReadPosts(ReadPostsData data) throws HttpException, InvalidResponseException
	{
		BrchanChanLocator locator = BrchanChanLocator.get(this);
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
						posts[i] = BrchanModelMapper.createPost(jsonArray.getJSONObject(i),
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
		BrchanChanLocator locator = BrchanChanLocator.get(this);
		Uri uri = locator.buildQuery("search.php", "board", data.boardName, "search", data.searchQuery);
		String responseText = new HttpRequest(uri, data.holder, data).read().getString();
		try
		{
			return new ReadSearchPostsResult(new BrchanSearchParser(responseText, this).convertPosts());
		}
		catch (ParseException e)
		{
			throw new InvalidResponseException(e);
		}
	}

	private ArrayList<BoardCategory> readBoards(HttpRequest.Preset preset, boolean user)
			throws HttpException, InvalidResponseException
	{
		BrchanChanLocator locator = BrchanChanLocator.get(this);
		Uri uri = locator.buildPath("");
		String responseText = new HttpRequest(uri, preset).read().getString();
		LinkedHashMap<String, String> officialBoards;
		try
		{
			officialBoards = new BrchanBoardsParser(responseText).convertMap();
		}
		catch (ParseException e)
		{
			throw new InvalidResponseException(e);
		}
		uri = locator.buildPath("boards.json");
		JSONArray jsonArray = new HttpRequest(uri, preset).read().getJsonArray();
		if (jsonArray == null) throw new InvalidResponseException();
		try
		{
			LinkedHashMap<String, ArrayList<Board>> boards = new LinkedHashMap<>();
			if (!user)
			{
				// Set boards map order
				for (String category : officialBoards.values()) boards.put(category, null);
			}
			for (int i = 0; i < jsonArray.length(); i++)
			{
				JSONObject jsonObject = jsonArray.getJSONObject(i);
				String boardName = CommonUtils.getJsonString(jsonObject, "uri");
				if (officialBoards.containsKey(boardName) != user)
				{
					String category = user ? null : officialBoards.get(boardName);
					ArrayList<Board> boardList = boards.get(category);
					if (boardList == null)
					{
						boardList = new ArrayList<>();
						boards.put(category, boardList);
					}
					String title = CommonUtils.getJsonString(jsonObject, "title");
					String description = CommonUtils.optJsonString(jsonObject, "subtitle");
					boardList.add(new Board(boardName, title, description));
				}
			}
			ArrayList<BoardCategory> categories = new ArrayList<>();
			for (LinkedHashMap.Entry<String, ArrayList<Board>> entry : boards.entrySet())
			{
				if (entry.getValue() != null) categories.add(new BoardCategory(entry.getKey(), entry.getValue()));
			}
			return categories;
		}
		catch (JSONException e)
		{
			throw new InvalidResponseException(e);
		}
	}

	@Override
	public ReadBoardsResult onReadBoards(ReadBoardsData data) throws HttpException, InvalidResponseException
	{
		return new ReadBoardsResult(readBoards(data, false));
	}

	@Override
	public ReadUserBoardsResult onReadUserBoards(ReadUserBoardsData data) throws HttpException, InvalidResponseException
	{
		ArrayList<BoardCategory> categories = readBoards(data, true);
		if (categories.size() >= 1) return new ReadUserBoardsResult(categories.get(0).getBoards());
		return null;
	}

	@Override
	public ReadPostsCountResult onReadPostsCount(ReadPostsCountData data) throws HttpException, InvalidResponseException
	{
		BrchanChanLocator locator = BrchanChanLocator.get(this);
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
	}

	private static final Pattern PATTERN_CAPTCHA = Pattern.compile("<image src=\"data:image/png;base64,(.*?)\">" +
			"(?:.*?value=['\"]([^'\"]+?)['\"])?");

	@Override
	public ReadCaptchaResult onReadCaptcha(ReadCaptchaData data) throws HttpException, InvalidResponseException
	{
		BrchanChanLocator locator = BrchanChanLocator.get(this);
		Uri uri = locator.buildQuery("settings.php", "board", data.boardName);
		JSONObject jsonObject = new HttpRequest(uri, data.holder, data).read().getJsonObject();
		if (jsonObject == null) throw new InvalidResponseException();
		try
		{
			boolean newThreadCaptcha = jsonObject.optBoolean("new_thread_capt");
			jsonObject = jsonObject.getJSONObject("captcha");
			if (jsonObject.getBoolean("enabled") || data.threadNumber == null && newThreadCaptcha)
			{
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
						Bitmap newImage = Bitmap.createBitmap(image.getWidth(), image.getHeight(),
								Bitmap.Config.ARGB_8888);
						Paint paint = new Paint();
						float[] colorMatrixArray = {0.3f, 0.3f, 0.3f, 0f, 48f, 0.3f, 0.3f, 0.3f, 0f, 48f,
								0.3f, 0.3f, 0.3f, 0f, 48f, 0f, 0f, 0f, 1f, 0f};
						paint.setColorFilter(new ColorMatrixColorFilter(colorMatrixArray));
						new Canvas(newImage).drawBitmap(image, 0f, 0f, paint);
						image.recycle();
						CaptchaData captchaData = new CaptchaData();
						captchaData.put(CaptchaData.CHALLENGE, CommonUtils.getJsonString(jsonObject, "cookie"));
						return new ReadCaptchaResult(CaptchaState.CAPTCHA, captchaData).setImage(newImage)
								.setValidity(BrchanChanConfiguration.Captcha.Validity.SHORT_LIFETIME);
					}
				}
				throw new InvalidResponseException();
			}
		}
		catch (JSONException e)
		{
			throw new InvalidResponseException(e);
		}
		return new ReadCaptchaResult(CaptchaState.SKIP, null);
	}

	@Override
	public SendPostResult onSendPost(SendPostData data) throws HttpException, ApiException, InvalidResponseException
	{
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

		BrchanChanLocator locator = BrchanChanLocator.get(this);
		Uri uri = locator.buildPath("post.php");
		JSONObject jsonObject = new HttpRequest(uri, data.holder, data).setPostMethod(entity)
				.addHeader("Referer", (data.threadNumber == null ? locator.createBoardUri(data.boardName, 0)
				: locator.createThreadUri(data.boardName, data.threadNumber)).toString())
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
			if (errorMessage.contains("CAPTCHA") || errorMessage.contains("Você errou o codigo de verificação"))
			{
				errorType = ApiException.SEND_ERROR_CAPTCHA;
			}
			else if (errorMessage.contains("O corpo do texto"))
			{
				errorType = ApiException.SEND_ERROR_EMPTY_COMMENT;
			}
			else if (errorMessage.contains("Você deve fazer upload de uma imagem"))
			{
				errorType = ApiException.SEND_ERROR_EMPTY_FILE;
			}
			else if (errorMessage.contains("Seu arquivo é grande demais") || errorMessage.contains("é maior que"))
			{
				errorType = ApiException.SEND_ERROR_FILE_TOO_BIG;
			}
			else if (errorMessage.contains("Você tentou fazer upload de muitas"))
			{
				errorType = ApiException.SEND_ERROR_FILES_TOO_MANY;
			}
			else if (errorMessage.contains("longo demais"))
			{
				errorType = ApiException.SEND_ERROR_FIELD_TOO_LONG;
			}
			else if (errorMessage.contains("Tópico trancado"))
			{
				errorType = ApiException.SEND_ERROR_CLOSED;
			}
			else if (errorMessage.contains("Board inválida"))
			{
				errorType = ApiException.SEND_ERROR_NO_BOARD;
			}
			else if (errorMessage.contains("O tópico especificado não existe"))
			{
				errorType = ApiException.SEND_ERROR_NO_THREAD;
			}
			else if (errorMessage.contains("Formato de arquivo não aceito"))
			{
				errorType = ApiException.SEND_ERROR_FILE_NOT_SUPPORTED;
			}
			else if (errorMessage.contains("O arquivo"))
			{
				errorType = ApiException.SEND_ERROR_FILE_EXISTS;
			}
			else if (errorMessage.contains("Flood detectado"))
			{
				errorType = ApiException.SEND_ERROR_TOO_FAST;
			}
			if (errorType != 0) throw new ApiException(errorType);
			CommonUtils.writeLog("brchan send message", errorMessage);
			throw new ApiException(errorMessage);
		}
		throw new InvalidResponseException();
	}

	@Override
	public SendDeletePostsResult onSendDeletePosts(SendDeletePostsData data) throws HttpException, ApiException,
			InvalidResponseException
	{
		BrchanChanLocator locator = BrchanChanLocator.get(this);
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
			if (errorMessage.contains("Senha incorreta"))
			{
				errorType = ApiException.DELETE_ERROR_PASSWORD;
			}
			else if (errorMessage.contains("antes de apagar isso"))
			{
				errorType = ApiException.DELETE_ERROR_TOO_NEW;
			}
			if (errorType != 0) throw new ApiException(errorType);
			CommonUtils.writeLog("brchan delete message", errorMessage);
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
		BrchanChanLocator locator = BrchanChanLocator.get(this);
		UrlEncodedEntity entity = new UrlEncodedEntity("report", "1", "board", data.boardName);
		entity.add("delete_" + postNumber, "1");
		entity.add("reason", StringUtils.emptyIfNull(data.comment));
		if (data.options.contains("global")) entity.add("global", "1");
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
				CommonUtils.writeLog("brchan report message", errorMessage);
				throw new ApiException(errorMessage);
			}
			throw new InvalidResponseException();
		}
		return null;
	}
}