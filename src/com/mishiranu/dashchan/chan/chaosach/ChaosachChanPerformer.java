package com.mishiranu.dashchan.chan.chaosach;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.net.Uri;

import chan.content.ApiException;
import chan.content.ChanPerformer;
import chan.content.InvalidResponseException;
import chan.content.model.Post;
import chan.http.CookieBuilder;
import chan.http.HttpException;
import chan.http.HttpHolder;
import chan.http.HttpRequest;
import chan.http.MultipartEntity;
import chan.text.ParseException;
import chan.util.CommonUtils;

public class ChaosachChanPerformer extends ChanPerformer {
	private static final String COOKIE_SESSION = "_SESSION";

	private void handleCookie(HttpHolder holder) {
		String userCookie = holder.getCookieValue(COOKIE_SESSION);
		if (userCookie != null) {
			ChaosachChanConfiguration configuration = ChaosachChanConfiguration.get(this);
			configuration.storeCookie(COOKIE_SESSION, userCookie, "Session");
		}
	}

	private CookieBuilder buildCookies() {
		ChaosachChanConfiguration configuration = ChaosachChanConfiguration.get(this);
		String userCookie = configuration.getCookie(COOKIE_SESSION);
		if (userCookie != null) {
			return new CookieBuilder().append(COOKIE_SESSION, userCookie);
		}
		return null;
	}

	@Override
	public ReadThreadsResult onReadThreads(ReadThreadsData data) throws HttpException, InvalidResponseException {
		if ("feed".equals(data.boardName)) {
			throw HttpException.createNotFoundException();
		}
		ChaosachChanLocator locator = ChaosachChanLocator.get(this);
		Uri uri = data.isCatalog() ? locator.buildPath("catalog", data.boardName)
				: locator.createBoardUri(data.boardName, data.pageNumber);
		String responseText = new HttpRequest(uri, data).setValidator(data.validator)
				.addCookie(buildCookies()).read().getString();
		handleCookie(data.holder);
		try {
			return new ReadThreadsResult(data.isCatalog() ? new ChaosachCatalogParser(responseText, this).convert()
					: new ChaosachPostsParser(responseText, this, data.boardName).convertThreads());
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		}
	}

	@Override
	public ReadPostsResult onReadPosts(ReadPostsData data) throws HttpException, InvalidResponseException {
		String lastPostNumber = data.partialThreadLoading ? data.lastPostNumber : null;
		ChaosachChanLocator locator = ChaosachChanLocator.get(this);
		Uri uri = lastPostNumber == null ? locator.createThreadUri(data.boardName, data.threadNumber)
				: locator.buildPath("ajax", "thread", data.boardName, data.threadNumber, "new", lastPostNumber);
		String responseText = new HttpRequest(uri, data).setValidator(data.validator)
				.addCookie(buildCookies()).read().getString();
		handleCookie(data.holder);
		if (responseText.startsWith("No such thread")) {
			throw HttpException.createNotFoundException();
		}
		try {
			return new ReadPostsResult(new ChaosachPostsParser(responseText, this, data.boardName).convertPosts());
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		}
	}

	@Override
	public ReadSinglePostResult onReadSinglePost(ReadSinglePostData data) throws HttpException,
			InvalidResponseException {
		ChaosachChanLocator locator = ChaosachChanLocator.get(this);
		Uri uri = locator.buildPath("ajax", "post", data.boardName, data.postNumber);
		String responseText = new HttpRequest(uri, data).addCookie(buildCookies()).read().getString();
		handleCookie(data.holder);
		try {
			Post post = new ChaosachPostsParser(responseText, this, data.boardName).convertSinglePost();
			if (post == null) {
				throw new InvalidResponseException();
			}
			return new ReadSinglePostResult(post);
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		}
	}

	@Override
	public ReadBoardsResult onReadBoards(ReadBoardsData data) throws HttpException, InvalidResponseException {
		ChaosachChanLocator locator = ChaosachChanLocator.get(this);
		Uri uri = locator.buildPath();
		String responseText = new HttpRequest(uri, data).addCookie(buildCookies()).read().getString();
		handleCookie(data.holder);
		try {
			return new ReadBoardsResult(new ChaosachBoardsParser(responseText).convert());
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		}
	}

