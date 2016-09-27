package com.mishiranu.dashchan.chan.chiochan;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Pair;

import chan.content.ApiException;
import chan.content.ChanPerformer;
import chan.content.InvalidResponseException;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.content.model.ThreadSummary;
import chan.http.HttpException;
import chan.http.HttpRequest;
import chan.http.MultipartEntity;
import chan.http.UrlEncodedEntity;
import chan.text.ParseException;
import chan.util.CommonUtils;
import chan.util.StringUtils;

public class ChiochanChanPerformer extends ChanPerformer
{
	private static final String COOKIE_SESSION = "session";
	private static final String PREFIX_FAPTCHA = "faptcha_";

	private static final Pattern PATTERN_CATALOG = Pattern.compile("(?s)<a href=\"/\\w+/res/(.*?).html\">"
			+ "<div class=\"catalogthread.*?\">.*?<span class=\"catalogposts\">(\\d+)</span>");

	@Override
	public ReadThreadsResult onReadThreads(ReadThreadsData data) throws HttpException, InvalidResponseException
	{
		ChiochanChanLocator locator = ChiochanChanLocator.get(this);
		if (data.isCatalog())
		{
			Uri uri = locator.buildPath(data.boardName, "catalog.html");
			String responseText = new HttpRequest(uri, data).setValidator(data.validator).read().getString();
			ArrayList<Pair<String, Integer>> threadInfos = new ArrayList<>();
			Matcher matcher = PATTERN_CATALOG.matcher(responseText);
			while (matcher.find())
			{
				String threadNumber = matcher.group(1);
				int replies = Integer.parseInt(matcher.group(2));
				threadInfos.add(new Pair<>(threadNumber, replies));
			}
			if (threadInfos.isEmpty()) return null;
			uri = locator.buildQuery("expand.php", "board", data.boardName, "threadid", "0");
			responseText = new HttpRequest(uri, data).read().getString();
			ArrayList<Post> posts;
			try
			{
				posts = new ChiochanPostsParser(responseText, this, data.boardName).convertExpand();
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
			String responseText = new HttpRequest(uri, data).setValidator(data.validator).read().getString();
			try
			{
				return new ReadThreadsResult(new ChiochanPostsParser(responseText, this, data.boardName)
						.convertThreads());
			}
			catch (ParseException e)
			{
				throw new InvalidResponseException(e);
			}
		}
	}

	@Override
	public ReadPostsResult onReadPosts(ReadPostsData data) throws HttpException, InvalidResponseException
	{
		ChiochanChanLocator locator = ChiochanChanLocator.get(this);
		Uri uri = locator.createThreadUri(data.boardName, data.threadNumber);
		String responseText;
		boolean archived = false;
		try
		{
			responseText = new HttpRequest(uri, data).setValidator(data.validator).read().getString();
		}
		catch (HttpException e)
		{
			if (e.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND)
			{
				uri = locator.createThreadArchiveUri(data.boardName, data.threadNumber);
				responseText = new HttpRequest(uri, data).setValidator(data.validator).read().getString();
				archived = true;
			}
			else throw e;
		}
		try
		{
			ArrayList<Post> posts = new ChiochanPostsParser(responseText, this, data.boardName).convertPosts();
			if (archived && posts != null && posts.size() > 0) posts.get(0).setArchived(true);
			return new ReadPostsResult(posts);
		}
		catch (ParseException e)
		{
			throw new InvalidResponseException(e);
		}
	}

	@Override
	public ReadBoardsResult onReadBoards(ReadBoardsData data) throws HttpException, InvalidResponseException
	{
		ChiochanChanLocator locator = ChiochanChanLocator.get(this);
		Uri uri = locator.buildPath("menu.php");
		String responseText = new HttpRequest(uri, data).read().getString();
		try
		{
			return new ReadBoardsResult(new ChiochanBoardsParser(responseText).convert());
		}
		catch (ParseException e)
		{
			throw new InvalidResponseException(e);
		}
	}

	private static final Pattern PATTERN_ARCHIVED_THREAD = Pattern.compile("<a href=\"(\\d+).html\">.*?" +
			"<td align=\"right\">(.{15,}?)</td>");

	@Override
	public ReadThreadSummariesResult onReadThreadSummaries(ReadThreadSummariesData data) throws HttpException,
			InvalidResponseException
	{
		ChiochanChanLocator locator = ChiochanChanLocator.get(this);
		Uri uri = locator.createBoardUri(data.boardName, 0).buildUpon().appendEncodedPath("arch/res").build();
		String responseText = new HttpRequest(uri, data).setSuccessOnly(false).read().getString();
		if (data.holder.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) return null;
		data.holder.checkResponseCode();
		ArrayList<ThreadSummary> threadSummaries = new ArrayList<>();
		Matcher matcher = PATTERN_ARCHIVED_THREAD.matcher(responseText);
		while (matcher.find())
		{
			threadSummaries.add(new ThreadSummary(data.boardName, matcher.group(1), "#" + matcher.group(1) + ", "
					+ matcher.group(2).trim()));
		}
		return new ReadThreadSummariesResult(threadSummaries);
	}

	@Override
	public ReadPostsCountResult onReadPostsCount(ReadPostsCountData data) throws HttpException, InvalidResponseException
	{
		ChiochanChanLocator locator = ChiochanChanLocator.get(this);
		Uri uri = locator.buildPath(data.boardName, "res", data.threadNumber + "+50.html");
		String responseText = new HttpRequest(uri, data).setValidator(data.validator)
				.setSuccessOnly(false).read().getString();
		boolean notFound = data.holder.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND;
		int count = 0;
		if (notFound)
		{
			uri = locator.createThreadUri(data.boardName, data.threadNumber);
			responseText = new HttpRequest(uri, data).setValidator(data.validator).read().getString();
		}
		else data.holder.checkResponseCode();
		if (!responseText.contains("<form name=\"postform\"")) throw new InvalidResponseException();
		int index = 0;
		while (index != -1)
		{
			count++;
			index = StringUtils.nearestIndexOf(responseText, index + 1, "<div class=\"reply\"", "<td class=\"reply\"");
		}
		index = responseText.indexOf("<span class=\"omittedposts\">");
		if (index >= 0)
		{
			Matcher matcher = ChiochanPostsParser.NUMBER.matcher(responseText);
			matcher.find(index + 27);
			count += Integer.parseInt(matcher.group(1));
		}
		return new ReadPostsCountResult(count);
	}

	@Override
	public ReadCaptchaResult onReadCaptcha(ReadCaptchaData data) throws HttpException, InvalidResponseException
	{
		ChiochanChanLocator locator = ChiochanChanLocator.get(this);
		ChiochanChanConfiguration configuration = ChiochanChanConfiguration.get(this);
		String sessionCookie = configuration.getCookie(COOKIE_SESSION);
		if (sessionCookie != null)
		{
			String faptcha = configuration.getCookie(PREFIX_FAPTCHA + data.boardName);
			Uri uri = locator.buildPath("api_adaptive.php").buildUpon()
					.appendQueryParameter("board", data.boardName).build();
			String responseText = new HttpRequest(uri, data).addCookie(data.boardName, faptcha)
					.addCookie("PHPSESSID", sessionCookie).read().getString();
			if ("1".equals(responseText))
			{
				CaptchaData captchaData = new CaptchaData();
				captchaData.put(CaptchaData.CHALLENGE, sessionCookie);
				return new ReadCaptchaResult(CaptchaState.SKIP, captchaData);
			}
		}
		Uri uri = locator.buildPath("faptcha.php").buildUpon().appendQueryParameter("board",
				data.boardName).build();
		Bitmap image = new HttpRequest(uri, data).setRedirectHandler(HttpRequest.RedirectHandler.NONE)
				.addCookie("PHPSESSID", sessionCookie).read().getBitmap();
		String newSessionCookie = data.holder.getCookieValue("PHPSESSID");
		if (newSessionCookie != null) sessionCookie = newSessionCookie;
		if (image != null)
		{
			CaptchaData captchaData = new CaptchaData();
			captchaData.put(CaptchaData.CHALLENGE, sessionCookie);
			return new ReadCaptchaResult(CaptchaState.CAPTCHA, captchaData).setImage(image);
		}
		throw new InvalidResponseException();
	}

	private static final Pattern PATTERN_POST_ERROR = Pattern.compile("(?s)<h2.*?>(?:\r\n)?(.*?)(?:\r\n)?</h2>");

	@Override
	public SendPostResult onSendPost(SendPostData data) throws HttpException, ApiException, InvalidResponseException
	{
		ChiochanChanConfiguration configuration = ChiochanChanConfiguration.get(this);
		MultipartEntity entity = new MultipartEntity();
		entity.add("board", data.boardName);
		entity.add("replythread", data.threadNumber == null ? "0" : data.threadNumber);
		entity.add("name", data.name);
		entity.add("subject", data.subject);
		entity.add("message", StringUtils.emptyIfNull(data.comment));
		entity.add("postpassword", data.password);
		if (data.optionSage) entity.add("sage", "on");
		entity.add("noko", "on");
		if (data.attachments != null) data.attachments[0].addToEntity(entity, "imagefile");
		else entity.add("nofile", "on");
		String session = null;
		if (data.captchaData != null)
		{
			session = data.captchaData.get(CaptchaData.CHALLENGE);
			entity.add("faptcha", data.captchaData.get(CaptchaData.INPUT));
		}

		String faptcha = null;
		if (session != null)
		{
			faptcha = configuration.getCookie(PREFIX_FAPTCHA + data.boardName);
			configuration.storeCookie(COOKIE_SESSION, session, "Session");
		}
		ChiochanChanLocator locator = ChiochanChanLocator.get(this);
		Uri uri = locator.buildPath("board.php");
		String responseText;
		try
		{
			new HttpRequest(uri, data).setPostMethod(entity).addCookie(data.boardName, faptcha)
					.addCookie("PHPSESSID", session).setRedirectHandler(HttpRequest.RedirectHandler.NONE).execute();
			faptcha = data.holder.getCookieValue(data.boardName);
			if (faptcha != null)
			{
				configuration.storeCookie(PREFIX_FAPTCHA + data.boardName, faptcha,
						"Faptcha /" + data.boardName + '/');
			}
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
				if (errorMessage.contains("Please enter a faptcha") ||
						errorMessage.contains("Incorrect faptcha entered"))
				{
					errorType = ApiException.SEND_ERROR_CAPTCHA;
				}
				else if (errorMessage.contains("Old faptcha is old"))
				{
					configuration.storeCookie(PREFIX_FAPTCHA + data.boardName, null, null);
					errorType = ApiException.SEND_ERROR_CAPTCHA;
				}
				else if (errorMessage.contains("Для ответа необходимо") ||
						errorMessage.contains("is required for a reply"))
				{
					errorType = ApiException.SEND_ERROR_EMPTY_COMMENT;
				}
				else if (errorMessage.contains("требуется прикрепить файл") ||
						errorMessage.contains("file is required for a new thread"))
				{
					errorType = ApiException.SEND_ERROR_EMPTY_FILE;
				}
				else if (errorMessage.contains("Неверный номер нити") ||
						errorMessage.contains("Invalid thread ID"))
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
			CommonUtils.writeLog("Chiochan send message", errorMessage);
			throw new ApiException(errorMessage);
		}
		throw new InvalidResponseException();
	}

	@Override
	public SendDeletePostsResult onSendDeletePosts(SendDeletePostsData data) throws HttpException, ApiException,
			InvalidResponseException
	{
		ChiochanChanLocator locator = ChiochanChanLocator.get(this);
		UrlEncodedEntity entity = new UrlEncodedEntity("board", data.boardName, "deletepost", "1",
				"postpassword", data.password);
		for (String postNumber : data.postNumbers) entity.add("delete[]", postNumber);
		if (data.optionFilesOnly) entity.add("fileonly", "on");
		Uri uri = locator.buildPath("board.php");
		String responseText = new HttpRequest(uri, data).setPostMethod(entity).read().getString();
		if (responseText != null)
		{
			if (responseText.contains("Сообщение успешно удалено") ||
					responseText.contains("Image successfully deleted from your post"))
			{
				// Response has message for any post
				// Ignore them, if at least 1 of them was deleted
				return null;
			}
			if (responseText.contains("Неверный пароль") || responseText.contains("Incorrect password"))
			{
				throw new ApiException(ApiException.DELETE_ERROR_PASSWORD);
			}
			CommonUtils.writeLog("Chiochan delete message", responseText);
		}
		throw new InvalidResponseException();
	}

	@Override
	public SendReportPostsResult onSendReportPosts(SendReportPostsData data) throws HttpException, ApiException,
			InvalidResponseException
	{
		ChiochanChanLocator locator = ChiochanChanLocator.get(this);
		UrlEncodedEntity entity = new UrlEncodedEntity("board", data.boardName, "reportpost", "1");
		for (String postNumber : data.postNumbers) entity.add("delete[]", postNumber);
		Uri uri = locator.buildPath("board.php");
		String responseText = new HttpRequest(uri, data).setPostMethod(entity).read().getString();
		if (responseText != null)
		{
			if (responseText.contains("Post successfully reported") ||
					responseText.contains("На это сообщение уже жаловались") ||
					responseText.contains("That post is already in the report list"))
			{
				// Response has message for any post
				// Ignore them, if at least 1 of them was reported
				return null;
			}
			CommonUtils.writeLog("Chiochan report message", responseText);
		}
		throw new InvalidResponseException();
	}
}