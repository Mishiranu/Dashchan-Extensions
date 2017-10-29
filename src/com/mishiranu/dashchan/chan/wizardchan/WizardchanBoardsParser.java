package com.mishiranu.dashchan.chan.wizardchan;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.text.ParseException;
import chan.text.TemplateParser;
import chan.util.StringUtils;

public class WizardchanBoardsParser {
	private final String source;

	private final ArrayList<BoardCategory> boardCategories = new ArrayList<>();
	private final ArrayList<Board> boards = new ArrayList<>();

	private String boardCategoryTitle;

	private static final Pattern PATTERN_BOARD_URI = Pattern.compile("/(.*)/index.html");

	public WizardchanBoardsParser(String source) {
		this.source = source;
	}

	public ArrayList<BoardCategory> convert() throws ParseException {
		PARSER.parse(source, this);
		closeCategory();
		return boardCategories;
	}

	private void closeCategory() {
		if (boards.size() > 0) {
			boardCategories.add(new BoardCategory(boardCategoryTitle, boards));
			boards.clear();
		}
	}

	private static final TemplateParser<WizardchanBoardsParser> PARSER =
			TemplateParser.<WizardchanBoardsParser>builder().contains("span", "data-description", "")
			.open((instance, holder, tagName, attributes) -> {
		holder.closeCategory();
		holder.boardCategoryTitle = StringUtils.clearHtml(attributes.get("data-description"));
		return false;
	}).contains("a", "title", "").open((instance, holder, tagName, attributes) -> {
		if (holder.boardCategoryTitle != null) {
			String href = attributes.get("href");
			Matcher matcher = PATTERN_BOARD_URI.matcher(href);
			if (matcher.matches()) {
				String boardName = matcher.group(1);
				String title = StringUtils.clearHtml(attributes.get("title"));
				holder.boards.add(new Board(boardName, title));
			}
		}
		return false;
	}).name("div").close((instance, holder, tagName) -> {
		if (holder.boardCategoryTitle != null) {
			instance.finish();
		}
	}).prepare();
}
