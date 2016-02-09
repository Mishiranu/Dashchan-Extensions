package com.mishiranu.dashchan.chan.ponychan;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.net.Uri;
import android.util.Pair;

import chan.content.ApiException;
import chan.content.ChanPerformer;
import chan.content.ChanLocator;
import chan.content.InvalidResponseException;
import chan.content.ThreadRedirectException;
import chan.http.HttpException;
import chan.http.HttpHolder;
import chan.http.HttpRequest;
import chan.http.MultipartEntity;
import chan.http.UrlEncodedEntity;
import chan.text.ParseException;
import chan.util.CommonUtils;
import chan.util.StringUtils;

public class PonychanChanPerformer extends ChanPerformer
{
	@Override
	public ReadThreadsResult onReadThreads(ReadThreadsData data) throws HttpException, InvalidResponseException
	{
		PonychanChanLocator locator = ChanLocator.get(this);
		Uri uri = data.isCatalog() ? locator.buildSpecificPath(data.boardName, "catalog.html")
				: locator.createBoardUri(data.boardName, data.pageNumber);
		String responseText = new HttpRequest(uri, data.holder, data).setValidator(data.validator).read().getString();
		try
		{
			if (data.isCatalog()) return new ReadThreadsResult(new PonychanCatalogParser(responseText, this).convert());
			return new ReadThreadsResult(new PonychanPostsParser(responseText, this, data.boardName)
					.convertThreads(data.pageNumber));
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
		PonychanChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.createThreadUri(data.boardName, data.threadNumber);
		String responseText = new HttpRequest(uri, data.holder, data).setValidator(data.validator).read().getString();
		try
		{
			return new ReadPostsResult(new PonychanPostsParser(responseText, this, data.boardName).convertPosts());
		}
		catch (ParseException e)
		{
			throw new InvalidResponseException(e);
		}
	}
	
	@Override
	public ReadBoardsResult onReadBoards(ReadBoardsData data) throws HttpException, InvalidResponseException
	{
		PonychanChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildSpecificPath();
		String responseText = new HttpRequest(uri, data.holder, data).read().getString();
		try
		{
			return new ReadBoardsResult(new PonychanBoardsParser(responseText).convert());
		}
		catch (ParseException e)
		{
			throw new InvalidResponseException(e);
		}
	}
	
	@Override
	public ReadPostsCountResult onReadPostsCount(ReadPostsCountData data) throws HttpException, InvalidResponseException
	{
		PonychanChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.createThreadUri(data.boardName, data.threadNumber);
		String responseText = new HttpRequest(uri, data.holder, data).setValidator(data.validator).read().getString();
		int count = 0;
		int index = 0;
		while (index != -1)
		{
			index = responseText.indexOf("<div class=\"post ", index + 1);
			if (index != -1) count++;
		}
		return new ReadPostsCountResult(count);
	}
	
	private void readAndApplyTinyboardAntispamFields(HttpHolder holder, HttpRequest.Preset preset,
			MultipartEntity entity, String boardName, String threadNumber) throws HttpException,
			InvalidResponseException
	{
		PonychanChanLocator locator = ChanLocator.get(this);
		Uri uri = threadNumber != null ? locator.createThreadUri(boardName, threadNumber)
				: locator.createBoardUri(boardName, 0);
		String responseText = new HttpRequest(uri, holder, preset).read().getString();
		int start = responseText.indexOf("<form name=\"post\"");
		if (start == -1) throw new InvalidResponseException();
		responseText = responseText.substring(start, responseText.indexOf("</form>") + 7);
		ArrayList<Pair<String, String>> fields;
		try
		{
			fields = new TinyboardAntispamFieldsParser(responseText).convert();
		}
		catch (ParseException e)
		{
			throw new InvalidResponseException();
		}
		for (Pair<String, String> field : fields) entity.add(field.first, field.second);
	}
	
	private static final Pattern PATTERN_ERROR = Pattern.compile("<h2>(.*?)</h2>");
	
	@Override
	public SendPostResult onSendPost(SendPostData data) throws HttpException, ApiException, InvalidResponseException
	{
		MultipartEntity entity = new MultipartEntity();
		entity.add("board", data.boardName);
		entity.add("thread", data.threadNumber);
		entity.add("making_a_post", "1");
		entity.add("name", data.name);
		entity.add("email", data.email);
		entity.add("subject", data.subject);
		entity.add("body", StringUtils.emptyIfNull(data.comment));
		if (data.attachments != null)
		{
			SendPostData.Attachment attachment = data.attachments[0];
			attachment.addToEntity(entity, "file");
			if (attachment.optionSpoiler) entity.add("spoiler", "on");
		}
		entity.add("password", data.password);
		if (data.optionSpoiler) entity.add("spoiler_thread", "on");
		readAndApplyTinyboardAntispamFields(data.holder, data, entity, data.boardName, data.threadNumber);

		PonychanChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.createSendPostUri();
		String responseText;
		try
		{
			new HttpRequest(uri, data.holder, data).setPostMethod(entity).addHeader("Referer",
					locator.createBoardUri(data.boardName, 0).toString())
					.setRedirectHandler(HttpRequest.RedirectHandler.NONE).setSuccessOnly(false).execute();
			int responseCode = data.holder.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_NOT_FOUND)
			{
				throw new ApiException(ApiException.SEND_ERROR_NO_THREAD);
			}
			if (responseCode == HttpURLConnection.HTTP_SEE_OTHER)
			{
				uri = data.holder.getRedirectedUri();
				String threadNumber = locator.getThreadNumber(uri);
				return new SendPostResult(threadNumber, null);
			}
			// Posting errors has 500 error code
			if (responseCode != HttpURLConnection.HTTP_INTERNAL_ERROR) data.holder.checkResponseCode();
			responseText = data.holder.read().getString();
		}
		finally
		{
			data.holder.disconnect();
		}
		
		Matcher matcher = PATTERN_ERROR.matcher(responseText);
		if (matcher.find())
		{
			String errorMessage = matcher.group(1);
			if (errorMessage != null)
			{
				int errorType = 0;
				if (errorMessage.contains("The body was") || errorMessage.contains("must be at least"))
				{
					errorType = ApiException.SEND_ERROR_EMPTY_COMMENT;
				}
				else if (errorMessage.contains("You must upload an image"))
				{
					errorType = ApiException.SEND_ERROR_EMPTY_FILE;
				}
				else if (errorMessage.contains("was too long"))
				{
					errorType = ApiException.SEND_ERROR_FIELD_TOO_LONG;
				}
				else if (errorMessage.contains("The file was too big") || errorMessage.contains("is longer than"))
				{
					errorType = ApiException.SEND_ERROR_FILE_TOO_BIG;
				}
				else if (errorMessage.contains("Thread locked"))
				{
					errorType = ApiException.SEND_ERROR_CLOSED;
				}
				else if (errorMessage.contains("Unsupported image format"))
				{
					errorType = ApiException.SEND_ERROR_FILE_NOT_SUPPORTED;
				}
				else if (errorMessage.contains("Maximum file size"))
				{
					errorType = ApiException.SEND_ERROR_FILE_TOO_BIG;
				}
				else if (errorMessage.contains("Your IP address"))
				{
					errorType = ApiException.SEND_ERROR_BANNED;
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
			}
			CommonUtils.writeLog("Ponychan send message", errorMessage);
			throw new ApiException(errorMessage);
		}
		throw new InvalidResponseException();
	}
	
	@Override
	public SendDeletePostsResult onSendDeletePosts(SendDeletePostsData data) throws HttpException, ApiException,
			InvalidResponseException
	{
		PonychanChanLocator locator = ChanLocator.get(this);
		UrlEncodedEntity entity = new UrlEncodedEntity("board", data.boardName, "delete", "Delete",
				"password", data.password);
		for (String postNumber : data.postNumbers) entity.add("delete_" + postNumber, "on");
		if (data.optionFilesOnly) entity.add("file", "on");
		Uri uri = locator.createSendPostUri();
		String responseText = new HttpRequest(uri, data.holder, data).setPostMethod(entity).read().getString();
		if (responseText != null)
		{
			Matcher matcher = PATTERN_ERROR.matcher(responseText);
			if (matcher.find())
			{
				String errorMessage = matcher.group(1);
				if (errorMessage.contains("Wrong password"))
				{
					throw new ApiException(ApiException.DELETE_ERROR_PASSWORD);
				}
				else if (errorMessage.contains("You'll have to wait"))
				{
					throw new ApiException(ApiException.DELETE_ERROR_TOO_NEW);
				}
			}
		}
		return null;
	}
}