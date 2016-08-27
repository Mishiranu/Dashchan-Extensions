package com.mishiranu.dashchan.chan.alterchan;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;

import chan.content.ApiException;
import chan.content.ChanPerformer;
import chan.content.InvalidResponseException;
import chan.content.model.Post;
import chan.http.CookieBuilder;
import chan.http.HttpException;
import chan.http.HttpHolder;
import chan.http.HttpRequest;
import chan.http.MultipartEntity;
import chan.http.UrlEncodedEntity;
import chan.text.ParseException;
import chan.util.CommonUtils;
import chan.util.StringUtils;

public class AlterchanChanPerformer extends ChanPerformer
{
	@Override
	public ReadThreadsResult onReadThreads(ReadThreadsData data) throws HttpException, InvalidResponseException
	{
		AlterchanChanLocator locator = AlterchanChanLocator.get(this);
		Uri uri = locator.createBoardUri(data.boardName, data.pageNumber);
		String responseText = new HttpRequest(uri, data).addCookie(getAuthorizationCookie(data.holder))
				.setValidator(data.validator).read().getString();
		try
		{
			return new ReadThreadsResult(new AlterchanPostsParser(responseText, this).convertThreads());
		}
		catch (ParseException e)
		{
			throw new InvalidResponseException(e);
		}
	}
	
	private static final HttpRequest.RedirectHandler READ_POSTS_REDIRECT_HANDLER = new HttpRequest.RedirectHandler()
	{
		@Override
		public Action onRedirectReached(int responseCode, Uri requestedUri, Uri redirectedUri, HttpHolder holder)
				throws HttpException
		{
			String path = redirectedUri.getPath();
			if (StringUtils.isEmpty(path) || "/".equals(path)) throw HttpException.createNotFoundException();
			return BROWSER.onRedirectReached(responseCode, requestedUri, redirectedUri, holder);
		}
	};
	
	@Override
	public ReadPostsResult onReadPosts(ReadPostsData data) throws HttpException, InvalidResponseException
	{
		AlterchanChanLocator locator = AlterchanChanLocator.get(this);
		Uri uri = locator.createThreadUri(data.boardName, data.threadNumber);
		String responseText = new HttpRequest(uri, data).addCookie(getAuthorizationCookie(data.holder))
				.setValidator(data.validator).setRedirectHandler(READ_POSTS_REDIRECT_HANDLER).read().getString();
		try
		{
			ArrayList<Post> posts = new AlterchanPostsParser(responseText, this).convertPosts();
			if (posts == null || posts.isEmpty()) throw new InvalidResponseException();
			return new ReadPostsResult(posts);
		}
		catch (ParseException e)
		{
			throw new InvalidResponseException(e);
		}
	}
	
	@Override
	public ReadSinglePostResult onReadSinglePost(ReadSinglePostData data) throws HttpException, InvalidResponseException
	{
		AlterchanChanLocator locator = AlterchanChanLocator.get(this);
		Uri uri = locator.buildQuery("ajax_get_subpost.php", "id", locator.convertToPresentNumber(data.postNumber));
		String responseText = new HttpRequest(uri, data).addCookie(getAuthorizationCookie(data.holder))
				.read().getString();
		try
		{
			Post post = new AlterchanPostsParser(responseText, this).convertSignlePost();
			if (post == null) throw HttpException.createNotFoundException();
			return new ReadSinglePostResult(post);
		}
		catch (ParseException e)
		{
			throw new InvalidResponseException(e);
		}
	}
	
	@Override
	public ReadSearchPostsResult onReadSearchPosts(ReadSearchPostsData data) throws HttpException,
			InvalidResponseException
	{
		AlterchanChanLocator locator = AlterchanChanLocator.get(this);
		Uri uri = locator.buildQuery("search.php", "q", data.searchQuery);
		String responseText = new HttpRequest(uri, data).addCookie(getAuthorizationCookie(data.holder))
				.read().getString();
		try
		{
			return new ReadSearchPostsResult(new AlterchanPostsParser(responseText, this).convertPosts());
		}
		catch (ParseException e)
		{
			throw new InvalidResponseException(e);
		}
	}
	
	@Override
	public ReadPostsCountResult onReadPostsCount(ReadPostsCountData data) throws HttpException, InvalidResponseException
	{
		AlterchanChanLocator locator = AlterchanChanLocator.get(this);
		Uri uri = locator.createThreadUri(data.boardName, data.threadNumber);
		String responseText = new HttpRequest(uri, data).addCookie(getAuthorizationCookie(data.holder))
				.setValidator(data.validator).setRedirectHandler(READ_POSTS_REDIRECT_HANDLER).read().getString();
		if (!responseText.contains("<input type=\"hidden\" name=\"post_pid\"")) throw new InvalidResponseException();
		int count = 0;
		int index = 0;
		while (index != -1)
		{
			count++;
			index = responseText.indexOf("<div class=\"subpost_wrapper", index + 1);
		}
		return new ReadPostsCountResult(count);
	}
	
