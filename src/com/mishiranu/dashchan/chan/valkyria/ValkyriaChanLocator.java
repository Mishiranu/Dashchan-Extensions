package com.mishiranu.dashchan.chan.valkyria;

import java.util.regex.Pattern;

import android.net.Uri;

import chan.content.ChanLocator;

public class ValkyriaChanLocator extends ChanLocator
{
	private static final Pattern BOARD_PATH = Pattern.compile("(?:/board/u18chan)?/(\\w+)(?:/(?:\\d+)?)?");
	private static final Pattern THREAD_PATH = Pattern.compile("(?:/board/u18chan)?/(\\w+)/topic/(\\d+)");
	private static final Pattern ATTACHMENT_PATH = Pattern.compile("/uploads/data/\\d+/.*");

	public ValkyriaChanLocator()
	{
		addChanHost("u18chan.com");
		addConvertableChanHost("www.u18chan.com");
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
		String path = uri.getPath();
		String boardName = getGroupValue(path, BOARD_PATH, 1);
		if (boardName == null) boardName = getGroupValue(path, THREAD_PATH, 1);
		return boardName;
	}

	@Override
	public String getThreadNumber(Uri uri)
	{
		return getGroupValue(uri.getPath(), THREAD_PATH, 2);
	}

	@Override
	public String getPostNumber(Uri uri)
	{
		return uri.getFragment();
	}

	@Override
	public Uri createBoardUri(String boardName, int pageNumber)
	{
		return pageNumber > 0 ? buildPath(boardName, Integer.toString(pageNumber)) : buildPath(boardName, "");
	}

	@Override
	public Uri createThreadUri(String boardName, String threadNumber)
	{
		return buildPath(boardName, "topic", threadNumber);
	}

	@Override
	public Uri createPostUri(String boardName, String threadNumber, String postNumber)
	{
		return createThreadUri(boardName, threadNumber).buildUpon().fragment(postNumber).build();
	}

	@Override
	public String createAttachmentForcedName(Uri fileUri)
	{
		String segment = fileUri.getLastPathSegment();
		int index = segment.lastIndexOf("_u18chan.");
		if (index >= 0) return segment.substring(0, index) + segment.substring(index + 8);
		return null;
	}
}