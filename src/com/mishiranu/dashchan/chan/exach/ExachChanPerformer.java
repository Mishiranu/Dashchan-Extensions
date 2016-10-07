package com.mishiranu.dashchan.chan.exach;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.net.Uri;

import chan.content.ApiException;
import chan.content.ChanPerformer;
import chan.content.InvalidResponseException;
import chan.content.ThreadRedirectException;
import chan.content.model.Post;
import chan.http.HttpException;
import chan.http.HttpRequest;
import chan.http.MultipartEntity;
import chan.http.UrlEncodedEntity;
import chan.text.ParseException;
import chan.util.CommonUtils;
import chan.util.StringUtils;

public class ExachChanPerformer extends ChanPerformer
{
	@Override
	public ReadThreadsResult onReadThreads(ReadThreadsData data) throws HttpException, InvalidResponseException
	{
		if ("p".equals(data.boardName)) throw new InvalidResponseException();
		ExachChanLocator locator = ExachChanLocator.get(this);
		Uri uri = locator.createBoardUri(data.boardName, data.pageNumber);
		String responseText = new HttpRequest(uri, data.holder, data).setValidator(data.validator).read().getString();
		try
		{
			return new ReadThreadsResult(new ExachPostsParser(responseText, this, data.boardName).convertThreads());
		}
		catch (ParseException e)
		{
			throw new InvalidResponseException(e);
		}
	}

	private static final Pattern PATTERN_THREAD_REDIRECT = Pattern.compile("<p class=\"text\">" +
			"<a href=\"(.*?)\">Страница треда</a> не отображает отдельные посты.</p>");
	private static final Pattern PATTERN_PAGES = Pattern.compile("<div class=\"pages\">(.*?)</div>");

	@Override
	public ReadPostsResult onReadPosts(ReadPostsData data) throws HttpException, ThreadRedirectException,
			InvalidResponseException
	{
		if (!ExachChanLocator.DEFAULT_BOARD_NAME.equals(data.boardName))
		{
			throw new ThreadRedirectException(ExachChanLocator.DEFAULT_BOARD_NAME, data.threadNumber, null);
		}
		int lastPostNumber = data.partialThreadLoading && data.lastPostNumber != null
				? Integer.parseInt(data.lastPostNumber) : 0;
		ExachChanLocator locator = ExachChanLocator.get(this);
		Uri initialUri = locator.createThreadUri(data.boardName, data.threadNumber);
		int page = 0;
		int pagesCount = 0;
		LinkedList<Post> fullPosts = new LinkedList<>();
		while (true)
		{
			Uri uri = initialUri.buildUpon().appendQueryParameter("page", Integer.toString(page)).build();
			String responseText = new HttpRequest(uri, data.holder, data).setSuccessOnly(false).read().getString();
			if (page == 0)
			{
				Matcher matcher = PATTERN_THREAD_REDIRECT.matcher(responseText);
				if (matcher.find())
				{
					String threadNumber = locator.getThreadNumber(Uri.parse(matcher.group(1)));
					if (threadNumber != null)
					{
						throw new ThreadRedirectException(data.boardName, threadNumber, data.threadNumber);
					}
				}
			}
			data.holder.checkResponseCode();
			if (pagesCount == 0)
			{
				Matcher matcher = PATTERN_PAGES.matcher(responseText);
				if (matcher.find()) pagesCount = ExachPostsParser.extractPagesCount(matcher.group(1));
				if (pagesCount <= 0) pagesCount = 1;
				page = pagesCount;
			}
			ArrayList<Post> posts;
			try
			{
				posts = new ExachPostsParser(responseText, this, data.boardName).convertPosts(data.threadNumber);
			}
			catch (ParseException e)
			{
				throw new InvalidResponseException(e);
			}
			if (posts == null || posts.isEmpty()) throw new InvalidResponseException();
			if (page != 1) posts.remove(0);
			boolean finish = false;
			for (int i = posts.size() - 1; i >= 0; i--)
			{
				Post post = posts.get(i);
				fullPosts.addFirst(post);
				finish |= !finish && lastPostNumber > 0 && lastPostNumber > Integer.parseInt(post.getPostNumber());
			}
			if (finish || --page == 0) return new ReadPostsResult(fullPosts);
		}
	}

	@Override
	public ReadSearchPostsResult onReadSearchPosts(ReadSearchPostsData data) throws HttpException,
			InvalidResponseException
	{
		ExachChanLocator locator = ExachChanLocator.get(this);
		Uri uri = locator.buildQuery("search.php", "s", data.searchQuery);
		String responseText = new HttpRequest(uri, data.holder, data).read().getString();
		try
		{
			return new ReadSearchPostsResult(new ExachPostsParser(responseText, this, data.boardName).convertSearch());
		}
		catch (ParseException e)
		{
			throw new InvalidResponseException(e);
		}
	}

