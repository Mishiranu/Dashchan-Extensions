package com.mishiranu.dashchan.chan.exach;

import java.util.regex.Pattern;

import android.net.Uri;

import chan.content.ChanLocator;
import chan.util.StringUtils;

public class ExachChanLocator extends ChanLocator
{
	private static final Pattern BOARD_PATH = Pattern.compile("/\\w+\\.php");
	private static final String THREAD_PATH = "/post.php";
	private static final Pattern ATTACHMENT_PATH = Pattern.compile("/img/\\d+/\\d+/\\d+_big\\.\\w+");

	public static final String DEFAULT_BOARD_NAME = "b";

	public ExachChanLocator()
	{
		addChanHost("exach.com");
		addConvertableChanHost("www.exach.com");
	}

	@Override
	public boolean isBoardUri(Uri uri)
	{
		return isChanHostOrRelative(uri) && !THREAD_PATH.equals(uri.getPath()) && isPathMatches(uri, BOARD_PATH);
	}

	@Override
	public boolean isThreadUri(Uri uri)
	{
		return isChanHostOrRelative(uri) && THREAD_PATH.equals(uri.getPath()) &&
				!StringUtils.isEmpty(uri.getQueryParameter("id"));
	}

	@Override
	public boolean isAttachmentUri(Uri uri)
	{
		return isChanHostOrRelative(uri) && isPathMatches(uri, ATTACHMENT_PATH);
	}

	@Override
	public String getBoardName(Uri uri)
	{
		if (THREAD_PATH.equals(uri.getPath())) return DEFAULT_BOARD_NAME; else if (isPathMatches(uri, BOARD_PATH))
		{
			String segment = uri.getPathSegments().get(0);
			return segment.substring(0, segment.length() - 4);
		}
		return null;
	}

	@Override
	public String getThreadNumber(Uri uri)
	{
		return uri.getQueryParameter("id");
	}

	@Override
	public String getPostNumber(Uri uri)
	{
		String fragment = uri.getFragment();
		if (fragment != null && fragment.startsWith("c")) return fragment.substring(1);
		return null;
	}

	@Override
	public Uri createBoardUri(String boardName, int pageNumber)
	{
		return pageNumber > 0 ? buildQuery(boardName + ".php", "page", Integer.toString(pageNumber + 1))
				: buildPath(boardName + ".php");
	}

	@Override
	public Uri createThreadUri(String boardName, String threadNumber)
	{
		return buildQuery("post.php", "id", threadNumber);
	}

	@Override
	public Uri createPostUri(String boardName, String threadNumber, String postNumber)
	{
		return createThreadUri(boardName, threadNumber).buildUpon().fragment(postNumber).build();
	}
}