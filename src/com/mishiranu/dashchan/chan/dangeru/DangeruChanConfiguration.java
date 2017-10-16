package com.mishiranu.dashchan.chan.dangeru;

import chan.content.ChanConfiguration;

public class DangeruChanConfiguration extends ChanConfiguration {
	public DangeruChanConfiguration() {
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
}
