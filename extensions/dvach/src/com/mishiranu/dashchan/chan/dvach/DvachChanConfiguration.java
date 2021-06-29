package com.mishiranu.dashchan.chan.dvach;

import android.util.Pair;
import chan.content.ChanConfiguration;
import chan.util.CommonUtils;
import chan.util.StringUtils;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

public class DvachChanConfiguration extends ChanConfiguration {
	public static final String CAPTCHA_TYPE_2CH_CAPTCHA = "2ch_captcha";

	public static final Map<String, String> CAPTCHA_TYPES;

	static {
		Map<String, String> captchaTypes = new LinkedHashMap<>();
		captchaTypes.put(CAPTCHA_TYPE_2CH_CAPTCHA, "2chcaptcha");
		captchaTypes.put(CAPTCHA_TYPE_RECAPTCHA_2, "recaptcha");
		captchaTypes.put(CAPTCHA_TYPE_RECAPTCHA_2_INVISIBLE, "invisible_recaptcha");
		CAPTCHA_TYPES = Collections.unmodifiableMap(captchaTypes);
	}

	private static final String KEY_ICONS = "icons";
	private static final String KEY_IMAGES_ENABLED = "images_enabled";
	private static final String KEY_NAMES_ENABLED = "names_enabled";
	private static final String KEY_TRIPCODES_ENABLED = "tripcodes_enabled";
	private static final String KEY_SUBJECTS_ENABLED = "subjects_enabled";
	private static final String KEY_SAGE_ENABLED = "sage_enabled";
	private static final String KEY_FLAGS_ENABLED = "flags_enabled";
	private static final String KEY_MAX_COMMENT_LENGTH = "max_comment_length";

	public DvachChanConfiguration() {
		request(OPTION_READ_THREAD_PARTIALLY);
		request(OPTION_READ_SINGLE_POST);
		request(OPTION_READ_POSTS_COUNT);
		request(OPTION_READ_USER_BOARDS);
		request(OPTION_ALLOW_CAPTCHA_PASS);
		setDefaultName("Аноним");
		setBumpLimit(500);
		for (String captchaType : CAPTCHA_TYPES.keySet()) {
			addCaptchaType(captchaType);
		}
	}

	@Override
	public Board obtainBoardConfiguration(String boardName) {
		Board board = new Board();
		board.allowSearch = true;
		board.allowCatalog = true;
		board.allowArchive = true;
		board.allowPosting = true;
		board.allowReporting = true;
		return board;
	}

	@Override
	public Captcha obtainCustomCaptchaConfiguration(String captchaType) {
		if (CAPTCHA_TYPE_2CH_CAPTCHA.equals(captchaType)) {
			Captcha captcha = new Captcha();
			captcha.title = "2ch Captcha";
			captcha.input = Captcha.Input.LATIN;
			captcha.validity = Captcha.Validity.IN_THREAD;
			return captcha;
		}
		return null;
	}

