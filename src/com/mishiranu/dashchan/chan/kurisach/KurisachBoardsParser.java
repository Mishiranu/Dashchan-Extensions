package com.mishiranu.dashchan.chan.kurisach;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.text.GroupParser;
import chan.text.ParseException;
import chan.util.StringUtils;

public class KurisachBoardsParser implements GroupParser.Callback {
	private final String source;

	private final ArrayList<Board> boards = new ArrayList<>();

	private String boardName;

	private static final int EXPECT_NONE = 0;
	private static final int EXPECT_BOARD = 1;

	private int expect = EXPECT_NONE;

	private static final Pattern BOARD_URI = Pattern.compile("/(\\w+)/$");

	public KurisachBoardsParser(String source) {
		this.source = source;
	}

	public BoardCategory convert() throws ParseException {
		try {
			GroupParser.parse(source, this);
		} catch (FinishedException e) {
			// Ignore exception
		}
		return new BoardCategory(null, boards);
	}

	private static class FinishedException extends ParseException {
		private static final long serialVersionUID = 1L;
	}

	@Override
	public boolean onStartElement(GroupParser parser, String tagName, String attrs) throws FinishedException {
		if ("a".equals(tagName)) {
			String cssClass = parser.getAttr(attrs, "class");
			if ("boardlink".equals(cssClass)) {
				String href = parser.getAttr(attrs, "href");
				Matcher matcher = BOARD_URI.matcher(href);
				if (matcher.find()) {
					boardName = matcher.group(1);
					expect = EXPECT_BOARD;
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void onEndElement(GroupParser parser, String tagName) throws FinishedException {
		if ("div".equals(tagName) && boards.size() > 0) {
			throw new FinishedException();
		}
	}

	@Override
	public void onText(GroupParser parser, String source, int start, int end) {}

	@Override
	public void onGroupComplete(GroupParser parser, String text) {
		switch (expect) {
			case EXPECT_BOARD: {
				text = StringUtils.clearHtml(text).trim();
				boards.add(new Board(boardName, text));
				break;
			}
		}
		expect = EXPECT_NONE;
	}
}