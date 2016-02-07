package com.mishiranu.dashchan.chan.ponychan;

import chan.content.ChanConfiguration;

public class PonychanChanConfiguration extends ChanConfiguration
{
	private static final String KEY_SPOILER_THREADS = "spoiler_threads";
	private static final String KEY_MATURE_THREADS = "mature_threads";
	
	public PonychanChanConfiguration()
	{
		request(OPTION_READ_POSTS_COUNT);
		setDefaultName("Anonymous");
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
		boolean mayMarkSpoilerThread = newThread && get(boardName, KEY_SPOILER_THREADS, false);
		Posting posting = new Posting();
		posting.allowName = true;
		posting.allowTripcode = true;
		posting.allowSubject = true;
		posting.optionSpoiler = mayMarkSpoilerThread;
		posting.attachmentCount = 1;
		posting.attachmentMimeTypes.add("image/*");
		posting.attachmentSpoiler = !mayMarkSpoilerThread;
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
	
	public void storeSpoilersMatureEnabled(String boardName, boolean spoilerThreadsEnabled,
			boolean matureThreadsEnabled)
	{
		set(boardName, KEY_SPOILER_THREADS, spoilerThreadsEnabled);
		set(boardName, KEY_MATURE_THREADS, matureThreadsEnabled);
	}
}