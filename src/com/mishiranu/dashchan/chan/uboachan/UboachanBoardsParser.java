package com.mishiranu.dashchan.chan.uboachan;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.text.ParseException;
import chan.text.TemplateParser;
import chan.util.StringUtils;

public class UboachanBoardsParser
{
	private final String mSource;

	private final ArrayList<BoardCategory> mBoardCategories = new ArrayList<>();
	private final ArrayList<Board> mBoards = new ArrayList<>();

	private String mBoardCategoryTitle;
	private String mBoardName;

	private static final Pattern PATTERN_BOARD_URI = Pattern.compile("(\\w+)/");

	public UboachanBoardsParser(String source)
	{
		mSource = source;
	}

	public ArrayList<BoardCategory> convert() throws ParseException
	{
		PARSER.parse(mSource, this);
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

	private static final TemplateParser<UboachanBoardsParser> PARSER = new TemplateParser<UboachanBoardsParser>()
			.name("legend").content((instance, holder, text) ->
	{
		holder.closeCategory();
		holder.mBoardCategoryTitle = StringUtils.clearHtml(text);

	}).name("a").open((instance, holder, tagName, attributes) ->
	{
		String href = attributes.get("href");
		if (href != null)
		{
			Matcher matcher = PATTERN_BOARD_URI.matcher(href);
			if (matcher.matches())
			{
				holder.mBoardName = matcher.group(1);
				return true;
			}
		}
		return false;

	}).content((instance, holder, text) ->
	{
		holder.mBoards.add(new Board(holder.mBoardName, StringUtils.clearHtml(text)));

	}).prepare();
}