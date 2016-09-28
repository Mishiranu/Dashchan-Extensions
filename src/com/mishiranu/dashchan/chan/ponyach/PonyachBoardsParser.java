package com.mishiranu.dashchan.chan.ponyach;

import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.text.ParseException;
import chan.text.TemplateParser;
import chan.util.StringUtils;

public class PonyachBoardsParser
{
	private final String mSource;

	private final ArrayList<Board> mBoards = new ArrayList<>();

	private boolean mBoardListParsing = false;

	private static final Pattern PATTERN_BOARD_URI = Pattern.compile("/(.*?)/?");

	public PonyachBoardsParser(String source)
	{
		mSource = source;
	}

	public BoardCategory convert() throws ParseException
	{
		PARSER.parse(mSource, this);
		return new BoardCategory("Доски", mBoards);
	}

	private static final TemplateParser<PonyachBoardsParser> PARSER = new TemplateParser<PonyachBoardsParser>()
			.equals("div", "class", "navbar").open((i, h, t, a) -> !(h.mBoardListParsing = true))
			.name("a").open((instance, holder, tagName, attributes) ->
	{
		if (holder.mBoardListParsing)
		{
			String href = attributes.get("href");
			Matcher matcher = PATTERN_BOARD_URI.matcher(href);
			if (matcher.matches())
			{
				String boardName = matcher.group(1);
				String title = StringUtils.clearHtml(attributes.get("title"));
				title = validateBoardTitle(boardName, title);
				holder.mBoards.add(new Board(boardName, title));
			}
		}
		return false;

	}).text((instance, holder, source, start, end) ->
	{
		if (holder.mBoardListParsing && source.substring(start, end).contains("]")) instance.finish();

	}).prepare();

	static String validateBoardTitle(String boardName, String title)
	{
		if ("b".equals(boardName)) return "Was never good";
		else if ("r34".equals(boardName)) return "My little pony Rule 34";
		else if ("rf".equals(boardName)) return "Убежище";
		if (title != null && title.length() > 2)
		{
			int index = title.indexOf('-');
			if (index == -1) index = title.indexOf("— ");
			if (index >= 0) title = title.substring(index + 2);
			title = title.substring(0, 1).toUpperCase(Locale.getDefault()) + title.substring(1);
		}
		return title;
	}
}