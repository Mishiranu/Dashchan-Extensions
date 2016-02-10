package com.mishiranu.dashchan.chan.nulldvachin;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.text.GroupParser;
import chan.text.ParseException;
import chan.util.StringUtils;

public class NulldvachinBoardsParser implements GroupParser.Callback
{
	private final String mSource;
	
	private final ArrayList<Board> mBoards = new ArrayList<>();
	private String mTitle;
	
	private boolean mBoardListParsing = false;
	
	private static final Pattern PATTERN_BOARD_URI = Pattern.compile("/(.*?)/");
	
	public NulldvachinBoardsParser(String source)
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
		return mBoards.size() > 0 ? new BoardCategory(null, mBoards) : null;
	}
	
	private static class FinishedException extends ParseException
	{
		private static final long serialVersionUID = 1L;
	}
	
	@Override
	public boolean onStartElement(GroupParser parser, String tagName, String attrs)
	{
		if ("ul".equals(tagName))
		{
			String cssClass = parser.getAttr(attrs, "class");
			if ("menu".equals(cssClass)) mBoardListParsing = true;
		}
		else if (mBoardListParsing)
		{
			if ("li".equals(tagName))
			{
				mTitle = StringUtils.clearHtml(parser.getAttr(attrs, "title")).trim();
			}
			else if ("a".equals(tagName))
			{
				String href = parser.getAttr(attrs, "href");
				Matcher matcher = PATTERN_BOARD_URI.matcher(href);
				if (matcher.matches())
				{
					if (mTitle != null)
					{
						mBoards.add(new Board(matcher.group(1), mTitle));
						mTitle = null;
					}
				}
			}
		}
		return false;
	}
	
	@Override
	public void onEndElement(GroupParser parser, String tagName) throws FinishedException
	{
		if ("ul".equals(tagName) && mBoardListParsing)
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