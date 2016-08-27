package com.mishiranu.dashchan.chan.alterchan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Pair;

import chan.content.ChanMarkup;
import chan.text.CommentEditor;

public class AlterchanChanMarkup extends ChanMarkup
{
	private static final int SUPPORTED_TAGS = TAG_BOLD | TAG_ITALIC | TAG_UNDERLINE | TAG_STRIKE |
			TAG_SPOILER | TAG_CODE;
	
	public AlterchanChanMarkup()
	{
		addTag("span", "markup_b", TAG_BOLD);
		addTag("span", "markup_i", TAG_ITALIC);
		addTag("span", "markup_u", TAG_UNDERLINE);
		addTag("span", "markup_s", TAG_STRIKE);
		addTag("span", "markup_spoiler", TAG_SPOILER);
		addTag("span", "markup_quote", TAG_QUOTE);
		addTag("pre", TAG_CODE);
	}
	
	@Override
	public CommentEditor obtainCommentEditor(String boardName)
	{
		return new CommentEditor.BulletinBoardCodeCommentEditor();
	}
	
	@Override
	public boolean isTagSupported(String boardName, int tag)
	{
		return (SUPPORTED_TAGS & tag) == tag;
	}
	
	private static final Pattern THREAD_LINK = Pattern.compile("/discussion.php/(\\w+)(?:(\\w+))?$");
	
	@Override
	public Pair<String, String> obtainPostLinkThreadPostNumbers(String uriString)
	{
		Matcher matcher = THREAD_LINK.matcher(uriString);
		if (matcher.find())
		{
			AlterchanChanLocator locator = AlterchanChanLocator.get(this);
			return new Pair<>(locator.convertToDecimalNumber(matcher.group(1)),
					locator.convertToDecimalNumber(matcher.group(2)));
		}
		return null;
	}
}