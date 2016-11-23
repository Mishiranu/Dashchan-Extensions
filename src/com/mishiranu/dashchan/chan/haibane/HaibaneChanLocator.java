package com.mishiranu.dashchan.chan.haibane;

import java.util.List;
import java.util.regex.Pattern;

import android.net.Uri;

import chan.content.ChanLocator;

public class HaibaneChanLocator extends ChanLocator {
	private static final Pattern BOARD_PATH = Pattern.compile("/\\w+(?:/(?:page/\\d+/?)?)?");
	private static final Pattern THREAD_PATH = Pattern.compile("/\\w+/(\\d+)/?");
	private static final Pattern ATTACHMENT_PATH = Pattern.compile("/upload/\\d+/[^/]+");

	public HaibaneChanLocator() {
		addChanHost("haibane.ru");
		addConvertableChanHost("www.haibane.ru");
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
				return segments.get(0);
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
			return null; // Ignore real chan-wide post numbers
		}
		return fragment;
	}

	@Override
	public Uri createBoardUri(String boardName, int pageNumber) {
		return pageNumber > 0 ? buildPath(boardName, "page", Integer.toString(pageNumber)) : buildPath(boardName);
	}

	@Override
	public Uri createThreadUri(String boardName, String threadNumber) {
		return buildPath(boardName, threadNumber);
	}

	@Override
	public Uri createPostUri(String boardName, String threadNumber, String postNumber) {
		return createThreadUri(boardName, threadNumber); // Post numbers are not supported
	}
}