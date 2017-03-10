package com.mishiranu.dashchan.chan.horochan;

import java.util.List;
import java.util.regex.Pattern;

import android.net.Uri;

import chan.content.ChanLocator;

public class HorochanChanLocator extends ChanLocator {
	private static final Pattern BOARD_PATH = Pattern.compile("/\\w+(?:/(?:\\d+))?");
	private static final Pattern THREAD_PATH = Pattern.compile("/\\w+/thread/(\\d+)/?");
	private static final Pattern ATTACHMENT_PATH = Pattern.compile("/src/\\d+\\.\\w+");

	public HorochanChanLocator() {
		addChanHost("horochan.ru");
		addSpecialChanHost("api.horochan.ru");
		addSpecialChanHost("static.horochan.ru");
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
		return isChanHostOrRelative(uri) && isPathMatches(uri, ATTACHMENT_PATH);
	}

	@Override
	public String getBoardName(Uri uri) {
		if (uri != null) {
			List<String> segments = uri.getPathSegments();
			if (segments.size() > 0) {
				String segment = segments.get(0);
				if ("data".equals(segment) || "api".equals(segment) || "src".equals(segment)
						|| "thumb".equals(segment)) {
					return null;
				}
				return segment;
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
		if (fragment != null && (fragment.startsWith("i") || fragment.startsWith("p"))) {
			fragment = fragment.substring(1);
		}
		return fragment;
	}

	@Override
	public Uri createBoardUri(String boardName, int pageNumber) {
		return pageNumber > 0 ? buildPath(boardName, Integer.toString(pageNumber + 1)) : buildPath(boardName);
	}

	@Override
	public Uri createThreadUri(String boardName, String threadNumber) {
		return buildPath(boardName, "thread", threadNumber);
	}

	@Override
	public Uri createPostUri(String boardName, String threadNumber, String postNumber) {
		return createThreadUri(boardName, threadNumber).buildUpon().fragment(postNumber).build();
	}

	public Uri buildStaticPath(String... segments) {
		return buildPathWithHost("static.horochan.ru", segments);
	}

	public Uri buildApiPath(String... segments) {
		return buildPathWithHost("api.horochan.ru", segments);
	}
}