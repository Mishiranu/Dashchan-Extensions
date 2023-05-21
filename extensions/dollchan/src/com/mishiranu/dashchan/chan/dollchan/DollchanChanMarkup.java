package com.mishiranu.dashchan.chan.dollchan;

import android.util.Pair;
import chan.content.ChanMarkup;
import chan.text.CommentEditor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DollchanChanMarkup extends ChanMarkup {
	private static final int SUPPORTED_TAGS = TAG_BOLD | TAG_ITALIC | TAG_UNDERLINE | TAG_STRIKE | TAG_SPOILER
			| TAG_CODE | TAG_ASCII_ART | TAG_HEADING;

	public DollchanChanMarkup() {
		addTag("strong", TAG_BOLD);
		addTag("em", TAG_ITALIC);
		addTag("u", TAG_UNDERLINE);
		addTag("s", TAG_STRIKE);
		addTag("pre", TAG_CODE);
		addTag("span", "unkfunc", TAG_QUOTE);
		addTag("span", "spoiler", TAG_SPOILER);
		addTag("span", "redText", TAG_HEADING);
		addTag("pre", TAG_CODE);
		addBlock("span", "aa", true, false);
		addColorable("span", "colored", "true");
	}

	@Override
	public CommentEditor obtainCommentEditor(String boardName) {
		CommentEditor commentEditor = new CommentEditor();
		commentEditor.addTag(TAG_BOLD, "[b]", "[/b]", CommentEditor.FLAG_ONE_LINE);
		commentEditor.addTag(TAG_ITALIC, "[i]", "[/i]", CommentEditor.FLAG_ONE_LINE);
		commentEditor.addTag(TAG_UNDERLINE, "[u]", "[/u]", CommentEditor.FLAG_ONE_LINE);
		commentEditor.addTag(TAG_STRIKE, "[s]", "[/s]", CommentEditor.FLAG_ONE_LINE);
		commentEditor.addTag(TAG_SPOILER, "[spoiler]", "[/spoiler]", CommentEditor.FLAG_ONE_LINE);
		commentEditor.addTag(TAG_CODE, "[code]", "[/code]");
		return commentEditor;
	}

	@Override
	public boolean isTagSupported(String boardName, int tag) {
		if (tag == TAG_CODE) {
			DollchanChanConfiguration configuration = DollchanChanConfiguration.get(this);
			return configuration.isTagSupported(boardName, tag);
		}
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
