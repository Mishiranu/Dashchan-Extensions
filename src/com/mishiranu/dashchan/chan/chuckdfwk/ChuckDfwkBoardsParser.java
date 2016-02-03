package com.mishiranu.dashchan.chan.chuckdfwk;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.text.GroupParser;
import chan.text.ParseException;
import chan.util.StringUtils;

public class ChuckDfwkBoardsParser implements GroupParser.Callback
{
	private final String mSource;
	
	private final ArrayList<Board> mBoards = new ArrayList<>();
	
	private static final Pattern BOARD_URI = Pattern.compile("/(\\w+)/");
	
	private boolean mNavbarHandling = false;
	
	public ChuckDfwkBoardsParser(String source)
	{
		mSource = source;
	}
	
	public BoardCategory convert() throws ParseException
	{
		try
		{
			GroupParser.parse(mSource, this);
		}
		catch (FinishedException e)
		{
			
		}
		return new BoardCategory("Доски", mBoards);
	}
	
	private static class FinishedException extends ParseException
	{
		private static final long serialVersionUID = 1L;
	}
	
	@Override
	public boolean onStartElement(GroupParser parser, String tagName, String attrs) throws FinishedException
	{
		if ("div".equals(tagName))
		{
			String cssClass = parser.getAttr(attrs, "class");
			if ("navbar".equals(cssClass))
			{
				mNavbarHandling = true;
			}
		}
		else if ("a".equals(tagName))
		{
			String href = parser.getAttr(attrs, "href");
			Matcher matcher = BOARD_URI.matcher(href);
			if (matcher.matches())
			{
				String boardName = matcher.group(1);
				String title = parser.getAttr(attrs, "title");
				if (title != null) title = StringUtils.clearHtml(title);
				mBoards.add(new Board(boardName, title));
			}
		}
		return false;
	}
	
	@Override
	public void onEndElement(GroupParser parser, String tagName) throws FinishedException
	{
		if (mNavbarHandling && "div".equals(tagName))
		{
			throw new FinishedException();
		}
	}
	
	@Override
	public void onText(GroupParser parser, String source, int start, int end)
	{
		
	}
	
	@Override
	public void onGroupComplete(GroupParser parser, String text)
	{
		
	}
}