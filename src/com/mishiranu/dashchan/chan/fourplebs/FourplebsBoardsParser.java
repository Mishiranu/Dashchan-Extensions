package com.mishiranu.dashchan.chan.fourplebs;

import java.util.ArrayList;

import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.text.GroupParser;
import chan.text.ParseException;
import chan.util.StringUtils;

public class FourplebsBoardsParser implements GroupParser.Callback
{
	private final String mSource;
	private final ArrayList<Board> mBoards = new ArrayList<>();
	
	private String mBoardCategoryTitle;
	
	private static final int EXPECT_NONE = 0;
	private static final int EXPECT_CATEGORY = 1;
	private static final int EXPECT_BOARD = 2;
	
	private int mExpect = EXPECT_NONE;
	
	public FourplebsBoardsParser(String source)
	{
		mSource = source;
	}
	
	public BoardCategory convert() throws ParseException
	{
		GroupParser.parse(mSource, this);
		return new BoardCategory("Archives", mBoards);
	}
	
	@Override
	public boolean onStartElement(GroupParser parser, String tagName, String attrs)
	{
		if ("h2".equals(tagName))
		{
			mExpect = EXPECT_CATEGORY;
			return true;
		}
		else if (mBoardCategoryTitle != null)
		{
			if ("a".equals(tagName))
			{
				mExpect = EXPECT_BOARD;
				return true;
			}
		}
		return false;
	}
	
	@Override
	public void onEndElement(GroupParser parser, String tagName)
	{
		
	}
	
	@Override
	public void onText(GroupParser parser, String source, int start, int end)
	{
		
	}
	
	@Override
	public void onGroupComplete(GroupParser parser, String text)
	{
		switch (mExpect)
		{
			case EXPECT_CATEGORY:
			{
				if ("Archives".equals(text))
				{
					mBoardCategoryTitle = StringUtils.clearHtml(text);
				}
				else
				{
					mBoardCategoryTitle = null;
				}
				break;
			}
			case EXPECT_BOARD:
			{
				text = StringUtils.clearHtml(text).substring(1);
				int index = text.indexOf('/');
				if (index >= 0)
				{
					String boardName = text.substring(0, index);
					String title = text.substring(index + 2);
					mBoards.add(new Board(boardName, title));
				}
				break;
			}
		}
		mExpect = EXPECT_NONE;
	}
}