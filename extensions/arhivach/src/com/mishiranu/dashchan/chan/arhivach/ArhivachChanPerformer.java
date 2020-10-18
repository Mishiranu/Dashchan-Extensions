package com.mishiranu.dashchan.chan.arhivach;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.util.Pair;
import chan.content.ApiException;
import chan.content.ChanConfiguration;
import chan.content.ChanLocator;
import chan.content.ChanPerformer;
import chan.content.InvalidResponseException;
import chan.http.HttpException;
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
		String responseText = new HttpRequest(uri, data).setValidator(data.validator).perform().readString();
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
		String responseText = new HttpRequest(uri, data).setValidator(data.validator).perform().readString();
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
				String responseText = new HttpRequest(uri, data).perform().readString();
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
		HttpResponse response = new HttpRequest(uri, data).setSuccessOnly(false).perform();
		if (response.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
			return new ReadSearchPostsResult();
		}
		response.checkResponseCode();
		try {
			return new ReadSearchPostsResult(new ArhivachThreadsParser
					(response.readString(), this, false).convertPosts());
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
		if ("abload.de".equals(data.uri.getAuthority()) &&
				StringUtils.emptyIfNull(data.uri.getPath()).startsWith("/thumb/")) {
			HttpResponse response = new HttpRequest(data.uri, data).perform();
			try {
				Thread thread = Thread.currentThread();
				Bitmap bitmap = response.readBitmap();
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
			} catch (HttpException e) {
				throw e;
			} catch (Exception e) {
				// Ignore exception
			}
			return new ReadContentResult(response);
		}
		return super.onReadContent(data);
	}

	private Pair<String, String> userEmailPassword;

	private boolean checkEmailPassword(String email, String password) {
		return email == null && password == null && userEmailPassword == null ||
				new Pair<>(email, password).equals(userEmailPassword);
	}

	@Override
	public CheckAuthorizationResult onCheckAuthorization(CheckAuthorizationData data) throws HttpException,
			InvalidResponseException {
		String email = data.authorizationData[0];
		String password = data.authorizationData[1];
		return new CheckAuthorizationResult(authorizeUser(data, email, password) != null);
	}

	private Pair<String, String> authorizeUserFromConfiguration(HttpRequest.Preset preset)
			throws HttpException, InvalidResponseException {
		String[] authorizationData = ChanConfiguration.get(this).getUserAuthorizationData();
		String email = authorizationData[0];
		String password = authorizationData[1];
		if (!checkEmailPassword(email, password)) {
			if (email != null && password != null) {
				return authorizeUser(preset, email, password);
			} else {
				userEmailPassword = null;
			}
		}
		return userEmailPassword;
	}

	private Pair<String, String> authorizeUser(HttpRequest.Preset preset, String email,
			String password) throws HttpException, InvalidResponseException {
		userEmailPassword = null;
		ArhivachChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildPath("api", "add");
		try {
			JSONObject jsonObject = new JSONObject(new HttpRequest(uri, preset)
					.setPostMethod(new UrlEncodedEntity("email", email, "pass", password)).perform().readString());
			return updateAuthorizationData(jsonObject, new Pair<>(email, password));
		} catch (JSONException e) {
			throw new InvalidResponseException(e);
		}
	}

	private Pair<String, String> updateAuthorizationData(JSONObject jsonObject, Pair<String, String> emailPassword) {
		JSONArray jsonArray = jsonObject.optJSONArray("info_msg");
		if (jsonArray != null) {
			for (int i = 0; i < jsonArray.length(); i++) {
				String message = jsonArray.optString(i);
				if (message != null && message.contains("Вход выполнен")) {
					this.userEmailPassword = emailPassword;
					return emailPassword;
				}
			}
		}
		userEmailPassword = null;
		return null;
	}

	@Override
	public ReadCaptchaResult onReadCaptcha(ReadCaptchaData data) throws HttpException {
		ArhivachChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildPath("captcha");
		HttpResponse response = new HttpRequest(uri, data).perform();
		CaptchaData captchaData = new CaptchaData();
		captchaData.put(CaptchaData.CHALLENGE, response.getCookieValue("PHPSESSID"));
		return new ReadCaptchaResult(CaptchaState.CAPTCHA, captchaData).setImage(response.readBitmap());
	}

	private static class ArchiveRedirectHandler implements HttpRequest.RedirectHandler {
		private final ArhivachChanLocator locator;
		private String threadNumber;

		public ArchiveRedirectHandler(ArhivachChanLocator locator) {
			this.locator = locator;
		}

		@Override
		public Action onRedirect(HttpResponse response) throws HttpException {
			if (locator.isThreadUri(response.getRedirectedUri())) {
				threadNumber = locator.getThreadNumber(response.getRedirectedUri());
				return Action.CANCEL;
			}
			return STRICT.onRedirect(response);
		}
	}

	@Override
	public SendAddToArchiveResult onSendAddToArchive(SendAddToArchiveData data) throws HttpException, ApiException,
			InvalidResponseException {
		Pair<String, String> userEmailPassword = authorizeUserFromConfiguration(data);
		ArhivachChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildPath("api", "add");
		boolean first = true;
		OUTER: while (true) {
			String captchaChallenge = null;
			String captchaInput = null;
			if (userEmailPassword == null) {
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
				entity.add("email", userEmailPassword.first);
				entity.add("pass", userEmailPassword.second);
			}
			entity.add("thread_url", data.uri.toString());
			entity.add("captcha_code", captchaInput);
			entity.add("add_collapsed", data.options.contains("collapsed") ? "on" : null);
			ArchiveRedirectHandler redirectHandler = new ArchiveRedirectHandler(ArhivachChanLocator.get(this));
			HttpResponse response = new HttpRequest(uri, data).setPostMethod(entity)
					.addCookie("PHPSESSID", captchaChallenge).setRedirectHandler(redirectHandler).perform();
			if (redirectHandler.threadNumber != null) {
				// Api is broken, handle redirect instead
				return new SendAddToArchiveResult(null, redirectHandler.threadNumber);
			}
			JSONObject jsonObject;
			try {
				jsonObject = new JSONObject(response.readString());
			} catch (JSONException e) {
				throw new InvalidResponseException(e);
			}
			updateAuthorizationData(jsonObject, userEmailPassword);
			JSONArray errorsArray = jsonObject.optJSONArray("errors");
			String errorMessage = null;
			if (errorsArray != null) {
				for (int i = 0; i < errorsArray.length(); i++) {
					String message = errorsArray.optString(i);
					if (message != null) {
						if (message.contains("Ошибка ввода капчи")) {
							if (userEmailPassword != null) {
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
