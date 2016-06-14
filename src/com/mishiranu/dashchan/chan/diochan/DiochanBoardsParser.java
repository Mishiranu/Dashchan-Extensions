package com.mishiranu.dashchan.chan.diochan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.text.GroupParser;
import chan.text.ParseException;
import chan.util.StringUtils;

public class DiochanBoardsParser implements GroupParser.Callback
{
	private final String mSource;
	
	private final ArrayList<BoardCategory> mBoardCategories = new ArrayList<>();
	private final ArrayList<Board> mBoards = new ArrayList<>();
	
	private String mBoardCategoryTitle;

	private static final int EXPECT_NONE = 0;
	private static final int EXPECT_CATEGORY = 1;
	
	private int mExpect = EXPECT_NONE;
	
	private static final Pattern BOARD_URI = Pattern.compile("/(\\w+)/");
	
	public DiochanBoardsParser(String source)
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
		if ("a".equals(tagName))
		{
			if ("href=\"#\"".equals(attrs))
			{
				closeCategory();
				mExpect = EXPECT_CATEGORY;
				return true;
			}
			else if (mBoardCategoryTitle != null)
			{
				String title = parser.getAttr(attrs, "title");
				if (title != null)
				{
					Matcher matcher = BOARD_URI.matcher(parser.getAttr(attrs, "href"));
					if (matcher.matches())
					{
						String boardName = matcher.group(1);
						if ("d".equals(boardName) || "sug".equals(boardName)) return false;
						title = StringUtils.clearHtml(title);
						mBoards.add(new Board(boardName, title));
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
				mBoardCategoryTitle = StringUtils.clearHtml(text);
				break;
			}
		}
		mExpect = EXPECT_NONE;
	}
}