	@Override
	public ReadBoardsResult onReadBoards(ReadBoardsData data) throws HttpException, InvalidResponseException
	{
		ExachChanLocator locator = ExachChanLocator.get(this);
		Uri uri = locator.buildPath("");
		String responseText = new HttpRequest(uri, data.holder, data).read().getString();
		try
		{
			return new ReadBoardsResult(new ExachBoardsParser(responseText).convert());
		}
		catch (ParseException e)
		{
			throw new InvalidResponseException(e);
		}
	}

	private static final Pattern PATTERN_REPLIES_COUNT = Pattern.compile("<span id=\"ans\">(\\d+)</span>");

	@Override
	public ReadPostsCountResult onReadPostsCount(ReadPostsCountData data) throws HttpException, InvalidResponseException
	{
		if (!ExachChanLocator.DEFAULT_BOARD_NAME.equals(data.boardName)) throw HttpException.createNotFoundException();
		ExachChanLocator locator = ExachChanLocator.get(this);
		Uri uri = locator.createThreadUri(data.boardName, data.threadNumber).buildUpon()
				.appendQueryParameter("page", "1").build();
		String responseText = new HttpRequest(uri, data).setValidator(data.validator).read().getString();
		Matcher matcher = PATTERN_REPLIES_COUNT.matcher(responseText);
		if (matcher.find()) return new ReadPostsCountResult(Integer.parseInt(matcher.group(1)) + 1);
		else throw new InvalidResponseException();
	}

	private static final String COOKIE_POST_OWNER = "post_owner";

	private static final Pattern PATTERN_BOARD_ID = Pattern.compile("<input .*?name=\"id\" .*?value=\"(\\d+)\">");
	private static final Pattern PATTERN_TOKEN = Pattern.compile("<input .*?name=\"token\" .*?value=\"(\\d+)\">");
	private static final Pattern PATTERN_SEND_ERROR = Pattern.compile("<h2>(.*?)</h2>");

	@Override
	public SendPostResult onSendPost(SendPostData data) throws HttpException, ApiException, InvalidResponseException
	{
		if (data.threadNumber != null && data.attachments == null && StringUtils.isEmpty(data.comment))
		{
			throw new ApiException(ApiException.SEND_ERROR_EMPTY_COMMENT);
		}
		ExachChanLocator locator = ExachChanLocator.get(this);
		Uri uri = locator.createBoardUri(data.boardName, 0);
		String responseText = new HttpRequest(uri, data.holder).setSuccessOnly(false).read().getString();
		if (data.holder.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND)
		{
			throw new ApiException(ApiException.SEND_ERROR_NO_BOARD);
		}
		data.holder.checkResponseCode();
		Matcher matcher = PATTERN_BOARD_ID.matcher(responseText);
		if (!matcher.find()) throw new InvalidResponseException();
		String boardId = matcher.group(1);
		matcher = PATTERN_TOKEN.matcher(responseText);
		String token = matcher.find() ? matcher.group(1) : null;
		MultipartEntity entity = new MultipartEntity();
		entity.add("id", data.threadNumber != null ? data.threadNumber : boardId);
		entity.add("token", token);
		entity.add("title", data.subject);
		boolean dem = "dem".equals(data.boardName);
		if (data.threadNumber == null && dem)
		{
			entity.add("title2", data.comment != null ? data.comment.replace('\n', ' ') : null);
		}
		else
		{
			String comment = data.comment;
			if (comment != null)
			{
				// Replace >quote with [quote]quote[/quote]
				String[] lines = comment.split("\n", -1);
				StringBuilder builder = new StringBuilder();
				boolean inQuote = false;
				for (String line : lines)
				{
					boolean quote = line.startsWith(">");
					if (!quote && inQuote)
					{
						inQuote = false;
						builder.append("[/quote]");
					}
					if (builder.length() > 0) builder.append('\n');
					if (quote)
					{
						int from = 1;
						if (line.length() >= 2 && line.charAt(1) == ' ') from = 2;
						line = line.substring(from);
						if (!inQuote)
						{
							inQuote = true;
							builder.append("[quote]");
						}
					}
					builder.append(line);
				}
				if (inQuote) builder.append("[/quote]");
				comment = builder.toString();
			}
			entity.add("text", comment);
		}
		if (!StringUtils.isEmpty(data.name))
		{
			if (data.threadNumber == null) entity.add("password", data.name);
			entity.add("show_tripcode", "1");
		}
		if (data.attachments != null) data.attachments[0].addToEntity(entity, "img");
		if (data.optionOriginalPoster) entity.add("show_op", "1");

		ExachChanConfiguration configuration = ExachChanConfiguration.get(this);
		String postOwnerCookie = configuration.getCookie(COOKIE_POST_OWNER);
		if (postOwnerCookie == null)
		{
			uri = locator.buildPath("b.php");
			new HttpRequest(uri, data.holder).read();
			postOwnerCookie = data.holder.getCookieValue(COOKIE_POST_OWNER);
			if (StringUtils.isEmpty(postOwnerCookie)) throw new InvalidResponseException();
			configuration.storeCookie(COOKIE_POST_OWNER, postOwnerCookie, "Post Owner");
		}
		uri = locator.buildPath("functions", data.threadNumber != null ? "new_post.php"
				: dem ? "new_dem.php" : "new_thread.php");
		responseText = new HttpRequest(uri, data).setPostMethod(entity).addCookie(COOKIE_POST_OWNER, postOwnerCookie)
				.setRedirectHandler(HttpRequest.RedirectHandler.NONE).setSuccessOnly(false).read().getString();
		if (data.holder.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP)
		{
			uri = data.holder.getRedirectedUri();
			String threadNumber = locator.getThreadNumber(uri);
			String postNumber = null;
			if (data.threadNumber != null)
			{
				postNumber = locator.getPostNumber(uri);
				if (StringUtils.isEmpty(postNumber)) throw new ApiException(ApiException.REPORT_ERROR_NO_ACCESS);
			}
			return new SendPostResult(threadNumber, postNumber);
		}
		if (data.holder.getResponseCode() != HttpURLConnection.HTTP_NOT_FOUND) data.holder.checkResponseCode();
		if (responseText == null) throw new InvalidResponseException();

		matcher = PATTERN_SEND_ERROR.matcher(responseText);
		if (!matcher.find()) throw new InvalidResponseException();
		String errorMessage = matcher.group(1);
		int errorType = 0;
		if (errorMessage.contains("Отсутствует текст"))
		{
			errorType = ApiException.SEND_ERROR_EMPTY_COMMENT;
		}
		else if (errorMessage.contains("Неудалось обработать изображение"))
		{
			errorType = ApiException.SEND_ERROR_EMPTY_FILE;
		}
		else if (errorMessage.contains("Ошибка 404 Not Found"))
		{
			errorType = ApiException.SEND_ERROR_NO_THREAD;
		}
		else if (errorMessage.contains("Превышение лимита добавления новых тем")
				|| errorMessage.contains("Повторяющийся текст"))
		{
			errorType = ApiException.SEND_ERROR_TOO_FAST;
		}
		else if (errorMessage.contains("Идентичное изображение"))
		{
			errorType = ApiException.SEND_ERROR_FILE_EXISTS;
		}
		else if (responseText.contains("ОП треда заблокировал вас")) // check responseText
		{
			errorType = ApiException.SEND_ERROR_NO_ACCESS;
		}
		if (errorType != 0) throw new ApiException(errorType);
		CommonUtils.writeLog("Exach send message", errorMessage);
		throw new ApiException(errorMessage);
	}

