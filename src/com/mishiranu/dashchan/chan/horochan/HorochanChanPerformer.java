package com.mishiranu.dashchan.chan.horochan;

import java.net.HttpURLConnection;
import java.util.HashSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;

import chan.content.ApiException;
import chan.content.ChanConfiguration;
import chan.content.ChanLocator;
import chan.content.ChanPerformer;
import chan.content.InvalidResponseException;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.http.HttpException;
import chan.http.HttpRequest;
import chan.http.MultipartEntity;
import chan.http.UrlEncodedEntity;
import chan.util.CommonUtils;
import chan.util.StringUtils;

public class HorochanChanPerformer extends ChanPerformer {
	private static final String RECAPTCHA_KEY = "6LerWhMTAAAAABCXYL2CEv-YyPeM5WbUTx3CknKD";

	@Override
	public ReadThreadsResult onReadThreads(ReadThreadsData data) throws HttpException, InvalidResponseException {
		HorochanChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildApiPath("v1", "boards", Integer.toString(data.pageNumber + 1));
		JSONObject jsonObject = new HttpRequest(uri, data).setValidator(data.validator).read().getJsonObject();
		if (jsonObject != null) {
			checkReponse(jsonObject);
			try {
				int pagesCount = jsonObject.getInt("totalPages");
				HorochanChanConfiguration configuration = ChanConfiguration.get(this);
				configuration.storePagesCount(data.boardName, pagesCount);
				JSONArray jsonArray = jsonObject.getJSONArray("data");
				if (jsonArray != null) {
					Posts[] threads = new Posts[jsonArray.length()];
					for (int i = 0; i < jsonArray.length(); i++) {
						jsonObject = jsonArray.getJSONObject(i);
						int postsCount = jsonObject.getInt("replies_count") + 1;
						threads[i] = new Posts(HorochanModelMapper.createPosts(jsonObject, locator, null))
								.addPostsCount(postsCount);
					}
					return new ReadThreadsResult(threads);
				}
				return null;
			} catch (JSONException e) {
				throw new InvalidResponseException(e);
			}
		}
		throw new InvalidResponseException();
	}

	@Override
	public ReadPostsResult onReadPosts(ReadPostsData data) throws HttpException, InvalidResponseException {
		HorochanChanLocator locator = ChanLocator.get(this);
		boolean partial = data.partialThreadLoading && data.lastPostNumber != null;
		Uri uri = partial ? locator.buildApiPath("v1", "threads", data.threadNumber, "after", data.lastPostNumber)
				: locator.buildApiPath("v1", "threads", data.threadNumber);
		JSONObject jsonObject = new HttpRequest(uri, data).setValidator(data.validator).read().getJsonObject();
		if (jsonObject != null) {
			checkReponse(jsonObject);
			try {
				HashSet<String> postNumbers = new HashSet<>();
				if (data.cachedPosts != null) {
					Post[] posts = data.cachedPosts.getPosts();
					if (posts != null) {
						for (Post post : posts) {
							postNumbers.add(post.getPostNumber());
						}
					}
				}
				if (partial) {
					JSONArray jsonArray = jsonObject.getJSONArray("data");
					if (jsonArray == null || jsonArray.length() == 0) {
						return null;
					}
					return new ReadPostsResult(HorochanModelMapper.createPosts(jsonArray, locator, postNumbers));
				} else {
					jsonObject = jsonObject.getJSONObject("data");
					return new ReadPostsResult(HorochanModelMapper.createPosts(jsonObject, locator, postNumbers));
				}
			} catch (JSONException e) {
				throw new InvalidResponseException(e);
			}
		}
		throw new InvalidResponseException();
	}

	@Override
	public ReadSinglePostResult onReadSinglePost(ReadSinglePostData data) throws HttpException,
			InvalidResponseException {
		HorochanChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildApiPath("v1", "posts", data.postNumber);
		JSONObject jsonObject = new HttpRequest(uri, data).read().getJsonObject();
		if (jsonObject != null) {
			checkReponse(jsonObject);
			try {
				jsonObject = jsonObject.getJSONObject("data");
				return new ReadSinglePostResult(HorochanModelMapper.createPost(jsonObject, locator, null));
			} catch (JSONException e) {
				throw new InvalidResponseException(e);
			}
		}
		throw new InvalidResponseException();
	}

	private void checkReponse(JSONObject jsonObject) throws HttpException {
		if ("ERR".equals(CommonUtils.optJsonString(jsonObject, "status"))) {
			String message = CommonUtils.optJsonString(jsonObject, "message");
			throw new HttpException(0, message);
		}
	}

