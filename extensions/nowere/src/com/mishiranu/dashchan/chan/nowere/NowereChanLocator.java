package com.mishiranu.dashchan.chan.nowere;

import android.net.Uri;
import chan.content.WakabaChanLocator;
import java.util.List;
import java.util.regex.Pattern;

public class NowereChanLocator extends WakabaChanLocator {
	private static final Pattern THREAD_ARCHIVE_PATH = Pattern.compile("/\\w+/arch/(\\d+)/(?:wakaba.html)?");

	public NowereChanLocator() {
		boardPath = Pattern.compile("/\\w+(?:/(?:(?:index|\\d+)\\.html)?)?");
		threadPath = Pattern.compile("/\\w+/res/(\\d+)\\.html");
		attachmentPath = Pattern.compile("/\\w+/(?:arch/(\\d+)/)?src/\\d+\\.\\w+");
		addChanHost("nowere.net");
		addChanHost("sky.nowere.net");
		addConvertableChanHost("www.nowere.net");
		setHttpsMode(HttpsMode.CONFIGURABLE);
	}

	@Override
	public boolean isThreadUri(Uri uri) {
		if (super.isThreadUri(uri)) {
			return true;
		}
		return isChanHostOrRelative(uri) && isPathMatches(uri, THREAD_ARCHIVE_PATH);
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
		String threadNumber = super.getThreadNumber(uri);
		if (threadNumber != null) {
			return threadNumber;
		}
		String path = uri.getPath();
		threadNumber = getGroupValue(path, THREAD_ARCHIVE_PATH, 1);
		if (threadNumber != null) {
			return threadNumber;
		}
		return getGroupValue(path, attachmentPath, 1);
	}

	public Uri createThreadArchiveUri(String boardName, String threadNumber) {
		return buildPath(boardName, "arch", threadNumber, "");
	}
}
