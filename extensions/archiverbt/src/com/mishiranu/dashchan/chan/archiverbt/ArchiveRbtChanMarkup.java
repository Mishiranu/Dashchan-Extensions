package com.mishiranu.dashchan.chan.archiverbt;

import android.util.Pair;
import chan.content.ChanMarkup;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArchiveRbtChanMarkup extends ChanMarkup {
	public ArchiveRbtChanMarkup() {
		addTag("code", TAG_CODE);
		addTag("span", "unkfunc", TAG_QUOTE);
		addPreformatted("code", true);
	}

	private static final Pattern THREAD_LINK = Pattern.compile("(?:^|(?:thread|post)/(\\d+))(?:#p(\\d+))?$");

	@Override
	public Pair<String, String> obtainPostLinkThreadPostNumbers(String uriString) {
		Matcher matcher = THREAD_LINK.matcher(uriString);
		if (matcher.find()) {
			String threadNumber = matcher.group(1);
			String postNumber = matcher.group(2);
			if (postNumber == null && uriString.contains("/post/")) {
				postNumber = threadNumber;
				threadNumber = null;
			}
			return new Pair<>(threadNumber, postNumber);
		}
		return null;
	}
}
