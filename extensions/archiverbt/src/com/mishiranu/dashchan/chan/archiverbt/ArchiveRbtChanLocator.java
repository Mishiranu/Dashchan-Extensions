package com.mishiranu.dashchan.chan.archiverbt;

import android.net.Uri;
import chan.content.ChanLocator;
import chan.util.StringUtils;
import java.util.List;
import java.util.regex.Pattern;

public class ArchiveRbtChanLocator extends ChanLocator {
	private static final Pattern BOARD_PATH = Pattern.compile("/\\w+/?");
	private static final Pattern THREAD_PATH = Pattern.compile("/\\w+/(?:thread|post)/(\\d+)/?");
	private static final Pattern ATTACHMENT_PATH = Pattern.compile("/boards/\\w+/img/\\d+/\\d+/\\d+\\.\\w+");

	public ArchiveRbtChanLocator() {
		addChanHost("rbt.asia");
		addChanHost("archive.rebeccablacktech.com");
		addConvertableChanHost("www.rbt.asia");
		setHttpsMode(HttpsMode.CONFIGURABLE);
	}

	@Override
	public boolean isBoardUri(Uri uri) {
		if (isChanHostOrRelative(uri) && isPathMatches(uri, BOARD_PATH)) {
			String task = uri.getQueryParameter("task");
			return StringUtils.isEmpty(task) || "page".equals(task);
		}
		return false;
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
				String boardName = segments.get(0);
				if ("boards".equals(boardName) && segments.size() > 1) {
					return segments.get(1);
				}
				return boardName;
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
			fragment = fragment.substring(1).replace('_', '.');
		}
		return fragment;
	}

	@Override
	public Uri createBoardUri(String boardName, int pageNumber) {
		return pageNumber > 0 ? buildQuery(boardName + "/", "task", "page", "page", Integer.toString(pageNumber + 1))
				: buildPath(boardName, "");
	}

	@Override
	public Uri createThreadUri(String boardName, String threadNumber) {
		return buildPath(boardName, "thread", threadNumber);
	}

	@Override
	public Uri createPostUri(String boardName, String threadNumber, String postNumber) {
		return createThreadUri(boardName, threadNumber).buildUpon().fragment(postNumber.replace('.', '_')).build();
	}
}
