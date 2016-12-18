package com.mishiranu.dashchan.chan.sevenchan;

import java.util.ArrayList;
import java.util.Arrays;

import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.text.ParseException;
import chan.text.TemplateParser;
import chan.util.StringUtils;

public class SevenchanBoardsParser {
	private final String source;

	private final ArrayList<BoardCategory> boardCategories = new ArrayList<>();
	private final ArrayList<Board> boards = new ArrayList<>();

	private boolean boardsReached = false;
	private String boardCategoryTitle;

	public SevenchanBoardsParser(String source) {
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

	private static final TemplateParser<SevenchanBoardsParser> PARSER = new TemplateParser<SevenchanBoardsParser>()
			.equals("section", "id", "boardlist").open((i, h, t, a) -> !(h.boardsReached = true))
			.name("section").close((instance, holder, tagName) -> {
		if (holder.boardsReached) {
			instance.finish();
		}
	}).name("h4").open((i, h, t, a) -> h.boardsReached).content((instance, holder, text) -> {
		holder.closeCategory();
		holder.boardCategoryTitle = StringUtils.clearHtml(text).substring(3);
	}).name("a").open((instance, holder, tagName, attributes) -> {
		if (holder.boardsReached) {
			String title = attributes.get("title");
			String href = attributes.get("href");
			String boardName = href.substring(1, href.length() - 1);
			if (title.startsWith("7chan - ")) {
				title = title.substring(8);
			}
			title = title.trim();
			holder.boards.add(new Board(boardName, title));
		}
		return false;
	}).prepare();
}