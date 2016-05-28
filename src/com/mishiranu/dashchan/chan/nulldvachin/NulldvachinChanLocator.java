package com.mishiranu.dashchan.chan.nulldvachin;

import java.util.List;
import java.util.regex.Pattern;

import android.net.Uri;

import chan.content.ChanLocator;

public class NulldvachinChanLocator extends ChanLocator
{
	private static final Pattern BOARD_PATH = Pattern.compile("/\\w+(?:/(?:page/\\d+)?)?");
	private static final Pattern THREAD_PATH = Pattern.compile("/\\w+/thread/(\\d+)");
	private static final Pattern ATTACHMENT_PATH = Pattern.compile("/\\w+/src/.+");
	
	public NulldvachinChanLocator()
	{
		addChanHost("02ch.in");
		addChanHost("02ch.info");
		addConvertableChanHost("www.02ch.in");
		addConvertableChanHost("www.02ch.info");
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
		if (uri != null)
		{
			List<String> segments = uri.getPathSegments();
			if (segments.size() > 0) return segments.get(0);
		}
		return null;
	}
	
	@Override
	public String getThreadNumber(Uri uri)
	{
		return uri != null ? getGroupValue(uri.getPath(), THREAD_PATH, 1) : null;
	}
	
	@Override
	public String getPostNumber(Uri uri)
	{
		String fragment = uri.getFragment();
		if (fragment != null && fragment.startsWith("i")) return fragment.substring(1);
		return fragment;
	}
	
	@Override
	public Uri createBoardUri(String boardName, int pageNumber)
	{
		return pageNumber > 0 ? buildPath(boardName, "page", Integer.toString(pageNumber + 1))
				: buildPath(boardName, "");
	}
	
	@Override
	public Uri createThreadUri(String boardName, String threadNumber)
	{
		return buildPath(boardName, "thread", threadNumber);
	}
	
	@Override
	public Uri createPostUri(String boardName, String threadNumber, String postNumber)
	{
		return createThreadUri(boardName, threadNumber).buildUpon().fragment(postNumber).build();
	}
	
	@Override
	public String createAttachmentForcedName(Uri fileUri)
	{
		// Extract "name" from "/board/src/name/uploadname"
		List<String> segments = fileUri.getPathSegments();
		if (segments.size() == 4 && "src".equals(segments.get(1))) return segments.get(2);
		return super.createAttachmentForcedName(fileUri);
	}
}