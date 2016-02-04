package com.mishiranu.dashchan.chan.fourplebs;

import java.util.List;
import java.util.regex.Pattern;

import android.net.Uri;

import chan.content.ChanLocator;

public class FourplebsChanLocator extends ChanLocator
{
	private static final String HOST_ARCHIVE = "archive.4plebs.org";
	private static final String HOST_IMAGES = "img.4plebs.org";
	
	private static final Pattern BOARD_PATH = Pattern.compile("/\\w+(?:/(?:page/\\d+/?)?)?");
	private static final Pattern THREAD_PATH = Pattern.compile("/\\w+/(?:thread|post)/(\\d+)/?");
	private static final Pattern ATTACHMENT_PATH = Pattern.compile("/boards/\\w+/image/\\d+/\\d+/\\d+\\.\\w+");
	
	public FourplebsChanLocator()
	{
		addChanHost("4plebs.org");
		addSpecialChanHost("www.4plebs.org");
		addSpecialChanHost(HOST_ARCHIVE);
		addSpecialChanHost(HOST_IMAGES);
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
		if (segments.size() > 0)
		{
			String boardName = segments.get(0);
			if ("boards".equals(boardName) && segments.size() > 1) return segments.get(1);
			return boardName;
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
		return uri.getFragment();
	}
	
	@Override
	public Uri createBoardUri(String boardName, int pageNumber)
	{
		return pageNumber > 0 ? buildPathWithHost(HOST_ARCHIVE, boardName, "page", Integer.toString(pageNumber + 1), "")
				: buildPathWithHost(HOST_ARCHIVE, boardName, "");
	}
	
	@Override
	public Uri createThreadUri(String boardName, String threadNumber)
	{
		return buildPathWithHost(HOST_ARCHIVE, boardName, "thread", threadNumber, "");
	}
	
	@Override
	public Uri createPostUri(String boardName, String threadNumber, String postNumber)
	{
		return createThreadUri(boardName, threadNumber).buildUpon().fragment(postNumber).build();
	}
	
	public Uri createAttachmentUri(String path)
	{
		return buildPathWithHost(HOST_IMAGES, path);
	}
	
	public Uri buildArchivePath(String... segments)
	{
		return buildPathWithHost(HOST_ARCHIVE, segments);
	}
}