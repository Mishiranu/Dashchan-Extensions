package com.mishiranu.dashchan.chan.randomarchive;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Pair;

import chan.content.ChanMarkup;

public class RandomArchiveChanMarkup extends ChanMarkup {
	public RandomArchiveChanMarkup() {
		addTag("span", "quote", TAG_QUOTE);
	}

	private static final Pattern THREAD_LINK = Pattern.compile("thread/(\\d+)/?(?:#p?(\\d+))?$");

	@Override
	public Pair<String, String> obtainPostLinkThreadPostNumbers(String uriString) {
		Matcher matcher = THREAD_LINK.matcher(uriString);
		if (matcher.find()) {
			return new Pair<>(matcher.group(1), matcher.group(2));
		}
		return null;
	}
}