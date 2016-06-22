package com.mishiranu.dashchan.chan.anonfm;

import java.util.regex.Pattern;

import android.net.Uri;

import chan.content.ChanLocator;
import chan.util.StringUtils;

public class AnonfmChanLocator extends ChanLocator
{
	public static final String BOARD_NAME_FM = "fm";
	public static final String BOARD_NAME_TEXTUAL = "board";
	
	private static final Pattern BOARD_PATH_TEXTUAL_BOARD = Pattern.compile("/" + BOARD_NAME_TEXTUAL
			+ "//?(\\d+)\\.html");
	
	public AnonfmChanLocator()
	{
		addChanHost("anon.fm");
		setHttpsMode(HttpsMode.CONFIGURABLE);
	}
	
	private boolean isFmUri(Uri uri)
	{
		if (!isChanHostOrRelative(uri)) return false;
		String path = uri.getPath();
		return StringUtils.isEmpty(path) || "/".equals(path) || "/info.html".equals(path)
				|| "/newdes.html".equals(path) || "/userscripts/index.html".equals(path);
	}
	
	private boolean isTextBoardUri(Uri uri, boolean thread)
	{
		return isChanHostOrRelative(uri) && (thread && isPathMatches(uri, BOARD_PATH_TEXTUAL_BOARD)
				|| !thread && ("/" + BOARD_NAME_TEXTUAL + "/").equals(uri.getPath()));
	}
	
	@Override
	public boolean isBoardUri(Uri uri)
	{
		return isTextBoardUri(uri, false);
	}
	
	@Override
	public boolean isThreadUri(Uri uri)
	{
		return isFmUri(uri) || isTextBoardUri(uri, true);
	}
	
	@Override
	public boolean isAttachmentUri(Uri uri)
	{
		return false;
	}
	
	@Override
	public String getBoardName(Uri uri)
	{
		return isFmUri(uri) ? BOARD_NAME_FM : isTextBoardUri(uri, false) ? BOARD_NAME_TEXTUAL : null;
	}
	
	@Override
	public String getThreadNumber(Uri uri)
	{
		return isFmUri(uri) ? "1" : isTextBoardUri(uri, true) ? getGroupValue(uri.getPath(),
				BOARD_PATH_TEXTUAL_BOARD, 1) : null;
	}
	
	@Override
	public String getPostNumber(Uri uri)
	{
		return isFmUri(uri) || isTextBoardUri(uri, true) ? uri.getFragment() : null;
	}
	
	@Override
	public Uri createBoardUri(String boardName, int pageNumber)
	{
		return BOARD_NAME_TEXTUAL.equals(boardName) ? buildPath(BOARD_NAME_TEXTUAL, "") : buildPath();
	}
	
	@Override
	public Uri createThreadUri(String boardName, String threadNumber)
	{
		return BOARD_NAME_TEXTUAL.equals(boardName) ? buildPath(BOARD_NAME_TEXTUAL, threadNumber + ".html")
				: buildPath();
	}
	
	@Override
	public Uri createPostUri(String boardName, String threadNumber, String postNumber)
	{
		return createThreadUri(boardName, threadNumber);
	}
}