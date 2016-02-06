package com.mishiranu.dashchan.chan.owlchan;

import java.net.HttpURLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.net.Uri;

import chan.content.ApiException;
import chan.content.ChanLocator;
import chan.content.ChanPerformer;
import chan.content.InvalidResponseException;
import chan.http.HttpException;
import chan.http.HttpHolder;
import chan.http.HttpRequest;
import chan.http.MultipartEntity;
import chan.http.UrlEncodedEntity;
import chan.text.ParseException;
import chan.util.CommonUtils;
import chan.util.StringUtils;

public class OwlchanChanPerformer extends ChanPerformer
{
	private static final HttpRequest.RedirectHandler REDIRECT_404_HANDLER = new HttpRequest.RedirectHandler()
	{
		@Override
		public Action onRedirectReached(int responseCode, Uri requestedUri, Uri redirectedUri, HttpHolder holder)
				throws HttpException
		{
			if ("/404.php".equals(redirectedUri.getPath())) throw HttpException.createNotFoundException();
			return BROWSER.onRedirectReached(responseCode, requestedUri, redirectedUri, holder);
		}
	};
	
	@Override
	public ReadThreadsResult onReadThreads(ReadThreadsData data) throws HttpException, InvalidResponseException
	{
		OwlchanChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.createBoardUri(data.boardName, data.pageNumber);
		String responseText = new HttpRequest(uri, data.holder, data).setValidator(data.validator)
				.setRedirectHandler(REDIRECT_404_HANDLER).read().getString();
		try
		{
			return new ReadThreadsResult(new OwlchanPostsParser(responseText, this, data.boardName).convertThreads());
		}
		catch (ParseException e)
		{
			throw new InvalidResponseException(e);
		}
	}
	
	@Override
	public ReadPostsResult onReadPosts(ReadPostsData data) throws HttpException, InvalidResponseException
	{
		OwlchanChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.createThreadUri(data.boardName, data.threadNumber);
		String responseText = new HttpRequest(uri, data.holder, data).setValidator(data.validator)
				.setRedirectHandler(REDIRECT_404_HANDLER).read().getString();
		try
		{
			return new ReadPostsResult(new OwlchanPostsParser(responseText, this, data.boardName).convertPosts());
		}
		catch (ParseException e)
		{
			throw new InvalidResponseException(e);
		}
	}
	
	@Override
	public ReadBoardsResult onReadBoards(ReadBoardsData data) throws HttpException, InvalidResponseException
	{
		OwlchanChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildPath("menu.php");
		String responseText = new HttpRequest(uri, data.holder, data).setRedirectHandler(REDIRECT_404_HANDLER)
				.read().getString();
		try
		{
			return new ReadBoardsResult(new OwlchanBoardsParser(responseText).convert());
		}
		catch (ParseException e)
		{
			throw new InvalidResponseException(e);
		}
	}
	
	@Override
	public ReadPostsCountResult onReadPostsCount(ReadPostsCountData data) throws HttpException, InvalidResponseException
	{
		OwlchanChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.createThreadUri(data.boardName, data.threadNumber);
		String responseText = new HttpRequest(uri, data.holder, data).setValidator(data.validator)
				.setRedirectHandler(REDIRECT_404_HANDLER).read().getString();
		int count = 0;
		int index = 0;
		while (index != -1)
		{
			count++;
			index = responseText.indexOf("<td class=\"reply\"", index + 1);
		}
		return new ReadPostsCountResult(count);
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
			SendPostData.Attachment attachment = data.attachments[0];
			attachment.addToEntity(entity, "imagefile");
			if (attachment.optionSpoiler) entity.add("spoiler", "on");
		}
		else entity.add("nofile", "on");
		
		OwlchanChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildPath("board.php");
		String responseText;
		try
		{
			new HttpRequest(uri, data.holder, data).setPostMethod(entity)
					.setRedirectHandler(HttpRequest.RedirectHandler.NONE).execute();
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
		
		Matcher matcher = PATTERN_POST_ERROR.matcher(responseText);
		if (matcher.find())
		{
			String errorMessage = matcher.group(1);
			if (errorMessage != null)
			{
				int errorType = 0;
				if (errorMessage.contains("Чтобы ответить") || errorMessage.contains("A message is required"))
				{
					errorType = ApiException.SEND_ERROR_EMPTY_COMMENT;
				}
				else if (errorMessage.contains("file is required for a new thread"))
				{
					errorType = ApiException.SEND_ERROR_EMPTY_FILE;
				}
				else if (errorMessage.contains("Invalid thread ID"))
				{
					errorType = ApiException.SEND_ERROR_NO_THREAD;
				}
				else if (errorMessage.contains("Spam bot detected"))
				{
					errorType = ApiException.SEND_ERROR_BANNED;
				}
				else if (responseText.contains("your message is too long"))
				{
					// Check responseText, not errorMessage
					errorType = ApiException.SEND_ERROR_FIELD_TOO_LONG;
				}
				if (errorType != 0) throw new ApiException(errorType);
			}
			CommonUtils.writeLog("Owlchan send message", errorMessage);
			throw new ApiException(errorMessage);
		}
		throw new InvalidResponseException();
	}
	
	@Override
	public SendDeletePostsResult onSendDeletePosts(SendDeletePostsData data) throws HttpException, ApiException,
			InvalidResponseException
	{
		OwlchanChanLocator locator = ChanLocator.get(this);
		UrlEncodedEntity entity = new UrlEncodedEntity("board", data.boardName, "deletepost", "1",
				"postpassword", data.password);
		for (String postNumber : data.postNumbers) entity.add("post[]", postNumber);
		if (data.optionFilesOnly) entity.add("fileonly", "on");
		Uri uri = locator.buildPath("board.php");
		String responseText = new HttpRequest(uri, data.holder, data).setPostMethod(entity).read().getString();
		if (responseText != null)
		{
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
			CommonUtils.writeLog("Owlchan delete message", responseText);
			throw new ApiException(responseText);
		}
		throw new InvalidResponseException();
	}
	
	@Override
	public SendReportPostsResult onSendReportPosts(SendReportPostsData data) throws HttpException, ApiException,
			InvalidResponseException
	{
		OwlchanChanLocator locator = ChanLocator.get(this);
		UrlEncodedEntity entity = new UrlEncodedEntity("board", data.boardName, "reportpost", "1", "reportreason",
				data.comment);
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
			CommonUtils.writeLog("Owlchan report message", responseText);
		}
		throw new InvalidResponseException();
	}
}