package com.mishiranu.dashchan.chan.fiftyfive;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.text.GroupParser;
import chan.text.ParseException;
import chan.util.StringUtils;

public class FiftyfiveBoardsParser implements GroupParser.Callback {
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
		GroupParser.parse(source, this);
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

	@Override
	public boolean onStartElement(GroupParser parser, String tagName, String attrs) {
		if ("div".equals(tagName)) {
			String cssClass = parser.getAttr(attrs, "class");
			if ("fav_category_div".equals(cssClass)) {
				closeCategory();
				boardCategoryTitle = StringUtils.clearHtml(parser.getAttr(attrs, "id"));
			}
		} else if ("a".equals(tagName)) {
			String href = parser.getAttr(attrs, "href");
			Matcher matcher = PATTERN_BOARD_URI.matcher(href);
			if (matcher.matches()) {
				boardName = matcher.group(1);
			}
		}
		return false;
	}

	@Override
	public void onEndElement(GroupParser parser, String tagName) {}

	@Override
	public void onText(GroupParser parser, String source, int start, int end) {
		if (boardName != null) {
			String text = StringUtils.clearHtml(source.substring(start, end));
			if (text.startsWith("/" + boardName + "/ - ")) {
				text = text.substring(boardName.length() + 5);
			}
			boards.add(new Board(boardName, text));
			boardName = null;
		}
	}

	@Override
	public void onGroupComplete(GroupParser parser, String text) {}
}
