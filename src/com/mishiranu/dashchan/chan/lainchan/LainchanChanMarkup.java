package com.mishiranu.dashchan.chan.lainchan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Pair;

import chan.content.ChanMarkup;
import chan.text.CommentEditor;

public class LainchanChanMarkup extends ChanMarkup
{
	private static final int SUPPORTED_TAGS = TAG_SPOILER | TAG_CODE;
	
	public LainchanChanMarkup()
	{
		addTag("pre", TAG_CODE);
		addTag("span", "quote", TAG_QUOTE);
		addTag("span", "spoiler", TAG_SPOILER);
		addColorable("span");
	}
	
	@Override
	public CommentEditor obtainCommentEditor(String boardName)
	{
		return new CommentEditor.BulletinBoardCodeCommentEditor();
	}
	
	@Override
	public boolean isTagSupported(String boardName, int tag)
	{
		return (SUPPORTED_TAGS & tag) == tag;
	}
	
	private static final Pattern THREAD_LINK = Pattern.compile("(\\d+).html(?:#(\\d+))?$");
	
	@Override
	public Pair<String, String> obtainPostLinkThreadPostNumbers(String uriString)
	{
		Matcher matcher = THREAD_LINK.matcher(uriString);
		if (matcher.find()) return new Pair<>(matcher.group(1), matcher.group(2));
		return null;
	}
}