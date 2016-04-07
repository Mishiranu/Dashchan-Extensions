package com.mishiranu.dashchan.chan.tiretirech;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.net.Uri;

import chan.content.ApiException;
import chan.content.ChanLocator;
import chan.content.ChanPerformer;
import chan.content.InvalidResponseException;
import chan.http.HttpException;
import chan.http.HttpHolder;
import chan.http.HttpRequest;
import chan.http.HttpResponse;
import chan.http.MultipartEntity;
import chan.http.UrlEncodedEntity;
import chan.text.ParseException;
import chan.util.CommonUtils;
import chan.util.StringUtils;

public class TiretirechChanPerformer extends ChanPerformer
{
	@Override
	public ReadThreadsResult onReadThreads(ReadThreadsData data) throws HttpException, InvalidResponseException
	{
		TiretirechChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.createBoardUri(data.boardName, data.pageNumber);
		String responseText = new HttpRequest(uri, data.holder, data).setValidator(data.validator).read().getString();
		try
		{
			return new ReadThreadsResult(new TiretirechPostsParser(responseText, this, data.boardName)
			.convertThreads());
		}
		catch (ParseException e)
		{
			throw new InvalidResponseException(e);
		}
	}
	
	@Override
	public ReadPostsResult onReadPosts(ReadPostsData data) throws HttpException, InvalidResponseException
	{
		TiretirechChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.createThreadUri(data.boardName, data.threadNumber);
		String responseText = new HttpRequest(uri, data.holder, data).setValidator(data.validator).read().getString();
		try
		{
			return new ReadPostsResult(new TiretirechPostsParser(responseText, this, data.boardName).convertPosts());
		}
		catch (ParseException e)
		{
			throw new InvalidResponseException(e);
		}
	}
	
	@Override
	public ReadBoardsResult onReadBoards(ReadBoardsData data) throws HttpException, InvalidResponseException
	{
		TiretirechChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildPath("menu.html");
		String responseText = new HttpRequest(uri, data.holder, data).read().getString();
		try
		{
			return new ReadBoardsResult(new TiretirechBoardsParser(responseText).convert());
		}
		catch (ParseException e)
		{
			throw new InvalidResponseException(e);
		}
	}
	
	@Override
	public ReadPostsCountResult onReadPostsCount(ReadPostsCountData data) throws HttpException, InvalidResponseException
	{
		TiretirechChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.createThreadUri(data.boardName, data.threadNumber);
		String responseText = new HttpRequest(uri, data.holder, data).setValidator(data.validator).read().getString();
		int count = 0;
		int index = 0;
		while (index != -1)
		{
			count++;
			index = responseText.indexOf("<table class=\"post\">", index + 1);
		}
		return new ReadPostsCountResult(count);
	}
	
	private static class RedirectTracker implements HttpRequest.RedirectHandler
	{
		private final HttpRequest.RedirectHandler mRedirectHandler;
		
		public final ArrayList<Uri> redirects = new ArrayList<>();
		
		public RedirectTracker(HttpRequest.RedirectHandler redirectHandler)
		{
			mRedirectHandler = redirectHandler;
		}
		
		@Override
		public Action onRedirectReached(int responseCode, Uri requestedUri, Uri redirectedUri, HttpHolder holder)
				throws HttpException
		{
			redirects.add(redirectedUri);
			return mRedirectHandler.onRedirectReached(responseCode, requestedUri, redirectedUri, holder);
		}
	}
	
	@Override
	public ReadCaptchaResult onReadCaptcha(ReadCaptchaData data) throws HttpException, InvalidResponseException
	{
		if (data.mayShowLoadButton) return new ReadCaptchaResult(CaptchaState.NEED_LOAD, null);
		TiretirechChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildPath(data.boardName, "captcha.fpl");
		RedirectTracker tracker = new RedirectTracker(HttpRequest.RedirectHandler.BROWSER);
		HttpResponse response = new HttpRequest(uri, data.holder, data).setRedirectHandler(tracker).read();
		if (tracker.redirects.size() > 0)
		{
			Uri lastUri = tracker.redirects.get(tracker.redirects.size() - 1);
			if (lastUri.getPath().startsWith("/lib/nocap/")) return new ReadCaptchaResult(CaptchaState.SKIP, null);
		}
		Bitmap image = response.getBitmap();
		if (image == null) throw new InvalidResponseException();
		return new ReadCaptchaResult(CaptchaState.CAPTCHA, new CaptchaData()).setImage(image);
	}
	
