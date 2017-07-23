package com.mishiranu.dashchan.chan.fiftyfive;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.text.ParseException;
import chan.text.TemplateParser;
import chan.util.StringUtils;

public class FiftyfiveBoardsParser {
	private final String source;

	private final ArrayList<BoardCategory> boardCategories = new ArrayList<>();
	private final ArrayList<Board> boards = new ArrayList<>();

	private String boardCategoryTitle;
	private String boardName;

	private static final Pattern PATTERN_BOARD_URI = Pattern.compile("([^/]*)/");

	public FiftyfiveBoardsParser(String source) {
		this.source = source;
	}

	public ArrayList<BoardCategory> convert() throws ParseException {
		PARSER.parse(source, this);
		closeCategory();
		return boardCategories;
	}

	private void closeCategory() {
		ArrayList<Board> boards = this.boards;
		if (boards.size() > 0) {
			boardCategories.add(new BoardCategory(boardCategoryTitle, boards));
			boards.clear();
		}
	}

	private static final TemplateParser<FiftyfiveBoardsParser> PARSER = TemplateParser.<FiftyfiveBoardsParser>builder()
			.equals("div", "class", "fav_category_div").open((instance, holder, tagName, attributes) -> {
		holder.closeCategory();
		holder.boardCategoryTitle = StringUtils.clearHtml(attributes.get("id"));
		return false;
	}).name("a").open((instance, holder, tagName, attributes) -> {
		String href = attributes.get("href");
		Matcher matcher = PATTERN_BOARD_URI.matcher(href);
		if (matcher.matches()) {
			holder.boardName = matcher.group(1);
		}
		return false;
	}).text((instance, holder, source, start, end) -> {
		if (holder.boardName != null) {
			String text = StringUtils.clearHtml(source.substring(start, end));
			if (text.startsWith("/" + holder.boardName + "/ - ")) {
				text = text.substring(holder.boardName.length() + 5);
			}
			holder.boards.add(new Board(holder.boardName, text));
			holder.boardName = null;
		}
	}).prepare();
}