	@Override
	public Posting obtainPostingConfiguration(String boardName, boolean newThread) {
		Posting posting = new Posting();
		posting.allowName = get(boardName, KEY_NAMES_ENABLED, true);
		posting.allowTripcode = get(boardName, KEY_TRIPCODES_ENABLED, true);
		posting.allowEmail = true;
		posting.allowSubject = get(boardName, KEY_SUBJECTS_ENABLED, true);
		posting.optionSage = get(boardName, KEY_SAGE_ENABLED, true);
		posting.optionOriginalPoster = true;
		posting.maxCommentLength = get(boardName, KEY_MAX_COMMENT_LENGTH, 15000);
		posting.maxCommentLengthEncoding = "UTF-8";
		posting.attachmentCount = get(boardName, KEY_IMAGES_ENABLED, true)
				? maxFilesCountEnabled ? Math.max(4, filesCount) : 4 : 0;
		posting.attachmentMimeTypes.add("image/*");
		posting.attachmentMimeTypes.add("video/webm");
		posting.attachmentMimeTypes.add("video/mp4");
		try {
			JSONArray jsonArray = new JSONArray(get(boardName, KEY_ICONS, "[]"));
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject jsonObject = jsonArray.getJSONObject(i);
				String name = CommonUtils.getJsonString(jsonObject, "name");
				int num = jsonObject.getInt("num");
				posting.userIcons.add(new Pair<>(Integer.toString(num), name));
			}
		} catch (Exception e) {
			// Ignore exception
		}
		posting.hasCountryFlags = get(boardName, KEY_FLAGS_ENABLED, false);
		return posting;
	}

	@Override
	public Reporting obtainReportingConfiguration(String boardName) {
		Reporting reporting = new Reporting();
		reporting.comment = true;
		reporting.multiplePosts = true;
		return reporting;
	}

	private volatile int filesCount = -1;
	private volatile boolean maxFilesCountEnabled = false;

	public void setMaxFilesCount(int filesCount) {
		this.filesCount = filesCount;
	}

	public void revokeMaxFilesCount() {
		setMaxFilesCount(-1);
	}

	public void setMaxFilesCountEnabled(boolean enabled) {
		maxFilesCountEnabled = enabled;
	}

	public boolean isSageEnabled(String boardName) {
		return get(boardName, KEY_SAGE_ENABLED, true);
	}

	public void updateFromBoardsJson(String boardName, String defaultName, Integer bumpLimit) {
		if (!StringUtils.isEmpty(defaultName)) {
			storeDefaultName(boardName, defaultName);
		}
		if (bumpLimit != null && bumpLimit > 0) {
			storeBumpLimit(boardName, bumpLimit);
		}
	}

	public void updateFromThreadsPostsJson(String boardName, DvachModelMapper.BoardConfiguration configuration) {
		String description = transformBoardDescription(configuration.description);
		if (!StringUtils.isEmpty(configuration.title)) {
			storeBoardTitle(boardName, configuration.title);
		}
		if (!StringUtils.isEmpty(description)) {
			storeBoardDescription(boardName, description);
		}
		if (!StringUtils.isEmpty(configuration.defaultName)) {
			storeDefaultName(boardName, configuration.defaultName);
		}
		if (configuration.bumpLimit > 0) {
			storeBumpLimit(boardName, configuration.bumpLimit);
		}
		if (configuration.maxCommentLength > 0) {
			set(boardName, KEY_MAX_COMMENT_LENGTH, configuration.maxCommentLength);
		}
		editBoards(boardName, KEY_IMAGES_ENABLED, configuration.imagesEnabled);
		editBoards(boardName, KEY_NAMES_ENABLED, configuration.namesEnabled);
		editBoards(boardName, KEY_TRIPCODES_ENABLED, configuration.tripcodesEnabled);
		editBoards(boardName, KEY_SUBJECTS_ENABLED, configuration.subjectsEnabled);
		editBoards(boardName, KEY_SAGE_ENABLED, configuration.sageEnabled);
		editBoards(boardName, KEY_FLAGS_ENABLED, configuration.flagsEnabled);
		if (configuration.pagesCount > 0) {
			storePagesCount(boardName, configuration.pagesCount);
		}
		set(boardName, KEY_ICONS, "[]".equals(configuration.icons) ? null : configuration.icons);
	}

	private void editBoards(String boardName, String key, Boolean value) {
		if (value != null) {
			set(boardName, key, value);
		}
	}

	public String transformBoardDescription(String description) {
		description = StringUtils.nullIfEmpty(StringUtils.clearHtml(description).trim());
		if (description != null) {
			StringBuilder builder = new StringBuilder();
			builder.append(description.substring(0, 1).toUpperCase(Locale.getDefault()));
			builder.append(description.substring(1));
			if (!description.endsWith(".") && !description.endsWith("!")) {
				builder.append(".");
			}
			description = builder.toString();
		}
		return description;
	}
}
