package com.mishiranu.dashchan.chan.local;

import chan.content.ChanConfiguration;
import chan.util.DataFile;

public class LocalChanConfiguration extends ChanConfiguration {
	public LocalChanConfiguration() {
		request(OPTION_SINGLE_BOARD_MODE);
		request(OPTION_LOCAL_MODE);
		setBoardTitle(null, getResources().getString(R.string.text_local_archive));
		obtainStatisticsConfiguration();
	}

	@Override
	public Board obtainBoardConfiguration(String boardName) {
		Board board = new Board();
		board.allowDeleting = true;
		return board;
	}

	@Override
	public Deleting obtainDeletingConfiguration(String boardName) {
		return new Deleting();
	}

	@Override
	public Statistics obtainStatisticsConfiguration() {
		Statistics statistics = new Statistics();
		statistics.threadsViewed = false;
		statistics.postsSent = false;
		statistics.threadsCreated = false;
		return statistics;
	}

	public DataFile getLocalDownloadDirectory() {
		return getDownloadDirectory().getChild("Archive");
	}
}
