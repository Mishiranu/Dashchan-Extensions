package com.mishiranu.dashchan.chan.lainchan;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.text.ParseException;
import chan.text.TemplateParser;
import chan.util.StringUtils;

public class LainchanBoardsParser
{
	private final String mSource;

	private final ArrayList<BoardCategory> mBoardCategories = new ArrayList<>();
	private final ArrayList<Board> mBoards = new ArrayList<>();

	private boolean mBoardListParsing = false;

	private static final Pattern PATTERN_BOARD_URI = Pattern.compile("/(.*?)/index.html");

	public LainchanBoardsParser(String source)
	{
		mSource = source;
	}

	public ArrayList<BoardCategory> convert() throws ParseException
	{
		PARSER.parse(mSource, this);
		return mBoardCategories;
	}

	private void closeCategory()
	{
		ArrayList<Board> boards = mBoards;
		if (boards.size() > 0)
		{
			mBoardCategories.add(new BoardCategory(Integer.toString(mBoardCategories.size()), boards));
			mBoards.clear();
		}
	}

	private static final TemplateParser<LainchanBoardsParser> PARSER = new TemplateParser<LainchanBoardsParser>()
			.equals("div", "class", "boardlist").open((i, holder, t, a) -> !(holder.mBoardListParsing = true))
			.name("div").close((instance, holder, tagName) ->
	{
		if (holder.mBoardListParsing)
		{
			holder.closeCategory();
			instance.finish();
		}

	}).equals("span", "class", "sub").open((instance, holder, tagName, attributes) ->
	{
		holder.closeCategory();
		return false;

	}).ends("a", "href", "/index.html").open((instance, holder, tagName, attributes) ->
	{
		Matcher matcher = PATTERN_BOARD_URI.matcher(attributes.get("href"));
		if (matcher.matches())
		{
			holder.mBoards.add(new Board(matcher.group(1), StringUtils.clearHtml(attributes.get("title")).trim()));
		}
		return false;

	}).prepare();
}