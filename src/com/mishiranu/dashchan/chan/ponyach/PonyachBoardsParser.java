package com.mishiranu.dashchan.chan.ponyach;

import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.text.GroupParser;
import chan.text.ParseException;

public class PonyachBoardsParser implements GroupParser.Callback
{
	private final String mSource;
	
	private final ArrayList<Board> mBoards = new ArrayList<>();
	
	private boolean mBoardListParsing = false;
	
	private static final Pattern PATTERN_BOARD_URI = Pattern.compile("/(.*?)/?");
	
	public PonyachBoardsParser(String source)
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
	public boolean onStartElement(GroupParser parser, String tagName, String attrs)
	{
		if ("div".equals(tagName))
		{
			String cssClass = parser.getAttr(attrs, "class");
			if ("navbar".equals(cssClass)) mBoardListParsing = true;
		}
		else if (mBoardListParsing)
		{
			if ("a".equals(tagName))
			{
				String href = parser.getAttr(attrs, "href");
				Matcher matcher = PATTERN_BOARD_URI.matcher(href);
				if (matcher.matches())
				{
					String boardName = matcher.group(1);
					String title = parser.getAttr(attrs, "title");
					title = validateBoardTitle(boardName, title);
					mBoards.add(new Board(boardName, title));
				}
			}
		}
		return false;
	}
	
	@Override
	public void onEndElement(GroupParser parser, String tagName)
	{
		
	}
	
	@Override
	public void onText(GroupParser parser, String source, int start, int end) throws FinishedException
	{
		if (mBoardListParsing && source.substring(start, end).contains("]")) throw new FinishedException();
	}
	
	@Override
	public void onGroupComplete(GroupParser parser, String text)
	{
		
	}
	
	static final String validateBoardTitle(String boardName, String title)
	{
		if ("b".equals(boardName)) return "Was never good";
		else if ("r34".equals(boardName)) return "My little pony Rule 34";
		else if ("rf".equals(boardName)) return "Убежище";
		if (title != null && title.length() > 2)
		{
			int index = title.indexOf('-');
			if (index == -1) index = title.indexOf('—');
			if (index >= 0) title = title.substring(index + 2);
			title = title.substring(0, 1).toUpperCase(Locale.getDefault()) + title.substring(1);
		}
		return title;
	}
}