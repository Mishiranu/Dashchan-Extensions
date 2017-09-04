package com.mishiranu.dashchan.chan.awoo;

import org.json.JSONArray;
import org.json.JSONException;

import chan.content.ChanConfiguration;

public class AwooConfiguration extends ChanConfiguration {
    private static final String KEY_FLAGS_ENABLED = "flags_enabled";
    private static final String KEY_SPOILERS_ENABLED = "spoilers_enabled";
    private static final String KEY_CODE_ENABLED = "code_enabled";
    private static final String KEY_MAX_COMMENT_LENGTH = "max_comment_length";

    private static final String KEY_MATH_TAGS = "math_tags";

    public AwooConfiguration() {
        request(OPTION_READ_POSTS_COUNT);
        setDefaultName("Anonymous");
        setBumpLimit(250);
        addCustomPreference(KEY_MATH_TAGS, false);
    }

    public static boolean isTagSupported(String boardName, int tag) {
        return false;
    }

    @Override
    public Board obtainBoardConfiguration(String boardName) {
        Board board = new Board();
        board.allowCatalog = true;
        board.allowCatalogSearch = false;
        board.allowArchive = false;
        board.allowPosting = true;
        board.allowDeleting = false;
        board.allowReporting = false;
        return board;
    }

    @Override
    public Posting obtainPostingConfiguration(String boardName, boolean newThread) {
        Posting posting = new Posting();
        posting.allowName = false;
        posting.allowTripcode = false;
        posting.allowEmail = false;
        posting.allowSubject = newThread;
        posting.optionSage = false;
        posting.maxCommentLength = 500;
        posting.attachmentCount = 0;
        posting.attachmentSpoiler = false;
        posting.hasCountryFlags = false;
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
        Reporting reporting = new Reporting();
        return reporting;
    }

    @Override
    public Authorization obtainCaptchaPassConfiguration() {
        Authorization authorization = new Authorization();
        authorization.fieldsCount = 2;
        authorization.hints = new String[]{"Token", "PIN"};
        return authorization;
    }

    @Override
    public CustomPreference obtainCustomPreferenceConfiguration(String key) {
        return null;
    }

    public void updateFromBoardsJson(JSONArray jsonArray) {
        try {
            for (int i = 0; i < jsonArray.length(); i++) {
                String board = jsonArray.getString(i);
                set(board, KEY_SPOILERS_ENABLED, false);
                set(board, KEY_CODE_ENABLED, false);
                set(board, KEY_FLAGS_ENABLED, false);
                storeBumpLimit(board, 250);
                set(board, KEY_MAX_COMMENT_LENGTH, 500);
            }
        } catch (JSONException e) {
            // Ignore exception
        }
    }
}