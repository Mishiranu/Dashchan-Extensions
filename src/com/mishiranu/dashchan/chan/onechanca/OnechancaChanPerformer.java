package com.mishiranu.dashchan.chan.onechanca;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;

import chan.content.ApiException;
import chan.content.ChanPerformer;
import chan.content.InvalidResponseException;
import chan.content.RedirectException;
import chan.content.model.Post;
import chan.http.HttpException;
import chan.http.HttpRequest;
import chan.http.MultipartEntity;
import chan.http.UrlEncodedEntity;
import chan.text.ParseException;
import chan.util.CommonUtils;
import chan.util.StringUtils;

public class OnechancaChanPerformer extends ChanPerformer {
	@Override
	public ReadThreadsResult onReadThreads(ReadThreadsData data) throws HttpException, InvalidResponseException {
		OnechancaChanLocator locator = OnechancaChanLocator.get(this);
		Uri uri = locator.createBoardUri(data.boardName, data.pageNumber);
		String responseText = new HttpRequest(uri, data.holder, data).setValidator(data.validator)
				.read().getString();
		try {
			return new ReadThreadsResult(new OnechancaPostsParser(responseText, this).convertThreads());
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		}
	}

	@Override
	public ReadPostsResult onReadPosts(ReadPostsData data) throws HttpException, InvalidResponseException,
			RedirectException {
		if (data.boardName.startsWith("news-")) {
			throw RedirectException.toThread("news", data.threadNumber, null);
		}
		OnechancaChanLocator locator = OnechancaChanLocator.get(this);
		Uri uri = locator.createThreadUri(data.boardName, data.threadNumber);
		String responseText = new HttpRequest(uri, data.holder, data).setValidator(data.validator).read().getString();
		try {
			ArrayList<Post> posts = new OnechancaPostsParser(responseText, this).convertPosts();
			if (posts == null || posts.isEmpty()) {
				throw HttpException.createNotFoundException();
			}
			return new ReadPostsResult(posts);
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		}
	}

	@Override
	public ReadBoardsResult onReadBoards(ReadBoardsData data) throws HttpException, InvalidResponseException {
		OnechancaChanLocator locator = OnechancaChanLocator.get(this);
		Uri uri = locator.buildPath("news", "cat", "");
		String responseText = new HttpRequest(uri, data.holder, data).read().getString();
		try {
			return new ReadBoardsResult(new OnechancaBoardsParser(responseText, this).convert());
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		}
	}

	@Override
	public ReadPostsCountResult onReadPostsCount(ReadPostsCountData data) throws HttpException,
			InvalidResponseException {
		OnechancaChanLocator locator = OnechancaChanLocator.get(this);
		Uri uri = locator.createThreadUri(data.boardName, data.threadNumber);
		String responseText = new HttpRequest(uri, data.holder, data).setValidator(data.validator).read().getString();
		if (!responseText.contains("<div class=\"b-blog-entry\"")) {
			throw new InvalidResponseException();
		}
		int count = 0;
		int index = 0;
		while (index != -1) {
			count++;
			index = responseText.indexOf("<div class=\"b-comment\"", index + 1);
		}
		return new ReadPostsCountResult(count);
	}

