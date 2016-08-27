package com.mishiranu.dashchan.chan.valkyria;

import java.net.HttpURLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.net.Uri;

import chan.content.ApiException;
import chan.content.ChanPerformer;
import chan.content.InvalidResponseException;
import chan.http.HttpException;
import chan.http.HttpRequest;
import chan.http.MultipartEntity;
import chan.http.UrlEncodedEntity;
import chan.text.ParseException;
import chan.util.CommonUtils;
import chan.util.StringUtils;

public class ValkyriaChanPerformer extends ChanPerformer
{
	@Override
	public ReadThreadsResult onReadThreads(ReadThreadsData data) throws HttpException, InvalidResponseException
	{
		ValkyriaChanLocator locator = ValkyriaChanLocator.get(this);
		Uri uri = locator.createBoardUri(data.boardName, data.pageNumber);
		String responseText = new HttpRequest(uri, data.holder, data).setValidator(data.validator)
				.read().getString();
		try
		{
			return new ReadThreadsResult(new ValkyriaPostsParser(responseText, this, data.boardName).convertThreads());
		}
		catch (ParseException e)
		{
			throw new InvalidResponseException(e);
		}
	}
	
	@Override
	public ReadPostsResult onReadPosts(ReadPostsData data) throws HttpException, InvalidResponseException
	{
		ValkyriaChanLocator locator = ValkyriaChanLocator.get(this);
		Uri uri = locator.createThreadUri(data.boardName, data.threadNumber);
		String responseText = new HttpRequest(uri, data.holder, data).setValidator(data.validator).read().getString();
		checkThreadNotFound(responseText, data.threadNumber);
		try
		{
			return new ReadPostsResult(new ValkyriaPostsParser(responseText, this, data.boardName).convertPosts());
		}
		catch (ParseException e)
		{
			throw new InvalidResponseException(e);
		}
	}
	
	@Override
	public ReadBoardsResult onReadBoards(ReadBoardsData data) throws HttpException, InvalidResponseException
	{
		ValkyriaChanLocator locator = ValkyriaChanLocator.get(this);
		Uri uri = locator.buildPath("");
		String responseText = new HttpRequest(uri, data.holder, data).read().getString();
		try
		{
			return new ReadBoardsResult(new ValkyriaBoardsParser(responseText).convert());
		}
		catch (ParseException e)
		{
			throw new InvalidResponseException(e);
		}
	}
	
	@Override
	public ReadPostsCountResult onReadPostsCount(ReadPostsCountData data) throws HttpException, InvalidResponseException
	{
		ValkyriaChanLocator locator = ValkyriaChanLocator.get(this);
		Uri uri = locator.createThreadUri(data.boardName, data.threadNumber);
		String responseText = new HttpRequest(uri, data.holder, data).setValidator(data.validator).read().getString();
		checkThreadNotFound(responseText, data.threadNumber);
		if (!responseText.contains("<form id=\"DeleteForm\"")) throw new InvalidResponseException();
		int count = 0;
		int index = 0;
		while (index != -1)
		{
			count++;
			index = responseText.indexOf("<table class=\"ReplyBoxTable", index + 1);
		}
		return new ReadPostsCountResult(count);
	}
	
	private void checkThreadNotFound(String responseText, String threadNumber) throws HttpException,
			InvalidResponseException
	{
		if (responseText.contains("<div class=\"ErrorMessage\">"))
		{
			if (responseText.contains("The topic (id " + threadNumber + ") could not be found"))
			{
				throw HttpException.createNotFoundException();
			}
			throw new InvalidResponseException();
		}
	}
	
	private static final Pattern PATTERN_POST_ERROR = Pattern.compile("(?s)<div class=\"ErrorMessage\">(.*?)<br/>");
	
