package com.mishiranu.dashchan.chan.anonfm;

import chan.content.ChanConfiguration;

public class AnonfmChanConfiguration extends ChanConfiguration
{
	public static final int MAX_COMMENT_LENGTH_FM = 500;
	
	public AnonfmChanConfiguration()
	{
		setDefaultName("Аноним");
		addCaptchaType("anonfm");
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
		if ("anonfm".equals(captchaType))
		{
			Captcha captcha = new Captcha();
			captcha.title = "Anon FM";
			captcha.input = Captcha.Input.NUMERIC;
			captcha.validity = Captcha.Validity.IN_BOARD;
			return captcha;
		}
		return null;
	}
	
	@Override
	public Posting obtainPostingConfiguration(String boardName, boolean newThread)
	{
		Posting posting = new Posting();
		if (AnonfmChanLocator.BOARD_NAME_FM.equals(boardName))
		{
			posting.maxCommentLength = MAX_COMMENT_LENGTH_FM;
		}
		return posting;
	}
}