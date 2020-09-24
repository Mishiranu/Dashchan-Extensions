package com.mishiranu.dashchan.chan.yakujimoe;

import chan.content.WakabaChanLocator;

public class YakujiMoeChanLocator extends WakabaChanLocator {
	public YakujiMoeChanLocator() {
		addChanHost("ii.yakuji.moe");
		setHttpsMode(HttpsMode.CONFIGURABLE);
	}
}
