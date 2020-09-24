package com.mishiranu.dashchan.chan.cirno;

import chan.content.WakabaChanMarkup;

public class CirnoChanMarkup extends WakabaChanMarkup {
	private static final int SUPPORTED_TAGS = TAG_BOLD | TAG_ITALIC | TAG_STRIKE | TAG_SPOILER | TAG_CODE;

	public CirnoChanMarkup() {
		addTag("strong", TAG_BOLD);
		addTag("em", TAG_ITALIC);
		addTag("blockquote", TAG_QUOTE);
		addTag("code", TAG_CODE);
		addTag("del", TAG_STRIKE);
		addTag("span", "spoiler", TAG_SPOILER);
	}

	@Override
	public boolean isTagSupported(String boardName, int tag) {
		return (SUPPORTED_TAGS & tag) == tag;
	}
}
