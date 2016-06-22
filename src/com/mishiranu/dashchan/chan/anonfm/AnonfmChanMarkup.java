package com.mishiranu.dashchan.chan.anonfm;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Pair;

import chan.content.ChanMarkup;
import chan.text.CommentEditor;

public class AnonfmChanMarkup extends ChanMarkup
{
	public AnonfmChanMarkup()
	{
		addTag("span", "unkfunc", TAG_QUOTE);
		addTag("h3", TAG_HEADING);
		addTag("em", TAG_ITALIC);
	}
	
	@Override
	public CommentEditor obtainCommentEditor(String boardName)
	{
		return new CommentEditor();
	}
	
	private static final Pattern THREAD_LINK = Pattern.compile("(?:#(\\d+))?$");
	
	@Override
	public Pair<String, String> obtainPostLinkThreadPostNumbers(String uriString)
	{
		Matcher matcher = THREAD_LINK.matcher(uriString);
		if (matcher.find()) return new Pair<>("1", matcher.group(1));
		return null;
	}
}