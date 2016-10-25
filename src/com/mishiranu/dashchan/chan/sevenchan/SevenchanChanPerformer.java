package com.mishiranu.dashchan.chan.sevenchan;

import java.net.HttpURLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.net.Uri;

import chan.content.ApiException;
import chan.content.ChanConfiguration;
import chan.content.ChanPerformer;
import chan.content.ChanLocator;
import chan.content.InvalidResponseException;
import chan.content.ThreadRedirectException;
import chan.http.HttpException;
import chan.http.HttpRequest;
import chan.http.MultipartEntity;
import chan.http.UrlEncodedEntity;
import chan.text.ParseException;
import chan.util.CommonUtils;
import chan.util.StringUtils;

public class SevenchanChanPerformer extends ChanPerformer
{
	private static final String RECAPTCHA_KEY = "6LdVg8YSAAAAAOhqx0eFT1Pi49fOavnYgy7e-lTO";
	
	@Override
	public ReadThreadsResult onReadThreads(ReadThreadsData data) throws HttpException, InvalidResponseException
	{
		SevenchanChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.createBoardUri(data.boardName, data.pageNumber);
		String responseText = new HttpRequest(uri, data.holder, data).setValidator(data.validator).read().getString();
		try
		{
			return new ReadThreadsResult(new SevenchanPostsParser(responseText, this, data.boardName).convertThreads());
		}
		catch (ParseException e)
		{
			throw new InvalidResponseException(e);
		}
	}
	
	@Override
	public ReadPostsResult onReadPosts(ReadPostsData data) throws HttpException, ThreadRedirectException,
			InvalidResponseException
	{
		SevenchanChanLocator locator = ChanLocator.get(this);
		String lastPostNumber = data.partialThreadLoading ? data.lastPostNumber : null;
		if (lastPostNumber != null)
		{
			Uri uri = locator.buildQuery("ajax.php", "act", "spy", "board", data.boardName,
					"thread", data.threadNumber, "pastid", lastPostNumber);
			String responseText = new HttpRequest(uri, data.holder, data).setValidator(data.validator)
					.read().getString();
			if (StringUtils.isEmpty(responseText)) return null;
			try
			{
				return new ReadPostsResult(new SevenchanPostsParser(responseText, this, data.boardName,
						data.threadNumber).convertPosts());
			}
			catch (ParseException e)
			{
				throw new InvalidResponseException(e);
			}
		}
		else
		{
			Uri uri = locator.createThreadUri(data.boardName, data.threadNumber);
			String responseText = new HttpRequest(uri, data.holder, data).setValidator(data.validator)
					.read().getString();
			try
			{
				return new ReadPostsResult(new SevenchanPostsParser(responseText, this, data.boardName).convertPosts());
			}
			catch (ParseException e)
			{
				throw new InvalidResponseException(e);
			}
		}
	}
	
	@Override
	public ReadBoardsResult onReadBoards(ReadBoardsData data) throws HttpException, InvalidResponseException
	{
		SevenchanChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildPath();
		String responseText = new HttpRequest(uri, data.holder, data).read().getString();
		try
		{
			return new ReadBoardsResult(new SevenchanBoardsParser(responseText).convert());
		}
		catch (ParseException e)
		{
			throw new InvalidResponseException(e);
		}
	}
	
	@Override
	public ReadPostsCountResult onReadPostsCount(ReadPostsCountData data) throws HttpException, InvalidResponseException
	{
		SevenchanChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.createThreadUri(data.boardName, data.threadNumber);
		String responseText = new HttpRequest(uri, data.holder, data).setValidator(data.validator).read().getString();
		int count = 0;
		int index = 0;
		while (index != -1)
		{
			count++;
			index = responseText.indexOf("<div class=\"reply\"", index + 1);
		}
		return new ReadPostsCountResult(count);
	}
	
	@Override
	public ReadCaptchaResult onReadCaptcha(ReadCaptchaData data) throws HttpException, InvalidResponseException
	{
		ReadCaptchaResult result;
		if (data.threadNumber == null)
		{
			CaptchaData captchaData = new CaptchaData();
			captchaData.put(CaptchaData.API_KEY, RECAPTCHA_KEY);
			result = new ReadCaptchaResult(CaptchaState.CAPTCHA, captchaData);
		}
		else result = new ReadCaptchaResult(CaptchaState.SKIP, null);
		return result.setValidity(ChanConfiguration.Captcha.Validity.IN_THREAD);
	}
	
