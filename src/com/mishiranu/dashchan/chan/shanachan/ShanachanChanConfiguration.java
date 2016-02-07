package com.mishiranu.dashchan.chan.shanachan;

import android.util.Pair;
import chan.content.ChanConfiguration;

public class ShanachanChanConfiguration extends ChanConfiguration
{
	public ShanachanChanConfiguration()
	{
		request(OPTION_READ_POSTS_COUNT);
		setDefaultName("Anonymous");
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
		posting.allowName = !"fetish".equals(boardName);
		posting.allowTripcode = true;
		posting.allowSubject = true;
		posting.optionSage = true;
		posting.attachmentCount = 1;
		if ("f".equals(boardName) && newThread)
		{
			posting.attachmentMimeTypes.add("application/x-shockwave-flash");
			posting.attachmentRatings.add(new Pair<>("6", "Other"));
			posting.attachmentRatings.add(new Pair<>("0", "Hentai"));
			posting.attachmentRatings.add(new Pair<>("1", "Japanese"));
			posting.attachmentRatings.add(new Pair<>("2", "Anime"));
			posting.attachmentRatings.add(new Pair<>("3", "Game"));
			posting.attachmentRatings.add(new Pair<>("4", "Loop"));
			posting.attachmentRatings.add(new Pair<>("5", "Movie"));
		}
		else posting.attachmentMimeTypes.add("image/*");
		if ("m".equals(boardName)) posting.attachmentMimeTypes.add("audio/*");
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
}