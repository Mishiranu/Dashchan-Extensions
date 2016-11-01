package com.mishiranu.dashchan.chan.kropyvach;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.text.ParseException;
import chan.text.TemplateParser;
import chan.util.StringUtils;

public class KropyvachBoardsParser
{
	private final String mSource;

	private final ArrayList<Board> mBoards = new ArrayList<>();

	private static final Pattern PATTERN_BOARD_URI = Pattern.compile("/(\\w+)/");

	public KropyvachBoardsParser(String source)
	{
		mSource = source;
	}

	public BoardCategory convert() throws ParseException
	{
		PARSER.parse(mSource, this);
		return new BoardCategory(null, mBoards);
	}

	private static final TemplateParser<KropyvachBoardsParser> PARSER = new TemplateParser<KropyvachBoardsParser>()
			.name("a").open((instance, holder, tagName, attributes) ->
	{
		String href = attributes.get("href");
		if (href != null)
		{
			Matcher matcher = PATTERN_BOARD_URI.matcher(href);
			if (matcher.matches())
			{
				String title = StringUtils.clearHtml(attributes.get("title")).trim();
				holder.mBoards.add(new Board(matcher.group(1), title));
			}
		}
		return false;

	}).name("div").close((instance, holder, tagName) -> instance.finish()).prepare();
}