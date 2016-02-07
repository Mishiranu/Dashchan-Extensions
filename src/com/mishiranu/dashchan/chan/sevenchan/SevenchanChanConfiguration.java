package com.mishiranu.dashchan.chan.sevenchan;

import chan.content.ChanConfiguration;

public class SevenchanChanConfiguration extends ChanConfiguration
{
	private static final String KEY_NAMES_ENABLED = "names_enabled";
	private static final String KEY_MAX_REPLY_FILES_COUNT = "max_reply_files_count";
	
	public SevenchanChanConfiguration()
	{
		request(OPTION_READ_THREAD_PARTIALLY);
		request(OPTION_READ_POSTS_COUNT);
		setDefaultName("Anonymous");
		setDefaultName("irc", "Poe");
		setDefaultName("777", "Useless");
		setDefaultName("banner", "Bruce Banner");
		setDefaultName("class", "Sophisticated Gentleman");
		setDefaultName("eh", "John Smith");
		setDefaultName("jew", "Modern Mom");
		setDefaultName("lit", "Hipster Slut");
		setDefaultName("pr", "Neckbearded Basement Dweller");
		setDefaultName("rnb", "Teenage Girl");
		setDefaultName("w", "Sarah Palin");
		setDefaultName("zom", "Shambler");
		setDefaultName("a", "Anonymous-San");
		setDefaultName("grim", "Eeyore");
		setDefaultName("hi", "Historian");
		setDefaultName("x", "Tin Foil Enthusiast");
		setDefaultName("di", "Closet Homosexual");
		setDefaultName("fur", "Vulpes Inculta");
		setDefaultName("v", "Wine Connoisseur");
		addCaptchaType(CAPTCHA_TYPE_RECAPTCHA_1);
	}
	
	@Override
	public Board obtainBoardConfiguration(String boardName)
	{
		Board board = new Board();
		board.allowPosting = true;
		board.allowDeleting = true;
		board.allowReporting = true;
		return board;
	}
	
	@Override
	public Posting obtainPostingConfiguration(String boardName, boolean newThread)
	{
		Posting posting = new Posting();
		posting.allowName = get(boardName, KEY_NAMES_ENABLED, true);
		posting.allowTripcode = true;
		posting.allowSubject = true;
		posting.optionSage = true;
		posting.attachmentCount = newThread ? 1 : get(boardName, KEY_MAX_REPLY_FILES_COUNT, 1);
		posting.attachmentMimeTypes.add("image/*");
		posting.attachmentMimeTypes.add("video/webm");
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
		reporting.comment = true;
		return reporting;
	}
	
	public void storeNamesEnabled(String boardName, boolean namesEnabled)
	{
		set(boardName, KEY_NAMES_ENABLED, namesEnabled);
	}
	
	public void storeMaxReplyFilesCount(String boardName, int maxReplyFilesCount)
	{
		set(boardName, KEY_MAX_REPLY_FILES_COUNT, maxReplyFilesCount);
	}
}