	private static final HttpRequest.RedirectHandler POST_REDIRECT_HANDLER = new HttpRequest.RedirectHandler()
	{
		@Override
		public Action onRedirectReached(int responseCode, Uri requestedUri, Uri redirectedUri, HttpHolder holder)
				throws HttpException
		{
			if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP) return Action.CANCEL;
			return STRICT.onRedirectReached(responseCode, requestedUri, redirectedUri, holder);
		}
	};
	
	@Override
	public CheckAuthorizationResult onCheckAuthorization(CheckAuthorizationData data) throws HttpException,
			InvalidResponseException
	{
		return new CheckAuthorizationResult(getAuthorizationCookie(data.holder, data.authorizationData) != null);
	}
	
	private String mLastAuthorizationData = null;
	private CookieBuilder mLastAuthorizationCookieBuilder = null;
	
	private CookieBuilder getAuthorizationCookie(HttpHolder holder, String[] userAuthorizationData) throws HttpException
	{
		synchronized (this)
		{
			String nextAuthorizationData = null;
			CookieBuilder nextAuthorizationCookieBuilder = null;
			try
			{
				if (userAuthorizationData == null || StringUtils.isEmpty(userAuthorizationData[1])) return null;
				AlterchanChanLocator locator = AlterchanChanLocator.get(this);
				Uri uri = locator.buildPath("signin.php");
				String authorizationData = userAuthorizationData[0] + "|" + userAuthorizationData[1];
				if (authorizationData.equals(mLastAuthorizationData))
				{
					new HttpRequest(uri, holder).setRedirectHandler(POST_REDIRECT_HANDLER).read().getString();
					if (holder.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP)
					{
						nextAuthorizationData = mLastAuthorizationData;
						nextAuthorizationCookieBuilder = mLastAuthorizationCookieBuilder;
						return nextAuthorizationCookieBuilder;
					}
					// User was signed out
				}
				UrlEncodedEntity entity = new UrlEncodedEntity();
				if (!StringUtils.isEmpty(userAuthorizationData[0]))
				{
					entity.add("06dc484f16062aee", userAuthorizationData[0]);
					entity.add("979819a4e254fb76", userAuthorizationData[1]);
				}
				else entity.add("8b44f8bb93b681e8", userAuthorizationData[1]);
				new HttpRequest(uri, holder).setPostMethod(entity)
						.setRedirectHandler(POST_REDIRECT_HANDLER).read().getString();
				List<String> headers = holder.getHeaderFields().get("Set-Cookie");
				CookieBuilder cookieBuilder = new CookieBuilder();
				if (headers != null)
				{
					for (String cookie : headers)
					{
						int index = cookie.indexOf(';');
						if (index >= 0) cookie = cookie.substring(0, index);
						index = cookie.indexOf('=');
						String name = cookie.substring(0, index);
						String value = cookie.substring(index + 1);
						if (name.length() == 16) cookieBuilder.append(name, value);
					}
				}
				if (cookieBuilder.build().isEmpty()) return null;
				nextAuthorizationData = authorizationData;
				nextAuthorizationCookieBuilder = cookieBuilder;
				return cookieBuilder;
			}
			finally
			{
				mLastAuthorizationData = nextAuthorizationData;
				mLastAuthorizationCookieBuilder = nextAuthorizationCookieBuilder;
			}
		}
	}
	
	private CookieBuilder getAuthorizationCookie(HttpHolder holder) throws HttpException
	{
		AlterchanChanConfiguration configuration = AlterchanChanConfiguration.get(this);
		return getAuthorizationCookie(holder, configuration.getUserAuthorizationData());
	}
	
	@Override
	public ReadCaptchaResult onReadCaptcha(ReadCaptchaData data) throws HttpException, InvalidResponseException
	{
		CookieBuilder cookieBuilder = getAuthorizationCookie(data.holder);
		if (cookieBuilder != null) return new ReadCaptchaResult(CaptchaState.SKIP, null);
		AlterchanChanLocator locator = AlterchanChanLocator.get(this);
		Uri uri = locator.buildPath("func", "securimage", "securimage_show.php");
		Bitmap image = new HttpRequest(uri, data).addCookie(cookieBuilder).read().getBitmap();
		if (image == null) throw new InvalidResponseException();
		String sessionCookie = data.holder.getCookieValue("PHPSESSID");
		Bitmap editable = image.copy(Bitmap.Config.ARGB_8888, true);
		image.recycle();
		if (editable == null) throw new RuntimeException();
		int[] pixels = new int[9];
		int center = pixels.length / 2;
		for (int i = 1; i < editable.getWidth() - 1; i++)
		{
			for (int j = 1; j < editable.getHeight() - 1; j++)
			{
				editable.getPixels(pixels, 0, 3, i - 1, j - 1, 3, 3);
				if (pixels[center] != 0xffffffff)
				{
					int count = 0;
					for (int k = 0; k < pixels.length; k++)
					{
						if (pixels[k] != 0xffffffff) count++;
					}
					if (count < 5) editable.setPixel(i, j, 0x00000000);
				}
			}
		}
		image = Bitmap.createBitmap(editable.getWidth(), editable.getHeight(), Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(image);
		canvas.drawColor(0xffffffff);
		canvas.drawBitmap(editable, 0, 0, null);
		editable.recycle();
		CaptchaData captchaData = new CaptchaData();
		captchaData.put(CaptchaData.CHALLENGE, sessionCookie);
		return new ReadCaptchaResult(CaptchaState.CAPTCHA, captchaData).setImage(image);
	}
	
	private static final Pattern PATTERN_POST_REFERENCE = Pattern.compile(">>(\\d+)");
	private static final Pattern PATTERN_POST_ERROR = Pattern.compile("<p style=\"color:darkred;\">(.*?)</p>");
	
	@Override
	public SendPostResult onSendPost(SendPostData data) throws HttpException, ApiException, InvalidResponseException
	{
		AlterchanChanLocator locator = AlterchanChanLocator.get(this);
		String comment = StringUtils.replaceAll(data.comment, PATTERN_POST_REFERENCE,
				matcher -> ">>" + locator.convertToPresentNumber(matcher.group(1)));
		MultipartEntity entity = new MultipartEntity();
		entity.add("post_pid", locator.convertToPresentNumber(data.threadNumber));
		entity.add("post_name", data.name);
		entity.add("post_email", data.optionSage ? "sage" : data.email);
		entity.add("post_title", data.subject);
		entity.add("post_text", comment);
		if (data.threadNumber == null) entity.add("post_tags", "b");
		entity.add("to_thread", "on");
		if (data.attachments != null) data.attachments[0].addToEntity(entity, "post_file");
		String sessionCookie = null;
		if (data.captchaData != null)
		{
			sessionCookie = data.captchaData.get(CaptchaData.CHALLENGE);
			entity.add("post_captcha", data.captchaData.get(CaptchaData.INPUT));
		}
		
		Uri uri = locator.buildPath("post_submit.php");
		String responseText;
		try
		{
			new HttpRequest(uri, data).setPostMethod(entity).addCookie(getAuthorizationCookie(data.holder))
					.addCookie("PHPSESSID", sessionCookie).setRedirectHandler(POST_REDIRECT_HANDLER).execute();
			if (data.holder.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP)
			{
				uri = data.holder.getRedirectedUri();
				String threadNumber = locator.getThreadNumber(uri);
				return new SendPostResult(threadNumber, null);
			}
			responseText = data.holder.read().getString();
		}
		finally
		{
			data.holder.disconnect();
		}
		
		String errorMessage;
		Matcher matcher = PATTERN_POST_ERROR.matcher(responseText);
		if (matcher.find()) errorMessage = matcher.group(1);
		else if (!responseText.contains("<html")) errorMessage = responseText;
		else throw new InvalidResponseException();
		int errorType = 0;
		if (errorMessage.contains("invalid_captcha"))
		{
			errorType = ApiException.SEND_ERROR_CAPTCHA;
		}
		else if (errorMessage.contains("Посты без файла и сообщения"))
		{
			errorType = ApiException.SEND_ERROR_EMPTY_COMMENT;
		}
		else if (errorMessage.contains("doesn't exists"))
		{
			errorType = ApiException.SEND_ERROR_NO_THREAD;
		}
		if (errorType != 0) throw new ApiException(errorType);
		CommonUtils.writeLog("Alterchan send message", errorMessage);
		throw new ApiException(errorMessage);
	}
	
	@Override
	public SendDeletePostsResult onSendDeletePosts(SendDeletePostsData data) throws HttpException, ApiException,
			InvalidResponseException
	{
		CookieBuilder cookieBuilder = getAuthorizationCookie(data.holder);
		if (cookieBuilder == null) throw new ApiException(ApiException.DELETE_ERROR_NO_ACCESS);
		AlterchanChanLocator locator = AlterchanChanLocator.get(this);
		String postNumber = locator.convertToPresentNumber(data.postNumbers.get(0));
		Uri uri = locator.buildQuery("ajax_get_subpost.php", "id", postNumber);
		String responseText = new HttpRequest(uri, data).addCookie(cookieBuilder).read().getString();
		if (!responseText.contains("<div class=\"subpost_wrapper ownpost\">"))
		{
			throw new ApiException(ApiException.DELETE_ERROR_NO_ACCESS);
		}
		uri = locator.buildPath("blacklist_set.php");
		try
		{
			new HttpRequest(uri, data).setPostMethod(new UrlEncodedEntity("id", postNumber)).addCookie(cookieBuilder)
					.setRedirectHandler(POST_REDIRECT_HANDLER).execute();
			if (data.holder.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP) return null;
		}
		finally
		{
			data.holder.disconnect();
		}
		throw new InvalidResponseException();
	}
}