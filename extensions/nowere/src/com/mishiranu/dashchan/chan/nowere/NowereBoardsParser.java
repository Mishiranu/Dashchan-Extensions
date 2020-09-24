package com.mishiranu.dashchan.chan.nowere;

import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.text.ParseException;
import chan.text.TemplateParser;
import chan.util.StringUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NowereBoardsParser {
	private final String source;

	private final ArrayList<BoardCategory> boardCategories = new ArrayList<>();
	private final ArrayList<Board> boards = new ArrayList<>();

	private String boardCategoryTitle;
	private String boardName;

	private static final Pattern BOARD_NAME_PATTERN = Pattern.compile("/(\\w+)/");

	public NowereBoardsParser(String source) {
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

	private static final TemplateParser<NowereBoardsParser> PARSER = TemplateParser
			.<NowereBoardsParser>builder()
			.equals("div", "class", "reply")
			.content((instance, holder, text) -> {
				holder.closeCategory();
				holder.boardCategoryTitle = StringUtils.clearHtml(text);
			})
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
			.content((instance, holder, text) -> holder.boards.add(new Board(holder.boardName, text)))
			.prepare();
}
