package com.mishiranu.dashchan.chan.onechanca;

import java.util.ArrayList;

import android.net.Uri;

import chan.content.ChanLocator;
import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.text.ParseException;
import chan.text.TemplateParser;
import chan.util.StringUtils;

public class OnechancaBoardsParser {
	private final String source;
	private final OnechancaChanLocator locator;

	private final ArrayList<Board> newsBoards = new ArrayList<>();
	private final ArrayList<Board> socialBoards = new ArrayList<>();

	private String boardName;
	private String boardTitle;
	private String boardDescription;

	private boolean newsParsing = false;
	private boolean socialParsing = false;

	public OnechancaBoardsParser(String source, Object linked) {
		this.source = source;
		locator = ChanLocator.get(linked);
	}

	public ArrayList<BoardCategory> convert() throws ParseException {
		newsBoards.add(new Board("news", "Одобренные"));
		newsBoards.add(new Board("news-all", "Все"));
		newsBoards.add(new Board("news-hidden", "Скрытые"));
		PARSER.parse(source, this);
		ArrayList<BoardCategory> boardCategories = new ArrayList<>();
		boardCategories.add(new BoardCategory("Новости", newsBoards));
		boardCategories.add(new BoardCategory("Общение", socialBoards));
		return boardCategories;
	}

	private static final TemplateParser<OnechancaBoardsParser> PARSER = new TemplateParser<OnechancaBoardsParser>()
			.equals("div", "class", "b-menu-panel_b-links").open((i, h, t, a) -> !(h.socialParsing = true))
			.equals("div", "class", "b-blog-form_b-form_b-field").open((i, h, t, a) -> !(h.newsParsing = true))
			.name("a").open((instance, holder, tagName, attributes) -> {
		if (holder.newsParsing || holder.socialParsing) {
			String href = attributes.get("href");
			Uri uri = Uri.parse(href);
			String boardName = holder.locator.getBoardName(uri);
			if (boardName != null) {
				if ("fav".equals(boardName)) {
					holder.socialParsing = false;
					return false;
				}
				holder.boardName = boardName;
				return true;
			}
		}
		return false;
	}).content((instance, holder, text) -> {
		String title = StringUtils.clearHtml(text);
		if (holder.newsParsing) {
			int index = title.lastIndexOf(" (");
			if (index >= 0) {
				title = title.substring(0, index);
			}
			holder.boardTitle = title;
		} else if (holder.socialParsing) {
			int index = title.indexOf("- ");
			if (index >= 0) {
				title = title.substring(index + 2);
			}
			holder.socialBoards.add(new Board(holder.boardName, title));
			holder.boardName = null;
		}
	}).name("p").open((instance, holder, tagName, attributes) -> holder.newsParsing || holder.socialParsing)
			.content((instance, holder, text) -> {
		holder.boardDescription = StringUtils.clearHtml(text);
	}).name("div").close((instance, holder, tagName) -> {
		if (holder.newsParsing) {
			holder.newsBoards.add(new Board(holder.boardName, holder.boardTitle, holder.boardDescription));
			holder.boardName = null;
			holder.boardTitle = null;
			holder.boardDescription = null;
		}
		holder.newsParsing = false;
		holder.socialParsing = false;
	}).equals("li", "class", "m-active").open((i, h, t, a) -> h.socialParsing = false).prepare();
}