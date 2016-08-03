package com.mishiranu.dashchan.chan.chiochan;

import java.util.ArrayList;
import java.util.Arrays;

import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.text.ParseException;
import chan.text.TemplateParser;
import chan.util.StringUtils;

public class ChiochanBoardsParser
{
	private static final String[] PREFERRED_BOARDS_ORDER = {"Общее", "Радио", "Аниме", "На пробу"};
	
	private final String mSource;
	
	private final ArrayList<BoardCategory> mBoardCategories = new ArrayList<>();
	private final ArrayList<Board> mBoards = new ArrayList<>();
	
	private String mBoardCategoryTitle;
	private String mBoardName;

	public ChiochanBoardsParser(String source)
	{
		mSource = source;
	}
	
	public ArrayList<BoardCategory> convert() throws ParseException
	{
		PARSER.parse(mSource, this);
		closeCategory();
		ArrayList<BoardCategory> boardCategories = new ArrayList<>();
		for (String title : PREFERRED_BOARDS_ORDER)
		{
			for (BoardCategory boardCategory : mBoardCategories)
			{
				if (title.equals(boardCategory.getTitle()))
				{
					Arrays.sort(boardCategory.getBoards());
					boardCategories.add(boardCategory);
					break;
				}
			}
		}
		return boardCategories;
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
	
	private static final TemplateParser<ChiochanBoardsParser> PARSER = new TemplateParser<ChiochanBoardsParser>()
			.name("h2").content((instance, holder, text) ->
	{
		holder.closeCategory();
		holder.mBoardCategoryTitle = StringUtils.clearHtml(text).substring(2);
		
	}).equals("a", "class", "boardlink").open((instance, holder, tagName, attributes) ->
	{
		if (holder.mBoardCategoryTitle != null)
		{
			String href = attributes.get("href");
			holder.mBoardName = href.substring(1, href.length() - 1);
			return true;
		}
		return false;
		
	}).content((instance, holder, text) ->
	{
		text = StringUtils.clearHtml(text);
		holder.mBoards.add(new Board(holder.mBoardName, text));
		
	}).prepare();
}