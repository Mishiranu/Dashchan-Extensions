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

	private String boardTitle;

	public FourchanBoardsParser(String source, Object linked) {
		this.locator = FourchanChanLocator.get(linked);
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
			.content((instance, holder, text) -> {
				if (!text.contains("Not Safe For Work")) {
					holder.closeBoard();
					holder.boardTitle = StringUtils.clearHtml(text);
				}
			})
			.equals("a", "class", "boardlink")
			.open((instance, holder, tagName, attributes) -> {
				if (holder.boardTitle != null) {
					String boardName = holder.locator.getBoardName(Uri.parse(attributes.get("href")));
					if (boardName != null) {
						holder.list.add(boardName);
					}
				}
				return false;
			})
			.name("div")
			.close((i, h, t) -> h.closeBoard())
			.prepare();
}
