package com.mishiranu.dashchan.chan.bunbunmaru;

import android.net.Uri;
import chan.content.ChanLocator;
import java.util.List;
import java.util.regex.Pattern;

public class BunbunmaruChanLocator extends ChanLocator {
	private static final Pattern BOARD_PATH = Pattern.compile("/wakaba/\\w+(?:/(?:(?:index|\\d+)\\.html)?)?");
	private static final Pattern THREAD_PATH = Pattern.compile("/wakaba/\\w+/res/(\\d+)\\.html");
	private static final Pattern ATTACHMENT_PATH = Pattern.compile("/wakaba/\\w+/src/\\d+(?:\\.\\d+)?\\.\\w+");

	public BunbunmaruChanLocator() {
		addChanHost("bunbunmaru.com");
		addConvertableChanHost("www.bunbunmaru.com");
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
		return isChanHostOrRelative(uri) && isPathMatches(uri, ATTACHMENT_PATH);
	}

	@Override
	public String getBoardName(Uri uri) {
		List<String> segments = uri.getPathSegments();
		if (segments.size() > 1 && "wakaba".equals(segments.get(0))) {
			return segments.get(1);
		}
		return null;
	}

	@Override
	public String getThreadNumber(Uri uri) {
		return getGroupValue(uri.getPath(), THREAD_PATH, 1);
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
		return pageNumber > 0 ? buildPath("wakaba", boardName, pageNumber + ".html")
				: buildPath("wakaba", boardName, "");
	}

	@Override
	public Uri createThreadUri(String boardName, String threadNumber) {
		return buildPath("wakaba", boardName, "res", threadNumber + ".html");
	}

	@Override
	public Uri createPostUri(String boardName, String threadNumber, String postNumber) {
		return createThreadUri(boardName, threadNumber).buildUpon().fragment(postNumber).build();
	}
}
