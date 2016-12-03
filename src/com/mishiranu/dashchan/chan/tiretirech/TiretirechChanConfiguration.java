package com.mishiranu.dashchan.chan.tiretirech;

import chan.content.ChanConfiguration;

public class TiretirechChanConfiguration extends ChanConfiguration {
	public TiretirechChanConfiguration() {
		request(OPTION_READ_POSTS_COUNT);
		setDefaultName("Аноним");
		addCaptchaType("ochoba");
	}

	@Override
	public Board obtainBoardConfiguration(String boardName) {
		Board board = new Board();
		board.allowPosting = true;
		board.allowDeleting = true;
		return board;
	}

	@Override
	public Captcha obtainCustomCaptchaConfiguration(String captchaType) {
		if ("ochoba".equals(captchaType)) {
			Captcha captcha = new Captcha();
			captcha.title = "Ochoba";
			captcha.input = Captcha.Input.ALL;
			captcha.validity = Captcha.Validity.SHORT_LIFETIME;
			return captcha;
		}
		return null;
	}

	@Override
	public Posting obtainPostingConfiguration(String boardName, boolean newThread) {
		Posting posting = new Posting();
		posting.allowName = true;
		posting.allowTripcode = true;
		posting.allowEmail = true;
		posting.allowSubject = true;
		posting.optionSage = true;
		posting.attachmentCount = 4;
		posting.attachmentMimeTypes.add("image/*");
		posting.attachmentMimeTypes.add("audio/*");
		posting.attachmentMimeTypes.add("video/*");
		posting.attachmentMimeTypes.add("application/ogg");
		posting.attachmentMimeTypes.add("application/zip");
		posting.attachmentMimeTypes.add("application/rar");
		posting.attachmentMimeTypes.add("application/x-bittorrent");
		posting.attachmentMimeTypes.add("application/x-shockwave-flash");
		return posting;
	}

	@Override
	public Deleting obtainDeletingConfiguration(String boardName) {
		Deleting deleting = new Deleting();
		deleting.password = true;
		deleting.multiplePosts = true;
		return deleting;
	}
}