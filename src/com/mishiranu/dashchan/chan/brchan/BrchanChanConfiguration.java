package com.mishiranu.dashchan.chan.brchan;

import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.res.Resources;
import android.util.Pair;

import chan.content.ChanConfiguration;
import chan.util.CommonUtils;
import chan.util.StringUtils;

public class BrchanChanConfiguration extends ChanConfiguration {
	private static final String KEY_ICONS = "icons";
	private static final String KEY_IMAGES_ENABLED = "images_enabled";
	private static final String KEY_NAMES_ENABLED = "names_enabled";
	private static final String KEY_FLAGS_ENABLED = "flags_enabled";
	private static final String KEY_DELETE_ENABLED = "delete_enabled";
	private static final String KEY_CODE_ENABLED = "code_enabled";

	public BrchanChanConfiguration() {
		request(OPTION_READ_POSTS_COUNT);
		request(OPTION_READ_USER_BOARDS);
		setDefaultName("Anônimo");
		setBumpLimit(300);
		setBumpLimitMode(BumpLimitMode.AFTER_REPLY);
		addCaptchaType("infinite");
	}

	@Override
	public Board obtainBoardConfiguration(String boardName) {
		Board board = new Board();
		board.allowSearch = true;
		board.allowCatalog = true;
		board.allowPosting = true;
		board.allowDeleting = get(boardName, KEY_DELETE_ENABLED, false) || boardName == null;
		board.allowReporting = true;
		return board;
	}

	@Override
	public Captcha obtainCustomCaptchaConfiguration(String captchaType) {
		if ("infinite".equals(captchaType)) {
			Captcha captcha = new Captcha();
			captcha.title = "BRCHAN";
			captcha.input = Captcha.Input.ALL;
			captcha.validity = Captcha.Validity.IN_BOARD_SEPARATELY;
			return captcha;
		}
		return null;
	}

	@Override
	public Posting obtainPostingConfiguration(String boardName, boolean newThread) {
		Posting posting = new Posting();
		posting.allowName = posting.allowTripcode = get(boardName, KEY_NAMES_ENABLED, true);
		posting.allowSubject = true;
		posting.optionSage = true;
		posting.attachmentCount = get(boardName, KEY_IMAGES_ENABLED, true) ? 3 : 0;
		posting.attachmentMimeTypes.add("image/*");
		posting.attachmentMimeTypes.add("video/*");
		posting.attachmentMimeTypes.add("audio/*");
		posting.attachmentSpoiler = true;
		try {
			JSONObject jsonObject = new JSONObject(get(boardName, KEY_ICONS, "[]"));
			Iterator<String> keys = jsonObject.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				String name = StringUtils.clearHtml(CommonUtils.getJsonString(jsonObject, key));
				posting.userIcons.add(new Pair<>(key, name));
			}
		} catch (Exception e) {
			// Ignore exception
		}
		posting.hasCountryFlags = get(boardName, KEY_FLAGS_ENABLED, false);
		return posting;
	}

	@Override
	public Deleting obtainDeletingConfiguration(String boardName) {
		Deleting deleting = new Deleting();
		deleting.password = true;
		deleting.multiplePosts = true;
		deleting.optionFilesOnly = true;
		return deleting;
	}

	@Override
	public Reporting obtainReportingConfiguration(String boardName) {
		Resources resources = getResources();
		Reporting reporting = new Reporting();
		reporting.comment = true;
		reporting.options.add(new Pair<>("global", resources.getString(R.string.text_global_report)));
		return reporting;
	}

	public boolean isTagSupported(String boardName, int tag) {
		if (tag == BrchanChanMarkup.TAG_CODE) {
			return get(boardName, KEY_CODE_ENABLED, false);
		}
		return false;
	}

	public void updateFromBoardJson(String boardName, JSONObject jsonObject, boolean updateTitle) {
		if (updateTitle) {
			try {
				String title = CommonUtils.getJsonString(jsonObject, "title");
				String description = CommonUtils.optJsonString(jsonObject, "subtitle");
				storeBoardTitle(boardName, title);
				storeBoardDescription(boardName, description);
			} catch (JSONException e) {
				// Ignore exception
			}
		}
		String defaultName = CommonUtils.optJsonString(jsonObject, "anonymous");
		int bumpLimit = jsonObject.optInt("reply_limit");
		boolean imagesEnabled = !jsonObject.optBoolean("disable_images");
		boolean namesEnabled = !jsonObject.optBoolean("field_disable_name");
		boolean flagsEnabled = jsonObject.optBoolean("country_flags");
		boolean deleteEnabled = jsonObject.optBoolean("allow_delete");
		boolean codeEnabled = jsonObject.optBoolean("code_tags");
		JSONObject userFlags = jsonObject.optJSONObject("user_flags");
		if (!StringUtils.isEmpty(defaultName)) {
			storeDefaultName(boardName, defaultName);
		}
		if (bumpLimit != 0) {
			storeBumpLimit(boardName, bumpLimit);
		}
		set(boardName, KEY_IMAGES_ENABLED, imagesEnabled);
		set(boardName, KEY_NAMES_ENABLED, namesEnabled);
		set(boardName, KEY_FLAGS_ENABLED, flagsEnabled);
		set(boardName, KEY_DELETE_ENABLED, deleteEnabled);
		set(boardName, KEY_CODE_ENABLED, codeEnabled);
		set(boardName, KEY_ICONS, userFlags != null ? userFlags.toString() : null);
	}
}
