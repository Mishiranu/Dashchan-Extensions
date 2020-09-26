package com.mishiranu.dashchan.chan.arhivach;

import android.util.Pair;
import chan.content.ChanMarkup;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArhivachChanMarkup extends ChanMarkup {
	public ArhivachChanMarkup() {
		addTag("strong", TAG_BOLD);
		addTag("em", TAG_ITALIC);
		addTag("blockquote", TAG_QUOTE);
		addTag("code", TAG_CODE);
		addTag("sub", TAG_SUBSCRIPT);
		addTag("sup", TAG_SUPERSCRIPT);
		addTag("del", TAG_STRIKE);
		addTag("span", "unkfunc", TAG_QUOTE);
		addTag("span", "spoiler", TAG_SPOILER);
		addTag("span", "s", TAG_STRIKE);
		addTag("span", "u", TAG_UNDERLINE);
	}

	private static final Pattern THREAD_LINK = Pattern.compile("#(\\d+)?$");

	@Override
	public Pair<String, String> obtainPostLinkThreadPostNumbers(String uriString) {
		Matcher matcher = THREAD_LINK.matcher(uriString);
		if (matcher.find()) {
			return new Pair<>(null, matcher.group(1));
		}
		return null;
	}
}
