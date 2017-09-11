package com.mishiranu.dashchan.chan.awoo;

import android.net.Uri;

import java.util.List;
import java.util.regex.Pattern;

import chan.content.ChanLocator;
import chan.util.StringUtils;

public class AwooLocator extends ChanLocator {
	private static final Pattern BOARD_PATH = Pattern.compile("/\\w+(?:/(?:\\d+|catalog)?)?");
	private static final Pattern THREAD_PATH = Pattern.compile("/\\w+/thread/(\\d+)(?:/.*)?");

	public AwooLocator() {
		addChanHost("dangeru.us");
		setHttpsMode(HttpsMode.HTTPS_ONLY);
	}

	@Override
	public boolean isBoardUri(Uri uri) {
		return isBoardUriOrSearch(uri) && StringUtils.isEmpty(uri.getFragment());
	}

	public boolean isBoardUriOrSearch(Uri uri) {
		return isChanHostOrRelative(uri) && isPathMatches(uri, BOARD_PATH);
	}

	@Override
	public boolean isThreadUri(Uri uri) {
		return isChanHostOrRelative(uri) && isPathMatches(uri, THREAD_PATH);
	}

	@Override
	public boolean isAttachmentUri(Uri uri) {
		return false;
	}

	@Override
	public String getBoardName(Uri uri) {
		List<String> segments = uri.getPathSegments();
		if (!segments.isEmpty()) {
			return segments.get(0);
		}
		return null;
	}

	@Override
	public String getThreadNumber(Uri uri) {
		// TODO
		return getGroupValue(uri.getPath(), THREAD_PATH, 1);
	}

	@Override
	public String getPostNumber(Uri uri) {
		// TODO
		String fragment = uri.getFragment();
		if (fragment != null && fragment.startsWith("p")) {
			fragment = fragment.substring(1);
		}
		return fragment;
	}

	@Override
	public Uri createBoardUri(String boardName, int pageNumber) {
		return pageNumber > 0 ? buildPath(boardName, (pageNumber + 1) + ".html") : buildPath(boardName, "");
	}

	@Override
	public Uri createThreadUri(String boardName, String threadNumber) {
		// probably wrong
		return buildPath(boardName, "thread", threadNumber);
	}

	@Override
	public Uri createPostUri(String boardName, String threadNumber, String postNumber) {
		// probably wrong
		return createThreadUri(boardName, threadNumber).buildUpon().fragment("p" + postNumber).build();
	}
}
