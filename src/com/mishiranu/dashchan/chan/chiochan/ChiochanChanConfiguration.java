package com.mishiranu.dashchan.chan.chiochan;

import chan.content.ChanConfiguration;

public class ChiochanChanConfiguration extends ChanConfiguration
{
	private static final String KEY_NAMES_ENABLED = "names_enabled";
	
	public ChiochanChanConfiguration()
	{
		request(OPTION_READ_POSTS_COUNT);
		setDefaultName("Аноним");
		setDefaultName("b", "Пассажир");
		setDefaultName("int", "Anonymous");
		setDefaultName("dev", "Стив Балмер");
		setDefaultName("ts", "Baka Inu");
		setDefaultName("tm", "Шики");
		setDefaultName("gnx", "Ноно");
		addCaptchaType("faptcha");
	}
	
	@Override
	public Board obtainBoardConfiguration(String boardName)
	{
		Board board = new Board();
		board.allowCatalog = true;
		board.allowArchive = true;
		board.allowPosting = true;
		board.allowDeleting = true;
		board.allowReporting = true;
		return board;
	}
	
	@Override
	public Captcha obtainCustomCaptchaConfiguration(String captchaType)
	{
		if ("faptcha".equals(captchaType))
		{
			Captcha captcha = new Captcha();
			captcha.title = "Faptcha";
			captcha.input = Captcha.Input.ALL;
			captcha.validity = Captcha.Validity.IN_BOARD;
			return captcha;
		}
		return null;
	}
	
	@Override
	public Posting obtainPostingConfiguration(String boardName, boolean newThread)
	{
		Posting posting = new Posting();
		posting.allowName = get(boardName, KEY_NAMES_ENABLED, true);
		posting.allowTripcode = true;
		posting.allowEmail = true;
		posting.allowSubject = true;
		posting.optionSage = true;
		posting.attachmentCount = 1;
		posting.attachmentMimeTypes.add("image/*");
		posting.hasCountryFlags = "int".equals(boardName);
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
	
	@Override
	public Reporting obtainReportingConfiguration(String boardName)
	{
		Reporting reporting = new Reporting();
		reporting.multiplePosts = true;
		return reporting;
	}
	
	public void storeNamesEnabled(String boardName, boolean namesEnabled)
	{
		set(boardName, KEY_NAMES_ENABLED, namesEnabled);
	}
}