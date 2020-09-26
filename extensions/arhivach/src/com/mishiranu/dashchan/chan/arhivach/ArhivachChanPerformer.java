package com.mishiranu.dashchan.chan.arhivach;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import chan.content.ApiException;
import chan.content.ChanConfiguration;
import chan.content.ChanLocator;
import chan.content.ChanPerformer;
import chan.content.InvalidResponseException;
import chan.http.HttpException;
import chan.http.HttpHolder;
import chan.http.HttpRequest;
import chan.http.HttpResponse;
import chan.http.UrlEncodedEntity;
import chan.text.ParseException;
import chan.util.CommonUtils;
import chan.util.StringUtils;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ArhivachChanPerformer extends ChanPerformer {
	public static final int PAGE_SIZE = 25;

	@Override
	public ReadThreadsResult onReadThreads(ReadThreadsData data) throws HttpException, InvalidResponseException {
		ArhivachChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildPath("index/" + (data.pageNumber * PAGE_SIZE));
		String responseText = new HttpRequest(uri, data.holder, data).setValidator(data.validator).read().getString();
		try {
			return new ReadThreadsResult(new ArhivachThreadsParser(responseText, this, true).convertThreads());
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		}
	}

	@Override
	public ReadPostsResult onReadPosts(ReadPostsData data) throws HttpException, InvalidResponseException {
		ArhivachChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.createThreadUri(null, data.threadNumber);
		String responseText = new HttpRequest(uri, data.holder, data).setValidator(data.validator).read().getString();
		try {
			return new ReadPostsResult(new ArhivachPostsParser(responseText, this, data.threadNumber).convert());
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		}
	}

	private static final Pattern PATTERN_LONG_QUERY_PART = Pattern.compile("(?:^| )\"(.*?)\"(?= |$)");

	private String lastSearchQuery;
	private String lastSearchTags;
	private ArrayList<String> lastSearchTagsList;

	@Override
	public ReadSearchPostsResult onReadSearchPosts(ReadSearchPostsData data) throws HttpException,
			InvalidResponseException {
		ArhivachChanLocator locator = ChanLocator.get(this);
		ArhivachChanConfiguration configuration = ChanConfiguration.get(this);
		String searchQuery = data.searchQuery;
		boolean tagsOnly = searchQuery.startsWith(":");
		if (tagsOnly) {
			searchQuery = searchQuery.substring(1);
		}
		String searchTags = null;
		ArrayList<String> searchTagsList = null;
		boolean equals;
		synchronized (this) {
			equals = searchQuery.equals(lastSearchQuery);
			if (equals) {
				searchTags = lastSearchTags;
				searchTagsList = lastSearchTagsList;
			}
		}
		if (!equals) {
			StringBuilder queryBuilder = null;
			int shift = 0;
			Matcher matcher = PATTERN_LONG_QUERY_PART.matcher(searchQuery);
			HashSet<String> tags = new HashSet<>();
			while (matcher.find()) {
				tags.add(matcher.group(1));
				int start = matcher.start(), end = matcher.end();
				int remove = end - start;
				if (queryBuilder == null) {
					queryBuilder = new StringBuilder(searchQuery);
				}
				queryBuilder.delete(start - shift, end - shift);
				shift += remove;
			}
			Collections.addAll(tags, (queryBuilder != null ? queryBuilder.toString() : searchQuery).split(" +"));
			tags.remove("");
			if (tags.size() == 0) {
				return new ReadSearchPostsResult();
			}
			StringBuilder searchTagsBuilder = new StringBuilder();
			ArrayList<String> tagsList = new ArrayList<>();
			for (String tag : tags) {
				Uri uri = locator.buildQuery("ajax", "callback", "", "act", "tagcomplete", "q", tag);
				String responseText = new HttpRequest(uri, data.holder, data).read().getString();
				try {
					JSONObject jsonObject = new JSONObject(responseText.substring(1, responseText.length() - 1));
					JSONArray jsonArray = jsonObject.optJSONArray("tags");
					if (jsonArray == null || jsonArray.length() == 0) {
						if (tagsOnly) {
							throw new HttpException(0, configuration.getResources()
									.getString(R.string.message_tag_not_found_format, tag));
						}
						return new ReadSearchPostsResult();
					}
					if (searchTagsBuilder.length() > 0) {
						searchTagsBuilder.append(',');
					}
					jsonObject = jsonArray.getJSONObject(0);
					searchTagsBuilder.append(CommonUtils.getJsonString(jsonObject, "id"));
					tagsList.add(tag + ": " + CommonUtils.getJsonString(jsonObject, "title"));
				} catch (JSONException e) {
					throw new InvalidResponseException(e);
				}
			}
			synchronized (this) {
				searchTags = searchTagsBuilder.toString();
				searchTagsList = tagsList;
				lastSearchQuery = searchQuery;
				lastSearchTags = searchTags;
				lastSearchTagsList = searchTagsList;
			}
		}
		if (tagsOnly) {
			StringBuilder builder = new StringBuilder();
			builder.append(configuration.getResources().getString(R.string.message_list_of_found_tags)).append(":\n");
			for (String tag : searchTagsList) {
				builder.append('\n').append(tag);
			}
			throw new HttpException(0, builder.toString());
		}
		Uri uri = locator.buildQuery("index/" + (data.pageNumber * PAGE_SIZE), "tags", searchTags);
		String responseText = new HttpRequest(uri, data.holder, data).setSuccessOnly(false).read().getString();
		if (data.holder.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
			return new ReadSearchPostsResult();
		}
		data.holder.checkResponseCode();
		try {
			return new ReadSearchPostsResult(new ArhivachThreadsParser(responseText, this, false).convertPosts());
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		}
	}

	private boolean isBlack(int[] line) {
		for (int color : line) {
			// With noise handling
			if (Color.red(color) > 0x30 || Color.green(color) > 0x30 || Color.blue(color) > 0x30) {
				return false;
			}
		}
		return true;
	}

	@Override
	public ReadContentResult onReadContent(ReadContentData data) throws HttpException, InvalidResponseException {
		// noinspection ConstantConditions
		if ("abload.de".equals(data.uri.getAuthority()) &&
				StringUtils.emptyIfNull(data.uri.getPath()).startsWith("/thumb/")) {
			HttpResponse response = new HttpRequest(data.uri, data.holder, data).read();
			try {
				Thread thread = Thread.currentThread();
				Bitmap bitmap = response.getBitmap();
				if (bitmap != null && bitmap.getWidth() == 132 && bitmap.getHeight() == 147) {
					int top = 1;
					int[] line = new int[130];
					for (int i = 2; i <= 130; i++) {
						bitmap.getPixels(line, 0, 130, 1, i, 130, 1);
						if (isBlack(line)) {
							top = i + 1;
						} else {
							break;
						}
					}
					if (thread.isInterrupted()) {
						return null;
					}
					int bottom = 130;
					for (int i = 130; i >= 1; i--) {
						bitmap.getPixels(line, 0, 130, 1, i, 130, 1);
						if (isBlack(line)) {
							bottom = i - 1;
						} else {
							break;
						}
					}
					if (thread.isInterrupted()) {
						return null;
					}
					int left = 1;
					for (int i = 1; i <= 130; i++) {
						bitmap.getPixels(line, 0, 1, i, 1, 1, 130);
						if (isBlack(line)) {
							left = i + 1;
						} else {
							break;
						}
					}
					if (thread.isInterrupted()) {
						return null;
					}
					int right = 130;
					for (int i = 130; i >= 1; i--) {
						bitmap.getPixels(line, 0, 1, i, 1, 1, 130);
						if (isBlack(line)) {
							right = i - 1;
						} else {
							break;
						}
					}
					if (thread.isInterrupted()) {
						return null;
					}
					top = Math.min(top, 132 - bottom);
					bottom = 132 - top;
					left = Math.min(left, 132 - right);
					right = 132 - left;
					Bitmap newBitmap = Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top);
					bitmap.recycle();
					ByteArrayOutputStream stream = new ByteArrayOutputStream();
					newBitmap.compress(Bitmap.CompressFormat.JPEG, 40, stream);
					newBitmap.recycle();
					return new ReadContentResult(new HttpResponse(stream.toByteArray()));
				}
			} catch (Exception e) {
				// Ignore exception
			}
			return new ReadContentResult(response);
		}
		return super.onReadContent(data);
	}

	private String userEmailPassword;
	private String userCaptchaKey;

	private boolean checkEmailPassword(String email, String password) {
		return email == null && password == null && userEmailPassword == null ||
				(email + " " + password).equals(userEmailPassword);
	}

	@Override
	public CheckAuthorizationResult onCheckAuthorization(CheckAuthorizationData data) throws HttpException,
			InvalidResponseException {
		String email = data.authorizationData[0];
		String password = data.authorizationData[1];
		return new CheckAuthorizationResult(authorizeUser(data.holder, data, email, password));
	}

	private boolean authorizeUser(HttpHolder holder, HttpRequest.Preset preset, String email, String password)
			throws HttpException, InvalidResponseException {
		userEmailPassword = null;
		userCaptchaKey = null;
		ArhivachChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildPath("api", "add");
		JSONObject jsonObject = new HttpRequest(uri, holder, preset).setPostMethod(new UrlEncodedEntity("email", email,
				"pass", password)).read().getJsonObject();
		if (jsonObject == null) {
			throw new InvalidResponseException();
		}
		return updateAuthorizationData(jsonObject, email, password);
	}

	private boolean updateAuthorizationData(JSONObject jsonObject, String email, String password) {
		JSONArray jsonArray = jsonObject.optJSONArray("info_msg");
		if (jsonArray != null) {
			for (int i = 0; i < jsonArray.length(); i++) {
				String message = jsonArray.optString(i);
				if (message != null && message.contains("Вход выполнен")) {
					userCaptchaKey = null;
					userEmailPassword = email + " " + password;
					return true;
				}
			}
		}
		userCaptchaKey = CommonUtils.optJsonString(jsonObject, "captcha_public_key");
		if (userCaptchaKey == null) {
			// Old key
			userCaptchaKey = "6LeJS9cSAAAAAHtATbfTO-L3awJxtICupWftpnbL";
		}
		userEmailPassword = null;
		return false;
	}

	@Override
	public ReadCaptchaResult onReadCaptcha(ReadCaptchaData data) {
		CaptchaData captchaData = new CaptchaData();
		captchaData.put(CaptchaData.API_KEY, userCaptchaKey);
		return new ReadCaptchaResult(CaptchaState.CAPTCHA, captchaData);
	}

	@Override
	public SendAddToArchiveResult onSendAddToArchive(SendAddToArchiveData data) throws HttpException, ApiException,
			InvalidResponseException {
		String[] authorizationData = ChanConfiguration.get(this).getUserAuthorizationData();
		String email = authorizationData[0];
		String password = authorizationData[1];
		if (!checkEmailPassword(email, password)) {
			if (email != null && password != null) {
				authorizeUser(data.holder, data, email, password);
			} else {
				userEmailPassword = null;
				userCaptchaKey = null;
			}
		}
		ArhivachChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildPath("api", "add");
		boolean first = true;
		OUTER: while (true) {
			String captchaChallenge = null;
			String captchaInput = null;
			if (userCaptchaKey != null) {
				CaptchaData captchaData = requireUserCaptcha(null, null, null, !first);
				if (captchaData == null) {
					throw new ApiException(ApiException.ARCHIVE_ERROR_NO_ACCESS);
				}
				captchaChallenge = captchaData.get(CaptchaData.CHALLENGE);
				captchaInput = captchaData.get(CaptchaData.INPUT);
				first = false;
			}
			UrlEncodedEntity entity = new UrlEncodedEntity();
			if (userEmailPassword != null) {
				entity.add("email", email);
				entity.add("pass", password);
			}
			entity.add("thread_url", data.uri.toString());
			entity.add("recaptcha_challenge_field", captchaChallenge);
			entity.add("recaptcha_response_field", captchaInput);
			entity.add("add_collapsed", data.options.contains("collapsed") ? "on" : null);
			entity.add("save_image_bytoken", data.options.contains("bytoken") ? "on" : null);
			JSONObject jsonObject = new HttpRequest(uri, data.holder, data).setPostMethod(entity)
					.read().getJsonObject();
			if (jsonObject == null) {
				throw new InvalidResponseException();
			}
			updateAuthorizationData(jsonObject, email, password);
			JSONArray errorsArray = jsonObject.optJSONArray("errors");
			String errorMessage = null;
			if (errorsArray != null) {
				for (int i = 0; i < errorsArray.length(); i++) {
					String message = errorsArray.optString(i);
					if (message != null) {
						if (message.contains("Ошибка ввода капчи")) {
							if (userCaptchaKey == null) {
								throw new InvalidResponseException();
							}
							continue OUTER;
						} else if (message.contains("Вы достигли лимита")) {
							throw new ApiException(ApiException.ARCHIVE_ERROR_TOO_OFTEN);
						} else if (!message.contains("Неверная пара")) {
							errorMessage = message;
						}
					}
				}
			}
			String threadUriString = CommonUtils.optJsonString(jsonObject, "added_thread_url");
			if (threadUriString != null) {
				uri = Uri.parse(threadUriString);
				String threadNumber = locator.getThreadNumber(uri);
				return new SendAddToArchiveResult(null, threadNumber);
			}
			if (errorMessage != null) {
				throw new ApiException(errorMessage);
			}
			break;
		}
		throw new InvalidResponseException();
	}
}
