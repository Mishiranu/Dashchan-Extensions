package com.mishiranu.dashchan.chan.cirno;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;
import chan.content.ApiException;
import chan.content.ChanPerformer;
import chan.content.InvalidResponseException;
import chan.content.RedirectException;
import chan.content.model.Post;
import chan.content.model.ThreadSummary;
import chan.http.HttpException;
import chan.http.HttpHolder;
import chan.http.HttpRequest;
import chan.http.MultipartEntity;
import chan.http.UrlEncodedEntity;
import chan.text.ParseException;
import chan.util.CommonUtils;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CirnoChanPerformer extends ChanPerformer {
	private static final Pattern PATTERN_REDIRECT = Pattern.compile("<meta http-equiv=\"Refresh\" " +
			"content=\"0; ?url=(.*?)\" />");

	@Override
	public ReadThreadsResult onReadThreads(ReadThreadsData data) throws HttpException, InvalidResponseException,
			RedirectException {
		CirnoChanLocator locator = CirnoChanLocator.get(this);
		Uri uri = data.isCatalog() ? locator.buildPath(data.boardName, "catalogue.html")
				: locator.createBoardUri(data.boardName, data.pageNumber);
		String responseText = new HttpRequest(uri, data).setValidator(data.validator).read().getString();
		Matcher matcher = PATTERN_REDIRECT.matcher(responseText);
		if (matcher.find()) {
			throw RedirectException.toUri(Uri.parse(matcher.group(1)));
		}
		try {
			return new ReadThreadsResult(data.isCatalog() ? new CirnoCatalogParser(responseText, this).convert()
					: new CirnoPostsParser(responseText, this, data.boardName).convertThreads());
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		}
	}

	private static class ArchiveRedirectHandler implements HttpRequest.RedirectHandler {
		public boolean archived = false;

		@Override
		public Action onRedirectReached(int responseCode, Uri requestedUri, Uri redirectedUri, HttpHolder holder)
				throws HttpException {
			String path = redirectedUri.getPath();
			if (path != null && path.contains("/arch/")) {
				archived = true;
			}
			return BROWSER.onRedirectReached(responseCode, requestedUri, redirectedUri, holder);
		}
	}

	@Override
	public ReadPostsResult onReadPosts(ReadPostsData data) throws HttpException, InvalidResponseException {
		CirnoChanLocator locator = CirnoChanLocator.get(this);
		Uri uri = locator.createThreadUri(data.boardName, data.threadNumber);
		String responseText;
		ArchiveRedirectHandler redirectHandler = new ArchiveRedirectHandler();
		boolean archived;
		try {
			responseText = new HttpRequest(uri, data).setValidator(data.validator)
					.setRedirectHandler(redirectHandler).read().getString();
			archived = redirectHandler.archived;
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
			ArrayList<Post> posts = new CirnoPostsParser(responseText, this, data.boardName).convertPosts();
			if (posts == null || posts.isEmpty()) {
				throw new InvalidResponseException();
			}
			if (archived) {
				posts.get(0).setArchived(true);
			}
			return new ReadPostsResult(posts);
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		}
	}

	@Override
	public ReadBoardsResult onReadBoards(ReadBoardsData data) throws HttpException, InvalidResponseException {
		CirnoChanLocator locator = CirnoChanLocator.get(this);
		Uri uri = locator.buildPath("n", "list_ru_m.html");
		String responseText = new HttpRequest(uri, data).read().getString();
		try {
			return new ReadBoardsResult(new CirnoBoardsParser(responseText).convert());
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		}
	}

	private static final Pattern PATTERN_ARCHIVED_THREAD = Pattern.compile("<a href=\"(\\d+).html\">.*?" +
			"<td align=\"right\">(.{15,}?)</td>");

	@Override
	public ReadThreadSummariesResult onReadThreadSummaries(ReadThreadSummariesData data) throws HttpException {
		CirnoChanLocator locator = CirnoChanLocator.get(this);
		Uri uri = locator.createBoardUri(data.boardName, 0).buildUpon().appendEncodedPath("arch/res").build();
		String responseText = new HttpRequest(uri, data).read().getString();
		ArrayList<ThreadSummary> threadSummaries = new ArrayList<>();
		Matcher matcher = PATTERN_ARCHIVED_THREAD.matcher(responseText);
		while (matcher.find()) {
			threadSummaries.add(new ThreadSummary(data.boardName, matcher.group(1), "#" + matcher.group(1) + ", "
					+ matcher.group(2).trim()));
		}
		return new ReadThreadSummariesResult(threadSummaries);
	}

	@Override
	public ReadPostsCountResult onReadPostsCount(ReadPostsCountData data) throws HttpException,
			InvalidResponseException {
		CirnoChanLocator locator = CirnoChanLocator.get(this);
		Uri uri = locator.createThreadUri(data.boardName, data.threadNumber);
		ArchiveRedirectHandler redirectHandler = new ArchiveRedirectHandler();
		String responseText = new HttpRequest(uri, data).setValidator(data.validator)
				.setRedirectHandler(redirectHandler).read().getString();
		if (redirectHandler.archived) {
			throw HttpException.createNotFoundException();
		}
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

	private final HashSet<String> noCaptchaBoards = new HashSet<>(Arrays
			.asList("mu", "o", "ph", "tv", "vg", "a", "tan", "to"));

	private static final ColorMatrixColorFilter CAPTCHA_FILTER = new ColorMatrixColorFilter(new float[]
			{0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f});

	@Override
	public ReadCaptchaResult onReadCaptcha(ReadCaptchaData data) throws HttpException, InvalidResponseException {
		synchronized (noCaptchaBoards) {
			if (noCaptchaBoards.contains(data.boardName)) {
				return new ReadCaptchaResult(CaptchaState.SKIP, null);
			}
		}
		CirnoChanLocator locator = CirnoChanLocator.get(this);
		String script = "a".equals(data.boardName) || "b".equals(data.boardName) ? "captcha1.pl" : "captcha.pl";
		Uri uri = locator.buildQuery("cgi-bin/" + script + "/" + data.boardName + "/", "key",
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
		entity.add("postredir", "1");
		entity.add("password", data.password);
		if (data.attachments != null) {
			SendPostData.Attachment attachment = data.attachments[0];
			attachment.addToEntity(entity, "file");
			if (attachment.optionSpoiler) {
				entity.add("spoiler", "on");
			}
		} else {
			entity.add("nofile", "1");
		}
		if (data.captchaData != null) {
			entity.add("captcha", data.captchaData.get(CaptchaData.INPUT));
		}

		CirnoChanLocator locator = CirnoChanLocator.get(this);
		Uri uri = locator.buildPath("cgi-bin", "wakaba.pl", data.boardName);
		String responseText;
		try {
			new HttpRequest(uri, data).setPostMethod(entity)
					.setRedirectHandler(HttpRequest.RedirectHandler.NONE).execute();
			if (data.holder.getResponseCode() == HttpURLConnection.HTTP_SEE_OTHER) {
				uri = data.holder.getRedirectedUri();
				String threadNumber = locator.getThreadNumber(uri);
				String postNumber = locator.getPostNumber(uri);
				if (threadNumber.equals(postNumber)) {
					postNumber = null;
				}
				return new SendPostResult(threadNumber, postNumber);
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
				if (errorMessage.contains("Введён неверный код подтверждения") ||
						errorMessage.contains("Код подтверждения не найден в базе")) {
					synchronized (noCaptchaBoards) {
						noCaptchaBoards.remove(data.boardName);
					}
					errorType = ApiException.SEND_ERROR_CAPTCHA;
				} else if (errorMessage.contains("Пустое поле сообщения")) {
					errorType = ApiException.SEND_ERROR_EMPTY_COMMENT;
					flags |= ApiException.FLAG_KEEP_CAPTCHA;
				} else if (errorMessage.contains("Сообщения без изображений запрещены")) {
					errorType = ApiException.SEND_ERROR_EMPTY_FILE;
					flags |= ApiException.FLAG_KEEP_CAPTCHA;
				} else if (errorMessage.contains("Изображение слишком большое")) {
					errorType = ApiException.SEND_ERROR_FILE_TOO_BIG;
					flags |= ApiException.FLAG_KEEP_CAPTCHA;
				} else if (errorMessage.contains("превышает заданный предел")) {
					errorType = ApiException.SEND_ERROR_FIELD_TOO_LONG;
					flags |= ApiException.FLAG_KEEP_CAPTCHA;
				} else if (errorMessage.contains("Этот файл уже был запощен")) {
					errorType = ApiException.SEND_ERROR_FILE_EXISTS;
				} else if (errorMessage.contains("Тред не существует")) {
					errorType = ApiException.SEND_ERROR_NO_THREAD;
				} else if (errorMessage.contains("Флуд")) {
					errorType = ApiException.SEND_ERROR_TOO_FAST;
				} else if (errorMessage.contains("Строка отклонена")) {
					errorType = ApiException.SEND_ERROR_SPAM_LIST;
					flags |= ApiException.FLAG_KEEP_CAPTCHA;
				}
				if (errorType != 0) {
					throw new ApiException(errorType, flags);
				}
			}
			CommonUtils.writeLog("Cirno send message", errorMessage);
			throw new ApiException(errorMessage);
		}
		if (responseText.contains("<h1>Anti-spam filters triggered.</h1>")) {
			throw new ApiException(ApiException.SEND_ERROR_SPAM_LIST, ApiException.FLAG_KEEP_CAPTCHA);
		}
		throw new InvalidResponseException();
	}

	@Override
	public SendDeletePostsResult onSendDeletePosts(SendDeletePostsData data) throws HttpException, ApiException,
			InvalidResponseException {
		CirnoChanLocator locator = CirnoChanLocator.get(this);
		Uri uri = locator.buildPath("cgi-bin", "wakaba.pl", data.boardName);
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
				if (errorMessage.contains("Введён неверный пароль для удаления")) {
					errorType = ApiException.DELETE_ERROR_PASSWORD;
				}
				if (errorType != 0) {
					throw new ApiException(errorType);
				}
			}
			CommonUtils.writeLog("Cirno delete message", errorMessage);
			throw new ApiException(errorMessage);
		}
		throw new InvalidResponseException();
	}
}
