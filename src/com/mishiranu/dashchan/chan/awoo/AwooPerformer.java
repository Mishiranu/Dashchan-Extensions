package com.mishiranu.dashchan.chan.awoo;

import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import chan.content.ApiException;
import chan.content.ChanConfiguration;
import chan.content.ChanLocator;
import chan.content.ChanPerformer;
import chan.content.InvalidResponseException;
import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.content.model.Posts;
import chan.http.CookieBuilder;
import chan.http.HttpException;
import chan.http.HttpRequest;
import chan.http.HttpResponse;
import chan.http.MultipartEntity;
import chan.http.RequestEntity;
import chan.util.CommonUtils;

public class AwooPerformer extends ChanPerformer {
    private static final String[] PREFERRED_BOARDS_ORDER = {"all"};

    private static final String[][] PREFERRED_BOARDS_MAPPING = {{"u", "test"}};
    private static final Pattern PATTERN_POST_SUCCESS = Pattern.compile("Thread (\\d+) on danger");
    private static final Pattern PATTERN_REPLY_SUCCESS = Pattern.compile("OK/(\\d+)");

    private static String getPreferredBoardCategory(String boardName) {
        for (int i = 0; i < PREFERRED_BOARDS_ORDER.length; i++) {
            String category = PREFERRED_BOARDS_ORDER[i];
            for (int j = 0; j < PREFERRED_BOARDS_MAPPING[i].length; j++) {
                if (PREFERRED_BOARDS_MAPPING[i][j].equals(boardName)) {
                    return category;
                }
            }
        }
        return PREFERRED_BOARDS_ORDER[0];
    }

    private static CookieBuilder buildCookies() {
        // TODO
        //CookieBuilder builder = new CookieBuilder();
        //builder.append("rack.session", "something");
        //return builder;
        return null;
    }

    @Override
    public ReadThreadsResult onReadThreads(ReadThreadsData data) throws HttpException, InvalidResponseException {
        AwooLocator locator = ChanLocator.get(this);
        Uri uri = locator.createApiUri("board", data.boardName);
        HttpResponse response = new HttpRequest(uri, data.holder, data).setValidator(data.validator).read();
        JSONArray jsonArray = response.getJsonArray();
        try {
            Posts[] threads = new Posts[jsonArray.length()];
            for (int i = 0; i < threads.length; i++) {
                threads[i] = AwooModelMapper.createThreadFromCatalog(jsonArray.getJSONObject(i));
            }
            return new ReadThreadsResult(threads);
        } catch (JSONException e) {
            throw new InvalidResponseException(e);
        }
    }

    @Override
    public ReadPostsResult onReadPosts(ReadPostsData data) throws HttpException, InvalidResponseException {
        AwooLocator locator = ChanLocator.get(this);
        Uri uri = locator.createApiUri("thread", data.threadNumber, "replies");
        JSONArray jsonArray = new HttpRequest(uri, data.holder, data).setValidator(data.validator)
                .read().getJsonArray();
        try {
            return new ReadPostsResult(AwooModelMapper.createThreadFromReplies(jsonArray));
        } catch (JSONException e) {
            throw new InvalidResponseException(e);
        }
    }

    @Override
    public ReadBoardsResult onReadBoards(ReadBoardsData data) throws HttpException, InvalidResponseException {
        AwooLocator locator = ChanLocator.get(this);
        Uri uri = locator.createApiUri("boards");
        JSONArray jsonArray = new HttpRequest(uri, data.holder, data).read().getJsonArray();
        AwooConfiguration configuration = ChanConfiguration.get(this);
        Map<String, ArrayList<Board>> boardsMap = new LinkedHashMap<>();
        for (String title : PREFERRED_BOARDS_ORDER) {
            boardsMap.put(title, new ArrayList<>());
        }
        try {
            for (int i = 0; i < jsonArray.length(); i++) {
                String board = jsonArray.getString(i);
                Board board_obj = new Board(board, board);
                String category = getPreferredBoardCategory(board);
                ArrayList<Board> boards = boardsMap.get(category);
                if (boards != null) {
                    boards.add(board_obj);
                }
            }
            ArrayList<BoardCategory> boardCategories = new ArrayList<>();
            for (LinkedHashMap.Entry<String, ArrayList<Board>> entry : boardsMap.entrySet()) {
                ArrayList<Board> boards = entry.getValue();
                if (!boards.isEmpty()) {
                    Collections.sort(boards);
                    boardCategories.add(new BoardCategory(entry.getKey(), boards));
                }
            }
            configuration.updateFromBoardsJson(jsonArray);
            return new ReadBoardsResult(boardCategories);
        } catch (JSONException e) {
            throw new InvalidResponseException(e);
        }
    }

