package com.mishiranu.dashchan.chan.fiftyfive;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.text.GroupParser;
import chan.text.ParseException;
import chan.util.StringUtils;

public class FiftyfiveBoardsParser implements GroupParser.Callback
{
	private final String mSource;
	
	private final ArrayList<BoardCategory> mBoardCategories = new ArrayList<>();
	private final ArrayList<Board> mBoards = new ArrayList<>();
	
	private String mBoardCategoryTitle;
	private String mBoardName;
	
	private static final Pattern PATTERN_BOARD_URI = Pattern.compile("([^/]*)/");
	
	public FiftyfiveBoardsParser(String source)
	{
		mSource = source;
	}
	
	public ArrayList<BoardCategory> convert() throws ParseException
	{
		GroupParser.parse(mSource, this);
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
	
	@Override
	public boolean onStartElement(GroupParser parser, String tagName, String attrs)
	{
		if ("div".equals(tagName))
		{
			String cssClass = parser.getAttr(attrs, "class");
			if ("fav_category_div".equals(cssClass))
			{
				closeCategory();
				mBoardCategoryTitle = StringUtils.clearHtml(parser.getAttr(attrs, "id"));
			}
		}
		else if ("a".equals(tagName))
		{
			String href = parser.getAttr(attrs, "href");
			Matcher matcher = PATTERN_BOARD_URI.matcher(href);
			if (matcher.matches()) mBoardName = matcher.group(1);
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
		if (mBoardName != null)
		{
			String text = StringUtils.clearHtml(source.substring(start, end));
			if (text.startsWith("/" + mBoardName + "/ - ")) text = text.substring(mBoardName.length() + 5);
			mBoards.add(new Board(mBoardName, text));
			mBoardName = null;
		}
	}
	
	@Override
	public void onGroupComplete(GroupParser parser, String text)
	{
		
	}
}