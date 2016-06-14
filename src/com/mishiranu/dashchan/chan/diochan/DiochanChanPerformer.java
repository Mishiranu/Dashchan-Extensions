package com.mishiranu.dashchan.chan.diochan;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.net.Uri;
import android.util.Pair;

import chan.content.ApiException;
import chan.content.ChanLocator;
import chan.content.ChanPerformer;
import chan.content.InvalidResponseException;
import chan.content.ThreadRedirectException;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.http.HttpException;
import chan.http.HttpRequest;
import chan.http.MultipartEntity;
import chan.http.UrlEncodedEntity;
import chan.text.ParseException;
import chan.util.CommonUtils;
import chan.util.StringUtils;

public class DiochanChanPerformer extends ChanPerformer
{
	private static final Pattern PATTERN_CATALOG = Pattern.compile("(?s)<a href=\"/.*?/res/(\\d+).html\">.*?</a>"
			+ "<br />.*?<small>(\\d+)</small>");
	
	@Override
	public ReadThreadsResult onReadThreads(ReadThreadsData data) throws HttpException, InvalidResponseException
	{
		DiochanChanLocator locator = ChanLocator.get(this);
		if (data.isCatalog())
		{
			Uri uri = locator.buildPath(data.boardName, "catalog.html");
			String responseText = new HttpRequest(uri, data.holder, data).setValidator(data.validator)
					.read().getString();
			ArrayList<Pair<String, Integer>> threadInfos = new ArrayList<>();
			Matcher matcher = PATTERN_CATALOG.matcher(responseText);
			while (matcher.find())
			{
				String threadNumber = matcher.group(1);
				if (threadNumber != null)
				{
					int replies = Integer.parseInt(matcher.group(2));
					threadInfos.add(new Pair<String, Integer>(threadNumber, replies));
				}
			}
			if (threadInfos.isEmpty()) return null;
			uri = locator.buildQuery("expand.php", "board", data.boardName);
			responseText = new HttpRequest(uri, data.holder, data).read().getString();
			ArrayList<Post> posts;
			try
			{
				posts = new DiochanPostsParser(responseText, this, data.boardName).convertPosts();
				if (posts == null || posts.isEmpty()) return null;
			}
			catch (ParseException e)
			{
				throw new InvalidResponseException(e);
			}
			HashMap<String, Post> postsMap = new HashMap<>();
			for (Post post : posts)
			{
				if (post.getParentPostNumber() == null) postsMap.put(post.getPostNumber(), post);
			}
			ArrayList<Posts> threads = new ArrayList<>();
			for (Pair<String, Integer> threadInfo : threadInfos)
			{
				Post post = postsMap.get(threadInfo.first);
				if (post != null)
				{
					Posts thread = new Posts(post);
					thread.addPostsCount(1 + threadInfo.second);
					threads.add(thread);
				}
			}
			return new ReadThreadsResult(threads);
		}
		else
		{
			Uri uri = locator.createBoardUri(data.boardName, data.pageNumber);
			String responseText = new HttpRequest(uri, data.holder, data).setValidator(data.validator)
					.read().getString();
			try
			{
				return new ReadThreadsResult(new DiochanPostsParser(responseText, this, data.boardName)
						.convertThreads());
			}
			catch (ParseException e)
			{
				throw new InvalidResponseException(e);
			}
		}
	}
	
	@Override
	public ReadPostsResult onReadPosts(ReadPostsData data) throws HttpException, ThreadRedirectException,
			InvalidResponseException
	{
		DiochanChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.createThreadUri(data.boardName, data.threadNumber);
		String responseText = new HttpRequest(uri, data.holder, data).setValidator(data.validator).read().getString();
		try
		{
			return new ReadPostsResult(new DiochanPostsParser(responseText, this, data.boardName).convertPosts());
		}
		catch (ParseException e)
		{
			throw new InvalidResponseException(e);
		}
	}
	
	@Override
	public ReadSinglePostResult onReadSinglePost(ReadSinglePostData data) throws HttpException, InvalidResponseException
	{
		DiochanChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildQuery("read.php", "b", data.boardName, "t", "0", "p", data.postNumber, "single", "");
		String responseText = new HttpRequest(uri, data.holder, data).read().getString();
		try
		{
			Post post = new DiochanPostsParser(responseText, this, data.boardName).convertSinglePost();
			if (post == null) throw HttpException.createNotFoundException();
			return new ReadSinglePostResult(post);
		}
		catch (ParseException e)
		{
			throw new InvalidResponseException(e);
		}
	}
	
	@Override
	public ReadBoardsResult onReadBoards(ReadBoardsData data) throws HttpException, InvalidResponseException
	{
		DiochanChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildPath("navigator.htm");
		String responseText = new HttpRequest(uri, data.holder, data).read().getString();
		try
		{
			return new ReadBoardsResult(new DiochanBoardsParser(responseText).convert());
		}
		catch (ParseException e)
		{
			throw new InvalidResponseException(e);
		}
	}
	