	@Override
	public SendPostResult onSendPost(SendPostData data) throws HttpException, ApiException, InvalidResponseException
	{
		MultipartEntity entity = new MultipartEntity();
		entity.add("task", "post");
		entity.add("parent", data.threadNumber);
		entity.add("name", data.name);
		entity.add("email", data.optionSage ? "sage" : data.email);
		entity.add("subject", data.subject);
		entity.add("comment", StringUtils.emptyIfNull(data.comment));
		entity.add("password", data.password);
		if (data.captchaData != null) entity.add("captcha", data.captchaData.get(CaptchaData.INPUT));
		if (data.attachments != null)
		{
			SendPostData.Attachment attachment = data.attachments[0];
			attachment.addToEntity(entity, "file");
		}
		
		TiretirechChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildPath(data.boardName, "post.fpl");
		JSONObject jsonObject = new HttpRequest(uri, data.holder, data).setPostMethod(entity)
				.setRedirectHandler(HttpRequest.RedirectHandler.STRICT).read().getJsonObject();
		if (jsonObject == null) throw new InvalidResponseException();
		String postNumber = CommonUtils.optJsonString(jsonObject, "post");
		if (postNumber != null) return new SendPostResult(data.threadNumber, postNumber);
		String threadPath = CommonUtils.optJsonString(jsonObject, "redirect");
		if (threadPath != null)
		{
			uri = locator.buildPath(threadPath);
			String threadNumber = locator.getThreadNumber(uri);
			if (threadNumber == null) throw new InvalidResponseException();
			return new SendPostResult(threadNumber, null);
		}
		
		String message;
		try
		{
			jsonObject = jsonObject.getJSONObject("error");
			message = CommonUtils.getJsonString(jsonObject, "text");
		}
		catch (JSONException e)
		{
			throw new InvalidResponseException(e);
		}
		
		if (message != null)
		{
			int errorType = 0;
			int flags = 0;
			if (message.contains("Нельзя создавать пустые сообщения"))
			{
				errorType = ApiException.SEND_ERROR_EMPTY_COMMENT;
				flags = ApiException.FLAG_KEEP_CAPTCHA;
			}
			else if (message.contains("Загрузите файл для создания треда"))
			{
				errorType = ApiException.SEND_ERROR_EMPTY_FILE;
				flags = ApiException.FLAG_KEEP_CAPTCHA;
			}
			else if (message.contains("Такого треда не существует"))
			{
				errorType = ApiException.SEND_ERROR_NO_THREAD;
				flags = ApiException.FLAG_KEEP_CAPTCHA;
			}
			else if (message.contains("Неверно введена капча"))
			{
				errorType = ApiException.SEND_ERROR_CAPTCHA;
			}
			if (errorType != 0) throw new ApiException(errorType, flags);
		}
		CommonUtils.writeLog("Tiretirech send message", message);
		throw new ApiException(message);
	}
	
	private static final Pattern PATTERN_DELETE_HEADER = Pattern.compile("<h1 style=\"text-align: center; " +
			"margin: 100px;\">(.*?)(?=<br/>|\n)");
	private static final Pattern PATTERN_DELETE_MESSAGE = Pattern.compile("<br/>(\\d+) - (.*?)(?=<br/>|\n)");
	
	@Override
	public SendDeletePostsResult onSendDeletePosts(SendDeletePostsData data) throws HttpException, ApiException,
			InvalidResponseException
	{
		TiretirechChanLocator locator = ChanLocator.get(this);
		UrlEncodedEntity entity = new UrlEncodedEntity("password", data.password);
		for (String postNumber : data.postNumbers) entity.add("delete", postNumber);
		Uri uri = locator.buildPath(data.boardName, "delete.fpl");
		String responseText = new HttpRequest(uri, data.holder, data).setPostMethod(entity)
				.setRedirectHandler(HttpRequest.RedirectHandler.STRICT).setSuccessOnly(false).read().getString();
		if (data.holder.getResponseCode() != HttpURLConnection.HTTP_BAD_METHOD) data.holder.checkResponseCode();
		if (responseText != null)
		{
			Matcher matcher = PATTERN_DELETE_HEADER.matcher(responseText);
			if (matcher.find())
			{
				String message = matcher.group(1);
				if (message.contains("Некоторые посты не были удалены"))
				{
					HashSet<String> postNumbers = new HashSet<>(data.postNumbers);
					String firstReason = null;
					matcher = PATTERN_DELETE_MESSAGE.matcher(responseText);
					while (matcher.find())
					{
						String postNumber = matcher.group(1);
						postNumbers.remove(postNumber);
						if (firstReason == null) firstReason = matcher.group(2);
					}
					if (postNumbers.isEmpty())
					{
						message = firstReason;
					}
					else return null; // At least 1 post was deleted
				}
				int errorType = 0;
				if (message.contains("Вы не ввели пароль") || message.contains("Неверный пароль"))
				{
					errorType = ApiException.DELETE_ERROR_PASSWORD;
				}
				if (errorType != 0) throw new ApiException(errorType);
				CommonUtils.writeLog("Tiretirech delete message", message);
				throw new ApiException(message);
			}
			else return null;
		}
		throw new InvalidResponseException();
	}
}