package com.mishiranu.dashchan.chan.owlchan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.text.GroupParser;
import chan.text.ParseException;
import chan.util.StringUtils;

public class OwlchanBoardsParser implements GroupParser.Callback
{
	private final String mSource;
	
	private final ArrayList<BoardCategory> mBoardCategories = new ArrayList<>();
	private final ArrayList<Board> mBoards = new ArrayList<>();
	
	private String mBoardCategoryTitle;
	private String mBoardName;

	private static final int EXPECT_NONE = 0;
	private static final int EXPECT_CATEGORY = 1;
	private static final int EXPECT_BOARD = 2;
	
	private int mExpect = EXPECT_NONE;
	
	private static final Pattern BOARD_URI = Pattern.compile("/(\\w+)/$");
	
	public OwlchanBoardsParser(String source)
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
		if ("h2".equals(tagName))
		{
			closeCategory();
			mExpect = EXPECT_CATEGORY;
			return true;
		}
		else if (mBoardCategoryTitle != null)
		{
			if ("a".equals(tagName))
			{
				String cssClass = parser.getAttr(attrs, "class");
				if ("boardlink".equals(cssClass))
				{
					String href = parser.getAttr(attrs, "href");
					Matcher matcher = BOARD_URI.matcher(href);
					if (matcher.find())
					{
						mBoardName = matcher.group(1);
						mExpect = EXPECT_BOARD;
						return true;
					}
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
				mBoardCategoryTitle = StringUtils.clearHtml(text).substring(1).trim();
				break;
			}
			case EXPECT_BOARD:
			{
				text = StringUtils.clearHtml(text);
				mBoards.add(new Board(mBoardName, text));
				break;
			}
		}
		mExpect = EXPECT_NONE;
	}
}