package com.mishiranu.dashchan.chan.nulldvachin;

import org.json.JSONArray;
import org.json.JSONObject;

import chan.content.ChanConfiguration;
import chan.util.CommonUtils;
import chan.util.StringUtils;

public class NulldvachinChanConfiguration extends ChanConfiguration
{
	public static final String CAPTCHA_TYPE_PHUTABA = "phutaba";
	
	private static final String KEY_NAMES_ENABLED = "names_enabled";
	private static final String KEY_THREAD_IMAGES_ENABLED = "thread_images_enabled";
	private static final String KEY_REPLY_IMAGES_ENABLED = "reply_images_enabled";
	private static final String KEY_ATTACHMENT_COUNT = "attachment_count";
	
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
		posting.allowName = posting.allowTripcode = get(boardName, KEY_NAMES_ENABLED, true);
		posting.allowSubject = true;
		posting.optionSage = true;
		posting.attachmentCount = get(boardName, newThread ? KEY_THREAD_IMAGES_ENABLED : KEY_REPLY_IMAGES_ENABLED, true)
				? get(boardName, KEY_ATTACHMENT_COUNT, 4) : 0;
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
			JSONObject configObject = infoObject.optJSONObject("config");
			if (configObject != null)
			{
				int bumpLimit = configObject.optInt("max_res");
				boolean namesEnabled = configObject.optInt("names_allowed", 1) != 0;
				boolean threadImagesEnabled = configObject.optInt("image_op", 1) != 0;
				boolean replyImagesEnabled = configObject.optInt("image_replies", 1) != 0;
				int attachmentCount = configObject.optInt("max_res", 4);
				if (bumpLimit > 0) storeBumpLimit(boardName, bumpLimit);
				set(boardName, KEY_NAMES_ENABLED, namesEnabled);
				set(boardName, KEY_THREAD_IMAGES_ENABLED, threadImagesEnabled);
				set(boardName, KEY_REPLY_IMAGES_ENABLED, replyImagesEnabled);
				set(boardName, KEY_ATTACHMENT_COUNT, attachmentCount);
			}
		}
	}
}