	@Override
	public ReadPostsCountResult onReadPostsCount(ReadPostsCountData data) throws HttpException,
			InvalidResponseException {
		ChaosachChanLocator locator = ChaosachChanLocator.get(this);
		Uri uri = locator.createThreadUri(data.boardName, data.threadNumber);
		String responseText = new HttpRequest(uri, data).setValidator(data.validator)
				.addCookie(buildCookies()).read().getString();
		handleCookie(data.holder);
		if (!responseText.contains("<form class=\"plain-post-form\" id=\"post-form\"")) {
			throw new InvalidResponseException();
		}
		int count = 0;
		int index = 0;
		while (index != -1) {
			count++;
			index = responseText.indexOf("<div class=\"post reply\"", index + 1);
		}
		return new ReadPostsCountResult(count);
	}

	private static final Pattern PATTERN_TOKEN = Pattern.compile("name=\"_token\" value=\"(.*?)\"");

	private static final String CAPTCHA_TOKEN = "token";

	@Override
	public ReadCaptchaResult onReadCaptcha(ReadCaptchaData data) throws HttpException, InvalidResponseException {
		ChaosachChanLocator locator = ChaosachChanLocator.get(this);
		Uri uri = locator.buildPath(data.boardName);
		String responseText = new HttpRequest(uri, data).addCookie(buildCookies()).read().getString();
		handleCookie(data.holder);
		Matcher matcher = PATTERN_TOKEN.matcher(responseText);
		if (!matcher.find()) {
			throw new InvalidResponseException();
		}
		String token = matcher.group(1);
		CaptchaData captchaData = new CaptchaData();
		captchaData.put(CAPTCHA_TOKEN, token);
		if (!responseText.contains("<input id=\"hident6\" name=\"f5\"")) {
			return new ReadCaptchaResult(CaptchaState.SKIP, captchaData);
		}
		uri = locator.buildPath("captcha");
		responseText = new HttpRequest(uri, data).addCookie(buildCookies()).read().getString();
		handleCookie(data.holder);
		int start = responseText.indexOf("/static/");
		int end = responseText.indexOf('"', start + 1);
		if (end < start || start < 0) {
			throw new InvalidResponseException();
		}
		uri = locator.buildPath(responseText.substring(start, end));
		Bitmap image = new HttpRequest(uri, data).addCookie(buildCookies()).read().getBitmap();
		handleCookie(data.holder);
		if (image != null) {
			return new ReadCaptchaResult(CaptchaState.CAPTCHA, captchaData).setImage(image);
		}
		throw new InvalidResponseException();
	}

	private static class ManualRedirectHandler implements HttpRequest.RedirectHandler {
		public Uri redirectedUri;

		@Override
		public Action onRedirectReached(int responseCode, Uri requestedUri, Uri redirectedUri, HttpHolder holder)
				throws HttpException {
			if (responseCode == HttpURLConnection.HTTP_SEE_OTHER) {
				this.redirectedUri = redirectedUri;
				return HttpRequest.RedirectHandler.Action.CANCEL;
			}
			return HttpRequest.RedirectHandler.STRICT.onRedirectReached(responseCode,
					requestedUri, redirectedUri, holder);
		}
	}

	private static final Pattern PATTERN_SEND_ERROR = Pattern.compile("<div id=\"message\">(.*?)</div>");

