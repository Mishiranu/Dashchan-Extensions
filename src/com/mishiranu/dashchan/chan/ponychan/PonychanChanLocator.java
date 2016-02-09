package com.mishiranu.dashchan.chan.ponychan;

import java.util.List;
import java.util.regex.Pattern;

import android.net.Uri;

import chan.content.ChanLocator;

public class PonychanChanLocator extends ChanLocator
{
	private static final String HOST = "www.ponychan.net";
	
	private static final Pattern BOARD_PATH = Pattern.compile("/\\w+(?:/(?:(?:\\d+|catalog)\\.html)?)?");
	private static final Pattern THREAD_PATH = Pattern.compile("/\\w+/res/(\\d+)\\.html");
	private static final Pattern ATTACHMENT_PATH = Pattern.compile("(?:/\\w+/src|/chan/\\w+/src)/" +
			"(?:mtr_)?\\d+\\.\\w+");
	
	public PonychanChanLocator()
	{
		addChanHost("ponychan.net");
		addSpecialChanHost(HOST);
		addSpecialChanHost("ml.ponychan.net");
		addSpecialChanHost("vintage.ponychan.net");
		setHttpsMode(HttpsMode.CONFIGURABLE);
	}
	
	@Override
	public boolean isBoardUri(Uri uri)
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
		return uri.getFragment();
	}
	
	@Override
	public Uri createBoardUri(String boardName, int pageNumber)
	{
		return pageNumber > 0 ? buildSpecificPath(boardName, (pageNumber + 1) + ".html")
				: buildSpecificPath(boardName, "");
	}
	
	@Override
	public Uri createThreadUri(String boardName, String threadNumber)
	{
		return buildSpecificPath(boardName, "res", threadNumber + ".html");
	}
	
	@Override
	public Uri createPostUri(String boardName, String threadNumber, String postNumber)
	{
		return createThreadUri(boardName, threadNumber).buildUpon().fragment(postNumber).build();
	}
	
	public Uri createSendPostUri()
	{
		return buildPathWithSchemeHost(true, HOST, "post.php");
	}
	
	public Uri buildSpecificPath(String... segments)
	{
		return buildPathWithHost(HOST, segments);
	}
	
	public Uri buildSpecificQuery(String path, String... alternation)
	{
		return buildQueryWithHost(HOST, path, alternation);
	}
}