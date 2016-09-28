package com.mishiranu.dashchan.chan.ponyach;

import java.net.HttpURLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.net.Uri;

import chan.content.ApiException;
import chan.content.ChanConfiguration;
import chan.content.ChanLocator;
import chan.content.ChanPerformer;
import chan.content.InvalidResponseException;
import chan.http.CookieBuilder;
import chan.http.HttpException;
import chan.http.HttpRequest;
import chan.http.MultipartEntity;
import chan.http.UrlEncodedEntity;
import chan.text.ParseException;
import chan.util.CommonUtils;
import chan.util.StringUtils;

public class PonyachChanPerformer extends ChanPerformer
{
	private static final String COOKIE_SESSION = "PHPSESSID";
	private static final CookieBuilder SPECIAL_COOKIES = new CookieBuilder().append("show_spoiler_9", "true")
			.append("show_spoiler_10", "true").append("show_spoiler_11", "true").append("r34", "1").append("rf", "1");

	@Override
	public ReadThreadsResult onReadThreads(ReadThreadsData data) throws HttpException, InvalidResponseException
	{
		PonyachChanConfiguration configuration = ChanConfiguration.get(this);
		PonyachChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.createBoardUri(data.boardName, data.pageNumber);
		String responseText = new HttpRequest(uri, data.holder, data).setValidator(data.validator)
				.addCookie(SPECIAL_COOKIES).addCookie(COOKIE_SESSION, configuration.getSession()).read().getString();
		try
		{
			return new ReadThreadsResult(new PonyachPostsParser(responseText, this, data.boardName).convertThreads());
		}
		catch (ParseException e)
		{
			throw new InvalidResponseException(e);
		}
	}

	@Override
	public ReadPostsResult onReadPosts(ReadPostsData data) throws HttpException, InvalidResponseException
	{
		PonyachChanConfiguration configuration = ChanConfiguration.get(this);
		PonyachChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.createThreadUri(data.boardName, data.threadNumber);
		String responseText = new HttpRequest(uri, data.holder, data).setValidator(data.validator)
				.addCookie(SPECIAL_COOKIES).addCookie(COOKIE_SESSION, configuration.getSession()).read().getString();
		try
		{
			return new ReadPostsResult(new PonyachPostsParser(responseText, this, data.boardName).convertPosts());
		}
		catch (ParseException e)
		{
			throw new InvalidResponseException(e);
		}
	}

	@Override
	public ReadBoardsResult onReadBoards(ReadBoardsData data) throws HttpException, InvalidResponseException
	{
		PonyachChanConfiguration configuration = ChanConfiguration.get(this);
		PonyachChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildPath("b", "");
		String responseText = new HttpRequest(uri, data.holder, data).addCookie(SPECIAL_COOKIES)
				.addCookie(COOKIE_SESSION, configuration.getSession()).read().getString();
		try
		{
			return new ReadBoardsResult(new PonyachBoardsParser(responseText).convert());
		}
		catch (ParseException e)
		{
			throw new InvalidResponseException(e);
		}
	}

	@Override
	public ReadPostsCountResult onReadPostsCount(ReadPostsCountData data) throws HttpException, InvalidResponseException
	{
		PonyachChanConfiguration configuration = ChanConfiguration.get(this);
		PonyachChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.createThreadUri(data.boardName, data.threadNumber);
		String responseText = new HttpRequest(uri, data.holder, data).setValidator(data.validator)
				.addCookie(SPECIAL_COOKIES).addCookie(COOKIE_SESSION, configuration.getSession()).read().getString();
		int count = 0;
		int index = 0;
		while (index != -1)
		{
			count++;
			index = responseText.indexOf("<td class=\"reply\"", index + 1);
		}
		return new ReadPostsCountResult(count);
	}

	private static final Pattern PATTERN_CAPTCHA_KEY = Pattern.compile("'sitekey' *: *'(.*?)'");

	@Override
	public ReadCaptchaResult onReadCaptcha(ReadCaptchaData data) throws HttpException, InvalidResponseException
	{
		PonyachChanConfiguration configuration = ChanConfiguration.get(this);
		PonyachChanLocator locator = ChanLocator.get(this);
		String session = configuration.getSession();
		Uri uri = locator.buildQuery("recaptchav2.php", "c", "isnd");
		String responseText = new HttpRequest(uri, data.holder, data).addCookie(COOKIE_SESSION, session)
				.read().getString();
		String newSession = data.holder.getCookieValue(COOKIE_SESSION);
		if (newSession != null && !newSession.equals(session))
		{
			session = newSession;
			configuration.storeSession(session);
		}
		if ("0".equals(responseText)) return new ReadCaptchaResult(CaptchaState.SKIP, null);
		if (!"1".equals(responseText)) throw new InvalidResponseException();
		uri = locator.buildPath("lib", "javascript", "recaptcha-logic.js");
		responseText = new HttpRequest(uri, data.holder, data).addCookie(COOKIE_SESSION, session).read().getString();
		Matcher matcher = PATTERN_CAPTCHA_KEY.matcher(responseText);
		if (!matcher.find()) throw new InvalidResponseException();
		CaptchaData captchaData = new CaptchaData();
		captchaData.put(CaptchaData.API_KEY, matcher.group(1));
		return new ReadCaptchaResult(CaptchaState.CAPTCHA, captchaData);
	}

