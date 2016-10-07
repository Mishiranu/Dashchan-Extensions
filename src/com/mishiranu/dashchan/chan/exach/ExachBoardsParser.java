package com.mishiranu.dashchan.chan.exach;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.text.ParseException;
import chan.text.TemplateParser;
import chan.util.StringUtils;

public class ExachBoardsParser
{
	private final String mSource;

	private final ArrayList<Board> mBoards = new ArrayList<>();

	private boolean mBoardsParsing;
	private String mBoardName;
	private String mDescription;

	private static final Pattern PATTERN_BOARD_URI = Pattern.compile("/(\\w+)\\.php$");

	public ExachBoardsParser(String source)
	{
		mSource = source;
	}

	public BoardCategory convert() throws ParseException
	{
		PARSER.parse(mSource, this);
		return mBoards.size() > 0 ? new BoardCategory(null, mBoards) : null;
	}

	private static final TemplateParser<ExachBoardsParser> PARSER = new TemplateParser<ExachBoardsParser>()
			.name("a").open((instance, holder, tagName, attributes) ->
	{
		Matcher matcher = PATTERN_BOARD_URI.matcher(attributes.get("href"));
		if (matcher.find())
		{
			String boardName = matcher.group(1);
			if (!"p".equals(boardName))
			{
				holder.mBoardName = matcher.group(1);
				holder.mDescription = StringUtils.nullIfEmpty(StringUtils.clearHtml(attributes.get("title")).trim());
				return true;
			}
		}
		return false;

	}).content((instance, holder, text) ->
	{
		holder.mBoards.add(new Board(holder.mBoardName, StringUtils.clearHtml(text).trim(), holder.mDescription));

	}).name("ul").close((instance, holder, tagName) ->
	{
		if (holder.mBoards.size() > 0) instance.finish();

	}).prepare();
}