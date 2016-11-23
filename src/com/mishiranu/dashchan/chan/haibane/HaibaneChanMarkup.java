package com.mishiranu.dashchan.chan.haibane;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Pair;

import chan.content.ChanMarkup;
import chan.text.CommentEditor;

public class HaibaneChanMarkup extends ChanMarkup {
	private static final int SUPPORTED_TAGS = TAG_BOLD | TAG_ITALIC | TAG_UNDERLINE | TAG_STRIKE
			| TAG_SPOILER | TAG_CODE;

	public HaibaneChanMarkup() {
		addTag("strong", TAG_BOLD);
		addTag("em", TAG_ITALIC);
		addTag("u", TAG_UNDERLINE);
		addTag("span", "style", "text-decoration:line-through", TAG_STRIKE);
		addTag("span", "spoiler", TAG_SPOILER);
		addTag("pre", TAG_CODE);
		addTag("span", "quote", TAG_QUOTE);
	}

	@Override
	public CommentEditor obtainCommentEditor(String boardName) {
		CommentEditor commentEditor = new CommentEditor.BulletinBoardCodeCommentEditor();
		commentEditor.addTag(TAG_CODE, "[code=text]", "[/code]");
		return commentEditor;
	}

	@Override
	public boolean isTagSupported(String boardName, int tag) {
		return (SUPPORTED_TAGS & tag) == tag;
	}

	private static final Pattern THREAD_LINK = Pattern.compile("(\\d+)/?(?:#(\\d+))?$");

	@Override
	public Pair<String, String> obtainPostLinkThreadPostNumbers(String uriString) {
		Matcher matcher = THREAD_LINK.matcher(uriString);
		if (matcher.find()) {
			return new Pair<>(matcher.group(1), matcher.group(2));
		}
		return null;
	}
}