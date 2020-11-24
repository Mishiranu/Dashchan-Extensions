package com.mishiranu.dashchan.chan.fourchan;

import android.content.res.Resources;
import android.util.Pair;
import chan.content.ChanConfiguration;
import chan.content.ChanMarkup;
import chan.text.JsonSerial;
import chan.text.ParseException;
import chan.util.StringUtils;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;

public class FourchanChanConfiguration extends ChanConfiguration {
	private static final String KEY_FLAGS_ENABLED = "flags_enabled";
	private static final String KEY_TROLL_FLAGS_ENABLED = "troll_flags_enabled";
	private static final String KEY_SPOILERS_ENABLED = "spoilers_enabled";
	private static final String KEY_CODE_ENABLED = "code_enabled";
	private static final String KEY_MAX_COMMENT_LENGTH = "max_comment_length";
	private static final String KEY_SAFE_FOR_WORK = "safe_for_work";
	private static final String KEY_TROLL_FLAGS = "troll_flags";
	private static final String KEY_REPORT_REASONS = "report_reasons";

	private static final String KEY_MATH_TAGS = "math_tags";

	public FourchanChanConfiguration() {
		request(OPTION_READ_THREAD_PARTIALLY);
		request(OPTION_ALLOW_CAPTCHA_PASS);
		setDefaultName("Anonymous");
		setBumpLimit(300);
		addCaptchaType(CAPTCHA_TYPE_RECAPTCHA_2);
		addCustomPreference(KEY_MATH_TAGS, false);
	}

	@Override
	public Board obtainBoardConfiguration(String boardName) {
		Board board = new Board();
		board.allowCatalog = true;
		board.allowCatalogSearch = true;
		board.allowArchive = true;
		board.allowPosting = true;
		board.allowDeleting = true;
		board.allowReporting = !StringUtils.isEmpty(get(boardName, KEY_REPORT_REASONS, ""));
		return board;
	}

	@SuppressWarnings("ComparatorCombinators")
	@Override
	public Posting obtainPostingConfiguration(String boardName, boolean newThread) {
		Posting posting = new Posting();
		boolean isCancerBoard = "b".equals(boardName) || "soc".equals(boardName);
		posting.allowName = !isCancerBoard;
		posting.allowTripcode = !isCancerBoard;
		posting.allowEmail = true;
		posting.allowSubject = newThread && !isCancerBoard;
		posting.optionSage = true;
		posting.maxCommentLength = get(boardName, KEY_MAX_COMMENT_LENGTH, 2000);
		posting.attachmentCount = 1;
		posting.attachmentMimeTypes.add("image/*");
		posting.attachmentMimeTypes.add("video/webm");
		posting.attachmentSpoiler = get(boardName, KEY_SPOILERS_ENABLED, false);
		posting.hasCountryFlags = get(boardName, KEY_FLAGS_ENABLED, false);
		if (get(boardName, KEY_TROLL_FLAGS_ENABLED, false)) {
			String flags = StringUtils.emptyIfNull(get(null, KEY_TROLL_FLAGS, null));
			try {
				JSONObject jsonObject = new JSONObject(flags);
				Iterator<String> iterator = jsonObject.keys();
				while (iterator.hasNext()) {
					String key = iterator.next();
					String title = jsonObject.getString(key);
					posting.userIcons.add(new Pair<>(key, title));
				}
				Collections.sort(posting.userIcons, (lhs, rhs) -> lhs.first.compareTo(rhs.first));
			} catch (JSONException e) {
				// Ignore
			}
		}
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
		List<ReportReason> reportReasons = ReportReason.parse(get(boardName, KEY_REPORT_REASONS, ""));
		Reporting reporting = new Reporting();
		for (ReportReason reportReason : reportReasons) {
			reporting.types.add(new Pair<>(reportReason.getKey(), reportReason.title));
		}
		return reporting;
	}

	@Override
	public Authorization obtainCaptchaPassConfiguration() {
		Authorization authorization = new Authorization();
		authorization.fieldsCount = 2;
		authorization.hints = new String[] {"Token", "PIN"};
		return authorization;
	}