	@Override
	public ReadCaptchaResult onReadCaptcha(ReadCaptchaData data) throws HttpException, InvalidResponseException {
		OnechancaChanLocator locator = OnechancaChanLocator.get(this);
		boolean news = data.boardName.startsWith("news");
		Uri uri;
		String key;
		if (news) {
			if (data.threadNumber == null) {
				uri = locator.buildPath("news", "add", "");
				key = "post";
			} else {
				uri = locator.buildPath("news", "res", data.threadNumber, "");
				key = "comment";
			}
		} else {
			if (data.threadNumber == null) {
				uri = locator.buildPath(data.boardName, "");
				key = "board";
			} else {
				uri = locator.buildPath(data.boardName, "res", data.threadNumber, "");
				key = "board_comment";
			}
		}
		new HttpRequest(uri, data.holder, data).read();
		String sessionCookie = data.holder.getCookieValue("PHPSESSID");
		if (StringUtils.isEmpty(sessionCookie)) {
			throw new InvalidResponseException();
		}
		uri = locator.buildQuery("captcha/", "key", key);
		Bitmap image = new HttpRequest(uri, data.holder, data).addCookie("PHPSESSID", sessionCookie).read().getBitmap();
		if (image != null) {
			int color = 0xffffffff;
			int sum = 0xff + 0xff + 0xff;
			int[] pixels = new int[image.getWidth()];
			for (int y = 0; y < image.getHeight(); y++) {
				image.getPixels(pixels, 0, pixels.length, 0, y, pixels.length, 1);
				for (int x = 0; x < pixels.length; x++) {
					int itColor = pixels[x];
					int r = Color.red(itColor);
					int g = Color.green(itColor);
					int b = Color.blue(itColor);
					int itSum = r + g + b;
					if (itSum < sum) {
						color = itColor;
						sum = itSum;
					}
				}
			}
			if (color != 0xff) {
				Bitmap mutable = image.copy(image.getConfig(), true);
				image.recycle();
				image = mutable;
				for (int y = 0; y < image.getHeight(); y++) {
					image.getPixels(pixels, 0, pixels.length, 0, y, pixels.length, 1);
					for (int x = 0; x < pixels.length; x++) {
						int itColor = pixels[x];
						int a = Color.alpha(itColor);
						int r = Color.red(itColor);
						int g = Color.green(itColor);
						int b = Color.blue(itColor);
						int itSum = r + g + b;
						int c = 0xff * (itSum - sum) / (3 * 0xff - sum);
						pixels[x] = Color.argb(a, c, c, c);
					}
					image.setPixels(pixels, 0, pixels.length, 0, y, pixels.length, 1);
				}
			}
			CaptchaData captchaData = new CaptchaData();
			captchaData.put(CaptchaData.CHALLENGE, sessionCookie);
			return new ReadCaptchaResult(CaptchaState.CAPTCHA, captchaData).setImage(image);
		}
		throw new InvalidResponseException();
	}

	private static final Pattern PATTERN_POST = Pattern.compile("<script type=\"text/javascript\">"
			+ "top\\.(?:board|comment)_callback\\((.*?)\\);</script>");
	private static final Pattern PATTERN_NEWS_POST = Pattern.compile("<div.*?id=\"blog_form_error\">(.*?)</div>");
	private static final Pattern PATTERN_NEWS_REPLY = Pattern.compile("<em id=\"comment_form_error\">(.*?)</em>");

	private static final HttpRequest.RedirectHandler POST_REDIRECT_HANDLER =
			(responseCode, requestedUri, redirectedUri, holder) -> {
		if (responseCode == HttpURLConnection.HTTP_SEE_OTHER) {
			return HttpRequest.RedirectHandler.Action.CANCEL;
		}
		return HttpRequest.RedirectHandler.STRICT.onRedirectReached(responseCode, requestedUri, redirectedUri, holder);
	};

