package com.mishiranu.dashchan.chan.erlach;

import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.text.ParseException;
import chan.text.TemplateParser;
import chan.util.StringUtils;
import java.util.ArrayList;
import java.util.Locale;

public class ErlachBoardsParser {
	private final ArrayList<BoardCategory> boardCategories = new ArrayList<>();
	private final ArrayList<Board> boards = new ArrayList<>();

	private String boardCategoryTitle;

	public ArrayList<BoardCategory> convert(String source) throws ParseException {
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

	private static final TemplateParser<ErlachBoardsParser> PARSER = TemplateParser
			.<ErlachBoardsParser>builder()
			.contains("div", "class", "black")
			.content((instance, holder, text) -> {
				holder.closeCategory();
				String title = StringUtils.clearHtml(text);
				if (title.length() > 0) {
					title = title.substring(0, 1).toUpperCase(Locale.US) + title.substring(1).toLowerCase(Locale.US);
				}
				holder.boardCategoryTitle = title;
			})
			.contains("a", "class", "blue")
			.open((instance, holder, tagName, attributes) -> {
				String href = attributes.get("href");
				if (href != null) {
					String boardName = href.substring(1);
					String title = StringUtils.clearHtml(attributes.get("title"));
					holder.boards.add(new Board(boardName, title));
				}
				return false;
			})
			.prepare();
}