    @Override
    public ReadThreadSummariesResult onReadThreadSummaries(ReadThreadSummariesData data) throws HttpException,
            InvalidResponseException {
        return super.onReadThreadSummaries(data);
    }

    @Override
    public ReadPostsCountResult onReadPostsCount(ReadPostsCountData data) throws HttpException,
            InvalidResponseException {
        AwooLocator locator = ChanLocator.get(this);
        Uri uri = locator.createApiUri("thread", data.threadNumber, "metadata");
        JSONObject jsonObject = new HttpRequest(uri, data.holder, data).setValidator(data.validator)
                .read().getJsonObject();
        if (jsonObject != null) {
            try {
                return new ReadPostsCountResult(jsonObject.getInt("number_of_replies"));
            } catch (JSONException e) {
                throw new InvalidResponseException(e);
            }
        }
        throw new InvalidResponseException();
    }

    @Override
    public ReadContentResult onReadContent(ReadContentData data) throws HttpException, InvalidResponseException {
        return super.onReadContent(data);
    }

    @Override
    public CheckAuthorizationResult onCheckAuthorization(CheckAuthorizationData data) throws HttpException,
            InvalidResponseException {
        // TODO what?
        return new CheckAuthorizationResult(true);
    }

    /*
    @Override
    public ReadCaptchaResult onReadCaptcha(ReadCaptchaData data) throws HttpException, InvalidResponseException {
        return null;
    }
    */

    @Override
    public SendPostResult onSendPost(SendPostData data) throws HttpException, ApiException, InvalidResponseException {
        RequestEntity entity = new MultipartEntity();
        entity.add("board", data.boardName);
        Uri uri;
        AwooLocator locator = ChanLocator.get(this);
        if (data.optionOriginalPoster) {
            uri = locator.createSysUri(data.boardName, "post");
            entity.add("title", data.subject);
            entity.add("content", data.comment);
        } else {
            uri = locator.createSysUri(data.boardName, "reply");
            entity.add("parent", data.threadNumber);
            entity.add("content", data.comment);
        }
        String responseText = new HttpRequest(uri, data.holder, data)
                .setPostMethod(entity).setRedirectHandler(HttpRequest.RedirectHandler.STRICT).read().getString();

        Matcher matcher = PATTERN_POST_SUCCESS.matcher(responseText);
        if (matcher.find()) {
            // NEW THREAD success
            return new SendPostResult(matcher.group(1), null);
        }
        matcher = PATTERN_REPLY_SUCCESS.matcher(responseText);
        if (matcher.find()) {
            // REPLY success
            return new SendPostResult(data.threadNumber, matcher.group(1));
        }
        if (matcher.find()) {
            String errorMessage = matcher.group(1);
            if (errorMessage != null) {
                int errorType = 0;
                if (errorMessage.contains("Bump limit reached")) {
                    errorType = ApiException.SEND_ERROR_CLOSED;
                } else if (errorMessage.contains("Reply too long")) {
                    errorType = ApiException.SEND_ERROR_FIELD_TOO_LONG;
                } else if (errorMessage.contains("Flood detected")) {
                    errorType = ApiException.SEND_ERROR_TOO_FAST;
                } else if (errorMessage.contains("banned")) {
                    errorType = ApiException.SEND_ERROR_BANNED;
                }
                if (errorType != 0) {
                    throw new ApiException(errorType);
                }
            }
            CommonUtils.writeLog("Awoo send message", errorMessage);
            throw new ApiException(errorMessage);
        }
        throw new InvalidResponseException();
    }

    @Override
    public SendDeletePostsResult onSendDeletePosts(SendDeletePostsData data) throws HttpException, ApiException,
            InvalidResponseException {
        return null;
    }

    @Override
    public SendReportPostsResult onSendReportPosts(SendReportPostsData data) throws HttpException, ApiException,
            InvalidResponseException {
        return null;
    }
}