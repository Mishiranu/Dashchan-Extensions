package com.mishiranu.dashchan.chan.alphachan;

import chan.content.ChanConfiguration;

public class AlphachanChanConfiguration extends ChanConfiguration
{
	public AlphachanChanConfiguration()
	{
		request(OPTION_READ_POSTS_COUNT);
		setDefaultName("Анонимус");
		addCaptchaType("alphachan");
	}

	@Override
	public Board obtainBoardConfiguration(String boardName)
	{
		Board board = new Board();
		board.allowPosting = true;
		return board;
	}

	@Override
	public Captcha obtainCustomCaptchaConfiguration(String captchaType)
	{
		if ("alphachan".equals(captchaType))
		{
			Captcha captcha = new Captcha();
			captcha.title = "Alphachan";
			captcha.input = Captcha.Input.NUMERIC;
			captcha.validity = Captcha.Validity.LONG_LIFETIME;
			return captcha;
		}
		return null;
	}

	@Override
	public Posting obtainPostingConfiguration(String boardName, boolean newThread)
	{
		Posting posting = new Posting();
		posting.allowSubject = true;
		posting.attachmentCount = 1;
		posting.attachmentMimeTypes.add("image/*");
		return posting;
	}
}