	@Override
	public ReadPostsCountResult onReadPostsCount(ReadPostsCountData data) throws HttpException, InvalidResponseException
	{
		DiochanChanLocator locator = ChanLocator.get(this);
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
	
	private static final Pattern PATTERN_POST_ERROR = Pattern.compile("(?s)<h2.*?>(.*?)</h2>");
	
	@Override
	public SendPostResult onSendPost(SendPostData data) throws HttpException, ApiException, InvalidResponseException
	{
		MultipartEntity entity = new MultipartEntity();
		entity.add("board", data.boardName);
		entity.add("replythread", data.threadNumber == null ? "0" : data.threadNumber);
		entity.add("name", data.name);
		entity.add("em", data.optionSage ? "salvia" : data.email);
		entity.add("subject", data.subject);
		entity.add("message", StringUtils.emptyIfNull(data.comment));
		entity.add("postpassword", data.password);
		entity.add("gotothread", "on");
		entity.add("embed", ""); // Otherwise there will be a "Please enter an embed ID" error
		if (data.attachments != null)
		{
			for (SendPostData.Attachment attachment : data.attachments)
			{
				attachment.addToEntity(entity, "imagefile[]");
				if (attachment.optionSpoiler) entity.add("spoiler", "on");
			}
		}
		else entity.add("nofile", "on");
		
		DiochanChanLocator locator = ChanLocator.get(this);
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
			String errorMessage = matcher.group(1).trim();
			int errorType = 0;
			if (errorMessage.contains("is required for a reply") || errorMessage.contains("richiesto per una risposta"))
			{
				errorType = ApiException.SEND_ERROR_EMPTY_COMMENT;
			}
			else if (errorMessage.contains("Please enter an embed ID"))
			{
				errorType = ApiException.SEND_ERROR_EMPTY_FILE;
			}
			else if (errorMessage.contains("Prego attendere un momento"))
			{
				errorType = ApiException.SEND_ERROR_TOO_FAST;
			}
			else if (errorMessage.contains("Invalid thread ID"))
			{
				errorType = ApiException.SEND_ERROR_NO_THREAD;
			}
			else if (errorMessage.contains("Your IP is recognized as dangerous") ||
					errorMessage.contains("SEI BANNATO!") || errorMessage.contains("YOU ARE BANNED!"))
			{
				errorType = ApiException.SEND_ERROR_BANNED;
			}
			if (errorType != 0) throw new ApiException(errorType);
			CommonUtils.writeLog("Diochan send message", errorMessage);
			throw new ApiException(errorMessage);
		}
		throw new InvalidResponseException();
	}
	
	@Override
	public SendDeletePostsResult onSendDeletePosts(SendDeletePostsData data) throws HttpException, ApiException,
			InvalidResponseException
	{
		DiochanChanLocator locator = ChanLocator.get(this);
		UrlEncodedEntity entity = new UrlEncodedEntity("board", data.boardName, "deletepost", "1",
				"postpassword", data.password);
		for (String postNumber : data.postNumbers) entity.add("post[]", postNumber);
		if (data.optionFilesOnly) entity.add("fileonly", "on");
		Uri uri = locator.buildPath("board.php");
		String responseText = new HttpRequest(uri, data.holder, data).setTimeouts(45000, 45000)
				.setPostMethod(entity).read().getString();
		if (responseText != null)
		{
			if (responseText.contains("Post successfully") || responseText.contains("Post correttamente") ||
					responseText.contains("Image successfully") || responseText.contains("Immagine correttamente"))
			{
				// Response has message for any post
				// Ignore them, if at least 1 of them was deleted
				return null;
			}
			else if (responseText.contains("Incorrect password") || responseText.contains("Password non corretta"))
			{
				throw new ApiException(ApiException.DELETE_ERROR_PASSWORD);
			}
			CommonUtils.writeLog("Diochan delete message", responseText);
		}
		throw new InvalidResponseException();
	}
	
	@Override
	public SendReportPostsResult onSendReportPosts(SendReportPostsData data) throws HttpException, ApiException,
			InvalidResponseException
	{
		DiochanChanLocator locator = ChanLocator.get(this);
		UrlEncodedEntity entity = new UrlEncodedEntity("board", data.boardName, "reportpost", "1",
				"reportreason", data.comment);
		for (String postNumber : data.postNumbers) entity.add("post[]", postNumber);
		Uri uri = locator.buildPath("board.php");
		String responseText = new HttpRequest(uri, data.holder, data).setPostMethod(entity).read().getString();
		if (responseText != null)
		{
			if (responseText.contains("Post successfully reported") ||
					responseText.contains("Post correttamente aggiunto alla lista dei report") ||
					responseText.contains("That post is already in the report list") ||
					responseText.contains("Quel post è già nella lista dei report"))
			{
				// Response has message for any post
				// Ignore them, if at least 1 of them was reported
				return null;
			}
			CommonUtils.writeLog("Diochan report message", responseText);
		}
		throw new InvalidResponseException();
	}
}