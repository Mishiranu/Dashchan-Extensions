package com.mishiranu.dashchan.chan.desustorage;

import chan.content.FoolFuukaChanLocator;

public class DesustorageChanLocator extends FoolFuukaChanLocator {
	public DesustorageChanLocator() {
		addChanHost("desuarchive.org");
		addConvertableChanHost("desustorage.org");
		setHttpsMode(HttpsMode.HTTPS_ONLY);
	}
}
