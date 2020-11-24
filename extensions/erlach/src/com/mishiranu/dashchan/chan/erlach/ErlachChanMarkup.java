package com.mishiranu.dashchan.chan.erlach;

import android.util.Pair;
import chan.content.ChanMarkup;
import chan.text.CommentEditor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ErlachChanMarkup extends ChanMarkup {
	private static final int SUPPORTED_TAGS = TAG_BOLD | TAG_ITALIC | TAG_SPOILER;

	public ErlachChanMarkup() {
		addTag("span", "bold", TAG_BOLD);
		addTag("span", "italic", TAG_ITALIC);
		addTag("span", "spoiler", TAG_SPOILER);
		addTag("span", "citate", TAG_QUOTE);
	}

	@Override
	public CommentEditor obtainCommentEditor(String boardName) {
		CommentEditor commentEditor = new CommentEditor();
		commentEditor.addTag(TAG_BOLD, "[b]", "[/b]");
		commentEditor.addTag(TAG_ITALIC, "[i]", "[/i]");
		commentEditor.addTag(TAG_SPOILER, "[s]", "[/s]");
		return commentEditor;
	}

	@Override
	public boolean isTagSupported(String boardName, int tag) {
		return (SUPPORTED_TAGS & tag) == tag;
	}

	private static final Pattern THREAD_LINK = Pattern.compile("/.*?/(\\w+)(/\\w+)?/?");

	@Override
	public Pair<String, String> obtainPostLinkThreadPostNumbers(String uriString) {
		Matcher matcher = THREAD_LINK.matcher(uriString);
		if (matcher.matches()) {
			ErlachChanLocator locator = ErlachChanLocator.get(this);
			return new Pair<>(locator.convertToDecimalNumber(matcher.group(1)),
					locator.convertToDecimalNumber(matcher.group(2)));
		}
		return null;
	}
}
