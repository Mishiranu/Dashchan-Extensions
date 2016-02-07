package com.mishiranu.dashchan.chan.taima;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Pair;

import chan.content.ChanMarkup;
import chan.text.CommentEditor;

public class TaimaChanMarkup extends ChanMarkup
{
	private static final int SUPPORTED_TAGS = TAG_BOLD | TAG_ITALIC | TAG_STRIKE | TAG_SUBSCRIPT | TAG_SUPERSCRIPT
			| TAG_SPOILER | TAG_CODE;
	
	public TaimaChanMarkup()
	{
		addTag("strong", TAG_BOLD);
		addTag("em", TAG_ITALIC);
		addTag("s", TAG_STRIKE);
		addTag("sub", TAG_SUBSCRIPT);
		addTag("sup", TAG_SUPERSCRIPT);
		addTag("blockquote", TAG_QUOTE);
		addTag("span", "spoiler", TAG_SPOILER);
		addTag("pre", TAG_CODE);
		addBlock("blockquote", false);
	}
	
	@Override
	public CommentEditor obtainCommentEditor(String boardName)
	{
		CommentEditor commentEditor = new CommentEditor.BulletinBoardCodeCommentEditor();
		commentEditor.addTag(TAG_SUPERSCRIPT, "[super]", "[/super]");
		commentEditor.addTag(TAG_CODE, "[pre]", "[/pre]");
		return commentEditor;
	}
	
	@Override
	public boolean isTagSupported(String boardName, int tag)
	{
		return (SUPPORTED_TAGS & tag) == tag;
	}
	
	private static final Pattern THREAD_LINK = Pattern.compile("(\\d+).php(?:#(\\d+))?$");
	
	@Override
	public Pair<String, String> obtainPostLinkThreadPostNumbers(String uriString)
	{
		Matcher matcher = THREAD_LINK.matcher(uriString);
		if (matcher.find()) return new Pair<>(matcher.group(1), matcher.group(2));
		return null;
	}
}