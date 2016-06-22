package com.mishiranu.dashchan.chan.ponychan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Pair;

import chan.content.ChanMarkup;
import chan.text.CommentEditor;

public class PonychanChanMarkup extends ChanMarkup
{
	private static final int SUPPORTED_TAGS = TAG_BOLD | TAG_ITALIC | TAG_UNDERLINE | TAG_STRIKE | TAG_SPOILER
			| TAG_CODE | TAG_HEADING;
	
	public PonychanChanMarkup()
	{
		addTag("b", TAG_BOLD);
		addTag("i", TAG_ITALIC);
		addTag("em", TAG_ITALIC);
		addTag("s", TAG_STRIKE);
		addTag("strike", TAG_STRIKE);
		addTag("u", TAG_UNDERLINE);
		addTag("span", "quote", TAG_QUOTE);
		addTag("span", "heading", TAG_HEADING);
		addTag("span", "spoiler", TAG_SPOILER);
		addTag("span", "style", "font-family: monospace", TAG_CODE);
		addBlock("div", true, true); // Better look for expandable blocks
	}
	
	@Override
	public CommentEditor obtainCommentEditor(String boardName)
	{
		CommentEditor commentEditor = new CommentEditor.BulletinBoardCodeCommentEditor();
		commentEditor.addTag(TAG_SPOILER, "[?]", "[/?]");
		commentEditor.addTag(TAG_CODE, "[tt]", "[/tt]");
		commentEditor.addTag(TAG_HEADING, "==", "==", CommentEditor.FLAG_ONE_LINE);
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