	@Override
	public SendPostResult onSendPost(SendPostData data) throws HttpException, ApiException, InvalidResponseException {
		MultipartEntity entity = new MultipartEntity();
		entity.add("f1", data.name);
		entity.add("f2", data.subject);
		entity.add("f3", data.comment);
		entity.add("f4", data.password);
		entity.add("f6", "1"); // Redirect to thread
		if (data.optionSage) {
			entity.add("f7", "yes");
		}
		if (data.attachments != null) {
			int maxFilesCount = 3;
			for (int i = 0; i < data.attachments.length; i++) {
				SendPostData.Attachment attachment = data.attachments[i];
				attachment.addToEntity(entity, "f" + (8 + i));
				entity.add("f" + (8 + maxFilesCount + i), attachment.rating);
			}
		}
		if (data.captchaData != null) {
			entity.add("_token", data.captchaData.get(CAPTCHA_TOKEN));
			entity.add("f5", data.captchaData.get(CaptchaData.INPUT));
		}

		ChaosachChanLocator locator = ChaosachChanLocator.get(this);
		Uri uri = locator.buildPath(data.boardName, data.threadNumber);
		ManualRedirectHandler handler = new ManualRedirectHandler();
		new HttpRequest(uri, data).setPostMethod(entity).addCookie(buildCookies())
				.addHeader("X-Requested-With", "XMLHttpRequest").setRedirectHandler(handler).read();
		handleCookie(data.holder);
		if (handler.redirectedUri == null) {
			throw new InvalidResponseException();
		}
		if (data.threadNumber == null && locator.isThreadUri(handler.redirectedUri)) {
			return new SendPostResult(locator.getThreadNumber(handler.redirectedUri), null);
		}
		String responseText = new HttpRequest(handler.redirectedUri, data.holder).addCookie(buildCookies())
				.read().getString();
		handleCookie(data.holder);
		String errorMessage;
		try {
			JSONObject jsonObject = new JSONObject(responseText);
			if (CommonUtils.optJsonString(jsonObject, "ok") != null) {
				return null; // Success
			} else {
				errorMessage = CommonUtils.optJsonString(jsonObject, "error");
				if (errorMessage == null) {
					throw new InvalidResponseException();
				}
			}
		} catch (JSONException e) {
			Matcher matcher = PATTERN_SEND_ERROR.matcher(responseText);
			if (!matcher.find()) {
				throw new InvalidResponseException();
			} else {
				errorMessage = matcher.group(1);
			}
		}

		int errorType = 0;
		if (errorMessage.contains("Wrong captcha")) {
			errorType = ApiException.SEND_ERROR_CAPTCHA;
		} else if (errorMessage.contains("Thread subject is required")) {
			errorType = ApiException.SEND_ERROR_EMPTY_SUBJECT;
		} else if (errorMessage.contains("File required")) {
			errorType = ApiException.SEND_ERROR_EMPTY_FILE;
		} else if (errorMessage.contains("File or message is required")) {
			errorType = ApiException.SEND_ERROR_EMPTY_COMMENT;
		}
		if (errorType != 0) {
			throw new ApiException(errorType);
		}
		CommonUtils.writeLog("Chaosach send message", errorMessage);
		throw new ApiException(errorMessage);
	}

	private static final Pattern PATTERN_CHECKBOX = Pattern.compile("<input .*?name=\"postdelete\" value=\"(\\d+)\">");

	@Override
	public SendDeletePostsResult onSendDeletePosts(SendDeletePostsData data) throws HttpException, ApiException,
			InvalidResponseException {
		ChaosachChanLocator locator = ChaosachChanLocator.get(this);
		ArrayList<String> alternation = new ArrayList<>();
		for (String postNumber : data.postNumbers) {
			Uri uri = locator.buildPath("ajax", "post", data.boardName, postNumber);
			String responseText = new HttpRequest(uri, data).addCookie(buildCookies()).read().getString();
			handleCookie(data.holder);
			Matcher matcher = PATTERN_CHECKBOX.matcher(responseText);
			if (!matcher.find()) {
				throw new InvalidResponseException();
			}
			alternation.add("postdelete");
			alternation.add(matcher.group(1));
		}
		if (data.optionFilesOnly) {
			alternation.add("onlyfiles");
			alternation.add("1");
		}
		alternation.add("postpassword");
		alternation.add(data.password);
		Uri uri = locator.buildQuery("delete", alternation.toArray(new String[alternation.size()]));
		ManualRedirectHandler handler = new ManualRedirectHandler();
		new HttpRequest(uri, data).addCookie(buildCookies()).setRedirectHandler(handler).read();
		handleCookie(data.holder);
		if (handler.redirectedUri == null) {
			throw new InvalidResponseException();
		}
		String responseText = new HttpRequest(handler.redirectedUri, data.holder).addCookie(buildCookies())
				.read().getString();
		handleCookie(data.holder);
		Matcher matcher = PATTERN_SEND_ERROR.matcher(responseText);
		if (matcher.find()) {
			String errorMessage = matcher.group(1);
			int errorType = 0;
			if (errorMessage.contains("Wrong password")) {
				errorType = ApiException.DELETE_ERROR_PASSWORD;
			}
			if (errorType != 0) {
				throw new ApiException(errorType);
			}
			CommonUtils.writeLog("Chaosach delete message", errorMessage);
			throw new ApiException(errorMessage);
		} else {
			return null;
		}
	}
}