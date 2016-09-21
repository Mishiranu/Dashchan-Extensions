package com.mishiranu.dashchan.chan.alphachan;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.text.ParseException;
import chan.text.TemplateParser;
import chan.util.StringUtils;

public class AlphachanBoardsParser
{
	private final String mSource;

	private final ArrayList<Board> mBoards = new ArrayList<>();

	private boolean mBoardsParsing;
	private String mBoardName;

	private static final Pattern PATTERN_BOARD_URI = Pattern.compile("/(\\w+)/$");

	public AlphachanBoardsParser(String source)
	{
		mSource = source;
	}

	public BoardCategory convert() throws ParseException
	{
		PARSER.parse(mSource, this);
		return mBoards.size() > 0 ? new BoardCategory("Доски", mBoards) : null;
	}

	private static final TemplateParser<AlphachanBoardsParser> PARSER = new TemplateParser<AlphachanBoardsParser>()
			.equals("div", "class", "boardlist").open((i, holder, t, a) -> !(holder.mBoardsParsing = true))
			.name("a").open((instance, holder, tagName, attributes) ->
	{
		if (holder.mBoardsParsing)
		{
			Matcher matcher = PATTERN_BOARD_URI.matcher(attributes.get("href"));
			if (matcher.find())
			{
				holder.mBoardName = matcher.group(1);
				return true;
			}
		}
		return false;

	}).content((instance, holder, text) ->
	{
		holder.mBoards.add(new Board(holder.mBoardName, StringUtils.clearHtml(text)));

	}).name("div").close((instance, holder, tagName) ->
	{
		if (holder.mBoardsParsing) instance.finish();

	}).prepare();
}