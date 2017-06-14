package com.mishiranu.dashchan.chan.twentyseven;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Pair;

import chan.content.ChanMarkup;
import chan.text.CommentEditor;

public class TwentySevenChanMarkup extends ChanMarkup {
	private static final int SUPPORTED_TAGS = TAG_SPOILER | TAG_CODE | TAG_BOLD | TAG_ITALIC | TAG_UNDERLINE | TAG_HEADING | TAG_STRIKE;

	public TwentySevenChanMarkup() {
		addTag("pre", TAG_CODE);
		addTag("span", "quote", TAG_QUOTE);
		addTag("span", "spoiler", TAG_SPOILER);
		addTag("strong", TAG_BOLD);
		addTag("em", TAG_ITALIC);
		addTag("u", TAG_UNDERLINE);
		addTag("s", TAG_STRIKE);
		addTag("span", "heading", TAG_HEADING);
		addColorable("span");
	}

	@Override
	public CommentEditor obtainCommentEditor(String boardName) {
		CommentEditor editor = new CommentEditor.BulletinBoardCodeCommentEditor();
		editor.addTag(TAG_BOLD, "'''", "'''");
		editor.addTag(TAG_ITALIC, "''", "''");
		editor.addTag(TAG_UNDERLINE, "[u]", "[/u]");
		editor.addTag(TAG_CODE, "[code]", "[/code]");
		editor.addTag(TAG_HEADING, "==", "==\n");
		editor.addTag(TAG_STRIKE, "[s]", "[/s]");
		editor.addTag(TAG_SPOILER, "**", "**");
		return editor;
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