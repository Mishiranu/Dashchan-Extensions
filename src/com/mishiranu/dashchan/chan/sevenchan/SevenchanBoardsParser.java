package com.mishiranu.dashchan.chan.sevenchan;

import java.util.ArrayList;
import java.util.Arrays;

import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.text.GroupParser;
import chan.text.ParseException;
import chan.util.StringUtils;

public class SevenchanBoardsParser implements GroupParser.Callback {
	private final String source;

	private final ArrayList<BoardCategory> boardCategories = new ArrayList<>();
	private final ArrayList<Board> boards = new ArrayList<>();

	private boolean boardsReached = false;
	private String boardCategoryTitle;

	private static final int EXPECT_NONE = 0;
	private static final int EXPECT_CATEGORY = 1;

	private int expect = EXPECT_NONE;

	public SevenchanBoardsParser(String source) {
		this.source = source;
	}

	public ArrayList<BoardCategory> convert() throws ParseException {
		try {
			GroupParser.parse(source, this);
		} catch (FinishedException e) {
			// Ignore exception
		}
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

	private static class FinishedException extends ParseException {
		private static final long serialVersionUID = 1L;
	}

	@Override
	public boolean onStartElement(GroupParser parser, String tagName, String attrs) {
		if ("section".equals(tagName)) {
			String id = parser.getAttr(attrs, "id");
			if ("boardlist".equals(id)) {
				boardsReached = true;
			}
		} else if (boardsReached) {
			if ("h4".equals(tagName)) {
				closeCategory();
				expect = EXPECT_CATEGORY;
				return true;
			} else if ("a".equals(tagName)) {
				String title = parser.getAttr(attrs, "title");
				String href = parser.getAttr(attrs, "href");
				String boardName = href.substring(1, href.length() - 1);
				if (title.startsWith("7chan - ")) {
					title = title.substring(8);
				}
				title = title.trim();
				boards.add(new Board(boardName, title));
			}
		}
		return false;
	}

	@Override
	public void onEndElement(GroupParser parser, String tagName) throws FinishedException {
		if (boardsReached && "section".equals(tagName)) {
			throw new FinishedException();
		}
	}

	@Override
	public void onText(GroupParser parser, String source, int start, int end) {}

	@Override
	public void onGroupComplete(GroupParser parser, String text) {
		switch (expect) {
			case EXPECT_CATEGORY: {
				boardCategoryTitle = StringUtils.clearHtml(text).substring(3);
				break;
			}
		}
		expect = EXPECT_NONE;
	}
}