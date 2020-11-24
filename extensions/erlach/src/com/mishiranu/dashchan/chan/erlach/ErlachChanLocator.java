package com.mishiranu.dashchan.chan.erlach;

import android.net.Uri;
import chan.content.ChanLocator;
import chan.util.StringUtils;
import java.util.List;
import java.util.regex.Pattern;

public class ErlachChanLocator extends ChanLocator {
	private static final Pattern BOARD_PATH = Pattern.compile("/[^/]+/?");
	private static final Pattern THREAD_PATH = Pattern.compile("/[^/]+/\\w+(?:/\\w+)?/?");
	private static final Pattern ATTACHMENT_PATH = Pattern.compile("/static/attachments/[^/]+/[^/]+/[^/]+\\.\\w+");

	public ErlachChanLocator() {
		addChanHost("erlach.co");
		addConvertableChanHost("www.erlach.co");
		setHttpsMode(HttpsMode.HTTPS_ONLY);
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
		String boardName =  segments.size() > 0 ? segments.get(0) : null;
		if ("static".equals(boardName)) {
			return null;
		}
		return boardName;
	}

	@Override
	public String getThreadNumber(Uri uri) {
		List<String> segments = uri.getPathSegments();
		return segments.size() > 1 ? convertToDecimalNumber(segments.get(1)) : null;
	}

	@Override
	public String getPostNumber(Uri uri) {
		List<String> segments = uri.getPathSegments();
		return segments.size() > 2 ? convertToDecimalNumber(segments.get(2)) : null;
	}

	@Override
	public Uri createBoardUri(String boardName, int pageNumber) {
		return buildPath(boardName);
	}

	@Override
	public Uri createThreadUri(String boardName, String threadNumber) {
		return buildPath(boardName, convertToPresentNumber(threadNumber));
	}

	@Override
	public Uri createPostUri(String boardName, String threadNumber, String postNumber) {
		return buildPath(boardName, convertToPresentNumber(threadNumber), convertToPresentNumber(postNumber));
	}

	private static final String NUMBER_ARRAY = "0123456789abcdefghijklmnopqrstuvwxyz";

	public String convertToDecimalNumber(String number) {
		if (!StringUtils.isEmpty(number)) {
			int result = 0;
			for (int i = number.length() - 1, m = 1; i >= 0; i--, m *= NUMBER_ARRAY.length()) {
				result += NUMBER_ARRAY.indexOf(number.charAt(i)) * m;
			}
			number = Integer.toString(result);
		}
		return number;
	}

	public String convertToPresentNumber(String number) {
		if (!StringUtils.isEmpty(number)) {
			int integer;
			try {
				integer = Integer.parseInt(number);
				if (integer < 0) return null;
			} catch (NumberFormatException e) {
				return null;
			}
			StringBuilder result = new StringBuilder();
			int depth = NUMBER_ARRAY.length();
			while (integer != 0) {
				result.append(NUMBER_ARRAY.charAt(integer % depth));
				integer /= depth;
			}
			if (result.length() == 0) {
				result.append('0');
			}
			number = result.reverse().toString();
		}
		return number;
	}
}
