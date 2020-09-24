package com.mishiranu.dashchan.chan.cirno;

import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.text.ParseException;
import chan.text.TemplateParser;
import chan.util.StringUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CirnoBoardsParser {
	private final String source;

	private final LinkedHashMap<String, BoardCategory> boardCategories = new LinkedHashMap<>();
	private final ArrayList<Board> boards = new ArrayList<>();

	private String boardCategoryTitle;

	private static final Pattern PATTERN_BOARD = Pattern.compile("(.*) \\[(\\w+)]");

	private static final HashMap<String, String> VALID_BOARD_TITLES = new HashMap<>();

	static {
		VALID_BOARD_TITLES.put("tv", "Кино и ТВ");
		VALID_BOARD_TITLES.put("bro", "My Little Pony");
		VALID_BOARD_TITLES.put("m", "Картинки-макросы и копипаста");
		VALID_BOARD_TITLES.put("s", "Электроника и ПО");
		VALID_BOARD_TITLES.put("azu", "Azumanga Daioh");
		VALID_BOARD_TITLES.put("ls", "Lucky\u2606Star");
		VALID_BOARD_TITLES.put("rm", "Rozen Maiden");
		VALID_BOARD_TITLES.put("sos", "Suzumiya Haruhi no Y\u016butsu");
		VALID_BOARD_TITLES.put("hau", "Higurashi no Naku Koro ni");
	}

	public CirnoBoardsParser(String source) {
		this.source = source;
	}

	public ArrayList<BoardCategory> convert() throws ParseException {
		PARSER.parse(source, this);
		closeCategory();
		for (BoardCategory boardCategory : boardCategories.values()) {
			Arrays.sort(boardCategory.getBoards());
		}
		BoardCategory boardCategory = boardCategories.remove("Обсуждения");
		if (boardCategory != null) {
			boardCategories.put(boardCategory.getTitle(), boardCategory);
		}
		return new ArrayList<>(boardCategories.values());
	}

	private void closeCategory() {
		if (boardCategoryTitle != null) {
			if (boards.size() > 0) {
				boardCategories.put(boardCategoryTitle, new BoardCategory(boardCategoryTitle, boards));
			}
			boardCategoryTitle = null;
			boards.clear();
		}
	}

	private static final TemplateParser<CirnoBoardsParser> PARSER = TemplateParser
			.<CirnoBoardsParser>builder()
			.equals("td", "class", "header")
			.content((instance, holder, text) -> {
				holder.closeCategory();
				holder.boardCategoryTitle = StringUtils.clearHtml(text);
			})
			.equals("a", "target", "board")
			.content((instance, holder, text) -> {
				Matcher matcher = PATTERN_BOARD.matcher(StringUtils.clearHtml(text));
				if (matcher.matches()) {
					String boardName = matcher.group(2);
					String title = VALID_BOARD_TITLES.get(boardName);
					if (title == null) {
						title = matcher.group(1);
						if (!title.isEmpty()) {
							title = title.toLowerCase(Locale.getDefault());
							title = title.substring(0, 1).toUpperCase(Locale.getDefault()) + title.substring(1);
						}
					}
					holder.boards.add(new Board(boardName, title));
				}
			})
			.prepare();
}
