package com.mishiranu.dashchan.chan.horochan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Pair;

import chan.content.ChanMarkup;
import chan.text.CommentEditor;

public class HorochanChanMarkup extends ChanMarkup {
	private static final int SUPPORTED_TAGS = TAG_BOLD | TAG_ITALIC | TAG_UNDERLINE | TAG_STRIKE
			| TAG_SPOILER | TAG_CODE;

	public HorochanChanMarkup() {
		addTag("strong", TAG_BOLD);
		addTag("em", TAG_ITALIC);
		addTag("s", TAG_STRIKE);
		addTag("ins", TAG_UNDERLINE);
		addTag("code", TAG_CODE);
		addTag("span", "quote", TAG_QUOTE);
		addTag("span", "spoiler", TAG_SPOILER);
		addTag("blockquote", TAG_QUOTE);
		addBlock("blockquote", true, false);
		addBlock("p", true, false);
	}

	@Override
	public CommentEditor obtainCommentEditor(String boardName) {
		CommentEditor commentEditor = new CommentEditor.WakabaMarkCommentEditor();
		commentEditor.addTag(TAG_STRIKE, "~~", "~~", CommentEditor.FLAG_ONE_LINE);
		return commentEditor;
	}

	@Override
	public boolean isTagSupported(String boardName, int tag) {
		return (SUPPORTED_TAGS & tag) == tag;
	}

	private static final Pattern THREAD_LINK = Pattern.compile("(\\d+)(?:/?#(\\d+))?$");

	@Override
	public Pair<String, String> obtainPostLinkThreadPostNumbers(String uriString) {
		Matcher matcher = THREAD_LINK.matcher(uriString);
		if (matcher.find()) {
			return new Pair<>(matcher.group(1), matcher.group(2));
		}
		return null;
	}
}
