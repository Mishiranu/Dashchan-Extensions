package com.mishiranu.dashchan.chan.awoo;

import java.util.List;
import java.util.regex.Pattern;

import android.net.Uri;

import chan.content.ChanLocator;
import chan.util.StringUtils;

public class AwooLocator extends ChanLocator {
	private static final String HOST_BOARDS = "dangeru.us";
	private static final String HOST_POST = HOST_BOARDS;
	private static final String HOST_API = HOST_BOARDS;

	private static final Pattern BOARD_PATH = Pattern.compile("/\\w+(?:/(?:\\d+|catalog)?)?");
	private static final Pattern THREAD_PATH = Pattern.compile("/\\w+/thread/(\\d+)(?:/.*)?");

	public AwooLocator() {
		addChanHost(HOST_BOARDS);
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
		return pageNumber > 0 ? buildPathWithHost(HOST_BOARDS, boardName, (pageNumber + 1) + ".html")
				: buildPathWithHost(HOST_BOARDS, boardName, "");
	}

	@Override
	public Uri createThreadUri(String boardName, String threadNumber) {
		return buildPathWithHost(HOST_BOARDS, boardName, "thread", threadNumber);
	}

	@Override
	public Uri createPostUri(String boardName, String threadNumber, String postNumber) {
		return createThreadUri(boardName, threadNumber).buildUpon().fragment("p" + postNumber).build();
	}

	public Uri createApiUri(String... segments) {
		String[] realsegments = new String[segments.length + 2];
		realsegments[0] = "api";
        realsegments[1] = "v2";
		System.arraycopy(segments, 0, realsegments, 2, segments.length);
		return buildPathWithHost(HOST_API, realsegments);
	}

	public Uri createSysUri(String... segments) {
		return buildPathWithSchemeHost(true, HOST_POST, segments);
	}

	@Override
	public NavigationData handleUriClickSpecial(Uri uri) {
        /*
		if (isBoardUriOrSearch(uri)) {
			String query = uri.getFragment();
			if (!StringUtils.isEmpty(query) && query.startsWith("s=")) {
				return new NavigationData(NavigationData.TARGET_SEARCH, getBoardName(uri),
						null, null, query.substring(2));
			}
		}
		*/
		return null;
	}
}