	private static final Pattern PATTERN_POST_ERROR = Pattern.compile("(?s)<h2.*?>(.*?)</h2>");
	
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
		entity.add("embed", ""); // Otherwise there will be an "Invalid embed ID" error
		if (data.attachments != null)
		{
			for (SendPostData.Attachment attachment : data.attachments)
			{
				attachment.addToEntity(entity, "imagefile[]");
			}
		}
		else entity.add("nofile", "on");
		if (data.captchaData != null)
		{
			entity.add("recaptcha_challenge_field", data.captchaData.get(CaptchaData.CHALLENGE));
			entity.add("recaptcha_response_field", data.captchaData.get(CaptchaData.INPUT));
		}
		
		SevenchanChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildPath("board.php");
		String responseText;
		try
		{
			new HttpRequest(uri, data.holder, data).setPostMethod(entity)
					.setRedirectHandler(HttpRequest.RedirectHandler.NONE).execute();
			if (data.holder.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP)
			{
				// Success
				return null;
			}
			responseText = data.holder.read().getString();
		}
		finally
		{
			data.holder.disconnect();
		}
		
		Matcher matcher = PATTERN_POST_ERROR.matcher(responseText);
		if (matcher.find())
		{
			String errorMessage = matcher.group(1).trim();
			int errorType = 0;
			if (errorMessage.contains("Incorrect captcha entered"))
			{
				errorType = ApiException.SEND_ERROR_CAPTCHA;
			}
			else if (errorMessage.contains("An image, or message, is required for a reply"))
			{
				errorType = ApiException.SEND_ERROR_EMPTY_COMMENT;
			}
			else if (errorMessage.contains("A file is required for a new thread"))
			{
				errorType = ApiException.SEND_ERROR_EMPTY_FILE;
			}
			else if (errorMessage.contains("Invalid thread ID"))
			{
				errorType = ApiException.SEND_ERROR_NO_THREAD;
			}
			else if (errorMessage.contains("Sorry, your message is too long"))
			{
				errorType = ApiException.SEND_ERROR_FIELD_TOO_LONG;
			}
			else if (errorMessage.contains("Duplicate file entry detected"))
			{
				errorType = ApiException.SEND_ERROR_FILE_EXISTS;
			}
			if (errorType != 0) throw new ApiException(errorType);
			CommonUtils.writeLog("Sevenchan send message", errorMessage);
			throw new ApiException(errorMessage);
		}
		throw new InvalidResponseException();
	}
	
	@Override
	public SendDeletePostsResult onSendDeletePosts(SendDeletePostsData data) throws HttpException, ApiException,
			InvalidResponseException
	{
		SevenchanChanLocator locator = ChanLocator.get(this);
		UrlEncodedEntity entity = new UrlEncodedEntity("board", data.boardName, "deletepost", "1",
				"postpassword", data.password);
		for (String postNumber : data.postNumbers) entity.add("post[]", postNumber);
		if (data.optionFilesOnly) entity.add("fileonly", "on");
		Uri uri = locator.buildPath("board.php");
		String responseText = new HttpRequest(uri, data.holder, data).setPostMethod(entity).read().getString();
		if (responseText != null)
		{
			if (responseText.contains("Post successfully"))
			{
				// Response has message for any post
				// Ignore them, if at least 1 of them was deleted
				return null;
			}
			else if (responseText.contains("Incorrect password"))
			{
				throw new ApiException(ApiException.DELETE_ERROR_PASSWORD);
			}
			CommonUtils.writeLog("Sevenchan delete message", responseText);
		}
		throw new InvalidResponseException();
	}
	
	@Override
	public SendReportPostsResult onSendReportPosts(SendReportPostsData data) throws HttpException, ApiException,
			InvalidResponseException
	{
		SevenchanChanLocator locator = ChanLocator.get(this);
		UrlEncodedEntity entity = new UrlEncodedEntity("board", data.boardName, "reportpost", "1");
		for (String postNumber : data.postNumbers) entity.add("post[]", postNumber);
		Uri uri = locator.buildPath("board.php");
		String responseText = new HttpRequest(uri, data.holder, data).setPostMethod(entity).read().getString();
		if (responseText != null)
		{
			if (responseText.contains("Post successfully reported") ||
					responseText.contains("That post is already in the report list"))
			{
				// Response has message for any post
				// Ignore them, if at least 1 of them was reported
				return null;
			}
			CommonUtils.writeLog("Sevenchan report message", responseText);
		}
		throw new InvalidResponseException();
	}
}