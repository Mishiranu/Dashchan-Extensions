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
import chan.http.HttpRequest;
import chan.http.HttpResponse;
import chan.http.RequestEntity;
import chan.text.ParseException;
import chan.util.StringUtils;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CirnoChanPerformer extends WakabaChanPerformer {
	@Override
	protected List<Posts> parseThreads(String boardName, InputStream input)
			throws IOException, ParseException, InvalidResponseException, RedirectException {
		try {
			return new CirnoPostsParser(this, boardName).convertThreads(input);
		} catch (CirnoPostsParser.RedirectException e) {
			String content = e.content;
			int index = content.indexOf("url=");
			if (index >= 0) {
				throw RedirectException.toUri(Uri.parse(content.substring(index + 4)));
			} else {
				throw new InvalidResponseException(e);
			}
		}
	}

	@Override
	public ReadThreadsResult onReadThreads(ReadThreadsData data) throws HttpException, InvalidResponseException,
			RedirectException {
		if (data.isCatalog()) {
			CirnoChanLocator locator = CirnoChanLocator.get(this);
			Uri uri = locator.buildPath(data.boardName, "catalogue.html");
			HttpResponse response = new HttpRequest(uri, data).setValidator(data.validator)
					.setSuccessOnly(false).perform();
			if (response.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
				// Check for redirect
				ReadThreadsResult result = super.onReadThreads(data);
				if (result != null) {
					throw new InvalidResponseException();
				}
			} else {
				response.checkResponseCode();
			}
			try (InputStream input = response.open()) {
				return new ReadThreadsResult(new CirnoCatalogParser(this).convert(input));
			} catch (ParseException e) {
				throw new InvalidResponseException(e);
			} catch (IOException e) {
				throw response.fail(e);
			}
		} else {
			return super.onReadThreads(data);
		}
	}

	private static class ArchiveRedirectHandler implements HttpRequest.RedirectHandler {
		public boolean archived = false;

		@Override
		public Action onRedirect(HttpResponse response) throws HttpException {
			String path = response.getRedirectedUri().getPath();
			if (path != null && path.contains("/arch/")) {
				archived = true;
			}
			return BROWSER.onRedirect(response);
		}
	}

	@Override
	protected List<Post> parsePosts(String boardName, InputStream input) throws IOException, ParseException {
		return new CirnoPostsParser(this, boardName).convertPosts(input);
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
		String responseText = new HttpRequest(uri, data).perform().readString();
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
		String responseText = new HttpRequest(uri, data).perform().readString();
		ArrayList<ThreadSummary> threadSummaries = new ArrayList<>();
		Matcher matcher = PATTERN_ARCHIVED_THREAD.matcher(responseText);
		while (matcher.find()) {
			threadSummaries.add(new ThreadSummary(data.boardName, matcher.group(1), "#" + matcher.group(1) + ", "
					+ StringUtils.emptyIfNull(matcher.group(2)).trim()));
		}
		return new ReadThreadSummariesResult(threadSummaries);
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
		Pair<HttpResponse, Uri> response = executeWakaba(data.boardName, entity, data);
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
		handleError(ErrorSource.POST, response.first.readString());
		throw new InvalidResponseException();
	}
}
