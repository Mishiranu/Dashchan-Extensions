package com.mishiranu.dashchan.chan.taima;

import java.util.List;
import java.util.regex.Pattern;

import android.net.Uri;

import chan.content.ChanLocator;

public class TaimaChanLocator extends ChanLocator
{
	private static final String HOST_BOARDS = "boards.420chan.org";
	private static final String HOST_API = "api.420chan.org";
	
	private static final Pattern BOARD_PATH = Pattern.compile("/\\w+(?:/(?:\\d+\\.php)?)?");
	private static final Pattern THREAD_PATH = Pattern.compile("/\\w+/res/(\\d+)\\.php");
	private static final Pattern ATTACHMENT_PATH = Pattern.compile("/\\w+/src/\\d+\\.\\w+");
	
	public TaimaChanLocator()
	{
		addChanHost("420chan.org");
		addSpecialChanHost(HOST_BOARDS);
		addSpecialChanHost(HOST_API);
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
		return pageNumber > 0 ? buildPathWithHost(HOST_BOARDS, boardName, pageNumber + ".php")
				: buildPathWithHost(HOST_BOARDS, boardName, "");
	}
	
	@Override
	public Uri createThreadUri(String boardName, String threadNumber)
	{
		return buildPathWithHost(HOST_BOARDS, boardName, "res", threadNumber + ".php");
	}
	
	@Override
	public Uri createPostUri(String boardName, String threadNumber, String postNumber)
	{
		return createThreadUri(boardName, threadNumber).buildUpon().fragment(postNumber).build();
	}
	
	public Uri createApiUri(String... segments)
	{
		return buildPathWithHost(HOST_API, segments);
	}
	
	public Uri createSpecialBoardUri(String... segments)
	{
		return buildPathWithHost(HOST_BOARDS, segments);
	}
}