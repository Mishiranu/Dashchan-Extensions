package com.mishiranu.dashchan.chan.chiochan;

import java.util.ArrayList;
import java.util.Arrays;

import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.text.ParseException;
import chan.text.TemplateParser;
import chan.util.StringUtils;

public class ChiochanBoardsParser {
	private static final String[] PREFERRED_BOARDS_ORDER = {"Общее", "Радио", "Аниме", "На пробу"};

	private final String source;

	private final ArrayList<BoardCategory> boardCategories = new ArrayList<>();
	private final ArrayList<Board> boards = new ArrayList<>();

	private String boardCategoryTitle;
	private String boardName;

	public ChiochanBoardsParser(String source) {
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

	private static final TemplateParser<ChiochanBoardsParser> PARSER = new TemplateParser<ChiochanBoardsParser>()
			.name("h2").content((instance, holder, text) -> {
		holder.closeCategory();
		holder.boardCategoryTitle = StringUtils.clearHtml(text).substring(2);
	}).equals("a", "class", "boardlink").open((instance, holder, tagName, attributes) -> {
		if (holder.boardCategoryTitle != null) {
			String href = attributes.get("href");
			holder.boardName = href.substring(1, href.length() - 1);
			return true;
		}
		return false;
	}).content((instance, holder, text) -> {
		text = StringUtils.clearHtml(text);
		holder.boards.add(new Board(holder.boardName, text));
	}).prepare();
}