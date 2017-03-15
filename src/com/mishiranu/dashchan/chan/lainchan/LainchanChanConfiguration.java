package com.mishiranu.dashchan.chan.lainchan;

import chan.content.ChanConfiguration;

public class LainchanChanConfiguration extends ChanConfiguration {
	public LainchanChanConfiguration() {
		request(OPTION_READ_POSTS_COUNT);
		setDefaultName("Anonymous");
	}

	@Override
	public Board obtainBoardConfiguration(String boardName) {
		Board board = new Board();
		board.allowCatalog = true;
		board.allowPosting = true;
		board.allowDeleting = true;
		board.allowReporting = true;
		return board;
	}

	@Override
	public Posting obtainPostingConfiguration(String boardName, boolean newThread) {
		Posting posting = new Posting();
		boolean namesAndEmails = !"layer".equals(boardName);
		posting.allowName = namesAndEmails;
		posting.allowTripcode = namesAndEmails;
		posting.allowEmail = namesAndEmails;
		posting.allowSubject = true;
		posting.optionSage = namesAndEmails;
		posting.attachmentCount = 3;
		posting.attachmentMimeTypes.add("image/*");
		return posting;
	}

	@Override
	public Deleting obtainDeletingConfiguration(String boardName) {
		Deleting deleting = new Deleting();
		deleting.password = true;
		deleting.multiplePosts = true;
		deleting.optionFilesOnly = true;
		return deleting;
	}

	@Override
	public Reporting obtainReportingConfiguration(String boardName) {
		Reporting reporting = new Reporting();
		reporting.comment = true;
		reporting.multiplePosts = true;
		return reporting;
	}
}