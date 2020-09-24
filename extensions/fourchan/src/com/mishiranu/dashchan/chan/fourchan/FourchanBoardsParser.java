package com.mishiranu.dashchan.chan.fourchan;

import android.net.Uri;
import chan.text.ParseException;
import chan.text.TemplateParser;
import chan.util.StringUtils;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FourchanBoardsParser {
	private final FourchanChanLocator locator;
	private final String source;

	private final Map<String, List<String>> map = new LinkedHashMap<>();
	private final List<String> list = new ArrayList<>();

	private boolean awaitBoardTitle;
	private String boardTitle;


	public FourchanBoardsParser(FourchanChanLocator locator, String source) {
		this.locator = locator;
		this.source = source;
	}

	public Map<String, List<String>> parse() throws ParseException {
		PARSER.parse(source, this);
		closeBoard();
		return map;
	}

	private void closeBoard() {
		if (boardTitle != null && !list.isEmpty()) {
			List<String> list = new ArrayList<>(this.list);
			map.put(boardTitle, list);
		}
		boardTitle = null;
		list.clear();
	}

	private static final TemplateParser<FourchanBoardsParser> PARSER = TemplateParser
			.<FourchanBoardsParser>builder()
			.name("h3")
			.open((i, h, t, a) -> {
				h.awaitBoardTitle = true;
				return true;
			})
			.content((i, h, t) -> {
				if (!t.contains("Not Safe For Work")) {
					h.closeBoard();
					h.boardTitle = StringUtils.clearHtml(t);
				}
			})
			.equals("a", "class", "boardlink")
			.open((i, h, t, a) -> {
				if (h.boardTitle != null) {
					String boardName = h.locator.getBoardName(Uri.parse(a.get("href")));
					if (boardName != null) {
						h.list.add(boardName);
					}
				}
				return false;
			})
			.name("div")
			.close((i, h, t) -> h.closeBoard())
			.prepare();
}
