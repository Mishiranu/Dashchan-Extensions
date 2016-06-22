package com.mishiranu.dashchan.chan.nulltirech;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Pair;

import chan.content.ChanMarkup;
import chan.text.CommentEditor;

public class NulltirechChanMarkup extends ChanMarkup
{
	private static final int SUPPORTED_TAGS = TAG_BOLD | TAG_ITALIC | TAG_UNDERLINE | TAG_STRIKE | TAG_SPOILER
			| TAG_CODE;
	
	public NulltirechChanMarkup()
	{
		addTag("b", TAG_BOLD);
		addTag("i", TAG_ITALIC);
		addTag("strike", TAG_STRIKE);
		addTag("span", "unkfunc", TAG_QUOTE);
		addTag("span", "spoiler", TAG_SPOILER);
		addTag("span", "style", "border-bottom: 1px solid", TAG_UNDERLINE);
		addTag("span", "style", "font-family: Mona,'MS PGothic' !important;", TAG_CODE);
		addTag("div", "codeblock", TAG_CODE);
		addBlock("pre", true, false); // Remove spacing
	}
	
	@Override
	public CommentEditor obtainCommentEditor(String boardName)
	{
		CommentEditor commentEditor = new CommentEditor.BulletinBoardCodeCommentEditor();
		commentEditor.addTag(TAG_CODE, "[code=text]", "[/code]");
		return commentEditor;
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