	@Override
	public ReadPostsCountResult onReadPostsCount(ReadPostsCountData data) throws HttpException,
			InvalidResponseException {
		HorochanChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildApiPath("v1", "threads", data.threadNumber, "count");
		JSONObject jsonObject = new HttpRequest(uri, data).setValidator(data.validator).read().getJsonObject();
		if (jsonObject != null) {
			try {
				return new ReadPostsCountResult(jsonObject.getJSONObject("data").getInt("replies_count") + 1);
			} catch (JSONException e) {
				throw new InvalidResponseException(e);
			}
		}
		throw new InvalidResponseException();
	}

	@Override
	public ReadCaptchaResult onReadCaptcha(ReadCaptchaData data) throws HttpException, InvalidResponseException {
		CaptchaData captchaData = new CaptchaData();
		captchaData.put(CaptchaData.API_KEY, RECAPTCHA_KEY);
		return new ReadCaptchaResult(CaptchaState.CAPTCHA, captchaData);
	}

	@Override
	public SendPostResult onSendPost(SendPostData data) throws HttpException, ApiException, InvalidResponseException {
		MultipartEntity entity = new MultipartEntity();
		entity.add("parent", data.threadNumber);
		entity.add("board", data.boardName);
		entity.add("password", data.password);
		entity.add("subject", StringUtils.emptyIfNull(data.subject));
		entity.add("message", StringUtils.emptyIfNull(data.comment));
		if (data.attachments != null) {
			for (int i = 0; i < data.attachments.length; i++) {
				data.attachments[i].addToEntity(entity, "file_" + (i + 1));
			}
		}
		if (data.captchaData != null) {
			entity.add("g-recaptcha-response", data.captchaData.get(CaptchaData.INPUT));
		}

		HorochanChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildApiPath("v1", data.threadNumber == null ? "threads" : "posts");
		JSONObject jsonObject = new HttpRequest(uri, data).setPostMethod(entity).setSuccessOnly(false)
				.read().getJsonObject();
		if (jsonObject == null) {
			data.holder.checkResponseCode();
			throw new InvalidResponseException();
		}

		if (data.holder.getResponseCode() == HttpURLConnection.HTTP_OK) {
			return null;
		}
		JSONArray messages = jsonObject.optJSONArray("message");
		String message = CommonUtils.optJsonString(jsonObject, "message");
		if (message == null) {
			throw new InvalidResponseException();
		}
		if (messages != null) {
			try {
				String newMessage = messages.getJSONObject(0).getJSONArray("errors").getString(0);
				if (newMessage != null) {
					message = newMessage;
				}
			} catch (JSONException e) {
				// Ignore exception
			}
		}
		int errorType = 0;
		if (message.contains("Invalid captcha")) {
			errorType = ApiException.SEND_ERROR_CAPTCHA;
		} else if (message.contains("'Subject'")) {
			errorType = ApiException.SEND_ERROR_EMPTY_SUBJECT;
		} else if (message.contains("'Message'")) {
			errorType = ApiException.SEND_ERROR_EMPTY_COMMENT;
		} else if (message.contains("You must upload an file")) {
			errorType = data.threadNumber == null ? ApiException.SEND_ERROR_EMPTY_FILE
					: ApiException.SEND_ERROR_EMPTY_COMMENT;
		}
		if (errorType != 0) {
			throw new ApiException(errorType);
		} else {
			CommonUtils.writeLog("Horochan send message", message);
			throw new ApiException(message);
		}
	}

	@Override
	public SendDeletePostsResult onSendDeletePosts(SendDeletePostsData data) throws HttpException, ApiException,
			InvalidResponseException {
		HorochanChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildApiPath("v1", "posts", data.postNumbers.get(0));
		JSONObject jsonObject = new HttpRequest(uri, data).setDeleteMethod(new UrlEncodedEntity("password",
				data.password)).setSuccessOnly(false).read().getJsonObject();
		if (jsonObject == null) {
			data.holder.checkResponseCode();
			throw new InvalidResponseException();
		}
		if (data.holder.getResponseCode() == HttpURLConnection.HTTP_OK) {
			return null;
		}
		String message = CommonUtils.optJsonString(jsonObject, "message");
		if (message == null) {
			throw new InvalidResponseException();
		}
		int errorType = 0;
		if (message.contains("Post not found or deleted")) {
			errorType = ApiException.DELETE_ERROR_NO_ACCESS;
		} else if (message.contains("Incorrect password")) {
			errorType = ApiException.DELETE_ERROR_PASSWORD;
		} else if (message.contains("expired")) {
			errorType = ApiException.DELETE_ERROR_TOO_OLD;
		}
		if (errorType != 0) {
			throw new ApiException(errorType);
		} else {
			CommonUtils.writeLog("Horochan delete message", message);
			throw new ApiException(message);
		}
	}
}