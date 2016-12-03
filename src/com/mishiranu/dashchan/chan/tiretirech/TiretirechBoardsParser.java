package com.mishiranu.dashchan.chan.tiretirech;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.text.ParseException;
import chan.text.TemplateParser;
import chan.util.StringUtils;

public class TiretirechBoardsParser {
	private final String source;

	private final ArrayList<BoardCategory> boardCategories = new ArrayList<>();
	private final ArrayList<Board> boards = new ArrayList<>();

	private String boardCategoryTitle;
	private String boardName;

	private static final Pattern BOARD_URI = Pattern.compile("(\\w+)/");

	public TiretirechBoardsParser(String source) {
		this.source = source;
	}

	public ArrayList<BoardCategory> convert() throws ParseException {
		PARSER.parse(source, this);
		closeCategory();
		for (BoardCategory boardCategory : boardCategories) {
			Arrays.sort(boardCategory.getBoards());
		}
		return boardCategories;
	}

	private void closeCategory() {
		if (boardCategoryTitle != null) {
			if (boards.size() > 0) {
				boardCategories.add(new BoardCategory(boardCategoryTitle, boards));
			}
			boardCategoryTitle = null;
			boards.clear();
		}
	}

	private static final TemplateParser<TiretirechBoardsParser> PARSER = new TemplateParser<TiretirechBoardsParser>()
			.name("dt").content((instance, holder, text) -> {
		holder.closeCategory();
		holder.boardCategoryTitle = StringUtils.clearHtml(text).trim();
	}).name("a").open((instance, holder, tagName, attributes) -> {
		if (holder.boardCategoryTitle != null) {
			String href = attributes.get("href");
			Matcher matcher = BOARD_URI.matcher(href);
			if (matcher.matches()) {
				holder.boardName = matcher.group(1);
				return true;
			}
		}
		return false;
	}).content((instance, holder, text) -> {
		text = StringUtils.clearHtml(text);
		int index = text.indexOf("— ");
		if (index >= 0) {
			text = text.substring(index + 2);
		}
		holder.boards.add(new Board(holder.boardName, text));
	}).prepare();
}