package com.mishiranu.dashchan.chan.horochan;

import chan.content.ChanConfiguration;

public class HorochanChanConfiguration extends ChanConfiguration {
	public HorochanChanConfiguration() {
		request(OPTION_SINGLE_BOARD_MODE);
		request(OPTION_READ_THREAD_PARTIALLY);
		request(OPTION_READ_SINGLE_POST);
		request(OPTION_READ_POSTS_COUNT);
		setSingleBoardName("b");
		setBoardTitle("b", "Random");
		setDefaultName("Anonymous");
		setBumpLimit(250);
		addCaptchaType(CAPTCHA_TYPE_RECAPTCHA_2);
	}

	@Override
	public Board obtainBoardConfiguration(String boardName) {
		Board board = new Board();
		board.allowPosting = true;
		board.allowDeleting = true;
		return board;
	}

	@Override
	public Posting obtainPostingConfiguration(String boardName, boolean newThread) {
		Posting posting = new Posting();
		posting.allowSubject = newThread;
		posting.attachmentCount = 4;
		posting.attachmentMimeTypes.add("image/*");
		posting.attachmentMimeTypes.add("video/webm");
		posting.attachmentMimeTypes.add("video/mp4");
		posting.attachmentMimeTypes.add("audio/mpeg");
		return posting;
	}

	@Override
	public Deleting obtainDeletingConfiguration(String boardName) {
		Deleting deleting = new Deleting();
		deleting.password = true;
		return deleting;
	}
}
