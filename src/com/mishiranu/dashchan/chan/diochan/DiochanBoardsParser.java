package com.mishiranu.dashchan.chan.diochan;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.text.ParseException;
import chan.text.TemplateParser;
import chan.util.StringUtils;

public class DiochanBoardsParser {
	private final String source;

	private final ArrayList<BoardCategory> boardCategories = new ArrayList<>();
	private final ArrayList<Board> boards = new ArrayList<>();

	private boolean boardListParsing = false;

	private static final Pattern PATTERN_BOARD_URI = Pattern.compile("/(.*?)/index.html");

	public DiochanBoardsParser(String source) {
		this.source = source;
	}

	public ArrayList<BoardCategory> convert() throws ParseException {
		PARSER.parse(source, this);
		/*
			Workaround for board parser. Can be improved.
		 */
		ArrayList<BoardCategory> categories = boardCategories;
		for (int i = 0; i < boardCategories.size(); i++){
			for(Board board : boardCategories.get(i).getBoards()){
				if("b".equalsIgnoreCase(board.getBoardName())){
					categories.set(i, new BoardCategory("NSFW", boardCategories.get(i).getBoards()));
					break;
				}
				if("v".equalsIgnoreCase(board.getBoardName())){
					categories.set(i, new BoardCategory("SFW", boardCategories.get(i).getBoards()));
					break;
				}
			}
		}
		Board[] otherBoards = new Board[2];
		otherBoards[0] = new Board("sug", "Suggerimenti & Lamentele");
		otherBoards[1] = new Board("p", "Prova");
		BoardCategory otherCategory = new BoardCategory("Altro", otherBoards);
		categories.add(otherCategory);
		return categories;
	}

	private void closeCategory() {
		if (boards.size() > 0) {
			boardCategories.add(new BoardCategory(Integer.toString(boardCategories.size()), boards));
			boards.clear();
		}
	}

	private static final TemplateParser<DiochanBoardsParser> PARSER = TemplateParser.<DiochanBoardsParser>builder()
			.equals("div", "class", "boardlist").open((i, holder, t, a) -> !(holder.boardListParsing = true))
			.name("div").close((instance, holder, tagName) -> {
		if (holder.boardListParsing) {
			holder.closeCategory();
			instance.finish();
		}
	}).equals("span", "class", "sub").open((instance, holder, tagName, attributes) -> {
		holder.closeCategory();
		return false;
	}).ends("a", "href", "/index.html").open((instance, holder, tagName, attributes) -> {
		Matcher matcher = PATTERN_BOARD_URI.matcher(attributes.get("href"));
		if (matcher.matches()) {
			holder.boards.add(new Board(matcher.group(1), StringUtils.clearHtml(attributes.get("title")).trim()));
		}
		return false;
	}).prepare();
}