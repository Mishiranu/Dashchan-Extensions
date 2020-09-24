package com.mishiranu.dashchan.chan.nowere;

import chan.content.WakabaChanMarkup;
import chan.text.CommentEditor;

public class NowereChanMarkup extends WakabaChanMarkup {
	private static final int SUPPORTED_TAGS = TAG_BOLD | TAG_ITALIC | TAG_STRIKE;

	public NowereChanMarkup() {
		addTag("strong", TAG_BOLD);
		addTag("em", TAG_ITALIC);
		addTag("blockquote", TAG_QUOTE);
		addTag("code", TAG_CODE);
		addTag("del", TAG_STRIKE);
	}

	@Override
	public CommentEditor obtainCommentEditor(String boardName) {
		CommentEditor commentEditor = super.obtainCommentEditor(boardName);
		commentEditor.addTag(TAG_ITALIC, "%%", "%%", CommentEditor.FLAG_ONE_LINE);
		return commentEditor;
	}

	@Override
	public boolean isTagSupported(String boardName, int tag) {
		return (SUPPORTED_TAGS & tag) == tag;
	}
}
