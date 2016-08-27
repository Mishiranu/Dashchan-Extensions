package com.mishiranu.dashchan.chan.alterchan;

import java.util.regex.Pattern;

import android.net.Uri;

import chan.content.ChanLocator;
import chan.util.StringUtils;

public class AlterchanChanLocator extends ChanLocator
{
	private static final Pattern THREAD_PATH = Pattern.compile("/discussion.php/([a-zA-Z0-9]+)");
	private static final Pattern ATTACHMENT_PATH = Pattern.compile("/var/src/\\w+\\.\\w+");
	
	public AlterchanChanLocator()
	{
		addChanHost("alterchan.in");
	}
	
	@Override
	public boolean isBoardUri(Uri uri)
	{
		String path = uri.getPath();
		return StringUtils.isEmpty(path) || "/".equals(path);
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
		return null;
	}
	
	@Override
	public String getThreadNumber(Uri uri)
	{
		return uri != null ? convertToDecimalNumber(getGroupValue(uri.getPath(), THREAD_PATH, 1)) : null;
	}
	
	@Override
	public String getPostNumber(Uri uri)
	{
		return convertToDecimalNumber(uri.getFragment());
	}
	
	@Override
	public Uri createBoardUri(String boardName, int pageNumber)
	{
		return pageNumber > 0 ? buildQuery("/", "page", Integer.toString(pageNumber + 1)) : buildPath();
	}
	
	@Override
	public Uri createThreadUri(String boardName, String threadNumber)
	{
		return buildPath("discussion.php", convertToPresentNumber(threadNumber));
	}
	
	@Override
	public Uri createPostUri(String boardName, String threadNumber, String postNumber)
	{
		return createThreadUri(boardName, threadNumber).buildUpon()
				.fragment(convertToPresentNumber(postNumber)).build();
	}
	
	private static final String NUMBER_ARRAY = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
	
	public String convertToDecimalNumber(String number)
	{
		if (!StringUtils.isEmpty(number))
		{
			int result = 0;
			for (int i = number.length() - 1, m = 1; i >= 0; i--, m *= NUMBER_ARRAY.length())
			{
				result += NUMBER_ARRAY.indexOf(number.charAt(i)) * m;
			}
			number = Integer.toString(result);
		}
		return number;
	}
	
	public String convertToPresentNumber(String number)
	{
		if (!StringUtils.isEmpty(number))
		{
			int integer;
			try
			{
				integer = Integer.parseInt(number);
				if (integer < 0) return null;
			}
			catch (NumberFormatException e)
			{
				return null;
			}
			StringBuilder result = new StringBuilder();
			int depth = NUMBER_ARRAY.length();
			while (integer != 0)
			{
				result.append(NUMBER_ARRAY.charAt(integer % depth));
				integer /= depth;
			}
			if (result.length() == 0) result.append('0');
			number = result.reverse().toString();
		}
		return number;
	}
}