	@Override
	public SendPostResult onSendPost(SendPostData data) throws HttpException, ApiException, InvalidResponseException
	{
		MultipartEntity entity = new MultipartEntity();
		entity.add("topicid", data.threadNumber);
		entity.add("st", "");
		entity.add("name", data.name);
		entity.add("email", data.optionSage ? "sage" : data.email);
		entity.add("subject", data.subject);
		entity.add("comment", data.comment);
		entity.add("password", data.password);
		if (data.attachments != null)
		{
			data.attachments[0].addToEntity(entity, "file1");
			if (data.attachments[0].optionSpoiler) entity.add("isSpoiler", "on");
		}

		ValkyriaChanLocator locator = ValkyriaChanLocator.get(this);
		Uri uri = locator.buildPath(data.boardName, "post");
		String responseText;
		try
		{
			new HttpRequest(uri, data.holder, data).setPostMethod(entity)
					.setRedirectHandler(HttpRequest.RedirectHandler.NONE).execute();
			if (data.holder.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP) return null;
			responseText = data.holder.read().getString();
		}
		finally
		{
			data.holder.disconnect();
		}
		
		Matcher matcher = PATTERN_POST_ERROR.matcher(responseText);
		if (matcher.find())
		{
			String errorMessage = StringUtils.clearHtml(matcher.group(1));
			int errorType = 0;
			if (errorMessage.contains("No comment was given"))
			{
				errorType = ApiException.SEND_ERROR_EMPTY_COMMENT;
			}
			else if (errorMessage.contains("No file given"))
			{
				errorType = ApiException.SEND_ERROR_EMPTY_FILE;
			}
			else if (errorMessage.contains("Topic no longer exists"))
			{
				errorType = ApiException.SEND_ERROR_NO_THREAD;
			}
			else if (errorMessage.contains("is the same as the previous file"))
			{
				errorType = ApiException.SEND_ERROR_FILE_EXISTS;
			}
			if (errorType != 0) throw new ApiException(errorType);
			CommonUtils.writeLog("Valkyria send message", errorMessage);
			throw new ApiException(errorMessage);
		}
		throw new InvalidResponseException();
	}
	
	@Override
	public SendDeletePostsResult onSendDeletePosts(SendDeletePostsData data) throws HttpException, ApiException,
			InvalidResponseException
	{
		ValkyriaChanLocator locator = ValkyriaChanLocator.get(this);
		Uri uri = locator.buildPath(data.boardName, "delete");
		UrlEncodedEntity entity = new UrlEncodedEntity("postID", data.threadNumber, "password", data.password);
		for (String postNumber : data.postNumbers) entity.add("post_" + postNumber, "on");
		if (data.optionFilesOnly) entity.add("fileOnly", "on");
		String responseText;
		try
		{
			new HttpRequest(uri, data.holder, data).setPostMethod(entity)
					.setRedirectHandler(HttpRequest.RedirectHandler.NONE).execute();
			if (data.holder.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP) return null;
			responseText = data.holder.read().getString();
		}
		finally
		{
			data.holder.disconnect();
		}
		Matcher matcher = PATTERN_POST_ERROR.matcher(responseText);
		if (matcher.find())
		{
			String errorMessage = StringUtils.clearHtml(matcher.group(1));
			int errorType = 0;
			if (errorMessage.contains("Password was incorrect"))
			{
				errorType = ApiException.DELETE_ERROR_PASSWORD;
			}
			if (errorType != 0) throw new ApiException(errorType);
			CommonUtils.writeLog("Valkyria delete message", errorMessage);
			throw new ApiException(errorMessage);
		}
		throw new InvalidResponseException();
	}
	
	@Override
	public SendReportPostsResult onSendReportPosts(SendReportPostsData data) throws HttpException, ApiException,
			InvalidResponseException
	{
		ValkyriaChanLocator locator = ValkyriaChanLocator.get(this);
		Uri uri = locator.buildPath(data.boardName, "report");
		UrlEncodedEntity entity = new UrlEncodedEntity("postID", data.threadNumber, "reportDescription", data.comment);
		for (String postNumber : data.postNumbers) entity.add("post_" + postNumber, "on");
		String responseText;
		try
		{
			new HttpRequest(uri, data.holder, data).setPostMethod(entity)
					.setRedirectHandler(HttpRequest.RedirectHandler.NONE).execute();
			if (data.holder.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP) return null;
			responseText = data.holder.read().getString();
		}
		finally
		{
			data.holder.disconnect();
		}
		Matcher matcher = PATTERN_POST_ERROR.matcher(responseText);
		if (matcher.find())
		{
			String errorMessage = StringUtils.clearHtml(matcher.group(1));
			CommonUtils.writeLog("Valkyria report message", errorMessage);
			throw new ApiException(errorMessage);
		}
		throw new InvalidResponseException();
	}
}