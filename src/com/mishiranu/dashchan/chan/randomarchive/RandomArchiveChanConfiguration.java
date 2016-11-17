package com.mishiranu.dashchan.chan.randomarchive;

import chan.content.ChanConfiguration;

public class RandomArchiveChanConfiguration extends ChanConfiguration {
	public RandomArchiveChanConfiguration() {
		request(OPTION_SINGLE_BOARD_MODE);
		setDefaultName("Anonymous");
		setSingleBoardName("b");
		setBoardTitle("b", "Random Archive");
	}

	@Override
	public Statistics obtainStatisticsConfiguration() {
		Statistics statistics = new Statistics();
		statistics.postsSent = false;
		statistics.threadsCreated = false;
		return statistics;
	}
}