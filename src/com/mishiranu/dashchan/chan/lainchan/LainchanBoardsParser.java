package com.mishiranu.dashchan.chan.lainchan;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.text.ParseException;
import chan.text.TemplateParser;
import chan.util.StringUtils;

public class LainchanBoardsParser {
	private final String source;

	private final ArrayList<BoardCategory> boardCategories = new ArrayList<>();
	private final ArrayList<Board> boards = new ArrayList<>();

	private boolean boardListParsing = false;

	private static final Pattern PATTERN_BOARD_URI = Pattern.compile("/(.*?)/index.html");

	public LainchanBoardsParser(String source) {
		this.source = source;
	}

	public ArrayList<BoardCategory> convert() throws ParseException {
		PARSER.parse(source, this);
		return boardCategories;
	}

	private void closeCategory() {
		if (boards.size() > 0) {
			boardCategories.add(new BoardCategory(Integer.toString(boardCategories.size()), boards));
			boards.clear();
		}
	}

	private static final TemplateParser<LainchanBoardsParser> PARSER = new TemplateParser<LainchanBoardsParser>()
			.equals("div", "class", "boardlist").open((i, holder, t, a) -> !(holder.boardListParsing = true))
			.name("div").close((instance, holder, tagName) -> {
		if (holder.boardListParsing) {
			holder.closeCategory();
			instance.finish();
		}
	}).equals("span", "class", "sub").open((instance, holder, tagName, attributes) -> {
		holder.closeCategory();
		return false;
	}).ends("a", "href", "/index.html").open((instance, holder, tagName, attributes) -> {
		Matcher matcher = PATTERN_BOARD_URI.matcher(attributes.get("href"));
		if (matcher.matches()) {
			holder.boards.add(new Board(matcher.group(1), StringUtils.clearHtml(attributes.get("title")).trim()));
		}
		return false;
	}).prepare();
}