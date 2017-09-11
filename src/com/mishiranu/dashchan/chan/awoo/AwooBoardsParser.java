package com.mishiranu.dashchan.chan.awoo;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.text.ParseException;
import chan.text.TemplateParser;
import chan.util.StringUtils;

public class AwooBoardsParser {
	private final String source;

	private final ArrayList<Board> boards = new ArrayList<>();

	private String boardName;

	public AwooBoardsParser(String source) {
		this.source = source;
	}

	public BoardCategory convert() throws ParseException {
		PARSER.parse(source, this);
		return new BoardCategory(null, boards);
	}

	private static final TemplateParser<AwooBoardsParser> PARSER = TemplateParser.<AwooBoardsParser>builder()
			.equals("a", "class", "boarda").open((instance, holder, tagName, attributes) -> {
		holder.boardName = attributes.get("href").substring(1);
		return true;
	}).content((instance, holder, text) -> {
		holder.boards.add(new Board(holder.boardName, StringUtils.clearHtml(text)));
	}).prepare();
}