	private static final Pattern PATTERN_POST_ERROR = Pattern.compile("(?s)<h2.*?>(?:\r\n)?(.*?)(?:\r\n)?</h2>");

	@Override
	public SendPostResult onSendPost(SendPostData data) throws HttpException, ApiException, InvalidResponseException
	{
		MultipartEntity entity = new MultipartEntity();
		entity.add("board", data.boardName);
		entity.add("replythread", data.threadNumber == null ? "0" : data.threadNumber);
		entity.add("name", data.name);
		entity.add("em", data.optionSage ? "sage" : data.email);
		entity.add("subject", data.subject);
		entity.add("message", StringUtils.emptyIfNull(data.comment));
		entity.add("postpassword", data.password);
		if (data.attachments != null)
		{
			for (int i = 0; i < data.attachments.length; i++)
			{
				SendPostData.Attachment attachment = data.attachments[i];
				attachment.addToEntity(entity, "upload[]");
				entity.add("upload-rating-" + (i + 1), attachment.rating);
			}
		}
		else entity.add("nofile", "on");
		if (data.captchaData != null) entity.add("g-recaptcha-response", data.captchaData.get(CaptchaData.INPUT));

		PonyachChanConfiguration configuration = ChanConfiguration.get(this);
		String session = configuration.getSession();
		PonyachChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildPath("board.php");
		String responseText;
		try
		{
			new HttpRequest(uri, data.holder, data).setPostMethod(entity).addCookie(SPECIAL_COOKIES)
					.addCookie(COOKIE_SESSION, session).setRedirectHandler(HttpRequest.RedirectHandler.NONE).execute();
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
		String newSession = data.holder.getCookieValue(COOKIE_SESSION);
		if (newSession != null && !newSession.equals(session))
		{
			session = newSession;
			configuration.storeSession(session);
		}

		Matcher matcher = PATTERN_POST_ERROR.matcher(responseText);
		if (matcher.find())
		{
			String errorMessage = matcher.group(1);
			if (errorMessage != null)
			{
				int errorType = 0;
				if (errorMessage.contains("Я не принимаю пустые сообщения"))
				{
					errorType = ApiException.SEND_ERROR_EMPTY_COMMENT;
				}
				else if (errorMessage.contains("Я не могу создать тред без картинки"))
				{
					errorType = ApiException.SEND_ERROR_EMPTY_FILE;
				}
				else if (errorMessage.contains("Invalid thread ID"))
				{
					errorType = ApiException.SEND_ERROR_NO_THREAD;
				}
				else if (errorMessage.contains("Ты отправляешь сообщения слишком быстро"))
				{
					errorType = ApiException.SEND_ERROR_TOO_FAST;
				}
				else if (responseText.contains("your message is too long"))
				{
					// Check responseText, not errorMessage
					errorType = ApiException.SEND_ERROR_FIELD_TOO_LONG;
				}
				if (errorType != 0) throw new ApiException(errorType);
			}
			CommonUtils.writeLog("Ponyach send message", errorMessage);
			throw new ApiException(errorMessage);
		}
		throw new InvalidResponseException();
	}

	@Override
	public SendDeletePostsResult onSendDeletePosts(SendDeletePostsData data) throws HttpException, ApiException,
			InvalidResponseException
	{
		PonyachChanLocator locator = ChanLocator.get(this);
		UrlEncodedEntity entity = new UrlEncodedEntity("board", data.boardName, "deletepost", "1",
				"postpassword", data.password);
		for (String postNumber : data.postNumbers) entity.add("post[]", postNumber);
		if (data.optionFilesOnly) entity.add("fileonly", "on");
		Uri uri = locator.buildPath("board.php");
		String responseText = new HttpRequest(uri, data.holder, data).setPostMethod(entity).read().getString();
		if (responseText != null)
		{
			if (responseText.contains("Ты отправляешь сообщения слишком быстро"))
			{
				throw new ApiException(ApiException.DELETE_ERROR_TOO_OFTEN);
			}
			if (responseText.contains("Сообщение удалено") || responseText.contains("Изображение из сообщения удалено"))
			{
				// Response has message for any post
				// Ignore them, if at least 1 of them was deleted
				return null;
			}
			if (responseText.contains("Неправильный пароль"))
			{
				throw new ApiException(ApiException.DELETE_ERROR_PASSWORD);
			}
			CommonUtils.writeLog("Ponyach delete message", responseText);
			throw new ApiException(responseText);
		}
		throw new InvalidResponseException();
	}
}