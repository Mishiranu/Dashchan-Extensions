package com.mishiranu.dashchan.chan.shanachan;

import java.util.ArrayList;

import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.text.GroupParser;
import chan.text.ParseException;
import chan.util.StringUtils;

public class ShanachanBoardsParser implements GroupParser.Callback
{
	private final String mSource;
	
	private final ArrayList<Board> mBoards = new ArrayList<>();
	
	public ShanachanBoardsParser(String source)
	{
		mSource = source;
	}
	
	public BoardCategory convert() throws ParseException
	{
		GroupParser.parse(mSource, this);
		return new BoardCategory(null, mBoards);
	}
	
	@Override
	public boolean onStartElement(GroupParser parser, String tagName, String attrs)
	{
		if ("a".equals(tagName))
		{
			String title = parser.getAttr(attrs, "title");
			if (title != null)
			{
				String boardName = parser.getAttr(attrs, "href");
				if (boardName != null)
				{
					title = StringUtils.clearHtml(title);
					mBoards.add(new Board(boardName, title));
				}
			}
		}
		return false;
	}
	
	@Override
	public void onEndElement(GroupParser parser, String tagName)
	{
		
	}
	
	@Override
	public void onText(GroupParser parser, String source, int start, int end)
	{
		
	}
	
	@Override
	public void onGroupComplete(GroupParser parser, String text)
	{
		
	}
}