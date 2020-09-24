package com.mishiranu.dashchan.chan.cirno;

import android.net.Uri;
import android.util.Pair;
import chan.content.ApiException;
import chan.content.InvalidResponseException;
import chan.content.RedirectException;
import chan.content.WakabaChanPerformer;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.content.model.ThreadSummary;
import chan.http.HttpException;
import chan.http.HttpHolder;
import chan.http.HttpRequest;
import chan.http.RequestEntity;
import chan.text.ParseException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CirnoChanPerformer extends WakabaChanPerformer {
	private static final Pattern PATTERN_REDIRECT = Pattern.compile("<meta http-equiv=\"Refresh\" " +
			"content=\"0; ?url=(.*?)\" />");

	private static void checkThreadsRedirect(String responseText) throws RedirectException {
		Matcher matcher = PATTERN_REDIRECT.matcher(responseText);
		if (matcher.find()) {
			throw RedirectException.toUri(Uri.parse(matcher.group(1)));
		}
	}

	@Override
	protected List<Posts> parseThreads(String boardName, String responseText)
			throws ParseException, RedirectException {
		checkThreadsRedirect(responseText);
		return new CirnoPostsParser(responseText, this, boardName).convertThreads();
	}

	@Override
	public ReadThreadsResult onReadThreads(ReadThreadsData data) throws HttpException, InvalidResponseException,
			RedirectException {
		if (data.isCatalog()) {
			CirnoChanLocator locator = CirnoChanLocator.get(this);
			Uri uri = locator.buildPath(data.boardName, "catalogue.html");
			String responseText = new HttpRequest(uri, data).setValidator(data.validator).read().getString();
			checkThreadsRedirect(responseText);
			try {
				return new ReadThreadsResult(new CirnoCatalogParser(responseText, this).convert());
			} catch (ParseException e) {
				throw new InvalidResponseException(e);
			}
		} else {
			return super.onReadThreads(data);
		}
	}

	private static class ArchiveRedirectHandler implements HttpRequest.RedirectHandler {
		public boolean archived = false;

		@Override
		public Action onRedirectReached(int responseCode, Uri requestedUri, Uri redirectedUri, HttpHolder holder)
				throws HttpException {
			String path = redirectedUri.getPath();
			if (path != null && path.contains("/arch/")) {
				archived = true;
			}
			return BROWSER.onRedirectReached(responseCode, requestedUri, redirectedUri, holder);
		}
	}

	@Override
	protected List<Post> parsePosts(String boardName, String responseText) throws ParseException {
		return new CirnoPostsParser(responseText, this, boardName).convertPosts();
	}

	@Override
	public ReadPostsResult onReadPosts(ReadPostsData data) throws HttpException, InvalidResponseException {
		CirnoChanLocator locator = CirnoChanLocator.get(this);
		Uri uri = locator.createThreadUri(data.boardName, data.threadNumber);
		List<Post> posts;
		boolean archived;
		try {
			ArchiveRedirectHandler redirectHandler = new ArchiveRedirectHandler();
			posts = readPosts(data, uri, redirectHandler);
			archived = redirectHandler.archived;
		} catch (HttpException e) {
			if (e.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
				uri = locator.createThreadArchiveUri(data.boardName, data.threadNumber);
				posts = readPosts(data, uri, null);
				archived = true;
			} else {
				throw e;
			}
		}
		if (archived) {
			posts.get(0).setArchived(true);
		}
		return new ReadPostsResult(posts);
	}

	@Override
	public ReadBoardsResult onReadBoards(ReadBoardsData data) throws HttpException, InvalidResponseException {
		CirnoChanLocator locator = CirnoChanLocator.get(this);
		Uri uri = locator.buildPath("n", "list_ru_m.html");
		String responseText = new HttpRequest(uri, data).read().getString();
		try {
			return new ReadBoardsResult(new CirnoBoardsParser(responseText).convert());
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		}
	}

	private static final Pattern PATTERN_ARCHIVED_THREAD = Pattern.compile("<a href=\"(\\d+).html\">.*?" +
			"<td align=\"right\">(.{15,}?)</td>");

	@Override
	public ReadThreadSummariesResult onReadThreadSummaries(ReadThreadSummariesData data) throws HttpException {
		CirnoChanLocator locator = CirnoChanLocator.get(this);
		Uri uri = locator.createBoardUri(data.boardName, 0).buildUpon().appendEncodedPath("arch/res").build();
		String responseText = new HttpRequest(uri, data).read().getString();
		ArrayList<ThreadSummary> threadSummaries = new ArrayList<>();
		Matcher matcher = PATTERN_ARCHIVED_THREAD.matcher(responseText);
		while (matcher.find()) {
			threadSummaries.add(new ThreadSummary(data.boardName, matcher.group(1), "#" + matcher.group(1) + ", "
					+ matcher.group(2).trim()));
		}
		return new ReadThreadSummariesResult(threadSummaries);
	}

	@Override
	public ReadPostsCountResult onReadPostsCount(ReadPostsCountData data) throws HttpException,
			InvalidResponseException {
		CirnoChanLocator locator = CirnoChanLocator.get(this);
		Uri uri = locator.createThreadUri(data.boardName, data.threadNumber);
		ArchiveRedirectHandler redirectHandler = new ArchiveRedirectHandler();
		String responseText = new HttpRequest(uri, data).setValidator(data.validator)
				.setRedirectHandler(redirectHandler).read().getString();
		if (redirectHandler.archived) {
			throw HttpException.createNotFoundException();
		}
		return createPostsCountResult(responseText);
	}

	public ReadCaptchaResult onReadCaptcha(ReadCaptchaData data) throws HttpException, InvalidResponseException {
		String script = "a".equals(data.boardName) || "b".equals(data.boardName) ? "captcha1.pl" : "captcha.pl";
		return readCaptchaScript(data, script);
	}

	@Override
	public SendPostResult onSendPost(SendPostData data) throws HttpException, ApiException, InvalidResponseException {
		RequestEntity entity = createSendPostEntity(data, field ->
				field.startsWith("field") ? "nya" + field.substring(5) : field);
		entity.add("postredir", "1");
		Pair<String, Uri> response = executeWakaba(data.boardName, entity, data.holder, data);
		if (response.first == null) {
			if (response.second != null) {
				CirnoChanLocator locator = CirnoChanLocator.get(this);
				String threadNumber = locator.getThreadNumber(response.second);
				String postNumber = locator.getPostNumber(response.second);
				if (threadNumber.equals(postNumber)) {
					postNumber = null;
				}
				return new SendPostResult(threadNumber, postNumber);
			}
			return null;
		}
		handleError(ErrorSource.POST, response.first);
		throw new InvalidResponseException();
	}
}
