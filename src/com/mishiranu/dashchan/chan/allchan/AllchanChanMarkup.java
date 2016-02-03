package com.mishiranu.dashchan.chan.allchan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Pair;

import chan.content.ChanMarkup;
import chan.text.CommentEditor;

public class AllchanChanMarkup extends ChanMarkup
{
	private static final int SUPPORTED_TAGS = TAG_BOLD | TAG_ITALIC | TAG_UNDERLINE | TAG_STRIKE
			| TAG_SUBSCRIPT | TAG_SUPERSCRIPT | TAG_SPOILER;
	
	public AllchanChanMarkup()
	{
		addTag("strong", TAG_BOLD);
		addTag("em", TAG_ITALIC);
		addTag("s", TAG_STRIKE);
		addTag("u", TAG_UNDERLINE);
		addTag("sub", TAG_SUBSCRIPT);
		addTag("sup", TAG_SUPERSCRIPT);
		addTag("pre", TAG_CODE);
		addTag("span", "spoiler", TAG_SPOILER);
		addTag("span", "quotation", TAG_QUOTE);
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