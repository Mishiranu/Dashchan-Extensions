package com.mishiranu.dashchan.chan.bunbunmaru;

import chan.content.WakabaChanMarkup;
import chan.text.CommentEditor;

public class BunbunmaruChanMarkup extends WakabaChanMarkup {
	private static final int SUPPORTED_TAGS = TAG_BOLD | TAG_ITALIC | TAG_UNDERLINE | TAG_OVERLINE | TAG_STRIKE
			| TAG_SUBSCRIPT | TAG_SUPERSCRIPT | TAG_SPOILER | TAG_CODE | TAG_ASCII_ART;

	public BunbunmaruChanMarkup() {
		addTag("b", TAG_BOLD);
		addTag("em", TAG_ITALIC);
		addTag("span", "u", TAG_UNDERLINE);
		addTag("span", "o", TAG_OVERLINE);
		addTag("span", "s", TAG_STRIKE);
		addTag("sub", TAG_SUBSCRIPT);
		addTag("sup", TAG_SUPERSCRIPT);
		addTag("span", "unkfunc", TAG_QUOTE);
		addTag("span", "spoiler", TAG_SPOILER);
		addTag("code", TAG_CODE);
		addTag("span", "aa", TAG_ASCII_ART);
	}

	@Override
	public CommentEditor obtainCommentEditor(String boardName) {
		return new CommentEditor.BulletinBoardCodeCommentEditor();
	}

	@Override
	public boolean isTagSupported(String boardName, int tag) {
		return (SUPPORTED_TAGS & tag) == tag;
	}
}
