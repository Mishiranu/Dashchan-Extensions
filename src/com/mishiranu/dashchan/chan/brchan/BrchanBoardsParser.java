package com.mishiranu.dashchan.chan.brchan;

import java.util.ArrayList;

import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.text.ParseException;
import chan.text.TemplateParser;
import chan.util.StringUtils;

public class BrchanBoardsParser {
	private final String source;

	private final ArrayList<Board> boards = new ArrayList<>();

	private boolean boardListParsing = false;

	public BrchanBoardsParser(String source) {
		this.source = source;
	}

	public BoardCategory convert() throws ParseException {
		PARSER.parse(source, this);
		return new BoardCategory(null, boards);
	}

	private static final TemplateParser<BrchanBoardsParser> PARSER = TemplateParser.<BrchanBoardsParser>builder()
			.equals("div", "class", "boardlist").open((instance, holder, tagName, attributes) -> {
		holder.boardListParsing = true;
		return false;
	}).ends("a", "href", "/index.html").open((instance, holder, tagName, attributes) -> {
		if (holder.boardListParsing) {
			String href = attributes.get("href");
			String boardName = href.substring(1, href.lastIndexOf('/'));
			String title = StringUtils.clearHtml(attributes.get("title"));
			holder.boards.add(new Board(boardName, title));
		}
		return false;
	}).name("div").close((instance, holder, tagName) -> {
		if (holder.boardListParsing) {
			instance.finish();
		}
	}).prepare();
}
