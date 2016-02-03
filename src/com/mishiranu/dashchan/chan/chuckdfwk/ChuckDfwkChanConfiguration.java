package com.mishiranu.dashchan.chan.chuckdfwk;

import chan.content.ChanConfiguration;

public class ChuckDfwkChanConfiguration extends ChanConfiguration
{
	public static final String CAPTCHA_TYPE_KUSABA = "kusaba";
	
	public ChuckDfwkChanConfiguration()
	{
		request(OPTION_READ_POSTS_COUNT);
		setBumpLimit(500);
		addCaptchaType(CAPTCHA_TYPE_KUSABA);
	}
	
	@Override
	public Board obtainBoardConfiguration(String boardName)
	{
		Board board = new Board();
		board.allowCatalog = true;
		board.allowPosting = true;
		board.allowDeleting = true;
		return board;
	}
	
	@Override
	public Captcha obtainCustomCaptchaConfiguration(String captchaType)
	{
		if (CAPTCHA_TYPE_KUSABA.equals(captchaType))
		{
			Captcha captcha = new Captcha();
			captcha.title = "Kusaba";
			captcha.input = Captcha.Input.LATIN;
			captcha.validity = Captcha.Validity.IN_THREAD;
			return captcha;
		}
		return null;
	}
	
	@Override
	public Posting obtainPostingConfiguration(String boardName, boolean newThread)
	{
		Posting posting = new Posting();
		posting.allowName = "hh".equals(boardName);
		posting.allowTripcode = true;
		posting.allowEmail = true;
		posting.allowSubject = true;
		posting.optionSage = true;
		posting.attachmentCount = 1;
		posting.attachmentMimeTypes.add("image/*");
		if ("df".equals(boardName))
		{
			posting.attachmentMimeTypes.add("application/zip");
			posting.attachmentMimeTypes.add("application/rar");
			posting.attachmentMimeTypes.add("application/x-7z-compressed");
			posting.attachmentMimeTypes.add("audio/mp3");
		}
		return posting;
	}
	
	@Override
	public Deleting obtainDeletingConfiguration(String boardName)
	{
		Deleting deleting = new Deleting();
		deleting.password = true;
		deleting.multiplePosts = true;
		deleting.optionFilesOnly = true;
		return deleting;
	}
}