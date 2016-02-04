package com.mishiranu.dashchan.chan.fourchan;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import android.net.Uri;

import chan.content.ChanLocator;
import chan.util.StringUtils;

public class FourchanChanLocator extends ChanLocator
{
	private static final String HOST_WWW = "www.4chan.org";
	private static final String HOST_BOARDS = "boards.4chan.org";
	private static final String HOST_POST = "sys.4chan.org";
	private static final String HOST_API = "a.4cdn.org";
	private static final String HOST_IMAGES = "i.4cdn.org";
	private static final String HOST_STATIC = "s.4cdn.org";
	
	private static final Pattern BOARD_PATH = Pattern.compile("/\\w+(?:/(?:(?:\\d+|catalog))?)?");
	private static final Pattern THREAD_PATH = Pattern.compile("/\\w+/thread/(\\d+)(?:/.*)?");
	private static final Pattern ATTACHMENT_PATH = Pattern.compile("/\\w+/\\d+\\.\\w+");
	
	public FourchanChanLocator()
	{
		addChanHost("4chan.org");
		addSpecialChanHost(HOST_WWW);
		addSpecialChanHost(HOST_BOARDS);
		addSpecialChanHost(HOST_POST);
		addSpecialChanHost(HOST_API);
		addSpecialChanHost(HOST_IMAGES);
		addSpecialChanHost(HOST_STATIC);
		setHttpsMode(HttpsMode.CONFIGURABLE);
	}
	
	@Override
	public boolean isBoardUri(Uri uri)
	{
		return isBoardUriOrSearch(uri) && StringUtils.isEmpty(uri.getFragment());
	}
	
	public boolean isBoardUriOrSearch(Uri uri)
	{
		return isChanHostOrRelative(uri) && isPathMatches(uri, BOARD_PATH);
	}
	
	@Override
	public boolean isThreadUri(Uri uri)
	{
		return isChanHostOrRelative(uri) && isPathMatches(uri, THREAD_PATH);
	}
	
	@Override
	public boolean isAttachmentUri(Uri uri)
	{
		return isChanHostOrRelative(uri) && isPathMatches(uri, ATTACHMENT_PATH);
	}
	
	@Override
	public String getBoardName(Uri uri)
	{
		List<String> segments = uri.getPathSegments();
		if (segments.size() > 0) return segments.get(0);
		return null;
	}
	
	@Override
	public String getThreadNumber(Uri uri)
	{
		return getGroupValue(uri.getPath(), THREAD_PATH, 1);
	}
	
	@Override
	public String getPostNumber(Uri uri)
	{
		String fragment = uri.getFragment();
		if (fragment != null && fragment.startsWith("p")) fragment = fragment.substring(1);
		return fragment;
	}
	
	@Override
	public Uri createBoardUri(String boardName, int pageNumber)
	{
		return pageNumber > 0 ? buildPathWithHost(HOST_BOARDS, boardName, (pageNumber + 1) + ".html")
				: buildPathWithHost(HOST_BOARDS, boardName, "");
	}
	
	@Override
	public Uri createThreadUri(String boardName, String threadNumber)
	{
		return buildPathWithHost(HOST_BOARDS, boardName, "thread", threadNumber);
	}
	
	@Override
	public Uri createPostUri(String boardName, String threadNumber, String postNumber)
	{
		return createThreadUri(boardName, threadNumber).buildUpon().fragment("p" + postNumber).build();
	}
	
	public Uri buildAttachmentPath(String... segments)
	{
		return buildPathWithHost(HOST_IMAGES, segments);
	}
	
	public Uri buildBasePath(String... segments)
	{
		return buildPathWithHost(HOST_WWW, segments);
	}
	
	public Uri createApiUri(String... segments)
	{
		return buildPathWithHost(HOST_API, segments);
	}
	
	public Uri createIconUri(String country)
	{
		return buildPathWithHost(HOST_STATIC, "image", "country", country.toLowerCase(Locale.US) + ".gif");
	}
	
	public Uri createSysUri(String... segments)
	{
		return buildPathWithSchemeHost(true, HOST_POST, segments);
	}
	
	@Override
	public NavigationData handleUriClickSpecial(Uri uri)
	{
		if (isBoardUriOrSearch(uri))
		{
			String query = uri.getFragment();
			if (!StringUtils.isEmpty(query) && query.startsWith("s="))
			{
				return new NavigationData(NavigationData.TARGET_SEARCH, getBoardName(uri),
						null, null, query.substring(2));
			}
		}
		return null;
	}
}