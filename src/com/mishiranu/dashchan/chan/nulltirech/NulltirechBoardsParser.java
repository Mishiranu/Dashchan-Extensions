package com.mishiranu.dashchan.chan.nulltirech;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.text.GroupParser;
import chan.text.ParseException;
import chan.util.StringUtils;

public class NulltirechBoardsParser implements GroupParser.Callback
{
	private final String mSource;
	
	private final LinkedHashMap<String, CategoryItem> mCategoryItems = new LinkedHashMap<>();
	
	private static class CategoryItem
	{
		public final ArrayList<Board> boards = new ArrayList<Board>();
		public final String title;
		
		public CategoryItem(String title)
		{
			this.title = title;
		}
	}
	
	private static final int EXPECT_NONE = 0;
	private static final int EXPECT_BOARD_CATEGORY_TITLE = 1;
	private static final int EXPECT_BOARD_TITLE = 2;
	
	private int mExpect = EXPECT_NONE;
	
	private CategoryItem mCurrentCategoryItem;
	private String mScript;
	private String mBoardName;
	
	public NulltirechBoardsParser(String source)
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
		ArrayList<BoardCategory> boardCategories = new ArrayList<>();
		for (CategoryItem categoryItem : mCategoryItems.values())
		{
			if (categoryItem.boards.size() > 0)
			{
				boardCategories.add(new BoardCategory(categoryItem.title, categoryItem.boards));
			}
		}
		return boardCategories;
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
			CategoryItem categoryItem = mCategoryItems.get(parser.getAttr(attrs, "onmouseover"));
			if (categoryItem == null && mCurrentCategoryItem != null) throw new FinishedException();
			mCurrentCategoryItem = categoryItem;
		}
		else if ("a".equals(tagName))
		{
			String onmouseover = parser.getAttr(attrs, "onmouseover");
			if (onmouseover != null && onmouseover.startsWith("javascript:menu_show"))
			{
				mScript = onmouseover;
				mExpect = EXPECT_BOARD_CATEGORY_TITLE;
				return true;
			}
			else if (mCurrentCategoryItem != null)
			{
				mBoardName = parser.getAttr(attrs, "title");
				mExpect = EXPECT_BOARD_TITLE;
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
			case EXPECT_BOARD_CATEGORY_TITLE:
			{
				String title = StringUtils.clearHtml(text);
				mCategoryItems.put(mScript, new CategoryItem(title));
				break;
			}
			case EXPECT_BOARD_TITLE:
			{
				String title = StringUtils.clearHtml(text);
				int index = title.indexOf("— ");
				if (index >= 0) title = title.substring(index + 2);
				mCurrentCategoryItem.boards.add(new Board(mBoardName, title));
				break;
			}
		}
		mExpect = EXPECT_NONE;
	}
}