package com.mishiranu.dashchan.chan.desustorage;

import chan.content.ChanConfiguration;

public class DesustorageChanConfiguration extends ChanConfiguration {
	public DesustorageChanConfiguration() {
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