package com.mishiranu.dashchan.chan.erlach;

import chan.content.ChanConfiguration;

public class ErlachChanConfiguration extends ChanConfiguration {
	public ErlachChanConfiguration() {
		setDefaultName("Anonymous");
	}

	@Override
	public Board obtainBoardConfiguration(String boardName) {
		Board board = new Board();
		board.allowSearch = true;
		board.allowPosting = true;
		return board;
	}

	@Override
	public Posting obtainPostingConfiguration(String boardName, boolean newThread) {
		Posting posting = new Posting();
		posting.allowSubject = newThread;
		posting.optionSage = !newThread;
		posting.attachmentCount = 1;
		posting.attachmentMimeTypes.add("image/*");
		return posting;
	}
}
