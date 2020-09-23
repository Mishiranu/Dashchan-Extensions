package com.mishiranu.dashchan.chan.local;

import android.util.Pair;
import chan.content.ChanMarkup;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocalChanMarkup extends ChanMarkup {
	public LocalChanMarkup() {
		addTag("b", TAG_BOLD);
		addTag("i", TAG_ITALIC);
		addTag("u", TAG_UNDERLINE);
		addTag("span", "overline", TAG_OVERLINE);
		addTag("s", TAG_STRIKE);
		addTag("sub", TAG_SUBSCRIPT);
		addTag("sup", TAG_SUPERSCRIPT);
		addTag("span", "spoiler", TAG_SPOILER);
		addTag("span", "unkfunc", TAG_QUOTE);
		addTag("span", "code", TAG_CODE);
		addTag("span", "aa", TAG_ASCII_ART);
		addTag("span", "heading", TAG_HEADING);
		addPreformatted("span", "code", true);
		addColorable("span");
	}

	private static final Pattern THREAD_LINK = Pattern.compile("#(\\d+)$");

	@Override
	public Pair<String, String> obtainPostLinkThreadPostNumbers(String uriString) {
		Matcher matcher = THREAD_LINK.matcher(uriString);
		if (matcher.matches()) {
			return new Pair<>(null, matcher.group(1));
		}
		return null;
	}
}