	private static final Pattern PATTERN_DELETE_ERROR = Pattern.compile("<p>(.*?)(?:<br>|</p>)");

	@Override
	public SendDeletePostsResult onSendDeletePosts(SendDeletePostsData data) throws HttpException, ApiException,
			InvalidResponseException
	{
		ExachChanLocator locator = ExachChanLocator.get(this);
		ExachChanConfiguration configuration = ExachChanConfiguration.get(this);
		String postOwnerCookie = configuration.getCookie(COOKIE_POST_OWNER);
		if (postOwnerCookie == null) postOwnerCookie = "null";
		String responseText;
		if (data.threadNumber.equals(data.postNumbers.get(0)))
		{
			Uri uri = locator.buildPath("functions", "move_thread.php");
			responseText = new HttpRequest(uri, data.holder).addCookie(COOKIE_POST_OWNER, postOwnerCookie)
					.setPostMethod(new UrlEncodedEntity("id", data.threadNumber, "to", "-1")).read().getString();
			if (StringUtils.isEmpty(responseText) || responseText.contains("Тред уже в том разделе"))
			{
				throw new ApiException(ApiException.DELETE_ERROR_NO_ACCESS);
			}
			if (responseText.contains("Тред перенесен в удаленные")) return null;
			CommonUtils.writeLog("Exach delete message", responseText);
			throw new ApiException(responseText);
		}
		else
		{
			Uri uri = locator.buildPath("functions", "delete_comment.php");
			responseText = new HttpRequest(uri, data.holder).addCookie(COOKIE_POST_OWNER, postOwnerCookie)
					.setPostMethod(new UrlEncodedEntity("id", data.postNumbers.get(0))).read().getString();
			if (StringUtils.isEmpty(responseText)) return null;
			Matcher matcher = PATTERN_DELETE_ERROR.matcher(responseText);
			if (!matcher.find()) throw new InvalidResponseException();
			String errorMessage = matcher.group(1);
			int errorType = 0;
			if (errorMessage.contains("либо уже удален, либо он вам не принадлежит")
					|| errorMessage.contains("не найден в базе данных"))
			{
				errorType = ApiException.DELETE_ERROR_NO_ACCESS;
			}
			if (errorType != 0) throw new ApiException(errorType);
			CommonUtils.writeLog("Exach delete message", errorMessage);
			throw new ApiException(errorMessage);
		}
	}
}