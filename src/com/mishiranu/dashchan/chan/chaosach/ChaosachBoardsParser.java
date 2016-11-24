package com.mishiranu.dashchan.chan.chaosach;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.text.ParseException;
import chan.text.TemplateParser;
import chan.util.StringUtils;

public class ChaosachBoardsParser {
	private final String source;

	private String boardCategoryTitle;
	private final ArrayList<Board> boards = new ArrayList<>();
	private final ArrayList<BoardCategory> boardCategories = new ArrayList<>();

	private static final Pattern PATTERN_BOARD = Pattern.compile("/(.*)/ - (.*)");

	public ChaosachBoardsParser(String source) {
		this.source = source;
	}

	public ArrayList<BoardCategory> convert() throws ParseException {
		PARSER.parse(source, this);
		closeCategory();
		return boardCategories;
	}

	private void closeCategory() {
		if (!boards.isEmpty()) {
			boardCategories.add(new BoardCategory(boardCategoryTitle, boards));
			boards.clear();
		}
	}

	static String transformBoardTitle(String boardName, String title) {
		String end = " [/" + boardName + "/]";
		if (title.endsWith(end)) {
			title = title.substring(0, title.length() - end.length());
		}
		return title;
	}

	private static final TemplateParser<ChaosachBoardsParser> PARSER = new TemplateParser<ChaosachBoardsParser>()
			.name("optgroup").open((instance, holder, tagName, attributes) -> {
		holder.closeCategory();
		holder.boardCategoryTitle = StringUtils.clearHtml(attributes.get("label")).trim();
		return false;
	}).name("option").content((instance, holder, text) -> {
		Matcher matcher = PATTERN_BOARD.matcher(StringUtils.clearHtml(text));
		if (matcher.matches()) {
			String boardName = matcher.group(1);
			String title = matcher.group(2);
			holder.boards.add(new Board(boardName, transformBoardTitle(boardName, title.trim())));
		}
	}).name("select").close((instance, holder, tagName) -> instance.finish()).prepare();
}