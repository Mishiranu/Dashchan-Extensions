package com.mishiranu.dashchan.chan.fourchan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Pair;

import chan.content.ChanConfiguration;
import chan.content.ChanMarkup;
import chan.text.CommentEditor;

public class FourchanChanMarkup extends ChanMarkup
{
	public FourchanChanMarkup()
	{
		addTag("s", TAG_SPOILER);
		addTag("pre", TAG_CODE);
		addTag("span", "quote", TAG_QUOTE);
	}
	
	@Override
	public CommentEditor obtainCommentEditor(String boardName)
	{
		return new CommentEditor.BulletinBoardCodeCommentEditor();
	}
	
	@Override
	public boolean isTagSupported(String boardName, int tag)
	{
		if (tag == TAG_SPOILER || tag == TAG_CODE)
		{
			FourchanChanConfiguration configuration = ChanConfiguration.get(this);
			return configuration.isTagSupported(boardName, tag);
		}
		return false;
	}
	
	private static final Pattern THREAD_LINK = Pattern.compile("(?:^|thread/(\\d+))(?:#p(\\d+))?$");
	
	@Override
	public Pair<String, String> obtainPostLinkThreadPostNumbers(String uriString)
	{
		Matcher matcher = THREAD_LINK.matcher(uriString);
		if (matcher.find()) return new Pair<>(matcher.group(1), matcher.group(2));
		return null;
	}
}