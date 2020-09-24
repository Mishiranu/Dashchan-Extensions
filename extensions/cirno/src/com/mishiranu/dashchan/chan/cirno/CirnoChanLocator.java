package com.mishiranu.dashchan.chan.cirno;

import android.net.Uri;
import chan.content.WakabaChanLocator;
import java.util.regex.Pattern;

public class CirnoChanLocator extends WakabaChanLocator {
	public CirnoChanLocator() {
		boardPath = Pattern.compile("/\\w+(?:/(?:(?:index|catalogue|\\d+)\\.html)?)?");
		threadPath = Pattern.compile("/\\w+/(?:arch/)?res/(\\d+)\\.html");
		attachmentPath = Pattern.compile("/\\w+/(?:arch/)?src/\\d+\\.\\w+");
		addChanHost("iichan.hk");
		addChanHost("n.iichan.hk");
		addChanHost("on.iichan.hk");
		addConvertableChanHost("iichan.moe");
		addConvertableChanHost("closed.iichan.hk");
		setHttpsMode(HttpsMode.CONFIGURABLE);
	}

	public Uri createThreadArchiveUri(String boardName, String threadNumber) {
		return buildPath(boardName, "arch", "res", threadNumber + ".html");
	}

	@Override
	public Uri createScriptUri(String boardName, String script) {
		return buildPath("cgi-bin", script, boardName, script.startsWith("captcha") ? "" : null);
	}
}
