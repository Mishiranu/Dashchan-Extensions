package com.mishiranu.dashchan.chan.nowere;

import android.net.Uri;
import android.util.Pair;
import chan.content.InvalidResponseException;
import chan.content.WakabaChanPerformer;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.content.model.ThreadSummary;
import chan.http.HttpException;
import chan.http.HttpRequest;
import chan.text.ParseException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NowereChanPerformer extends WakabaChanPerformer {
	@Override
	protected List<Posts> parseThreads(String boardName, String responseText) throws ParseException {
		return new NowerePostsParser(responseText, this, boardName).convertThreads();
	}

	@Override
	protected List<Post> parsePosts(String boardName, String responseText) throws ParseException {
		return new NowerePostsParser(responseText, this, boardName).convertPosts();
	}

	@Override
	public ReadPostsResult onReadPosts(ReadPostsData data) throws HttpException, InvalidResponseException {
		NowereChanLocator locator = NowereChanLocator.get(this);
		Uri uri = locator.createThreadUri(data.boardName, data.threadNumber);
		List<Post> posts;
		boolean archived = false;
		try {
			posts = readPosts(data, uri, null);
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
		NowereChanLocator locator = NowereChanLocator.get(this);
		Uri uri = locator.buildPath("nav.html");
		String responseText = new HttpRequest(uri, data).read().getString();
		try {
			return new ReadBoardsResult(new NowereBoardsParser(responseText).convert());
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		}
	}

	private static final Pattern PATTERN_ARCHIVED_THREAD = Pattern.compile("<a href=\"(\\d+)/\">.*?" +
			"</a> *(.{15,}?)-");

	@SuppressWarnings("ComparatorCombinators")
	private static final Comparator<Pair<Integer, ThreadSummary>> ARCHIVE_COMPARATOR =
			(lhs, rhs) -> lhs.first - rhs.first;

	@Override
	public ReadThreadSummariesResult onReadThreadSummaries(ReadThreadSummariesData data) throws HttpException {
		NowereChanLocator locator = NowereChanLocator.get(this);
		Uri uri = locator.createBoardUri(data.boardName, 0).buildUpon().appendEncodedPath("arch").build();
		String responseText = new HttpRequest(uri, data).read().getString();
		ArrayList<Pair<Integer, ThreadSummary>> threadSummaries = new ArrayList<>();
		Matcher matcher = PATTERN_ARCHIVED_THREAD.matcher(responseText);
		while (matcher.find()) {
			String threadNumber = matcher.group(1);
			threadSummaries.add(new Pair<>(Integer.parseInt(threadNumber), new ThreadSummary(data.boardName,
					threadNumber, "#" + threadNumber + ", " + matcher.group(2).trim())));
		}
		if (threadSummaries.size() > 0) {
			Collections.sort(threadSummaries, ARCHIVE_COMPARATOR);
			ThreadSummary[] threadSummariesArray = new ThreadSummary[threadSummaries.size()];
			for (int i = 0; i < threadSummariesArray.length; i++) {
				threadSummariesArray[i] = threadSummaries.get(i).second;
			}
			return new ReadThreadSummariesResult(threadSummariesArray);
		}
		return null;
	}
}
