package com.mishiranu.dashchan.chan.arhivach;

import android.net.Uri;
import chan.content.ChanLocator;
import java.util.List;
import java.util.regex.Pattern;

public class ArhivachChanLocator extends ChanLocator {
	private static final Pattern THREAD_PATH = Pattern.compile("/thread/\\d+/?");

	public ArhivachChanLocator() {
		addChanHost("arhivach.net");
		addConvertableChanHost("arhivach.org");
		addConvertableChanHost("arhivach.cf");
		addConvertableChanHost("arhivach.ng");
		addChanHost("arhivachovtj2jrp.onion");
		setHttpsMode(HttpsMode.CONFIGURABLE);
	}

	@Override
	public boolean isBoardUri(Uri uri) {
		return false;
	}

	@Override
	public boolean isThreadUri(Uri uri) {
		return isChanHostOrRelative(uri) && isPathMatches(uri, THREAD_PATH);
	}

	@Override
	public boolean isAttachmentUri(Uri uri) {
		return isImageExtension(uri.getPath()) || isAudioExtension(uri.getPath()) || isVideoExtension(uri.getPath());
	}

	@Override
	public String getBoardName(Uri uri) {
		return null;
	}

	@Override
	public String getThreadNumber(Uri uri) {
		if (uri != null) {
			List<String> segments = uri.getPathSegments();
			if (segments.size() > 1) {
				return segments.get(1);
			}
		}
		return null;
	}

	@Override
	public String getPostNumber(Uri uri) {
		return uri.getFragment();
	}

	@Override
	public Uri createBoardUri(String boardName, int pageNumber) {
		return pageNumber > 0 ? buildPath("index", Integer.toString(ArhivachChanPerformer.PAGE_SIZE * pageNumber))
				: buildPath();
	}

	@Override
	public Uri createThreadUri(String boardName, String threadNumber) {
		return buildPath("thread", threadNumber);
	}

	@Override
	public Uri createPostUri(String boardName, String threadNumber, String postNumber) {
		return createThreadUri(boardName, threadNumber).buildUpon().fragment(postNumber).build();
	}

	@Override
	public String createAttachmentForcedName(Uri fileUri) {
		if (isChanHostOrRelative(fileUri) && "a_cimg".equals(fileUri.getLastPathSegment())) {
			String query = fileUri.getQuery();
			if (query.startsWith("h=")) {
				query = query.substring(2);
			}
			query = query.replace("&", "");
			return query;
		}
		return null;
	}
}