	@Override
	public SendPostResult onSendPost(SendPostData data) throws HttpException, ApiException, InvalidResponseException {
		String sessionCookie = null;
		String captchaInput = null;
		if (data.captchaData != null) {
			sessionCookie = data.captchaData.get(CaptchaData.CHALLENGE);
			captchaInput = data.captchaData.get(CaptchaData.INPUT);
		}
		OnechancaChanLocator locator = OnechancaChanLocator.get(this);
		if (data.boardName.startsWith("news")) {
			if (data.threadNumber == null) {
				boolean hidden = data.boardName.equals("news-hidden");
				String category = data.boardName.equals("news") || data.boardName.equals("news-all") || hidden
						? null : data.boardName.substring(5);
				if (category != null) {
					Uri uri = locator.buildPath("news", "cat", "");
					JSONArray jsonArray = new HttpRequest(uri, data.holder, data)
							.addHeader("X-Requested-With", "XMLHttpRequest").read().getJsonArray();
					String title = null;
					if (jsonArray == null) {
						throw new InvalidResponseException();
					}
					try {
						for (int i = 0; i < jsonArray.length(); i++) {
							JSONObject jsonObject = jsonArray.getJSONObject(i);
							String value = CommonUtils.getJsonString(jsonObject, "value");
							if (category.equals(value)) {
								title = CommonUtils.getJsonString(jsonObject, "title");
								break;
							}
						}
					} catch (JSONException e) {
						throw new InvalidResponseException(e);
					}
					if (title == null) {
						throw new ApiException(ApiException.SEND_ERROR_NO_ACCESS);
					}
					category = title + " <" + category + ">";
				}
				String link = null;
				String comment = StringUtils.emptyIfNull(data.comment);
				if (comment.startsWith("http://") || comment.startsWith("https://")) {
					int index = comment.indexOf('\n');
					if (index >= 0) {
						link = comment.substring(0, index);
						if (comment.length() > index + 1 && comment.charAt(index + 1) == '\n') {
							index++;
						}
						comment = comment.substring(index + 1);
					} else {
						link = comment;
						comment = "";
					}
				}
				String commentFull = "";
				int minLength = 15;
				int maxLength = 1024;
				if (comment.length() > maxLength) {
					commentFull = comment;
					int index = comment.lastIndexOf('\n', maxLength);
					if (index >= minLength) {
						comment = comment.substring(0, index);
					} else {
						comment = comment.substring(0, maxLength - 1) + "…";
					}
				}
				UrlEncodedEntity entity = new UrlEncodedEntity("category", category, "title", data.subject,
						"link", link, "text", comment, "text_full", commentFull, "captcha_key", "post",
						"captcha", captchaInput);
				if (hidden) {
					entity.add("vip", "on");
				}
				Uri uri = locator.buildPath("news", "add", "");
				String responseText = new HttpRequest(uri, data.holder, data).setPostMethod(entity)
						.addCookie("PHPSESSID", sessionCookie).setRedirectHandler(POST_REDIRECT_HANDLER)
						.read().getString();
				if (data.holder.getResponseCode() == HttpURLConnection.HTTP_SEE_OTHER) {
					uri = data.holder.getRedirectedUri();
					return new SendPostResult(locator.getThreadNumber(uri), null);
				}
				Matcher matcher = PATTERN_NEWS_POST.matcher(responseText);
				if (matcher.find()) {
					String message = matcher.group(1);
					if (message.contains("Заголовок слишком короткий")) {
						throw new ApiException(ApiException.SEND_ERROR_EMPTY_SUBJECT);
					} else if (message.contains("Не введен вводный текст")
							|| message.contains("Вводный текст слишком короткий")) {
						throw new ApiException(ApiException.SEND_ERROR_EMPTY_COMMENT);
					} else if (message.contains("Капча введена неверно")) {
						throw new ApiException(ApiException.SEND_ERROR_CAPTCHA);
					} else if (message.contains("Таймаут")) {
						throw new ApiException(ApiException.SEND_ERROR_TOO_FAST);
					}
					CommonUtils.writeLog("Onechanca send message", message);
					throw new ApiException(message);
				} else {
					throw new InvalidResponseException();
				}
			} else {
				UrlEncodedEntity entity = new UrlEncodedEntity("post_id", data.threadNumber, "text",
						StringUtils.emptyIfNull(data.comment), "captcha_key", "comment", "captcha", captchaInput);
				Uri uri = locator.buildPath("news", "res", data.threadNumber, "add_comment", "");
				String responseText = new HttpRequest(uri, data.holder, data).setPostMethod(entity)
						.addCookie("PHPSESSID", sessionCookie).setRedirectHandler(HttpRequest.RedirectHandler.STRICT)
						.read().getString();
				Matcher matcher = PATTERN_NEWS_REPLY.matcher(responseText);
				if (matcher.find()) {
					String message = matcher.group(1);
					if (message.isEmpty()) {
						return new SendPostResult(data.threadNumber, null);
					} else if (message.contains("Не введен текст комментария")) {
						throw new ApiException(ApiException.SEND_ERROR_EMPTY_COMMENT);
					} else if (message.contains("Капча введена неверно")) {
						throw new ApiException(ApiException.SEND_ERROR_CAPTCHA);
					}
					CommonUtils.writeLog("Onechanca send message", message);
					throw new ApiException(message);
				} else {
					throw new InvalidResponseException();
				}
			}
		} else {
			MultipartEntity entity = new MultipartEntity();
			entity.add("parent_id", data.threadNumber);
			entity.add("title", data.subject);
			entity.add("text", StringUtils.emptyIfNull(data.comment));
			entity.add("password", data.password);
			if (data.attachments != null) {
				data.attachments[0].addToEntity(entity, "upload");
			}
			entity.add("captcha", captchaInput);
			Uri uri = locator.buildPath(data.boardName, data.threadNumber != null
					? "createPostAjaxForm" : "createAjaxForm", "");
			String responseText = new HttpRequest(uri, data.holder, data).setPostMethod(entity)
					.addCookie("PHPSESSID", sessionCookie).setRedirectHandler(HttpRequest.RedirectHandler.STRICT)
					.setSuccessOnly(false).read().getString();
			if (data.holder.getResponseCode() == HttpURLConnection.HTTP_ENTITY_TOO_LARGE) {
				throw new ApiException(ApiException.SEND_ERROR_FILE_TOO_BIG);
			}
			data.holder.checkResponseCode();
			Matcher matcher = PATTERN_POST.matcher(responseText);
			if (matcher.find()) {
				try {
					JSONObject jsonObject = new JSONObject(matcher.group(1));
					if (jsonObject.optBoolean("success") || jsonObject.optBoolean("sucess")) {
						String id = CommonUtils.getJsonString(jsonObject, "id");
						return new SendPostResult(data.threadNumber != null ? data.threadNumber : id,
								data.threadNumber != null ? id : null);
					}
					jsonObject = jsonObject.getJSONObject("errors");
					HashSet<String> errors = new HashSet<>();
					for (Iterator<String> it = jsonObject.keys(); it.hasNext();) {
						errors.add(it.next());
					}
					if (errors.contains("text")) {
						throw new ApiException(ApiException.SEND_ERROR_EMPTY_COMMENT);
					} else if (errors.contains("captcha")) {
						throw new ApiException(ApiException.SEND_ERROR_CAPTCHA);
					}
					String message = jsonObject.get(errors.iterator().next()).toString();
					CommonUtils.writeLog("Onechanca send message", message);
					throw new ApiException(message);
				} catch (JSONException e) {
					throw new InvalidResponseException(e);
				}
			} else {
				throw new InvalidResponseException();
			}
		}
	}

	@Override
	public SendDeletePostsResult onSendDeletePosts(SendDeletePostsData data) throws HttpException, ApiException,
			InvalidResponseException {
		if (!data.boardName.startsWith("news") && data.postNumbers.get(0).equals(data.threadNumber)) {
			OnechancaChanLocator locator = OnechancaChanLocator.get(this);
			Uri uri = locator.buildQuery(data.boardName + "/remove/", "id", data.postNumbers.get(0),
					"password", data.password);
			String responseText = new HttpRequest(uri, data.holder, data)
					.addHeader("X-Requested-With", "XMLHttpRequest").read().getString();
			if ("true".equals(responseText)) {
				return new SendDeletePostsResult();
			}
			if ("false".equals(responseText)) {
				throw new ApiException(ApiException.DELETE_ERROR_PASSWORD);
			}
			CommonUtils.writeLog("Onechan delete message", responseText);
			throw new InvalidResponseException();
		} else {
			throw new ApiException(ApiException.DELETE_ERROR_NO_ACCESS);
		}
	}
}