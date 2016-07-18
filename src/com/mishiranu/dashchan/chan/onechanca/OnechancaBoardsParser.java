package com.mishiranu.dashchan.chan.onechanca;

import java.util.ArrayList;

import android.net.Uri;

import chan.content.ChanLocator;
import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.text.GroupParser;
import chan.text.ParseException;
import chan.util.StringUtils;

public class OnechancaBoardsParser implements GroupParser.Callback
{
	private final String mSource;
	private final OnechancaChanLocator mLocator;

	private final ArrayList<Board> mNewsBoards = new ArrayList<>();
	private final ArrayList<Board> mSocialBoards = new ArrayList<>();
	
	private static final int EXPECT_NONE = 0;
	private static final int EXPECT_BOARD_TITLE = 1;
	private static final int EXPECT_BOARD_DESCRIPTION = 2;
	
	private int mExpect = EXPECT_NONE;

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
		try
		{
			GroupParser.parse(mSource, this);
		}
		catch (FinishedException e)
		{
			
		}
		ArrayList<BoardCategory> boardCategories = new ArrayList<>();
		boardCategories.add(new BoardCategory("Новости", mNewsBoards));
		boardCategories.add(new BoardCategory("Общение", mSocialBoards));
		return boardCategories;
	}
	
	private static class FinishedException extends ParseException
	{
		private static final long serialVersionUID = 1L;
	}
	
	@Override
	public boolean onStartElement(GroupParser parser, String tagName, String attrs) throws FinishedException
	{
		if ("div".equals(tagName))
		{
			String cssClass = parser.getAttr(attrs, "class");
			if ("b-menu-panel_b-links".equals(cssClass))
			{
				mSocialParsing = true;
			}
			else if ("b-blog-form_b-form_b-field".equals(cssClass))
			{
				mNewsParsing = true;
			}
		}
		else if (mNewsParsing || mSocialParsing)
		{
			if ("a".equals(tagName))
			{
				String href = parser.getAttr(attrs, "href");
				Uri uri = Uri.parse(href);
				String boardName = mLocator.getBoardName(uri);
				if (boardName != null)
				{
					if ("fav".equals(boardName))
					{
						mSocialParsing = false;
						return false;
					}
					mBoardName = boardName;
					mExpect = EXPECT_BOARD_TITLE;
					return true;
				}
			}
			else if ("p".equals(tagName))
			{
				mExpect = EXPECT_BOARD_DESCRIPTION;
				return true;
			}
			else if ("li".equals(tagName))
			{
				String cssClass = parser.getAttr(attrs, "class");
				if ("m-active".equals(cssClass))
				{
					mSocialParsing = false;
				}
			}
		}
		return false;
	}
	
	@Override
	public void onEndElement(GroupParser parser, String tagName)
	{
		if ("div".equals(tagName))
		{
			if (mNewsParsing)
			{
				mNewsBoards.add(new Board(mBoardName, mBoardTitle, mBoardDescription));
				mBoardName = null;
				mBoardTitle = null;
				mBoardDescription = null;
			}
			mNewsParsing = false;
			mSocialParsing = false;
		}
	}
	
	@Override
	public void onText(GroupParser parser, String source, int start, int end)
	{
		
	}
	
	@Override
	public void onGroupComplete(GroupParser parser, String text)
	{
		switch (mExpect)
		{
			case EXPECT_BOARD_TITLE:
			{
				String title = StringUtils.clearHtml(text);
				if (mNewsParsing)
				{
					int index = title.lastIndexOf(" (");
					if (index >= 0) title = title.substring(0, index);
					mBoardTitle = title;
				}
				else if (mSocialParsing)
				{
					int index = title.indexOf("- ");
					if (index >= 0) title = title.substring(index + 2);
					mSocialBoards.add(new Board(mBoardName, title));
					mBoardName = null;
				}
				break;
			}
			case EXPECT_BOARD_DESCRIPTION:
			{
				mBoardDescription = StringUtils.clearHtml(text);
				break;
			}
		}
		mExpect = EXPECT_NONE;
	}
}