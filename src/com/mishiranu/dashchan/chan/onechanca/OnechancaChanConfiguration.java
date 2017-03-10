package com.mishiranu.dashchan.chan.onechanca;

import chan.content.ChanConfiguration;

public class OnechancaChanConfiguration extends ChanConfiguration {
	public static final String CAPTCHA_TYPE_ONECHANCA = "onechanca";

	public OnechancaChanConfiguration() {
		request(OPTION_READ_SINGLE_POST);
		request(OPTION_READ_POSTS_COUNT);
		request(OPTION_ALLOW_CAPTCHA_PASS);
		setDefaultName("Аноним");
		addCaptchaType(CAPTCHA_TYPE_ONECHANCA);
	}

	@Override
	public Board obtainBoardConfiguration(String boardName) {
		Board board = new Board();
		board.allowPosting = true;
		board.allowDeleting = boardName == null || !boardName.startsWith("news");
		return board;
	}

	@Override
	public Captcha obtainCustomCaptchaConfiguration(String captchaType) {
		if (CAPTCHA_TYPE_ONECHANCA.equals(captchaType)) {
			Captcha captcha = new Captcha();
			captcha.title = "Onechanca";
			captcha.input = Captcha.Input.NUMERIC;
			captcha.validity = Captcha.Validity.SHORT_LIFETIME;
			return captcha;
		}
		return null;
	}

	@Override
	public Posting obtainPostingConfiguration(String boardName, boolean newThread) {
		Posting posting = new Posting();
		posting.allowSubject = newThread;
		posting.attachmentCount = boardName == null || !boardName.startsWith("news") ? 1 : 0;
		posting.attachmentMimeTypes.add("image/*");
		return posting;
	}

	@Override
	public Deleting obtainDeletingConfiguration(String boardName) {
		Deleting deleting = new Deleting();
		deleting.password = true;
		return deleting;
	}
}