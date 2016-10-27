package com.mishiranu.dashchan.chan.onechanca;

import java.util.ArrayList;

import android.net.Uri;

import chan.content.ChanLocator;
import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.text.ParseException;
import chan.text.TemplateParser;
import chan.util.StringUtils;

public class OnechancaBoardsParser
{
	private final String mSource;
	private final OnechancaChanLocator mLocator;

	private final ArrayList<Board> mNewsBoards = new ArrayList<>();
	private final ArrayList<Board> mSocialBoards = new ArrayList<>();

	private String mBoardName;
	private String mBoardTitle;
	private String mBoardDescription;

	private boolean mNewsParsing = false;
	private boolean mSocialParsing = false;

	public OnechancaBoardsParser(String source, Object linked)
	{
		mSource = source;
		mLocator = ChanLocator.get(linked);
	}

	public ArrayList<BoardCategory> convert() throws ParseException
	{
		mNewsBoards.add(new Board("news", "Одобренные"));
		mNewsBoards.add(new Board("news-all", "Все"));
		mNewsBoards.add(new Board("news-hidden", "Скрытые"));
		PARSER.parse(mSource, this);
		ArrayList<BoardCategory> boardCategories = new ArrayList<>();
		boardCategories.add(new BoardCategory("Новости", mNewsBoards));
		boardCategories.add(new BoardCategory("Общение", mSocialBoards));
		return boardCategories;
	}

	private static final TemplateParser<OnechancaBoardsParser> PARSER = new TemplateParser<OnechancaBoardsParser>()
			.equals("div", "class", "b-menu-panel_b-links").open((i, h, t, a) -> !(h.mSocialParsing = true))
			.equals("div", "class", "b-blog-form_b-form_b-field").open((i, h, t, a) -> !(h.mNewsParsing = true))
			.name("a").open((instance, holder, tagName, attributes) ->
	{
		if (holder.mNewsParsing || holder.mSocialParsing)
		{
			String href = attributes.get("href");
			Uri uri = Uri.parse(href);
			String boardName = holder.mLocator.getBoardName(uri);
			if (boardName != null)
			{
				if ("fav".equals(boardName))
				{
					holder.mSocialParsing = false;
					return false;
				}
				holder.mBoardName = boardName;
				return true;
			}
		}
		return false;

	}).content((instance, holder, text) ->
	{
		String title = StringUtils.clearHtml(text);
		if (holder.mNewsParsing)
		{
			int index = title.lastIndexOf(" (");
			if (index >= 0) title = title.substring(0, index);
			holder.mBoardTitle = title;
		}
		else if (holder.mSocialParsing)
		{
			int index = title.indexOf("- ");
			if (index >= 0) title = title.substring(index + 2);
			holder.mSocialBoards.add(new Board(holder.mBoardName, title));
			holder.mBoardName = null;
		}

	}).name("p").open((instance, holder, tagName, attributes) -> holder.mNewsParsing || holder.mSocialParsing)
			.content((instance, holder, text) ->
	{
		holder.mBoardDescription = StringUtils.clearHtml(text);

	}).name("div").close((instance, holder, tagName) ->
	{
		if (holder.mNewsParsing)
		{
			holder.mNewsBoards.add(new Board(holder.mBoardName, holder.mBoardTitle, holder.mBoardDescription));
			holder.mBoardName = null;
			holder.mBoardTitle = null;
			holder.mBoardDescription = null;
		}
		holder.mNewsParsing = false;
		holder.mSocialParsing = false;

	}).equals("li", "class", "m-active").open((i, h, t, a) -> h.mSocialParsing = false).prepare();
}