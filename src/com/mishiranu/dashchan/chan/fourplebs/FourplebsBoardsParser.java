package com.mishiranu.dashchan.chan.fourplebs;

import java.util.ArrayList;

import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.text.ParseException;
import chan.text.TemplateParser;
import chan.util.StringUtils;

public class FourplebsBoardsParser
{
	private final String mSource;
	private final ArrayList<Board> mBoards = new ArrayList<>();

	private String mBoardCategoryTitle;

	public FourplebsBoardsParser(String source)
	{
		mSource = source;
	}

	public BoardCategory convert() throws ParseException
	{
		PARSER.parse(mSource, this);
		return new BoardCategory("Archives", mBoards);
	}

	private static final TemplateParser<FourplebsBoardsParser> PARSER = new TemplateParser<FourplebsBoardsParser>()
			.name("h2").content((instance, holder, text) ->
	{
		if ("Archives".equals(text)) holder.mBoardCategoryTitle = StringUtils.clearHtml(text);
		else holder.mBoardCategoryTitle = null;

	}).name("a").open((i, h, t, a) -> h.mBoardCategoryTitle != null).content((instance, holder, text) ->
	{
		text = StringUtils.clearHtml(text).substring(1);
		int index = text.indexOf('/');
		if (index >= 0)
		{
			String boardName = text.substring(0, index);
			String title = text.substring(index + 2);
			holder.mBoards.add(new Board(boardName, title));
		}

	}).prepare();
}