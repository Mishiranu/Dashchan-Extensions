package com.mishiranu.dashchan.chan.sevenchan;

import java.util.ArrayList;
import java.util.Arrays;

import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.text.GroupParser;
import chan.text.ParseException;
import chan.util.StringUtils;

public class SevenchanBoardsParser implements GroupParser.Callback
{
	private final String mSource;
	
	private final ArrayList<BoardCategory> mBoardCategories = new ArrayList<>();
	private final ArrayList<Board> mBoards = new ArrayList<>();

	private boolean mBoardsReached = false;
	private String mBoardCategoryTitle;
	
	private static final int EXPECT_NONE = 0;
	private static final int EXPECT_CATEGORY = 1;
	
	private int mExpect = EXPECT_NONE;
	
	public SevenchanBoardsParser(String source)
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
	
	private static class FinishedException extends ParseException
	{
		private static final long serialVersionUID = 1L;
	}
	
	@Override
	public boolean onStartElement(GroupParser parser, String tagName, String attrs)
	{
		if ("section".equals(tagName))
		{
			String id = parser.getAttr(attrs, "id");
			if ("boardlist".equals(id))
			{
				mBoardsReached = true;
			}
		}
		else if (mBoardsReached)
		{
			if ("h4".equals(tagName))
			{
				closeCategory();
				mExpect = EXPECT_CATEGORY;
				return true;
			}
			else if ("a".equals(tagName))
			{
				String title = parser.getAttr(attrs, "title");
				String href = parser.getAttr(attrs, "href");
				String boardName = href.substring(1, href.length() - 1);
				if (title.startsWith("7chan - ")) title = title.substring(8);
				title = title.trim();
				mBoards.add(new Board(boardName, title));
			}
		}
		return false;
	}
	
	@Override
	public void onEndElement(GroupParser parser, String tagName) throws FinishedException
	{
		if (mBoardsReached && "section".equals(tagName))
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
		switch (mExpect)
		{
			case EXPECT_CATEGORY:
			{
				mBoardCategoryTitle = StringUtils.clearHtml(text).substring(3);
				break;
			}
		}
		mExpect = EXPECT_NONE;
	}
}