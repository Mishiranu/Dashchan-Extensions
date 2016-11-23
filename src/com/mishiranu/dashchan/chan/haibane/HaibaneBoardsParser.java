package com.mishiranu.dashchan.chan.haibane;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.text.ParseException;
import chan.text.TemplateParser;
import chan.util.StringUtils;

public class HaibaneBoardsParser {
	private final String source;

	private final ArrayList<Board> boards = new ArrayList<>();

	private static final Pattern PATTERN_BOARD = Pattern.compile("/(.*)/ - (.*)");

	public HaibaneBoardsParser(String source) {
		this.source = source;
	}

	public BoardCategory convert() throws ParseException {
		PARSER.parse(source, this);
		return new BoardCategory(null, boards);
	}

	private static final TemplateParser<HaibaneBoardsParser> PARSER = new TemplateParser<HaibaneBoardsParser>()
			.name("option").content((instance, holder, text) -> {
		Matcher matcher = PATTERN_BOARD.matcher(StringUtils.clearHtml(text));
		if (matcher.matches()) {
			String boardName = matcher.group(1);
			String title = matcher.group(2);
			holder.boards.add(new Board(boardName, title));
		}
	}).name("optgroup").close((instance, holder, tagName) -> instance.finish()).prepare();
}