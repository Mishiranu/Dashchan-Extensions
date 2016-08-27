package com.mishiranu.dashchan.chan.valkyria;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.text.ParseException;
import chan.text.TemplateParser;
import chan.util.StringUtils;

public class ValkyriaBoardsParser
{
	private final String mSource;
	
	private final ArrayList<BoardCategory> mBoardCategories = new ArrayList<>();
	private final ArrayList<Board> mBoards = new ArrayList<>();
	
	private boolean mNavigationParsing = false;
	private String mBoardCategoryTitle;
	private String mBoardName;

	private static final Pattern BOARD_NAME_PATTERN = Pattern.compile("/(\\w+)/");
	
	public ValkyriaBoardsParser(String source)
	{
		mSource = source;
	}
	
	public ArrayList<BoardCategory> convert() throws ParseException
	{
		PARSER.parse(mSource, this);
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
	
	private static final TemplateParser<ValkyriaBoardsParser> PARSER = new TemplateParser<ValkyriaBoardsParser>()
			.equals("div", "class", "board_nav").open((instance, holder, tagName, attributes) ->
	{
		holder.mNavigationParsing = true;
		return false;
		
	}).equals("a", "href", "#").content((instance, holder, text) ->
	{
		if ("Indices".equals(text) || "Misc".equals(text)) instance.finish(); else
		{
			holder.closeCategory();
			holder.mBoardCategoryTitle = StringUtils.clearHtml(text);
		}
		
	}).starts("a", "href", "/").open((instance, holder, tagName, attributes) ->
	{
		String href = attributes.get("href");
		Matcher matcher = BOARD_NAME_PATTERN.matcher(href);
		if (matcher.find())
		{
			holder.mBoardName = matcher.group(1);
			return true;
		}
		return false;
		
	}).content((instance, holder, text) ->
	{
		holder.mBoards.add(new Board(holder.mBoardName, StringUtils.clearHtml(text)));
		
	}).name("div").close((instance, holder, tagName) ->
	{
		if (holder.mNavigationParsing) instance.finish();
		
	}).prepare();
}