package com.mishiranu.dashchan.chan.dobrochan;

import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.text.ParseException;
import chan.text.TemplateParser;
import chan.util.StringUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DobrochanBoardsParser {
	private static final String[] PREFERRED_BOARDS_ORDER = {"Общее", "Доброчан", "Аниме", "На пробу"};

	private final String source;

	private final ArrayList<BoardCategory> boardCategories = new ArrayList<>();
	private final ArrayList<Board> boards = new ArrayList<>();

	private String boardCategoryTitle;
	private String boardName;

	private static final Pattern BOARD_NAME_PATTERN = Pattern.compile("/(\\w+)/index.xhtml");

	public DobrochanBoardsParser(String source) {
		this.source = source;
	}

	public ArrayList<BoardCategory> convert() throws ParseException {
		PARSER.parse(source, this);
		closeCategory();
		ArrayList<BoardCategory> boardCategories = new ArrayList<>();
		for (String title : PREFERRED_BOARDS_ORDER) {
			for (BoardCategory boardCategory : this.boardCategories) {
				if (title.equals(boardCategory.getTitle())) {
					Arrays.sort(boardCategory.getBoards());
					boardCategories.add(boardCategory);
					break;
				}
			}
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

	private static final TemplateParser<DobrochanBoardsParser> PARSER = TemplateParser
			.<DobrochanBoardsParser>builder()
			.equals("td", "class", "header")
			.open((instance, holder, tagName, attributes) -> {
				holder.closeCategory();
				return true;
			})
			.content((instance, holder, text) -> holder.boardCategoryTitle = StringUtils.clearHtml(text))
			.name("a")
			.open((instance, holder, tagName, attributes) -> {
				if (holder.boardCategoryTitle != null) {
					String href = StringUtils.emptyIfNull(attributes.get("href"));
					Matcher matcher = BOARD_NAME_PATTERN.matcher(href);
					if (matcher.matches()) {
						holder.boardName = matcher.group(1);
						return true;
					}
				}
				return false;
			})
			.content((instance, holder, text) -> {
				text = StringUtils.clearHtml(text).trim();
				text = text.substring(text.indexOf('—') + 2);
				holder.boards.add(new Board(holder.boardName, text));
			})
			.prepare();
}
