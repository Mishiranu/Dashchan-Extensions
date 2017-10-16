package com.mishiranu.dashchan.chan.dangeru;

import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import chan.content.ApiException;
import chan.content.ChanPerformer;
import chan.content.InvalidResponseException;
import chan.content.RedirectException;
import chan.content.model.Posts;
import chan.http.HttpException;
import chan.http.HttpRequest;
import chan.http.HttpResponse;
import chan.http.MultipartEntity;
import chan.http.RequestEntity;
import chan.text.ParseException;
import chan.util.CommonUtils;
import chan.util.StringUtils;

public class DangeruChanPerformer extends ChanPerformer {
	@Override
	public ReadThreadsResult onReadThreads(ReadThreadsData data) throws HttpException, InvalidResponseException {
		DangeruChanLocator locator = DangeruChanLocator.get(this);
		Uri uri = locator.buildQuery("api/v2/board/" + data.boardName, "page", Integer.toString(data.pageNumber));
		HttpResponse response = new HttpRequest(uri, data).setValidator(data.validator).read();
		JSONArray jsonArray = response.getJsonArray();
		try {
			if (jsonArray.length() == 0) {
				throw HttpException.createNotFoundException();
			}
			Posts[] threads = new Posts[jsonArray.length()];
			for (int i = 0; i < threads.length; i++) {
				threads[i] = DangeruModelMapper.createThread(jsonArray.getJSONObject(i));
			}
			return new ReadThreadsResult(threads);
		} catch (JSONException e) {
			throw new InvalidResponseException(e);
		}
	}

	@Override
	public ReadPostsResult onReadPosts(ReadPostsData data) throws HttpException, InvalidResponseException,
			RedirectException {
		DangeruChanLocator locator = DangeruChanLocator.get(this);
		Uri uri = locator.buildPath("api", "v2", "thread", data.threadNumber, "replies");
		JSONArray jsonArray = new HttpRequest(uri, data).setValidator(data.validator).read().getJsonArray();
		try {
			String boardName = jsonArray.getJSONObject(0).getString("board");
			if (!StringUtils.equals(data.boardName, boardName)) {
				throw RedirectException.toThread(boardName, data.threadNumber, null);
			}
			return new ReadPostsResult(DangeruModelMapper.createThreadFromReplies(jsonArray));
		} catch (JSONException e) {
			throw new InvalidResponseException(e);
		}
	}

	@Override
	public ReadBoardsResult onReadBoards(ReadBoardsData data) throws HttpException, InvalidResponseException {
		DangeruChanLocator locator = DangeruChanLocator.get(this);
		Uri uri = locator.buildPath("");
		String responseText = new HttpRequest(uri, data).read().getString();
		try {
			return new ReadBoardsResult(new DangeruBoardsParser(responseText).convert());
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		}
	}

	@Override
	public ReadPostsCountResult onReadPostsCount(ReadPostsCountData data) throws HttpException,
			InvalidResponseException {
		DangeruChanLocator locator = DangeruChanLocator.get(this);
		Uri uri = locator.buildPath("api", "v2", "thread", data.threadNumber, "metadata");
		JSONObject jsonObject = new HttpRequest(uri, data).setValidator(data.validator).read().getJsonObject();
		if (jsonObject != null) {
			try {
				return new ReadPostsCountResult(jsonObject.getInt("number_of_replies"));
			} catch (JSONException e) {
				throw new InvalidResponseException(e);
			}
		}
		throw new InvalidResponseException();
	}

	private static final Pattern PATTERN_POST_SUCCESS = Pattern.compile("Thread (\\d+) on danger");
	private static final Pattern PATTERN_REPLY_SUCCESS = Pattern.compile("OK/(\\d+)");

	@Override
	public SendPostResult onSendPost(SendPostData data) throws HttpException, ApiException, InvalidResponseException {
		RequestEntity entity = new MultipartEntity();
		entity.add("board", data.boardName);
		Uri uri;
		DangeruChanLocator locator = DangeruChanLocator.get(this);
		if (data.threadNumber == null) {
			uri = locator.buildPath("post");
			entity.add("title", StringUtils.emptyIfNull(data.subject));
			entity.add("comment", StringUtils.emptyIfNull(data.comment));
		} else {
			uri = locator.buildPath("reply");
			entity.add("parent", data.threadNumber);
			entity.add("content", StringUtils.emptyIfNull(data.comment));
		}
		String responseText = new HttpRequest(uri, data)
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
			CommonUtils.writeLog("Dangeru send message", errorMessage);
			throw new ApiException(errorMessage);
		}
		throw new InvalidResponseException();
	}
}
