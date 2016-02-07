package com.mishiranu.dashchan.chan.shanachan;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.net.Uri;

import chan.content.ApiException;
import chan.content.ChanLocator;
import chan.content.ChanPerformer;
import chan.content.InvalidResponseException;
import chan.content.model.Posts;
import chan.http.HttpException;
import chan.http.HttpRequest;
import chan.http.MultipartEntity;
import chan.http.UrlEncodedEntity;
import chan.text.ParseException;
import chan.util.CommonUtils;

public class ShanachanChanPerformer extends ChanPerformer
{
	@Override
	public ReadThreadsResult onReadThreads(ReadThreadsData data) throws HttpException, InvalidResponseException
	{
		ShanachanChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.createBoardUri(data.boardName, data.pageNumber);
		String responseText = new HttpRequest(uri, data.holder, data).setValidator(data.validator)
				.read().getString();
		try
		{
			ArrayList<Posts> threads = null;
			if ("f".equals(data.boardName))
			{
				threads = new ShanachanFlashThreadsParser(responseText, this, data.boardName).convertThreads();
			}
			else
			{
				threads = new ShanachanPostsParser(responseText, this, data.boardName).convertThreads();
			}
			return new ReadThreadsResult(threads);
		}
		catch (ParseException e)
		{
			throw new InvalidResponseException(e);
		}
	}
	
	@Override
	public ReadPostsResult onReadPosts(ReadPostsData data) throws HttpException, InvalidResponseException
	{
		ShanachanChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.createThreadUri(data.boardName, data.threadNumber);
		String responseText = new HttpRequest(uri, data.holder, data).setValidator(data.validator).read().getString();
		try
		{
			return new ReadPostsResult(new ShanachanPostsParser(responseText, this, data.boardName).convertPosts());
		}
		catch (ParseException e)
		{
			throw new InvalidResponseException(e);
		}
	}
	
	@Override
	public ReadBoardsResult onReadBoards(ReadBoardsData data) throws HttpException, InvalidResponseException
	{
		ShanachanChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildPath();
		String responseText = new HttpRequest(uri, data.holder, data).read().getString();
		try
		{
			return new ReadBoardsResult(new ShanachanBoardsParser(responseText).convert());
		}
		catch (ParseException e)
		{
			throw new InvalidResponseException(e);
		}
	}
	
	@Override
	public ReadPostsCountResult onReadPostsCount(ReadPostsCountData data) throws HttpException, InvalidResponseException
	{
		ShanachanChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.createThreadUri(data.boardName, data.threadNumber);
		String responseText = new HttpRequest(uri, data.holder, data).setValidator(data.validator).read().getString();
		int count = 0;
		int index = 0;
		while (index != -1)
		{
			count++;
			index = responseText.indexOf("<td class=\"reply\"", index + 1);
		}
		return new ReadPostsCountResult(count);
	}
	
	private static final Pattern PATTERN_POST_ERROR = Pattern.compile("<h1 style=\"text-align: center\">(.*?)<br />");
	
