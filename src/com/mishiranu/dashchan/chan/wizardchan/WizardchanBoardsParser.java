package com.mishiranu.dashchan.chan.wizardchan;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.text.GroupParser;
import chan.text.ParseException;
import chan.util.StringUtils;

public class WizardchanBoardsParser implements GroupParser.Callback
{
	private final String mSource;
	
	private final ArrayList<BoardCategory> mBoardCategories = new ArrayList<>();
	private final ArrayList<Board> mBoards = new ArrayList<>();
	
	private String mBoardCategoryTitle;
	
	private static final Pattern PATTERN_BOARD_URI = Pattern.compile("/(.*)/index.html");
	
	public WizardchanBoardsParser(String source)
	{
		mSource = source;
	}
	
	public ArrayList<BoardCategory> convert() throws ParseException
	{
		try
		{
			GroupParser.parse(mSource, this);
		}
		catch (FinishedException e)
		{
			
		}
		closeCategory();
		return mBoardCategories;
	}
	
	private void closeCategory()
	{
		ArrayList<Board> boards = mBoards;
		if (boards.size() > 0)
		{
			mBoardCategories.add(new BoardCategory(mBoardCategoryTitle, boards));
			mBoards.clear();
		}
	}
	
	private static class FinishedException extends ParseException
	{
		private static final long serialVersionUID = 1L;
	}
	
	@Override
	public boolean onStartElement(GroupParser parser, String tagName, String attrs)
	{
		if ("span".equals(tagName))
		{
			String description = parser.getAttr(attrs, "data-description");
			if (description != null)
			{
				closeCategory();
				mBoardCategoryTitle = StringUtils.clearHtml(description);
			}
		}
		else if ("a".equals(tagName))
		{
			if (mBoardCategoryTitle != null)
			{
				String href = parser.getAttr(attrs, "href");
				String title = parser.getAttr(attrs, "title");
				if (title != null)
				{
					Matcher matcher = PATTERN_BOARD_URI.matcher(href);
					if (matcher.matches())
					{
						String boardName = matcher.group(1);
						title = StringUtils.clearHtml(title);
						mBoards.add(new Board(boardName, title));
					}
				}
			}
		}
		return false;
	}
	
	@Override
	public void onEndElement(GroupParser parser, String tagName) throws FinishedException
	{
		if ("div".equals(tagName) && mBoardCategoryTitle != null) throw new FinishedException();
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