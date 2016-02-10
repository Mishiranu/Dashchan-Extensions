package com.mishiranu.dashchan.chan.nulldvachin;

import org.json.JSONArray;
import org.json.JSONObject;

import chan.content.ChanConfiguration;
import chan.util.CommonUtils;
import chan.util.StringUtils;

public class NulldvachinChanConfiguration extends ChanConfiguration
{
	public static final String CAPTCHA_TYPE_PHUTABA = "phutaba";
	
	public NulldvachinChanConfiguration()
	{
		request(OPTION_READ_THREAD_PARTIALLY);
		request(OPTION_READ_SINGLE_POST);
		request(OPTION_READ_POSTS_COUNT);
		setDefaultName("Аноним");
		addCaptchaType("wakaba");
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
	public Captcha obtainCustomCaptchaConfiguration(String captchaType)
	{
		if ("wakaba".equals(captchaType))
		{
			Captcha captcha = new Captcha();
			captcha.title = "Wakaba";
			captcha.input = Captcha.Input.LATIN;
			captcha.validity = Captcha.Validity.IN_THREAD;
			return captcha;
		}
		return null;
	}
	
	@Override
	public Posting obtainPostingConfiguration(String boardName, boolean newThread)
	{
		Posting posting = new Posting();
		posting.allowName = posting.allowTripcode = !"b".equals(boardName) && !"d".equals(boardName);
		posting.allowSubject = true;
		posting.optionSage = true;
		posting.attachmentCount = "d".equals(boardName) ? 0 : 4;
		posting.attachmentMimeTypes.add("image/*");
		posting.attachmentMimeTypes.add("audio/*");
		posting.attachmentMimeTypes.add("video/*");
		posting.attachmentMimeTypes.add("application/ogg");
		posting.attachmentMimeTypes.add("application/pdf");
		posting.attachmentMimeTypes.add("application/zip");
		posting.attachmentMimeTypes.add("application/rar");
		posting.attachmentMimeTypes.add("application/x-bittorrent");
		posting.attachmentMimeTypes.add("application/x-shockwave-flash");
		return posting;
	}
	
	@Override
	public Deleting obtainDeletingConfiguration(String boardName)
	{
		Deleting deleting = new Deleting();
		deleting.password = true;
		deleting.multiplePosts = true;
		return deleting;
	}
	
	public void updateFromThreadsPostsJson(String boardName, JSONObject jsonObject)
	{
		JSONArray pagesArray = jsonObject.optJSONArray("pages");
		if (pagesArray != null && pagesArray.length() > 0) storePagesCount(boardName, pagesArray.length());
		JSONObject infoObject = jsonObject.optJSONObject("boardinfo");
		if (infoObject != null)
		{
			String title = CommonUtils.optJsonString(infoObject, "board_name");
			String description = CommonUtils.optJsonString(infoObject, "board_desc");
			if (!StringUtils.isEmpty(title)) storeBoardTitle(boardName, title);
			if (!StringUtils.isEmpty(description)) storeBoardDescription(boardName, description);
		}
	}
}