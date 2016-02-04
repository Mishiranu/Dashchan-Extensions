package com.mishiranu.dashchan.chan.fourchan;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import chan.content.ChanConfiguration;
import chan.content.ChanMarkup;
import chan.util.CommonUtils;

public class FourchanChanConfiguration extends ChanConfiguration
{
	private static final String KEY_FLAGS_ENABLED = "flags_enabled";
	private static final String KEY_SPOILERS_ENABLED = "spoilers_enabled";
	private static final String KEY_CODE_ENABLED = "code_enabled";
	private static final String KEY_MAX_COMMENT_LENGTH = "max_comment_length";
	
	public FourchanChanConfiguration()
	{
		request(OPTION_READ_POSTS_COUNT);
		request(OPTION_READ_POPULAR_THREADS);
		setDefaultName("Anonymous");
		setBumpLimit(300);
		addCaptchaType(CAPTCHA_TYPE_RECAPTCHA_2);
		addCaptchaType(CAPTCHA_TYPE_RECAPTCHA_1);
	}
	
	@Override
	public Board obtainBoardConfiguration(String boardName)
	{
		Board board = new Board();
		board.allowCatalog = true;
		board.allowCatalogSearch = true;
		board.allowArchive = true;
		board.allowPosting = true;
		board.allowDeleting = true;
		return board;
	}
	
	@Override
	public Posting obtainPostingConfiguration(String boardName, boolean newThread)
	{
		Posting posting = new Posting();
		boolean isCancerBoard = "b".equals(boardName) || "soc".equals(boardName);
		posting.allowName = posting.allowTripcode = posting.allowSubject = !isCancerBoard;
		posting.optionSage = true;
		posting.maxCommentLength = get(boardName, KEY_MAX_COMMENT_LENGTH, 2000);
		posting.attachmentCount = 1;
		posting.attachmentMimeTypes.add("image/*");
		posting.attachmentMimeTypes.add("video/webm");
		posting.attachmentSpoiler = get(boardName, KEY_SPOILERS_ENABLED, false);
		posting.hasCountryFlags = get(boardName, KEY_FLAGS_ENABLED, false);
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
	
	public boolean isTagSupported(String boardName, int tag)
	{
		if (tag == ChanMarkup.TAG_SPOILER) return get(boardName, KEY_SPOILERS_ENABLED, false);
		if (tag == ChanMarkup.TAG_CODE) return get(boardName, KEY_CODE_ENABLED, false);
		return false;
	}
	
	public void updateFromBoardsJson(JSONObject jsonObject)
	{
		try
		{
			JSONArray jsonArray = jsonObject.getJSONArray("boards");
			for (int i = 0; i < jsonArray.length(); i++)
			{
				jsonObject = jsonArray.getJSONObject(i);
				String boardName = CommonUtils.getJsonString(jsonObject, "board");
				boolean areSpoilersEnabled = jsonObject.optInt("spoilers") != 0;
				boolean isCodeEnabled = jsonObject.optInt("code_tags") != 0;
				boolean areFlagsEnabled = jsonObject.optInt("country_flags") != 0;
				int bumpLimit = jsonObject.optInt("bump_limit");
				int maxCommentLength = jsonObject.optInt("max_comment_chars");
				set(boardName, KEY_SPOILERS_ENABLED, areSpoilersEnabled);
				set(boardName, KEY_CODE_ENABLED, isCodeEnabled);
				set(boardName, KEY_FLAGS_ENABLED, areFlagsEnabled);
				if (bumpLimit != 0) storeBumpLimit(boardName, bumpLimit);
				if (maxCommentLength > 0) set(boardName, KEY_MAX_COMMENT_LENGTH, maxCommentLength);
			}
		}
		catch (JSONException e)
		{
			
		}
	}
}