	@Override
	public SendPostResult onSendPost(SendPostData data) throws HttpException, ApiException, InvalidResponseException
	{
		MultipartEntity entity = new MultipartEntity();
		entity.add("task", "post");
		entity.add("parent", data.threadNumber);
		entity.add("field1", data.name);
		entity.add("field3", data.subject);
		entity.add("field4", data.comment);
		entity.add("password", data.password);
		if (data.optionSage) entity.add("sage", "on");
		if (data.attachments != null)
		{
			SendPostData.Attachment attachment = data.attachments[0];
			attachment.addToEntity(entity, "file");
			entity.add("filetag", attachment.rating);
		}
		else entity.add("nofile", "on");

		ShanachanChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildPath(data.boardName, "wakaba");
		String responseText;
		try
		{
			new HttpRequest(uri, data.holder, data).setPostMethod(entity)
					.setRedirectHandler(HttpRequest.RedirectHandler.NONE).execute();
			if (data.holder.getResponseCode() == HttpURLConnection.HTTP_SEE_OTHER)
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
			String errorMessage = matcher.group(1);
			if (errorMessage != null)
			{
				int errorType = 0;
				if (errorMessage.contains("Wrong verification code entered") ||
						errorMessage.contains("No verification code on record"))
				{
					errorType = ApiException.SEND_ERROR_CAPTCHA;
				}
				else if (errorMessage.contains("No comment entered"))
				{
					errorType = ApiException.SEND_ERROR_EMPTY_COMMENT;
				}
				else if (errorMessage.contains("No file selected"))
				{
					errorType = ApiException.SEND_ERROR_EMPTY_FILE;
				}
				else if (errorMessage.contains("This image is too large"))
				{
					errorType = ApiException.SEND_ERROR_FILE_TOO_BIG;
				}
				else if (errorMessage.contains("Too many characters"))
				{
					errorType = ApiException.SEND_ERROR_FIELD_TOO_LONG;
				}
				else if (errorMessage.contains("Thread does not exist"))
				{
					errorType = ApiException.SEND_ERROR_NO_THREAD;
				}
				else if (errorMessage.contains("String refused") || errorMessage.contains("Flood detected, "))
				{
					errorType = ApiException.SEND_ERROR_SPAM_LIST;
				}
				else if (errorMessage.contains("Host is banned"))
				{
					errorType = ApiException.SEND_ERROR_BANNED;
				}
				else if (errorMessage.contains("Flood detected"))
				{
					errorType = ApiException.SEND_ERROR_TOO_FAST;
				}
				if (errorType != 0) throw new ApiException(errorType);
			}
			CommonUtils.writeLog("Shanachan send message", errorMessage);
			throw new ApiException(errorMessage);
		}
		throw new InvalidResponseException();
	}
	
	@Override
	public SendDeletePostsResult onSendDeletePosts(SendDeletePostsData data) throws HttpException, ApiException,
			InvalidResponseException
	{
		ShanachanChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildPath(data.boardName, "wakaba");
		UrlEncodedEntity entity = new UrlEncodedEntity("task", "Delete", "password", data.password);
		for (String postNumber : data.postNumbers) entity.add("delete", postNumber);
		if (data.optionFilesOnly) entity.add("fileonly", "on");
		String responseText;
		try
		{
			new HttpRequest(uri, data.holder, data).setPostMethod(entity)
					.setRedirectHandler(HttpRequest.RedirectHandler.NONE).execute();
			if (data.holder.getResponseCode() == HttpURLConnection.HTTP_SEE_OTHER) return null;
			responseText = data.holder.read().getString();
		}
		finally
		{
			data.holder.disconnect();
		}
		Matcher matcher = PATTERN_POST_ERROR.matcher(responseText);
		if (matcher.find())
		{
			String errorMessage = matcher.group(1);
			if (errorMessage != null)
			{
				int errorType = 0;
				if (errorMessage.contains("Incorrect password for deletion"))
				{
					errorType = ApiException.DELETE_ERROR_PASSWORD;
				}
				if (errorType != 0) throw new ApiException(errorType);
			}
			CommonUtils.writeLog("Shanachan delete message", errorMessage);
			throw new ApiException(errorMessage);
		}
		throw new InvalidResponseException();
	}
	
	@Override
	public SendReportPostsResult onSendReportPosts(SendReportPostsData data) throws HttpException, ApiException,
			InvalidResponseException
	{
		ShanachanChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildPath(data.boardName, "wakaba");
		UrlEncodedEntity entity = new UrlEncodedEntity("task", "Report");
		for (String postNumber : data.postNumbers) entity.add("delete", postNumber);
		String responseText;
		try
		{
			new HttpRequest(uri, data.holder, data).setPostMethod(entity)
					.setRedirectHandler(HttpRequest.RedirectHandler.NONE).execute();
			if (data.holder.getResponseCode() == HttpURLConnection.HTTP_SEE_OTHER) return null;
			responseText = data.holder.read().getString();
		}
		finally
		{
			data.holder.disconnect();
		}
		Matcher matcher = PATTERN_POST_ERROR.matcher(responseText);
		if (matcher.find())
		{
			String errorMessage = matcher.group(1);
			CommonUtils.writeLog("Shanachan report message", errorMessage);
			throw new ApiException(errorMessage);
		}
		throw new InvalidResponseException();
	}
}