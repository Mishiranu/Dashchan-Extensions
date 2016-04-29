package com.mishiranu.dashchan.chan.fiftyfive;

import chan.content.ChanConfiguration;

public class FiftyfiveChanConfiguration extends ChanConfiguration
{
	public FiftyfiveChanConfiguration()
	{
		request(OPTION_READ_POSTS_COUNT);
		setDefaultName("Anônimo");
		addCaptchaType(CAPTCHA_TYPE_RECAPTCHA_2);
	}
	
	@Override
	public Board obtainBoardConfiguration(String boardName)
	{
		Board board = new Board();
		board.allowCatalog = true;
		board.allowPosting = true;
		board.allowDeleting = true;
		board.allowReporting = true;
		return board;
	}
	
	@Override
	public Posting obtainPostingConfiguration(String boardName, boolean newThread)
	{
		Posting posting = new Posting();
		posting.allowEmail = true;
		posting.allowSubject = true;
		posting.optionSage = true;
		posting.attachmentCount = 3;
		posting.attachmentMimeTypes.add("image/*");
		posting.attachmentMimeTypes.add("video/webm");
		posting.attachmentMimeTypes.add("audio/mp3");
		posting.attachmentMimeTypes.add("application/ogg");
		posting.attachmentSpoiler = true;
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
		reporting.multiplePosts = true;
		return reporting;
	}
}