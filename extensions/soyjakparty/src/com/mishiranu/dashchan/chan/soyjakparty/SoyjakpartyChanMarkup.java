package com.mishiranu.dashchan.chan.soyjakparty;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Pair;

import chan.content.ChanMarkup;
import chan.text.CommentEditor;

public class SoyjakpartyChanMarkup extends ChanMarkup {
	private static final int SUPPORTED_TAGS = TAG_BOLD | TAG_ITALIC | TAG_SPOILER | TAG_EMPHASIS | TAG_QUOTE;

	public SoyjakpartyChanMarkup() {
		addTag("strong", TAG_BOLD);
		addTag("em", TAG_ITALIC);
		addTag("span", "spoiler", TAG_SPOILER);
		addTag("span", "heading", TAG_EMPHASIS);
		addTag("span", "quote", TAG_QUOTE);
		addColorable("span");
	}

	@Override
	public CommentEditor obtainCommentEditor(String boardName) {
		CommentEditor commentEditor = new CommentEditor();
		commentEditor.addTag(TAG_BOLD, "'''", "'''", CommentEditor.FLAG_ONE_LINE);
		commentEditor.addTag(TAG_ITALIC, "''", "''", CommentEditor.FLAG_ONE_LINE);
		commentEditor.addTag(TAG_EMPHASIS, "==", "==", CommentEditor.FLAG_ONE_LINE);
		commentEditor.addTag(TAG_SPOILER, "**", "**", CommentEditor.FLAG_ONE_LINE);
		return commentEditor;
	}

	@Override
	public boolean isTagSupported(String boardName, int tag) {
		return (SUPPORTED_TAGS & tag) == tag;
	}

	private static final Pattern THREAD_LINK = Pattern.compile("(\\d+).html(?:#(\\d+))?$");

	@Override
	public Pair<String, String> obtainPostLinkThreadPostNumbers(String uriString) {
		Matcher matcher = THREAD_LINK.matcher(uriString);
		if (matcher.find()) {
			return new Pair<>(matcher.group(1), matcher.group(2));
		}
		return null;
	}
}