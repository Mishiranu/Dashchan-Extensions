package com.mishiranu.dashchan.chan.awoo;

import org.json.JSONArray;
import org.json.JSONException;

import chan.content.ChanConfiguration;

public class AwooConfiguration extends ChanConfiguration {
	public AwooConfiguration() {
		request(OPTION_READ_POSTS_COUNT);
		setDefaultName("Anonymous");
		setBumpLimit(250);
	}

	@Override
	public Board obtainBoardConfiguration(String boardName) {
		Board board = new Board();
		board.allowPosting = true;
		return board;
	}

	@Override
	public Posting obtainPostingConfiguration(String boardName, boolean newThread) {
		Posting posting = new Posting();
		posting.allowSubject = newThread;
		posting.maxCommentLength = 500;
		posting.attachmentCount = 0;
		return posting;
	}

	@Override
	public Authorization obtainCaptchaPassConfiguration() {
		Authorization authorization = new Authorization();
		authorization.fieldsCount = 2;
		authorization.hints = new String[] {"Token", "PIN"};
		return authorization;
	}
}
