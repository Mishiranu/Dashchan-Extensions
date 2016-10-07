package com.mishiranu.dashchan.chan.exach;

import chan.content.ChanConfiguration;

public class ExachChanConfiguration extends ChanConfiguration
{
	public ExachChanConfiguration()
	{
		request(OPTION_READ_THREAD_PARTIALLY);
		request(OPTION_READ_POSTS_COUNT);
		setDefaultName("Аноним");
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
	public Posting obtainPostingConfiguration(String boardName, boolean newThread)
	{
		Posting posting = new Posting();
		posting.allowName = true;
		posting.allowTripcode = true;
		posting.allowSubject = true;
		posting.optionOriginalPoster = !newThread;
		posting.attachmentCount = 1;
		posting.attachmentMimeTypes.add("image/*");
		return posting;
	}

	@Override
	public Deleting obtainDeletingConfiguration(String boardName)
	{
		return new Deleting();
	}
}