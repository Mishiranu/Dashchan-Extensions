package com.mishiranu.dashchan.chan.bunbunmaru;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;
import chan.content.ApiException;
import chan.content.ChanPerformer;
import chan.content.InvalidResponseException;
import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.http.HttpException;
import chan.http.HttpRequest;
import chan.http.MultipartEntity;
import chan.http.UrlEncodedEntity;
import chan.text.ParseException;
import chan.util.CommonUtils;
import java.net.HttpURLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BunbunmaruChanPerformer extends ChanPerformer {
	@Override
	public ReadThreadsResult onReadThreads(ReadThreadsData data) throws HttpException, InvalidResponseException {
		BunbunmaruChanLocator locator = BunbunmaruChanLocator.get(this);
		Uri uri = locator.createBoardUri(data.boardName, data.pageNumber);
		String responseText = new HttpRequest(uri, data).setValidator(data.validator).read().getString();
		try {
			return new ReadThreadsResult(new BunbunmaruPostsParser(responseText, this, data.boardName)
					.convertThreads());
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		}
	}

	@Override
	public ReadPostsResult onReadPosts(ReadPostsData data) throws HttpException, InvalidResponseException {
		BunbunmaruChanLocator locator = BunbunmaruChanLocator.get(this);
		Uri uri = locator.createThreadUri(data.boardName, data.threadNumber);
		String responseText = new HttpRequest(uri, data).setValidator(data.validator).read().getString();
		try {
			return new ReadPostsResult(new BunbunmaruPostsParser(responseText, this, data.boardName).convertPosts());
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		}
	}

	@Override
	public ReadBoardsResult onReadBoards(ReadBoardsData data) throws HttpException, InvalidResponseException {
		return new ReadBoardsResult(new BoardCategory(null, new Board[] {new Board("general", "Shameimaru General"),
				new Board("photos", "Mysterious Photograph Collection")}));
	}

	@Override
	public ReadPostsCountResult onReadPostsCount(ReadPostsCountData data) throws HttpException,
			InvalidResponseException {
		BunbunmaruChanLocator locator = BunbunmaruChanLocator.get(this);
		Uri uri = locator.createThreadUri(data.boardName, data.threadNumber);
		String responseText = new HttpRequest(uri, data).setValidator(data.validator).read().getString();
		if (!responseText.contains("<form id=\"delform\"")) {
			throw new InvalidResponseException();
		}
		int count = 0;
		int index = 0;
		while (index != -1) {
			count++;
			index = responseText.indexOf("<td class=\"reply\"", index + 1);
		}
		return new ReadPostsCountResult(count);
	}

	private static final ColorMatrixColorFilter CAPTCHA_FILTER = new ColorMatrixColorFilter(new float[]
			{0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f});

	@Override
	public ReadCaptchaResult onReadCaptcha(ReadCaptchaData data) throws HttpException, InvalidResponseException {
		BunbunmaruChanLocator locator = BunbunmaruChanLocator.get(this);
		Uri uri = locator.buildQuery("wakaba/" + data.boardName + "/captcha.pl", "key",
				data.threadNumber == null ? "mainpage" : "res" + data.threadNumber);
		Bitmap image = new HttpRequest(uri, data).read().getBitmap();
		if (image != null) {
			Bitmap newImage = Bitmap.createBitmap(image.getWidth(), 32, Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(newImage);
			canvas.drawColor(0xffffffff);
			Paint paint = new Paint();
			paint.setColorFilter(CAPTCHA_FILTER);
			canvas.drawBitmap(image, 0f, (newImage.getHeight() - image.getHeight()) / 2, paint);
			image.recycle();
			return new ReadCaptchaResult(CaptchaState.CAPTCHA, new CaptchaData()).setImage(newImage);
		}
		throw new InvalidResponseException();
	}

	private static final HttpRequest.RedirectHandler POST_REDIRECT_HANDLER =
			(responseCode, requestedUri, redirectedUri, holder) -> {
		if (responseCode == HttpURLConnection.HTTP_SEE_OTHER) {
			return HttpRequest.RedirectHandler.Action.CANCEL;
		}
		return HttpRequest.RedirectHandler.STRICT.onRedirectReached(responseCode, requestedUri, redirectedUri, holder);
	};

	private static final Pattern PATTERN_POST_ERROR = Pattern.compile("<h1 style=\"text-align: center\">(.*?)<br />");

	@Override
	public SendPostResult onSendPost(SendPostData data) throws HttpException, ApiException, InvalidResponseException {
		MultipartEntity entity = new MultipartEntity();
		entity.add("task", "post");
		entity.add("parent", data.threadNumber);
		entity.add("field2", data.email);
		entity.add("field3", data.subject);
		entity.add("field4", data.comment);
		entity.add("password", data.password);
		if (data.attachments != null) {
			SendPostData.Attachment attachment = data.attachments[0];
			attachment.addToEntity(entity, "file");
			if (attachment.optionSpoiler) {
				entity.add("spoiler", "on");
			}
		} else {
			entity.add("nofile", "on");
		}
		if (data.captchaData != null) {
			entity.add("captcha", data.captchaData.get(CaptchaData.INPUT));
		}

		BunbunmaruChanLocator locator = BunbunmaruChanLocator.get(this);
		Uri uri = locator.buildPath("wakaba", data.boardName, "wakaba.pl");
		String responseText;
		try {
			new HttpRequest(uri, data).setPostMethod(entity).setRedirectHandler(POST_REDIRECT_HANDLER).execute();
			if (data.holder.getResponseCode() == HttpURLConnection.HTTP_SEE_OTHER) {
				return null;
			}
			responseText = data.holder.read().getString();
		} finally {
			data.holder.disconnect();
		}

		Matcher matcher = PATTERN_POST_ERROR.matcher(responseText);
		if (matcher.find()) {
			String errorMessage = matcher.group(1);
			if (errorMessage != null) {
				int errorType = 0;
				int flags = 0;
				if (errorMessage.contains("Wrong verification code entered") ||
						errorMessage.contains("No verification code on record")) {
					errorType = ApiException.SEND_ERROR_CAPTCHA;
				} else if (errorMessage.contains("No comment entered")) {
					errorType = ApiException.SEND_ERROR_EMPTY_COMMENT;
					flags |= ApiException.FLAG_KEEP_CAPTCHA;
				} else if (errorMessage.contains("No file selected")) {
					errorType = ApiException.SEND_ERROR_EMPTY_FILE;
					flags |= ApiException.FLAG_KEEP_CAPTCHA;
				} else if (errorMessage.contains("This image is too large")) {
					errorType = ApiException.SEND_ERROR_FILE_TOO_BIG;
					flags |= ApiException.FLAG_KEEP_CAPTCHA;
				} else if (errorMessage.contains("Too many characters")) {
					errorType = ApiException.SEND_ERROR_FIELD_TOO_LONG;
					flags |= ApiException.FLAG_KEEP_CAPTCHA;
				} else if (errorMessage.contains("Thread does not exist")) {
					errorType = ApiException.SEND_ERROR_NO_THREAD;
				} else if (errorMessage.contains("String refused") || errorMessage.contains("Flood detected, ")) {
					errorType = ApiException.SEND_ERROR_SPAM_LIST;
					flags |= ApiException.FLAG_KEEP_CAPTCHA;
				} else if (errorMessage.contains("Host is banned")) {
					errorType = ApiException.SEND_ERROR_BANNED;
				} else if (errorMessage.contains("Flood detected")) {
					errorType = ApiException.SEND_ERROR_TOO_FAST;
				}
				if (errorType != 0) {
					throw new ApiException(errorType, flags);
				}
			}
			CommonUtils.writeLog("Bunbunmaru send message", errorMessage);
			throw new ApiException(errorMessage);
		}
		throw new InvalidResponseException();
	}

	@Override
	public SendDeletePostsResult onSendDeletePosts(SendDeletePostsData data) throws HttpException, ApiException,
			InvalidResponseException {
		BunbunmaruChanLocator locator = BunbunmaruChanLocator.get(this);
		Uri uri = locator.buildPath("wakaba", data.boardName, "wakaba.pl");
		UrlEncodedEntity entity = new UrlEncodedEntity("task", "delete", "password", data.password);
		for (String postNumber : data.postNumbers) {
			entity.add("delete", postNumber);
		}
		if (data.optionFilesOnly) {
			entity.add("fileonly", "on");
		}
		String responseText;
		try {
			new HttpRequest(uri, data).setPostMethod(entity).setRedirectHandler(POST_REDIRECT_HANDLER).execute();
			if (data.holder.getResponseCode() == HttpURLConnection.HTTP_SEE_OTHER) {
				return null;
			}
			responseText = data.holder.read().getString();
		} finally {
			data.holder.disconnect();
		}
		Matcher matcher = PATTERN_POST_ERROR.matcher(responseText);
		if (matcher.find()) {
			String errorMessage = matcher.group(1);
			if (errorMessage != null) {
				int errorType = 0;
				if (errorMessage.contains("Incorrect password for deletion")) {
					errorType = ApiException.DELETE_ERROR_PASSWORD;
				}
				if (errorType != 0) {
					throw new ApiException(errorType);
				}
			}
			CommonUtils.writeLog("Bunbunmaru delete message", errorMessage);
			throw new ApiException(errorMessage);
		}
		throw new InvalidResponseException();
	}
}
