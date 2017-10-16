package com.mishiranu.dashchan.chan.dangeru;

import android.util.Pair;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import chan.content.ChanMarkup;

public class DangeruChanMarkup extends ChanMarkup {
	private static final Pattern THREAD_LINK = Pattern.compile("/thread/(\\d+)(?:#(\\d+))?$");

	public DangeruChanMarkup() {
		addTag("span", "redtext", TAG_QUOTE);
	}

	@Override
	public Pair<String, String> obtainPostLinkThreadPostNumbers(String uriString) {
		Matcher matcher = THREAD_LINK.matcher(uriString);
		if (matcher.find()) {
			return new Pair<>(matcher.group(1), matcher.group(2));
		}
		return null;
	}
}
