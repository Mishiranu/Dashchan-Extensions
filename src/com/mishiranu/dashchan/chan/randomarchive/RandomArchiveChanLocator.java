package com.mishiranu.dashchan.chan.randomarchive;

import java.util.List;
import java.util.regex.Pattern;

import android.net.Uri;

import chan.content.ChanLocator;

public class RandomArchiveChanLocator extends ChanLocator {
	private static final Pattern BOARD_PATH = Pattern.compile("/board/\\w+(?:/(?:\\d+/?)?)?");
	private static final Pattern THREAD_PATH = Pattern.compile("/board/\\w+/thread/(\\d+)/?");
	private static final Pattern ATTACHMENT_PATH = Pattern.compile("/data/\\w+/\\d+/\\d+/\\d+\\.\\w+");

	public RandomArchiveChanLocator() {
		addChanHost("randomarchive.com");
		addConvertableChanHost("www.randomarchive.com");
		setHttpsMode(HttpsMode.CONFIGURABLE);
	}

	@Override
	public boolean isBoardUri(Uri uri) {
		return isChanHostOrRelative(uri) && isPathMatches(uri, BOARD_PATH);
	}

	@Override
	public boolean isThreadUri(Uri uri) {
		return isChanHostOrRelative(uri) && isPathMatches(uri, THREAD_PATH);
	}

	@Override
	public boolean isAttachmentUri(Uri uri) {
		return isChanHostOrRelative(uri) && isPathMatches(uri, ATTACHMENT_PATH) || "i.imgur.com".equals(uri.getHost());
	}

	@Override
	public String getBoardName(Uri uri) {
		List<String> segments = uri.getPathSegments();
		if (segments.size() >= 2) {
			String firstSegment = segments.get(0);
			if ("board".equals(firstSegment) || "data".equals(firstSegment)) {
				return segments.get(1);
			}
		}
		return null;
	}

	@Override
	public String getThreadNumber(Uri uri) {
		return uri != null ? getGroupValue(uri.getPath(), THREAD_PATH, 1) : null;
	}

	@Override
	public String getPostNumber(Uri uri) {
		String fragment = uri.getFragment();
		if (fragment != null && fragment.startsWith("p")) {
			return fragment.substring(1);
		}
		return fragment;
	}

	@Override
	public Uri createBoardUri(String boardName, int pageNumber) {
		return pageNumber > 0 ? buildPath("board", boardName, Integer.toString(pageNumber + 1), "")
				: buildPath("board", boardName);
	}

	@Override
	public Uri createThreadUri(String boardName, String threadNumber) {
		return buildPath("board", boardName, "thread", threadNumber);
	}

	@Override
	public Uri createPostUri(String boardName, String threadNumber, String postNumber) {
		return createThreadUri(boardName, threadNumber).buildUpon().fragment("p" + postNumber).build();
	}
}