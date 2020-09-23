package com.mishiranu.dashchan.chan.fourchan;

import android.net.Uri;
import chan.content.ChanConfiguration;
import chan.content.ChanLocator;
import chan.util.StringUtils;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class FourchanChanLocator extends ChanLocator {
	private static final String HOST_BOARDS = "boards.4chan.org";
	private static final String HOST_BOARDS_SAFE = "boards.4channel.org";
	private static final String HOST_POST = "sys.4chan.org";
	private static final String HOST_API = "a.4cdn.org";
	private static final String HOST_IMAGES = "i.4cdn.org";
	private static final String HOST_STATIC = "s.4cdn.org";

	private static final Pattern BOARD_PATH = Pattern.compile("/\\w+(?:/(?:\\d+|catalog)?)?");
	private static final Pattern THREAD_PATH = Pattern.compile("/\\w+/thread/(\\d+)(?:/.*)?");
	private static final Pattern ATTACHMENT_PATH = Pattern.compile("/\\w+/\\d+\\.\\w+");

	public FourchanChanLocator() {
		addChanHost("4chan.org");
		addConvertableChanHost("www.4chan.org");
		addConvertableChanHost("4channel.org");
		addConvertableChanHost("www.4channel.org");
		addSpecialChanHost(HOST_BOARDS);
		addSpecialChanHost(HOST_BOARDS_SAFE);
		addSpecialChanHost(HOST_POST);
		addSpecialChanHost(HOST_API);
		addSpecialChanHost(HOST_IMAGES);
		addSpecialChanHost(HOST_STATIC);
		setHttpsMode(HttpsMode.HTTPS_ONLY);
	}

	private String getBoardsHost(String boardName) {
		FourchanChanConfiguration configuration = ChanConfiguration.get(this);
		return configuration.isSafeForWork(boardName) ? HOST_BOARDS_SAFE : HOST_BOARDS;
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
		return isChanHostOrRelative(uri) && (isPathMatches(uri, ATTACHMENT_PATH) || extractMathData(uri) != null);
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
		return getGroupValue(uri.getPath(), THREAD_PATH, 1);
	}

	@Override
	public String getPostNumber(Uri uri) {
		String fragment = uri.getFragment();
		if (fragment != null && fragment.startsWith("p")) {
			fragment = fragment.substring(1);
		}
		return fragment;
	}

	@Override
	public Uri createBoardUri(String boardName, int pageNumber) {
		return pageNumber > 0 ? buildPathWithHost(getBoardsHost(boardName), boardName, (pageNumber + 1) + ".html")
				: buildPathWithHost(getBoardsHost(boardName), boardName, "");
	}

	@Override
	public Uri createThreadUri(String boardName, String threadNumber) {
		return buildPathWithHost(getBoardsHost(boardName), boardName, "thread", threadNumber);
	}

	public Uri createBoardsRootUri(String boardName) {
		return buildPathWithHost(getBoardsHost(boardName));
	}

	@Override
	public Uri createPostUri(String boardName, String threadNumber, String postNumber) {
		return createThreadUri(boardName, threadNumber).buildUpon().fragment("p" + postNumber).build();
	}

	public Uri buildAttachmentPath(String... segments) {
		return buildPathWithSchemeHost(true, HOST_IMAGES, segments);
	}

	public Uri createApiUri(String... segments) {
		return buildPathWithHost(HOST_API, segments);
	}

	public Uri createIconUri(String country) {
		return buildPathWithSchemeHost(true, HOST_STATIC, "image", "country", country.toLowerCase(Locale.US) + ".gif");
	}

	public Uri createSysUri(String... segments) {
		return buildPathWithSchemeHost(true, HOST_POST, segments);
	}

	public Uri buildMathUri(String data) {
		try {
			return buildPathWithHost(HOST_IMAGES, "math-tag",
					URLEncoder.encode(data, "UTF-8").replace("+", "%20") + ".png");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	public String extractMathData(Uri uri) {
		List<String> segments = uri.getPathSegments();
		if (segments.size() == 2 && "math-tag".equals(segments.get(0))) {
			String result = segments.get(1);
			if (result.endsWith(".png")) {
				result = result.substring(0, result.length() - 4);
			}
			return result;
		}
		return null;
	}

	@Override
	public NavigationData handleUriClickSpecial(Uri uri) {
		if (isBoardUriOrSearch(uri)) {
			String query = uri.getFragment();
			if (!StringUtils.isEmpty(query) && query.startsWith("s=")) {
				return new NavigationData(NavigationData.TARGET_SEARCH, getBoardName(uri),
						null, null, query.substring(2));
			}
		}
		return null;
	}
}
