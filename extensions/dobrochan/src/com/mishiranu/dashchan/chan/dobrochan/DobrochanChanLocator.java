package com.mishiranu.dashchan.chan.dobrochan;

import android.net.Uri;
import chan.content.ChanLocator;
import java.util.List;
import java.util.regex.Pattern;

public class DobrochanChanLocator extends ChanLocator {
	private static final Pattern BOARD_PATH = Pattern.compile("/\\w+(?:/(?:(?:index|\\d+)\\.xhtml)?)?");
	private static final Pattern THREAD_PATH = Pattern.compile("/\\w+/res/\\d+\\.xhtml");
	private static final Pattern ATTACHMENT_PATH = Pattern.compile("/src/\\w+/\\d+/.+\\.\\w+");

	private static final Pattern THREAD_NUMBER = Pattern.compile("^/\\w+/res/(\\d+)\\.xhtml");

	public DobrochanChanLocator() {
		addChanHost("dobrochan.com");
		addChanHost("dobrochan.org");
		addChanHost("dobrochan.ru");
		addConvertableChanHost("www.dobrochan.com");
		addConvertableChanHost("www.dobrochan.org");
		addConvertableChanHost("www.dobrochan.ru");
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
				if ("src".equals(segment) || "thumb".equals(segment) || "api".equals(segment)) {
					return null;
				}
				return segment;
			}
		}
		return null;
	}

	@Override
	public String getThreadNumber(Uri uri) {
		return uri != null ? getGroupValue(uri.getPath(), THREAD_NUMBER, 1) : null;
	}

	@Override
	public String getPostNumber(Uri uri) {
		String fragment = uri.getFragment();
		if (fragment != null && fragment.startsWith("i")) {
			fragment = fragment.substring(1);
		}
		return fragment;
	}

	@Override
	public Uri createBoardUri(String boardName, int pageNumber) {
		return buildPath(boardName, (pageNumber > 0 ? pageNumber : "index") + ".xhtml");
	}

	@Override
	public Uri createThreadUri(String boardName, String threadNumber) {
		return buildPath(boardName, "res", threadNumber + ".xhtml");
	}

	@Override
	public Uri createPostUri(String boardName, String threadNumber, String postNumber) {
		return createThreadUri(boardName, threadNumber).buildUpon().fragment("i" + postNumber).build();
	}

	public Uri createApiUri(String name, String boardName, String path, String... params) {
		Uri.Builder builder = buildPath("api", name, boardName, path).buildUpon();
		for (int i = 0; i < params.length; i += 2) {
			builder.appendQueryParameter(params[i], params[i + 1]);
		}
		return builder.build();
	}
}
