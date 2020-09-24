package com.mishiranu.dashchan.chan.archiverbt;

import chan.content.FoolFuukaChanLocator;

public class ArchiveRbtChanLocator extends FoolFuukaChanLocator {
	public ArchiveRbtChanLocator() {
		addChanHost("rbt.asia");
		addChanHost("archive.rebeccablacktech.com");
		addConvertableChanHost("www.rbt.asia");
		setHttpsMode(HttpsMode.HTTPS_ONLY);
	}
}
