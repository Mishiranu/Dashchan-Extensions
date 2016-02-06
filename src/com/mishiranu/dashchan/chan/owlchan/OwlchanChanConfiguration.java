package com.mishiranu.dashchan.chan.owlchan;

import chan.content.ChanConfiguration;

public class OwlchanChanConfiguration extends ChanConfiguration
{
	private static final String KEY_NAMES_ENABLED = "names_enabled";
	
	public OwlchanChanConfiguration()
	{
		request(OPTION_READ_POSTS_COUNT);
		setDefaultName("Аноним");
		setDefaultName("d", "Ольга Дмитриевна");
		setDefaultName("b", "Семён");
		setDefaultName("es", "Пионер");
		setDefaultName("izd", "Писатель");
		setDefaultName("art", "Художник-кун");
		setDefaultName("a", "Рена");
		setDefaultName("ra", "Тридогнайт");
		setDefaultName("ussr", "Товарищ");
		setBumpLimit(500);
		setBumpLimit("es", 1000);
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
		posting.allowEmail = true;
		posting.allowSubject = true;
		posting.optionSage = true;
		posting.attachmentCount = 1;
		posting.attachmentMimeTypes.add("image/*");
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
	
	public void storeNamesEnabled(String boardName, boolean namesEnabled)
	{
		set(boardName, KEY_NAMES_ENABLED, namesEnabled);
	}
}