	@Override
	public CustomPreference obtainCustomPreferenceConfiguration(String key) {
		if (KEY_MATH_TAGS.equals(key)) {
			Resources resources = getResources();
			CustomPreference customPreference = new CustomPreference();
			customPreference.title = resources.getString(R.string.preference_math_tags);
			customPreference.summary = resources.getString(R.string.preference_math_tags_summary);
			return customPreference;
		}
		return null;
	}

	public boolean isTagSupported(String boardName, int tag) {
		if (tag == ChanMarkup.TAG_SPOILER) {
			return get(boardName, KEY_SPOILERS_ENABLED, false);
		}
		if (tag == ChanMarkup.TAG_CODE) {
			return get(boardName, KEY_CODE_ENABLED, false);
		}
		return false;
	}

	public boolean isMathTagsHandlingEnabled() {
		return get(null, KEY_MATH_TAGS, false);
	}

	public boolean isSafeForWork(String boardName) {
		return get(boardName, KEY_SAFE_FOR_WORK, false);
	}

	public chan.content.model.Board updateBoard(JsonSerial.Reader reader) throws IOException, ParseException {
		String boardName = null;
		String title = null;
		String description = null;
		boolean areSpoilersEnabled = false;
		boolean isCodeEnabled = false;
		boolean areFlagsEnabled = false;
		boolean areTrollFlagsEnabled = false;
		int bumpLimit = 0;
		int maxCommentLength = 0;
		boolean safeForWork = false;
		reader.startObject();
		while (!reader.endStruct()) {
			switch (reader.nextName()) {
				case "board": {
					boardName = reader.nextString();
					break;
				}
				case "title": {
					title = reader.nextString();
					break;
				}
				case "meta_description": {
					description = StringUtils.clearHtml(reader.nextString());
					break;
				}
				case "spoilers": {
					areSpoilersEnabled = reader.nextBoolean();
					break;
				}
				case "code_tags": {
					isCodeEnabled = reader.nextBoolean();
					break;
				}
				case "country_flags": {
					areFlagsEnabled = reader.nextBoolean();
					break;
				}
				case "troll_flags": {
					areTrollFlagsEnabled = reader.nextBoolean();
					break;
				}
				case "bump_limit": {
					bumpLimit = reader.nextInt();
					break;
				}
				case "max_comment_chars": {
					maxCommentLength = reader.nextInt();
					break;
				}
				case "ws_board": {
					safeForWork = reader.nextBoolean();
					break;
				}
				default: {
					reader.skip();
					break;
				}
			}
		}
		if (boardName != null && title != null) {
			if (description != null) {
				String find = " is 4chan's ";
				int index = description.indexOf(find);
				if (index < 0) {
					find = " is the ";
					index = description.indexOf(find);
				}
				if (index < 0) {
					find = " is a ";
					index = description.indexOf(find);
				}
				if (index >= 0) {
					index += find.length();
					if (index + 1 < description.length()) {
						description = Character.toUpperCase(description.charAt(index)) +
								description.substring(index + 1);
					}
				}
			}
			set(boardName, KEY_SPOILERS_ENABLED, areSpoilersEnabled);
			set(boardName, KEY_CODE_ENABLED, isCodeEnabled);
			set(boardName, KEY_FLAGS_ENABLED, areFlagsEnabled);
			set(boardName, KEY_TROLL_FLAGS_ENABLED, areTrollFlagsEnabled);
			if (bumpLimit != 0) {
				storeBumpLimit(boardName, bumpLimit);
			}
			if (maxCommentLength > 0) {
				set(boardName, KEY_MAX_COMMENT_LENGTH, maxCommentLength);
			}
			set(boardName, KEY_SAFE_FOR_WORK, safeForWork);
			return new chan.content.model.Board(boardName, title, description);
		}
		return null;
	}

	public void updateTrollFlags(Map<String, String> flags) {
		JSONObject jsonObject = new JSONObject();
		try {
			for (Map.Entry<String, String> entry : flags.entrySet()) {
				jsonObject.put(entry.getKey(), entry.getValue());
			}
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		set(null, KEY_TROLL_FLAGS, jsonObject.toString());
	}

	public void updateReportingConfiguration(String boardName, List<ReportReason> reportReasons) {
		set(boardName, KEY_REPORT_REASONS, ReportReason.serialize(reportReasons));
	}
}
