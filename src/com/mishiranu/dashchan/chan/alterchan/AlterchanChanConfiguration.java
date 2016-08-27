package com.mishiranu.dashchan.chan.alterchan;

import android.content.res.Resources;

import chan.content.ChanConfiguration;

public class AlterchanChanConfiguration extends ChanConfiguration
{
	public AlterchanChanConfiguration()
	{
		request(OPTION_SINGLE_BOARD_MODE);
		request(OPTION_READ_SINGLE_POST);
		request(OPTION_READ_POSTS_COUNT);
		request(OPTION_ALLOW_USER_AUTHORIZATION);
		setDefaultName("Anonymous");
		addCaptchaType("alterchan");
		setBoardTitle(null, "Alterchan");
	}
	
	@Override
	public Board obtainBoardConfiguration(String boardName)
	{
		Board board = new Board();
		board.allowSearch = true;
		board.allowPosting = true;
		board.allowDeleting = true;
		return board;
	}
	
	@Override
	public Captcha obtainCustomCaptchaConfiguration(String captchaType)
	{
		if ("alterchan".equals(captchaType))
		{
			Captcha captcha = new Captcha();
			captcha.title = "Alterchan";
			captcha.input = Captcha.Input.LATIN;
			captcha.validity = Captcha.Validity.LONG_LIFETIME;
			return captcha;
		}
		return null;
	}
	
	@Override
	public Posting obtainPostingConfiguration(String boardName, boolean newThread)
	{
		Posting posting = new Posting();
		posting.allowName = true;
		posting.allowEmail = true;
		posting.allowSubject = true;
		posting.optionSage = true;
		posting.attachmentCount = 1;
		posting.attachmentMimeTypes.add("image/*");
		posting.attachmentMimeTypes.add("audio/*");
		posting.attachmentMimeTypes.add("video/webm");
		return posting;
	}
	
	@Override
	public Deleting obtainDeletingConfiguration(String boardName)
	{
		return new Deleting();
	}
	
	@Override
	public Authorization obtainUserAuthorizationConfiguration()
	{
		Resources resources = getResources();
		Authorization authorization = new Authorization();
		authorization.fieldsCount = 2;
		authorization.hints = new String[] {resources.getString(R.string.text_login),
				resources.getString(R.string.text_password)};
		return authorization;
	}
}