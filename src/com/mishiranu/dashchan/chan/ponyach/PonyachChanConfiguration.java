package com.mishiranu.dashchan.chan.ponyach;

import android.content.res.Resources;
import android.util.Pair;
import chan.content.ChanConfiguration;

public class PonyachChanConfiguration extends ChanConfiguration
{
	private static final String ATTACHMENT_COUNT_KEY = "attachment_count";
	private static final String COOKIE_SESSION_KEY = "session";

	public PonyachChanConfiguration()
	{
		request(OPTION_READ_POSTS_COUNT);
		setDefaultName("Аноним");
		setDefaultName("rf", "Беженец");
		addCaptchaType(CAPTCHA_TYPE_RECAPTCHA_2);
	}

	@Override
	public Board obtainBoardConfiguration(String boardName)
	{
		Board board = new Board();
		board.allowPosting = true;
		board.allowDeleting = true;
		return board;
	}

	@Override
	public Posting obtainPostingConfiguration(String boardName, boolean newThread)
	{
		Resources resources = getResources();
		Posting posting = new Posting();
		posting.allowName = true;
		posting.allowTripcode = true;
		posting.allowEmail = true;
		posting.allowSubject = true;
		posting.optionSage = true;
		posting.attachmentCount = get(boardName, ATTACHMENT_COUNT_KEY, 1);
		posting.attachmentMimeTypes.add("image/*");
		posting.attachmentMimeTypes.add("video/webm");
		posting.attachmentRatings.add(new Pair<>("", resources.getString(R.string.text_rating_none)));
		posting.attachmentRatings.add(new Pair<>("9", resources.getString(R.string.text_rating_nsfw)));
		posting.attachmentRatings.add(new Pair<>("10", resources.getString(R.string.text_rating_spoiler)));
		posting.attachmentRatings.add(new Pair<>("11", resources.getString(R.string.text_rating_unrelated)));
		return posting;
	}

	@Override
	public Deleting obtainDeletingConfiguration(String boardName)
	{
		Deleting deleting = new Deleting();
		deleting.password = true;
		deleting.multiplePosts = true;
		deleting.optionFilesOnly = true;
		return deleting;
	}

	public String getSession()
	{
		return getCookie(COOKIE_SESSION_KEY);
	}

	public void storeSession(String session)
	{
		storeCookie(COOKIE_SESSION_KEY, session, session != null ? "Session" : null);
	}

	public void storeAttachmentCount(String boardName, int count)
	{
		set(boardName, ATTACHMENT_COUNT_KEY, count);
	}
}