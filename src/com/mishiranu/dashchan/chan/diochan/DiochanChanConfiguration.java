package com.mishiranu.dashchan.chan.diochan;

import chan.content.ChanConfiguration;

public class DiochanChanConfiguration extends ChanConfiguration
{
	private static final String KEY_NAMES_ENABLED = "names_enabled";
	private static final String KEY_IMAGE_SPOILERS_ENABLED = "image_spoilers_enabled";
	
	public DiochanChanConfiguration()
	{
		request(OPTION_READ_SINGLE_POST);
		request(OPTION_READ_POSTS_COUNT);
		setDefaultName("Anonimo");
		setDefaultName("int", " Anonymous");
	}
	
	@Override
	public Board obtainBoardConfiguration(String boardName)
	{
		Board board = new Board();
		board.allowCatalog = true;
		board.allowPosting = true;
		board.allowDeleting = !"b".equals(boardName);
		board.allowReporting = true;
		return board;
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
		posting.attachmentCount = !"scr".equals(boardName) ? newThread ? 1 : 3 : 0;
		if (!"mu".equals(boardName)) posting.attachmentMimeTypes.add("image/*");
		if ("b".equals(boardName)) posting.attachmentMimeTypes.add("video/webm");
		posting.attachmentMimeTypes.add("audio/mp3");
		posting.attachmentSpoiler = get(boardName, KEY_IMAGE_SPOILERS_ENABLED, false);
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
	
	public void storeNamesSpoilersEnabled(String boardName, boolean namesEnabled, boolean imageSpoilersEnabled)
	{
		set(boardName, KEY_NAMES_ENABLED, namesEnabled);
		set(boardName, KEY_IMAGE_SPOILERS_ENABLED, imageSpoilersEnabled);
	}
}