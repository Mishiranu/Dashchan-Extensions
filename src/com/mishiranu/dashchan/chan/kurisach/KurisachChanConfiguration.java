package com.mishiranu.dashchan.chan.kurisach;

import chan.content.ChanConfiguration;

public class KurisachChanConfiguration extends ChanConfiguration {
	public static final String CAPTCHA_TYPE_INCH_NUMERIC = "inch_numeric";
	public static final String CAPTCHA_TYPE_INCH_LATIN = "inch_latin";
	public static final String CAPTCHA_TYPE_INCH_CYRILLIC = "inch_cyrillic";

	public KurisachChanConfiguration() {
		request(OPTION_READ_THREAD_PARTIALLY);
		request(OPTION_READ_SINGLE_POST);
		request(OPTION_READ_POSTS_COUNT);
		setDefaultName("Anonymous");
		setBumpLimit(500);
		addCaptchaType(CAPTCHA_TYPE_INCH_NUMERIC);
		addCaptchaType(CAPTCHA_TYPE_INCH_LATIN);
		addCaptchaType(CAPTCHA_TYPE_INCH_CYRILLIC);
	}

	@Override
	public Board obtainBoardConfiguration(String boardName) {
		Board board = new Board();
		board.allowSearch = true;
		board.allowPosting = true;
		board.allowDeleting = true;
		board.allowReporting = true;
		return board;
	}

	@Override
	public Captcha obtainCustomCaptchaConfiguration(String captchaType) {
		if (captchaType.startsWith("inch_")) {
			Captcha captcha = new Captcha();
			if (CAPTCHA_TYPE_INCH_NUMERIC.equals(captchaType)) {
				captcha.title = "Numeric";
				captcha.input = Captcha.Input.NUMERIC;
			} else if (CAPTCHA_TYPE_INCH_LATIN.equals(captchaType)) {
				captcha.title = "Simple";
				captcha.input = Captcha.Input.NUMERIC;
			} else if (CAPTCHA_TYPE_INCH_CYRILLIC.equals(captchaType)) {
				captcha.title = "Cyrillic";
				captcha.input = Captcha.Input.ALL;
			}
			captcha.validity = Captcha.Validity.IN_THREAD;
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
		posting.attachmentCount = 1;
		posting.attachmentMimeTypes.add("image/*");
		posting.attachmentMimeTypes.add("video/webm");
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
}