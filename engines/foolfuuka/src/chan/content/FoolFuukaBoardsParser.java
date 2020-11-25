package chan.content;

import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.text.ParseException;
import chan.text.TemplateParser;
import chan.util.StringUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class FoolFuukaBoardsParser {
	private final ArrayList<Board> boards = new ArrayList<>();

	private String boardCategoryTitle;

	public BoardCategory convert(InputStream input) throws IOException, ParseException {
		PARSER.parse(new InputStreamReader(input), this);
		return new BoardCategory("Archives", boards);
	}

	private static final TemplateParser<FoolFuukaBoardsParser> PARSER = TemplateParser
			.<FoolFuukaBoardsParser>builder()
			.name("h2")
			.content((instance, holder, text) -> {
				if ("Archives".equals(text)) {
					holder.boardCategoryTitle = StringUtils.clearHtml(text);
				} else {
					holder.boardCategoryTitle = null;
				}
			})
			.name("a")
			.open((i, h, t, a) -> h.boardCategoryTitle != null)
			.content((instance, holder, text) -> {
				text = StringUtils.clearHtml(text).substring(1);
				int index = text.indexOf('/');
				if (index >= 0) {
					String boardName = text.substring(0, index);
					String title = text.substring(index + 2);
					holder.boards.add(new Board(boardName, title));
				}
			})
			.prepare();
}
