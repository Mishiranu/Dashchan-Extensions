package com.mishiranu.dashchan.chan.archiverbt;

import android.net.Uri;
import chan.content.ChanPerformer;
import chan.content.InvalidResponseException;
import chan.content.RedirectException;
import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.content.model.Posts;
import chan.http.HttpException;
import chan.http.HttpRequest;
import chan.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArchiveRbtChanPerformer extends ChanPerformer {
	@Override
	public ReadThreadsResult onReadThreads(ReadThreadsData data) throws HttpException, InvalidResponseException {
		ArchiveRbtChanLocator locator = ArchiveRbtChanLocator.get(this);
		Uri uri = locator.createBoardUri(data.boardName, data.pageNumber);
		String responseText = new HttpRequest(uri, data).setValidator(data.validator).read().getString();
		try {
			ArrayList<Posts> threads = new ArchiveRbtPostsParser(responseText, this).convertThreads();
			checkResponseAsNotFound(threads, responseText);
			return new ReadThreadsResult(threads);
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		}
	}

	@Override
	public ReadPostsResult onReadPosts(ReadPostsData data) throws HttpException, InvalidResponseException,
			RedirectException {
		final ArchiveRbtChanLocator locator = ArchiveRbtChanLocator.get(this);
		// Check post->thread redirect if thread opened first time
		Uri uri = data.cachedPosts != null ? locator.createThreadUri(data.boardName, data.threadNumber)
				: locator.buildPath(data.boardName, "post", data.threadNumber);
		String responseText;
		try {
			final String[] realThreadNumber = new String[] {null, null};
			new HttpRequest(uri, data).setValidator(data.validator)
					.setRedirectHandler((responseCode, requestedUri, redirectedUri, holder) -> {
				if (locator.isThreadUri(redirectedUri)) {
					realThreadNumber[0] = locator.getThreadNumber(redirectedUri);
					realThreadNumber[1] = locator.getPostNumber(redirectedUri);
				}
				return HttpRequest.RedirectHandler.BROWSER.onRedirectReached(responseCode, requestedUri,
						redirectedUri, holder);
			}).execute();
			if (realThreadNumber[0] != null && !data.threadNumber.equals(realThreadNumber[0])) {
				throw RedirectException.toThread(data.boardName, realThreadNumber[0], realThreadNumber[1]);
			}
			data.holder.checkResponseCode();
			responseText = data.holder.read().getString();
		} finally {
			data.holder.disconnect();
		}
		try {
			Uri threadUri = locator.buildPathWithHost("boards.4chan.org", data.boardName, "thread", data.threadNumber);
			Posts posts = new ArchiveRbtPostsParser(responseText, this).convertPosts(threadUri);
			checkResponseAsNotFound(posts, responseText);
			return new ReadPostsResult(posts);
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		}
	}

	private void checkResponseAsNotFound(Object model, String responseText) throws HttpException {
		if (model == null && responseText != null && responseText.length() > 1) {
			throw HttpException.createNotFoundException();
		}
	}

	@Override
	public ReadSearchPostsResult onReadSearchPosts(ReadSearchPostsData data) throws HttpException,
			InvalidResponseException {
		ArchiveRbtChanLocator locator = ArchiveRbtChanLocator.get(this);
		Uri uri = locator.buildQuery(data.boardName + "/", "task", "search", "search_text", data.searchQuery,
				"offset", Integer.toString(24 * data.pageNumber));
		String responseText = new HttpRequest(uri, data).read().getString();
		try {
			return new ReadSearchPostsResult(new ArchiveRbtPostsParser(responseText, this).convertSearch());
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		}
	}

	private static final HashMap<String, String> DEFAULT_BOARD_TITLES;

	static {
		DEFAULT_BOARD_TITLES = new HashMap<>();
		DEFAULT_BOARD_TITLES.put("cgl", "Cosplay & EGL");
		DEFAULT_BOARD_TITLES.put("con", "Conventions");
		DEFAULT_BOARD_TITLES.put("g", "Technology");
		DEFAULT_BOARD_TITLES.put("mu", "Music");
		DEFAULT_BOARD_TITLES.put("qa", "Question & Answer");
		DEFAULT_BOARD_TITLES.put("w", "Anime/Wallpapers");
	}

	private static final Pattern PATTERN_BOARD = Pattern.compile("<a href=\"/(.*?)/\"");

	@Override
	public ReadBoardsResult onReadBoards(ReadBoardsData data) throws HttpException, InvalidResponseException {
		ArchiveRbtChanLocator locator = ArchiveRbtChanLocator.get(this);
		Uri uri = locator.buildPath();
		String responseText = new HttpRequest(uri, data).read().getString();
		ArrayList<Board> boards = new ArrayList<>();
		Matcher matcher = PATTERN_BOARD.matcher(responseText);
		while (matcher.find()) {
			String boardName = matcher.group(1);
			String title = DEFAULT_BOARD_TITLES.get(boardName);
			boards.add(new Board(boardName, title != null ? title : "Untitled"));
		}
		return boards.size() > 0 ? new ReadBoardsResult(new BoardCategory("Archives", boards)) : null;
	}
}
