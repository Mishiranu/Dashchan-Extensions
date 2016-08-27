package com.mishiranu.dashchan.chan.valkyria;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Pair;

import chan.content.ChanMarkup;
import chan.text.CommentEditor;

public class ValkyriaChanMarkup extends ChanMarkup
{
	private static final int SUPPORTED_TAGS = TAG_BOLD | TAG_ITALIC | TAG_UNDERLINE | TAG_STRIKE
			| TAG_SPOILER | TAG_CODE;
	
	public ValkyriaChanMarkup()
	{
		addTag("b", TAG_BOLD);
		addTag("i", TAG_ITALIC);
		addTag("u", TAG_UNDERLINE);
		addTag("s", TAG_STRIKE);
		addTag("tt", TAG_CODE);
		addTag("span", "SpoilerBox", TAG_SPOILER);
		addTag("span", "QuoteText", TAG_QUOTE);
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
	
	private static final Pattern THREAD_LINK = Pattern.compile("topic/(\\d+)(?:#(\\d+))?$");
	
	@Override
	public Pair<String, String> obtainPostLinkThreadPostNumbers(String uriString)
	{
		Matcher matcher = THREAD_LINK.matcher(uriString);
		if (matcher.find()) return new Pair<>(matcher.group(1), matcher.group(2));
		return null;
	}
}