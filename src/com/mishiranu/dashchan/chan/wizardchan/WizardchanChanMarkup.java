package com.mishiranu.dashchan.chan.wizardchan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Pair;

import chan.content.ChanMarkup;
import chan.text.CommentEditor;

public class WizardchanChanMarkup extends ChanMarkup {
	private static final int SUPPORTED_TAGS = TAG_BOLD | TAG_ITALIC | TAG_SPOILER | TAG_HEADING;

	public WizardchanChanMarkup() {
		addTag("strong", TAG_BOLD);
		addTag("em", TAG_ITALIC);
		addTag("span", "quote", TAG_QUOTE);
		addTag("span", "spoiler", TAG_SPOILER);
		addTag("span", "heading", TAG_HEADING);
	}

	@Override
	public CommentEditor obtainCommentEditor(String boardName) {
		CommentEditor commentEditor = new CommentEditor.BulletinBoardCodeCommentEditor();
		commentEditor.addTag(TAG_HEADING, "==", "==", CommentEditor.FLAG_ONE_LINE);
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
