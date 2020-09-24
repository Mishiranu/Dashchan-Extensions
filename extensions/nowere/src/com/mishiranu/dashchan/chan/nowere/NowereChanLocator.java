package com.mishiranu.dashchan.chan.nowere;

import android.net.Uri;
import chan.content.ChanLocator;
import java.util.List;
import java.util.regex.Pattern;

public class NowereChanLocator extends ChanLocator {
	private static final Pattern BOARD_PATH = Pattern.compile("/\\w+(?:/(?:(?:index|\\d+)\\.html)?)?");
	private static final Pattern THREAD_PATH = Pattern.compile("/\\w+/res/(\\d+)\\.html");
	private static final Pattern THREAD_ARCHIVE_PATH = Pattern.compile("/\\w+/arch/(\\d+)/(?:wakaba.html)?");
	private static final Pattern ATTACHMENT_PATH = Pattern.compile("/\\w+/src/\\d+\\.\\w+");
	private static final Pattern ATTACHMENT_ARCHIVE_PATH = Pattern.compile("/\\w+/arch/(\\d+)/src/\\d+\\.\\w+");

	public NowereChanLocator() {
		addChanHost("nowere.net");
		addChanHost("sky.nowere.net");
		addConvertableChanHost("www.nowere.net");
		setHttpsMode(HttpsMode.CONFIGURABLE);
	}

	@Override
	public boolean isBoardUri(Uri uri) {
		return isChanHostOrRelative(uri) && isPathMatches(uri, BOARD_PATH);
	}

	@Override
	public boolean isThreadUri(Uri uri) {
		return isChanHostOrRelative(uri) && (isPathMatches(uri, THREAD_PATH)
				|| isPathMatches(uri, THREAD_ARCHIVE_PATH));
	}

	@Override
	public boolean isAttachmentUri(Uri uri) {
		return isChanHostOrRelative(uri) && (isPathMatches(uri, ATTACHMENT_PATH)
				|| isPathMatches(uri, ATTACHMENT_ARCHIVE_PATH));
	}

	@Override
	public String getBoardName(Uri uri) {
		List<String> segments = uri.getPathSegments();
		if (segments.size() > 0) {
			return segments.get(0);
		}
		return null;
	}

	@Override
	public String getThreadNumber(Uri uri) {
		String path = uri.getPath();
		String threadNumber = getGroupValue(path, THREAD_PATH, 1);
		if (threadNumber == null) {
			threadNumber = getGroupValue(path, THREAD_ARCHIVE_PATH, 1);
		}
		if (threadNumber == null) {
			threadNumber = getGroupValue(path, ATTACHMENT_ARCHIVE_PATH, 1);
		}
		return threadNumber;
	}

	@Override
	public String getPostNumber(Uri uri) {
		String fragment = uri.getFragment();
		if (fragment != null && fragment.startsWith("i")) {
			return fragment.substring(1);
		}
		return fragment;
	}

	@Override
	public Uri createBoardUri(String boardName, int pageNumber) {
		return pageNumber > 0 ? buildPath(boardName, pageNumber + ".html") : buildPath(boardName, "");
	}

	@Override
	public Uri createThreadUri(String boardName, String threadNumber) {
		return buildPath(boardName, "res", threadNumber + ".html");
	}

	public Uri createThreadArchiveUri(String boardName, String threadNumber) {
		return buildPath(boardName, "arch", threadNumber, "");
	}

	@Override
	public Uri createPostUri(String boardName, String threadNumber, String postNumber) {
		return createThreadUri(boardName, threadNumber).buildUpon().fragment(postNumber).build();
	}
}
