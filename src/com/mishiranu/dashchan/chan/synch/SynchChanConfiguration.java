package com.mishiranu.dashchan.chan.synch;

import chan.content.ChanConfiguration;

public class SynchChanConfiguration extends ChanConfiguration {
	public SynchChanConfiguration() {
		request(OPTION_READ_POSTS_COUNT);
		setDefaultName("Аноним");
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
		posting.allowName = !"d".equals(boardName) && !"mlp".equals(boardName);
		posting.allowEmail = true;
		posting.allowSubject = true;
		posting.optionSage = true;
		posting.allowTripcode = true;
		posting.attachmentCount = 1;
		posting.attachmentMimeTypes.add("image/*");
		posting.attachmentMimeTypes.add("video/*");
		posting.attachmentMimeTypes.add("audio/*");
		posting.attachmentMimeTypes.add("application/*");
		posting.attachmentSpoiler = true;
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
