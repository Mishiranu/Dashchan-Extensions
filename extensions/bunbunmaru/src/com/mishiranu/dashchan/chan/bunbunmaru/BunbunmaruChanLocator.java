package com.mishiranu.dashchan.chan.bunbunmaru;

import android.net.Uri;
import chan.content.WakabaChanLocator;
import java.util.List;
import java.util.regex.Pattern;

public class BunbunmaruChanLocator extends WakabaChanLocator {
	public BunbunmaruChanLocator() {
		boardPath = Pattern.compile("/wakaba/\\w+(?:/(?:(?:index|\\d+)\\.html)?)?");
		threadPath = Pattern.compile("/wakaba/\\w+/res/(\\d+)\\.html");
		attachmentPath = Pattern.compile("/wakaba/\\w+/src/\\d+(?:\\.\\d+)?\\.\\w+");
		addChanHost("bunbunmaru.com");
		addConvertableChanHost("www.bunbunmaru.com");
		setHttpsMode(HttpsMode.HTTPS_ONLY);
	}

	@Override
	public String getBoardName(Uri uri) {
		List<String> segments = uri.getPathSegments();
		if (segments.size() >= 2 && "wakaba".equals(segments.get(0))) {
			return segments.get(1);
		}
		return null;
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
	public Uri createScriptUri(String boardName, String script) {
		return buildPath("wakaba", boardName, script);
	}
}
