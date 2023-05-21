package com.mishiranu.dashchan.chan.dollchan;

import android.net.Uri;
import android.webkit.MimeTypeMap;
import chan.content.ChanLocator;
import chan.content.WakabaChanLocator;
import chan.util.StringUtils;
import java.util.List;
import java.util.regex.Pattern;

public class DollchanChanLocator extends WakabaChanLocator {
	private static final Pattern BOARD_PATH = Pattern.compile("/\\w+(?:/(?:(?:catalog|\\d+)\\.html)?)?");
	private static final Pattern THREAD_PATH = Pattern.compile("/\\w+/res/(\\d+)\\.html");
	private static final Pattern ATTACHMENT_PATH = Pattern.compile("/\\.media/.*");

	public DollchanChanLocator() {
		addChanHost("dollchan.net");
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
		if (uri != null) {
			List<String> segments = uri.getPathSegments();
			if (segments.size() > 0) {
				String boardName = segments.get(0);
				if (!boardName.startsWith(".")) {
					return boardName;
				}
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
		return uri.getFragment();
	}

	@Override
	public Uri createBoardUri(String boardName, int pageNumber) {
		return pageNumber > 0 ? buildPath(boardName, (pageNumber + 1) + ".html") : buildPath(boardName, "");
	}

	@Override
	public Uri createThreadUri(String boardName, String threadNumber) {
		return buildPath(boardName, "res", threadNumber + ".html");
	}

	@Override
	public Uri createPostUri(String boardName, String threadNumber, String postNumber) {
		return createThreadUri(boardName, threadNumber).buildUpon().fragment(postNumber).build();
	}

	@Override
	public String createAttachmentForcedName(Uri fileUri) {
		String fileName = StringUtils.emptyIfNull(fileUri.getLastPathSegment());
		int index = fileName.indexOf('-');
		if (index >= 0) {
			String mimeType = fileName.substring(index + 1);
			fileName = fileName.substring(0, index);
			String extension = getFileExtension(mimeType);
			if (extension == null) {
				int insert = mimeType.startsWith("text") ? 4 : mimeType.equals("application") ? 11 : 5;
				mimeType = mimeType.substring(0, insert) + '/' + mimeType.substring(insert);
				extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
			}
			if (extension != null) {
				return fileName + '.' + extension;
			}
		}
		return fileName;
	}
}
