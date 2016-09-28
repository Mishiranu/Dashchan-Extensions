package com.mishiranu.dashchan.chan.allchan;

import java.util.List;
import java.util.regex.Pattern;

import android.net.Uri;

import chan.content.ChanLocator;
import chan.util.StringUtils;

public class AllchanChanLocator extends ChanLocator
{
	private static final Pattern BOARD_PATH = Pattern.compile("/\\w+(?:/(?:\\d+)?)?");
	private static final Pattern THREAD_PATH = Pattern.compile("/\\w+/res/(\\d+)\\.html");
	private static final Pattern ATTACHMENT_PATH = Pattern.compile("/\\w+/src/\\d+\\.\\w+");
	
	public AllchanChanLocator()
	{
		addChanHost("allchan.su");
		addConvertableChanHost("www.allchan.su");
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
		if (fragment != null && fragment.startsWith("post-")) fragment = fragment.substring(5);
		return fragment;
	}
	
	@Override
	public Uri createBoardUri(String boardName, int pageNumber)
	{
		return pageNumber > 0 ? buildPath(boardName, pageNumber + ".html") : buildPath(boardName, "");
	}
	
	@Override
	public Uri createThreadUri(String boardName, String threadNumber)
	{
		return buildPath(boardName, "res", threadNumber + ".html");
	}
	
	@Override
	public Uri createPostUri(String boardName, String threadNumber, String postNumber)
	{
		return createThreadUri(boardName, threadNumber).buildUpon().fragment("post-" + postNumber).build();
	}
}