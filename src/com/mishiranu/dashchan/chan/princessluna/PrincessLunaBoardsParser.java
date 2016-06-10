package com.mishiranu.dashchan.chan.princessluna;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.text.GroupParser;
import chan.text.ParseException;

public class PrincessLunaBoardsParser implements GroupParser.Callback
{
	private final String mSource;
	
	private final ArrayList<Board> mBoards = new ArrayList<>();
	
	private boolean mBoardListParsing = false;
	
	private static final Pattern PATTERN_BOARD_URI = Pattern.compile("/(.*)/index.html");
	
	public PrincessLunaBoardsParser(String source)
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
		return new BoardCategory(null, mBoards);
	}
	
	private static class FinishedException extends ParseException
	{
		private static final long serialVersionUID = 1L;
	}
	
	@Override
	public boolean onStartElement(GroupParser parser, String tagName, String attrs)
	{
		if ("div".equals(tagName))
		{
			String cssClass = parser.getAttr(attrs, "class");
			if ("boardlist".equals(cssClass)) mBoardListParsing = true;
		}
		else if (mBoardListParsing)
		{
			if ("a".equals(tagName))
			{
				String href = parser.getAttr(attrs, "href");
				Matcher matcher = PATTERN_BOARD_URI.matcher(href);
				if (matcher.matches())
				{
					String title = parser.getAttr(attrs, "title");
					mBoards.add(new Board(matcher.group(1), title));
				}
			}
		}
		return false;
	}
	
	@Override
	public void onEndElement(GroupParser parser, String tagName) throws FinishedException
	{
		if ("div".equals(tagName) && mBoardListParsing) throw new FinishedException();
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