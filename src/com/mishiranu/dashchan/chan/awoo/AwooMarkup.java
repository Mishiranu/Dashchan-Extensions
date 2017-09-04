package com.mishiranu.dashchan.chan.awoo;

import android.util.Pair;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import chan.content.ChanMarkup;
import chan.text.CommentEditor;

public class AwooMarkup extends ChanMarkup {
    private static final Pattern THREAD_LINK = Pattern.compile("(?:^|thread/(\\d+))(?:#p(\\d+))?$");

    public AwooMarkup() {
        addTag("span", "redtext", TAG_QUOTE);
    }

    @Override
    public CommentEditor obtainCommentEditor(String boardName) {
        return new CommentEditor.BulletinBoardCodeCommentEditor();
    }

    @Override
    public boolean isTagSupported(String boardName, int tag) {
        return false;
    }

    @Override
    public Pair<String, String> obtainPostLinkThreadPostNumbers(String uriString) {
        Matcher matcher = THREAD_LINK.matcher(uriString);
        if (matcher.find()) {
            return new Pair<>(matcher.group(1), matcher.group(2));
        }
        return null;
    }
}