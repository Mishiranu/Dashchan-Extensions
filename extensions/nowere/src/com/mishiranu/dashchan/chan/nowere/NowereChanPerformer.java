package com.mishiranu.dashchan.chan.nowere;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;
import android.util.Pair;
import chan.content.ApiException;
import chan.content.ChanPerformer;
import chan.content.InvalidResponseException;
import chan.content.model.Post;
import chan.content.model.ThreadSummary;
import chan.http.HttpException;
import chan.http.HttpRequest;
import chan.http.MultipartEntity;
import chan.http.UrlEncodedEntity;
import chan.text.ParseException;
import chan.util.CommonUtils;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NowereChanPerformer extends ChanPerformer {
	@Override
	public ReadThreadsResult onReadThreads(ReadThreadsData data) throws HttpException, InvalidResponseException {
		NowereChanLocator locator = NowereChanLocator.get(this);
		Uri uri = locator.createBoardUri(data.boardName, data.pageNumber);
		String responseText = new HttpRequest(uri, data).setValidator(data.validator).read().getString();
		try {
			return new ReadThreadsResult(new NowerePostsParser(responseText, this, data.boardName).convertThreads());
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		}
	}

	@Override
	public ReadPostsResult onReadPosts(ReadPostsData data) throws HttpException, InvalidResponseException {
		NowereChanLocator locator = NowereChanLocator.get(this);
		Uri uri = locator.createThreadUri(data.boardName, data.threadNumber);
		String responseText;
		boolean archived = false;
		try {
			responseText = new HttpRequest(uri, data).setValidator(data.validator).read().getString();
		} catch (HttpException e) {
			if (e.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
				uri = locator.createThreadArchiveUri(data.boardName, data.threadNumber);
				responseText = new HttpRequest(uri, data).setValidator(data.validator).read().getString();
				archived = true;
			} else {
				throw e;
			}
		}
		try {
			ArrayList<Post> posts = new NowerePostsParser(responseText, this, data.boardName).convertPosts();
			if (archived && posts != null && posts.size() > 0) {
				posts.get(0).setArchived(true);
			}
			return new ReadPostsResult(posts);
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		}
	}

	@Override
	public ReadBoardsResult onReadBoards(ReadBoardsData data) throws HttpException, InvalidResponseException {
		NowereChanLocator locator = NowereChanLocator.get(this);
		Uri uri = locator.buildPath("nav.html");
		String responseText = new HttpRequest(uri, data).read().getString();
		try {
			return new ReadBoardsResult(new NowereBoardsParser(responseText).convert());
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		}
	}

	private static final Pattern PATTERN_ARCHIVED_THREAD = Pattern.compile("<a href=\"(\\d+)/\">.*?" +
			"</a> *(.{15,}?)-");

	@SuppressWarnings("ComparatorCombinators")
	private static final Comparator<Pair<Integer, ThreadSummary>> ARCHIVE_COMPARATOR =
			(lhs, rhs) -> lhs.first - rhs.first;

	@Override
	public ReadThreadSummariesResult onReadThreadSummaries(ReadThreadSummariesData data) throws HttpException {
		NowereChanLocator locator = NowereChanLocator.get(this);
		Uri uri = locator.createBoardUri(data.boardName, 0).buildUpon().appendEncodedPath("arch").build();
		String responseText = new HttpRequest(uri, data).read().getString();
		ArrayList<Pair<Integer, ThreadSummary>> threadSummaries = new ArrayList<>();
		Matcher matcher = PATTERN_ARCHIVED_THREAD.matcher(responseText);
		while (matcher.find()) {
			String threadNumber = matcher.group(1);
			threadSummaries.add(new Pair<>(Integer.parseInt(threadNumber), new ThreadSummary(data.boardName,
					threadNumber, "#" + threadNumber + ", " + matcher.group(2).trim())));
		}
		if (threadSummaries.size() > 0) {
			Collections.sort(threadSummaries, ARCHIVE_COMPARATOR);
			ThreadSummary[] threadSummariesArray = new ThreadSummary[threadSummaries.size()];
			for (int i = 0; i < threadSummariesArray.length; i++) {
				threadSummariesArray[i] = threadSummaries.get(i).second;
			}
			return new ReadThreadSummariesResult(threadSummariesArray);
		}
		return null;
	}

	@Override
	public ReadPostsCountResult onReadPostsCount(ReadPostsCountData data) throws HttpException,
			InvalidResponseException {
		NowereChanLocator locator = NowereChanLocator.get(this);
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
		NowereChanLocator locator = NowereChanLocator.get(this);
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
		entity.add("field1", data.name);
		entity.add("field2", data.optionSage ? "sage" : data.email);
		entity.add("field3", data.subject);
		entity.add("field4", data.comment);
		entity.add("password", data.password);
		if (data.attachments != null) {
			SendPostData.Attachment attachment = data.attachments[0];
			attachment.addToEntity(entity, "file");
		} else {
			entity.add("nofile", "on");
		}
		if (data.captchaData != null) {
			entity.add("captcha", data.captchaData.get(CaptchaData.INPUT));
		}

		NowereChanLocator locator = NowereChanLocator.get(this);
		Uri uri = locator.buildPath(data.boardName, "wakaba.pl");
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
			CommonUtils.writeLog("Nowere send message", errorMessage);
			throw new ApiException(errorMessage);
		}
		throw new InvalidResponseException();
	}

	@Override
	public SendDeletePostsResult onSendDeletePosts(SendDeletePostsData data) throws HttpException, ApiException,
			InvalidResponseException {
		NowereChanLocator locator = NowereChanLocator.get(this);
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
			CommonUtils.writeLog("Nowere delete message", errorMessage);
			throw new ApiException(errorMessage);
		}
		throw new InvalidResponseException();
	}
}
