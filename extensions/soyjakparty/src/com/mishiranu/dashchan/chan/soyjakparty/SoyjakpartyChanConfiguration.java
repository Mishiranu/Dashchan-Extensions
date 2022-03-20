package com.mishiranu.dashchan.chan.soyjakparty;

import chan.content.ChanConfiguration;

public class SoyjakpartyChanConfiguration extends ChanConfiguration {
	public static final String CAPTCHA_TYPE_NONE = "None";

	public SoyjakpartyChanConfiguration() {
		request(OPTION_READ_POSTS_COUNT);
		addCaptchaType(CAPTCHA_TYPE_NONE);
		addCaptchaType(CAPTCHA_TYPE_RECAPTCHA_2);
		setDefaultName("Chud");
	}

	@Override
	public Board obtainBoardConfiguration(String boardName) {
		Board board = new Board();
		board.allowCatalog = true;
		board.allowPosting = true;
		board.allowDeleting = true;
		board.allowReporting = true;
		return board;
	}

	@Override
	public Posting obtainPostingConfiguration(String boardName, boolean newThread) {
		Posting posting = new Posting();
		boolean namesAndEmails = !"layer".equals(boardName);
		posting.allowName = namesAndEmails;
		posting.allowTripcode = namesAndEmails;
		posting.allowEmail = namesAndEmails;
		posting.allowSubject = true;
		posting.optionSage = namesAndEmails;
		posting.attachmentCount = 3;
		posting.attachmentMimeTypes.add("image/*");
		posting.attachmentMimeTypes.add("video/webm");
		posting.attachmentMimeTypes.add("video/mp4");
		posting.attachmentSpoiler = true;
		return posting;
	}

	@Override
	public Deleting obtainDeletingConfiguration(String boardName) {
		Deleting deleting = new Deleting();
		deleting.password = true;
		deleting.multiplePosts = true;
		deleting.optionFilesOnly = true;
		return deleting;
	}

	@Override
	public Reporting obtainReportingConfiguration(String boardName) {
		Reporting reporting = new Reporting();
		reporting.comment = true;
		reporting.multiplePosts = true;
		return reporting;
	}

	@Override
	public Captcha obtainCustomCaptchaConfiguration(String captchaType) {
		if (CAPTCHA_TYPE_NONE.equals(captchaType)) {
			Captcha captcha = new Captcha();
			captcha.title = "None";
			captcha.input = Captcha.Input.ALL;
			captcha.validity = Captcha.Validity.IN_BOARD;
			return captcha;
		}
		return null;
	}
}