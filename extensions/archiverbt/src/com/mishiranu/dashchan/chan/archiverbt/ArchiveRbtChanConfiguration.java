package com.mishiranu.dashchan.chan.archiverbt;

import chan.content.ChanConfiguration;

public class ArchiveRbtChanConfiguration extends ChanConfiguration {
	public ArchiveRbtChanConfiguration() {
		setDefaultName("Anonymous");
	}

	@Override
	public Board obtainBoardConfiguration(String boardName) {
		Board board = new Board();
		board.allowSearch = true;
		return board;
	}

	@Override
	public Statistics obtainStatisticsConfiguration() {
		Statistics statistics = new Statistics();
		statistics.postsSent = false;
		statistics.threadsCreated = false;
		return statistics;
	}
}