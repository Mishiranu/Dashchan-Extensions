package com.mishiranu.dashchan.chan.chuckdfwk;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Pair;

import chan.content.ChanMarkup;
import chan.text.CommentEditor;

public class ChuckDfwkChanMarkup extends ChanMarkup
{
	private static final int SUPPORTED_TAGS = TAG_BOLD | TAG_ITALIC | TAG_SPOILER;
	
	public ChuckDfwkChanMarkup()
	{
		addTag("b", TAG_BOLD);
		addTag("i", TAG_ITALIC);
		addTag("span", "unkfunc", TAG_QUOTE);
		addTag("span", "spoiler", TAG_SPOILER);
	}
	
	@Override
	public CommentEditor obtainCommentEditor(String boardName)
	{
		return new CommentEditor.WakabaMarkCommentEditor();
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