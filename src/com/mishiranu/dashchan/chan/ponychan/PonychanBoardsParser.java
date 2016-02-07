package com.mishiranu.dashchan.chan.ponychan;

import java.util.ArrayList;
import java.util.Arrays;

import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.text.GroupParser;
import chan.text.ParseException;
import chan.util.StringUtils;

public class PonychanBoardsParser implements GroupParser.Callback
{
	private final String mSource;
	
	private final ArrayList<BoardCategory> mBoardCategories = new ArrayList<>();
	private final ArrayList<Board> mBoards = new ArrayList<>();
	
	private boolean mBoardsBlock;
	private String mBoardCategoryTitle;
	private String mBoardName;
	private String mBoardTitle;

	private static final int EXPECT_NONE = 0;
	private static final int EXPECT_CATEGORY = 1;
	private static final int EXPECT_TITLE = 2;
	private static final int EXPECT_DESCRIPTION = 3;
	
	private int mExpect = EXPECT_NONE;
	
	public PonychanBoardsParser(String source)
	{
		mSource = source;
	}
	
	public ArrayList<BoardCategory> convert() throws ParseException
	{
		GroupParser.parse(mSource, this);
		closeCategory();
		for (BoardCategory boardCategory : mBoardCategories) Arrays.sort(boardCategory.getBoards());
		return mBoardCategories;
	}
	
	private void closeCategory()
	{
		if (mBoardCategoryTitle != null)
		{
			if (mBoards.size() > 0) mBoardCategories.add(new BoardCategory(mBoardCategoryTitle, mBoards));
			mBoardCategoryTitle = null;
			mBoards.clear();
		}
	}
	
	@Override
	public boolean onStartElement(GroupParser parser, String tagName, String attrs)
	{
		if ("div".equals(tagName))
		{
			mBoardsBlock = "boards".equals(parser.getAttr(attrs, "id"));
		}
		else if (mBoardsBlock)
		{
			if ("h3".equals(tagName))
			{
				closeCategory();
				mExpect = EXPECT_CATEGORY;
				return true;
			}
			else if ("a".equals(tagName))
			{
				String href = parser.getAttr(attrs, "href");
				mBoardName = href.substring(0, href.length() - 1);
				mExpect = EXPECT_TITLE;
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
		if (mExpect == EXPECT_DESCRIPTION)
		{
			String description = source.substring(start + 3, end);
			mBoards.add(new Board(mBoardName, mBoardTitle, description));
			mExpect = EXPECT_NONE;
		}
	}
	
	@Override
	public void onGroupComplete(GroupParser parser, String text)
	{
		switch (mExpect)
		{
			case EXPECT_CATEGORY:
			{
				mBoardCategoryTitle = text;
				break;
			}
			case EXPECT_TITLE:
			{
				mBoardTitle = StringUtils.clearHtml(text).substring(text.indexOf('-') + 2);
				mExpect = EXPECT_DESCRIPTION;
				return;
			}
		}
		mExpect = EXPECT_NONE;
	}
}