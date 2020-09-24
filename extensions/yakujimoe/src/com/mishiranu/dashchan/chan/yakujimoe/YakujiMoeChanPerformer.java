package com.mishiranu.dashchan.chan.yakujimoe;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;
import chan.content.ApiException;
import chan.content.ChanPerformer;
import chan.content.InvalidResponseException;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.http.HttpException;
import chan.http.HttpRequest;
import chan.http.MultipartEntity;
import chan.http.UrlEncodedEntity;
import chan.text.ParseException;
import chan.util.CommonUtils;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YakujiMoeChanPerformer extends ChanPerformer {
	@Override
	public ReadThreadsResult onReadThreads(ReadThreadsData data) throws HttpException, InvalidResponseException {
		YakujiMoeChanLocator locator = YakujiMoeChanLocator.get(this);
		Uri uri = locator.createBoardUri(data.boardName, data.pageNumber);
		String responseText = new HttpRequest(uri, data).setValidator(data.validator).read().getString();
		try {
			return new ReadThreadsResult(new YakujiMoePostsParser(responseText, this, data.boardName).convertThreads());
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		}
	}

	@Override
	public ReadPostsResult onReadPosts(ReadPostsData data) throws HttpException, InvalidResponseException {
		YakujiMoeChanLocator locator = YakujiMoeChanLocator.get(this);
		Uri uri = locator.createThreadUri(data.boardName, data.threadNumber);
		String responseText = new HttpRequest(uri, data).setValidator(data.validator).read().getString();
		try {
			ArrayList<Post> posts = new YakujiMoePostsParser(responseText, this, data.boardName).convertPosts();
			Posts thread = null;
			if (posts != null && !posts.isEmpty()) {
				thread = new Posts(posts);
				if (!"dev".equals(data.boardName)) {
					thread.setArchivedThreadUri(uri.buildUpon().authority("iichan.hk").build());
				}
			}
			return new ReadPostsResult(thread);
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		}
	}

	@Override
	public ReadBoardsResult onReadBoards(ReadBoardsData data) throws HttpException, InvalidResponseException {
		YakujiMoeChanLocator locator = YakujiMoeChanLocator.get(this);
		Uri uri = locator.buildPath("n", "list.html");
		String responseText = new HttpRequest(uri, data).read().getString();
		try {
			return new ReadBoardsResult(new YakujiMoeBoardsParser(responseText).convert());
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		}
	}

	@Override
	public ReadPostsCountResult onReadPostsCount(ReadPostsCountData data) throws HttpException,
			InvalidResponseException {
		YakujiMoeChanLocator locator = YakujiMoeChanLocator.get(this);
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
		YakujiMoeChanLocator locator = YakujiMoeChanLocator.get(this);
		Uri uri = locator.buildQuery(data.boardName + "/captcha.pl", "key",
				data.threadNumber == null ? "mainpage" : "res" + data.threadNumber);
		Bitmap image = new HttpRequest(uri, data).read().getBitmap();
		if (image != null) {
			Bitmap newImage = Bitmap.createBitmap(image.getWidth(), 32, Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(newImage);
			canvas.drawColor(0xffffffff);
			Paint paint = new Paint();
			paint.setColorFilter(CAPTCHA_FILTER);
			canvas.drawBitmap(image, 0f, (newImage.getHeight() - image.getHeight()) / 2f, paint);
			image.recycle();
			return new ReadCaptchaResult(CaptchaState.CAPTCHA, new CaptchaData()).setImage(newImage);
		}
		throw new InvalidResponseException();
	}

	private static final Pattern PATTERN_POST_ERROR = Pattern.compile("<h1 style=\"text-align: center\">(.*?)<br />");

	@Override
	public SendPostResult onSendPost(SendPostData data) throws HttpException, ApiException, InvalidResponseException {
		MultipartEntity entity = new MultipartEntity();
		entity.add("task", "post");
		entity.add("parent", data.threadNumber);
		entity.add("nya1", data.name);
		entity.add("nya2", data.email);
		entity.add("nya3", data.subject);
		entity.add("nya4", data.comment);
		entity.add("password", data.password);
		if (data.attachments != null) {
			SendPostData.Attachment attachment = data.attachments[0];
			attachment.addToEntity(entity, "file");
		} else {
			entity.add("nofile", "1");
		}
		if (data.captchaData != null) {
			entity.add("captcha", data.captchaData.get(CaptchaData.INPUT));
		}

		YakujiMoeChanLocator locator = YakujiMoeChanLocator.get(this);
		Uri uri = locator.buildPath(data.boardName, "wakaba.pl");
		String responseText;
		try {
			new HttpRequest(uri, data).setPostMethod(entity)
					.setRedirectHandler(HttpRequest.RedirectHandler.NONE).execute();
			if (data.holder.getResponseCode() == HttpURLConnection.HTTP_SEE_OTHER) {
				return null; // Success
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
				} else if (errorMessage.contains("ред не существует")) {
					errorType = ApiException.SEND_ERROR_NO_THREAD;
				} else if (errorMessage.contains("постинг запрещён")) {
					errorType = ApiException.SEND_ERROR_NO_ACCESS;
					flags |= ApiException.FLAG_KEEP_CAPTCHA;
				}
				if (errorType != 0) {
					throw new ApiException(errorType, flags);
				}
			}
			CommonUtils.writeLog("YakujiMoe send message", errorMessage);
			throw new ApiException(errorMessage);
		}
		throw new InvalidResponseException();
	}

	@Override
	public SendDeletePostsResult onSendDeletePosts(SendDeletePostsData data) throws HttpException, ApiException,
			InvalidResponseException {
		YakujiMoeChanLocator locator = YakujiMoeChanLocator.get(this);
		Uri uri = locator.buildPath(data.boardName, "wakaba.pl");
		UrlEncodedEntity entity = new UrlEncodedEntity("task", "delete", "password", data.password);
		for (String postNumber : data.postNumbers) {
			entity.add("delete", postNumber);
		}
		if (data.optionFilesOnly) {
			entity.add("fileonly", "on");
		}
		String responseText;
		try {
			new HttpRequest(uri, data).setPostMethod(entity)
					.setRedirectHandler(HttpRequest.RedirectHandler.NONE).execute();
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
				if (errorMessage.contains("неверный пароль для удаления")) {
					errorType = ApiException.DELETE_ERROR_PASSWORD;
				}
				if (errorType != 0) {
					throw new ApiException(errorType);
				}
			}
			CommonUtils.writeLog("YakujiMoe delete message", errorMessage);
			throw new ApiException(errorMessage);
		}
		throw new InvalidResponseException();
	}
}
