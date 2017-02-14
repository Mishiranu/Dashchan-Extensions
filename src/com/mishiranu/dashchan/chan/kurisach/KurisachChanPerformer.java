package com.mishiranu.dashchan.chan.kurisach;

import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import chan.content.ApiException;
import chan.content.ChanPerformer;
import chan.content.ChanLocator;
import chan.content.InvalidResponseException;
import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.content.model.Posts;
import chan.http.HttpException;
import chan.http.HttpRequest;
import chan.http.MultipartEntity;
import chan.http.SimpleEntity;
import chan.http.UrlEncodedEntity;
import chan.util.CommonUtils;
import chan.util.StringUtils;

public class KurisachChanPerformer extends ChanPerformer {
	@Override
	public ReadThreadsResult onReadThreads(ReadThreadsData data) throws HttpException, InvalidResponseException {
		KurisachChanLocator locator = ChanLocator.get(this);
		int postsPerPage = 15;
		int boardSpeed = -1;
		if ("sg".equals(data.boardName) && data.pageNumber == 0) {
			try {
				Uri uri = locator.buildQuery("api.php", "id", "0", "method", "get_stats", "type", "postslasthour");
				JSONObject jsonObject = new HttpRequest(uri, data.holder, data).setValidator(data.validator)
						.read().getJsonObject();
				if (jsonObject != null) {
					boardSpeed = jsonObject.getJSONObject("result").getInt("result");
				}
			} catch (JSONException e) {
				// Ignore exception
			} catch (HttpException e) {
				if (!e.isHttpException() && !e.isSocketException()) {
					throw e;
				}
			}
		}
		Uri uri = locator.buildQuery("api.php", "id", "0", "method", "get_part_of_board", "board", data.boardName,
				"start", data.isCatalog() ? "0" : Integer.toString(data.pageNumber * postsPerPage),
				"threadnum", Integer.toString(data.isCatalog() ? Integer.MAX_VALUE : postsPerPage),
				"previewnum", data.isCatalog() ? "0" : "5");
		JSONObject jsonObject = new HttpRequest(uri, data.holder, data).setValidator(data.validator)
				.read().getJsonObject();
		if (jsonObject == null) {
			throw new InvalidResponseException();
		}
		try {
			JSONArray jsonArray = jsonObject.getJSONArray("result");
			ArrayList<Posts> threads = new ArrayList<>();
			for (int i = 0; i < jsonArray.length(); i++) {
				threads.add(KurisachModelMapper.createThread(jsonArray.getJSONObject(i), locator, data.boardName));
			}
			return new ReadThreadsResult(threads).setBoardSpeed(boardSpeed);
		} catch (JSONException e) {
			throw new InvalidResponseException(e);
		}
	}

	@Override
	public ReadPostsResult onReadPosts(ReadPostsData data) throws HttpException, InvalidResponseException {
		KurisachChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildQuery("api.php", "id", "0", "method", "get_thread",
				"board", data.boardName, "thread_id", data.threadNumber);
		JSONObject jsonObject = new HttpRequest(uri, data.holder, data).setValidator(data.validator)
				.read().getJsonObject();
		if (jsonObject == null) {
			throw new InvalidResponseException();
		}
		try {
			return new ReadPostsResult(KurisachModelMapper.createPosts(jsonObject.getJSONObject("result"),
					locator, data.boardName));
		} catch (JSONException e) {
			throw new InvalidResponseException(e);
		}
	}

	@Override
	public ReadSinglePostResult onReadSinglePost(ReadSinglePostData data) throws HttpException,
			InvalidResponseException {
		KurisachChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildPath("api.php");
		SimpleEntity entity = new SimpleEntity();
		entity.setContentType("application/json");
		try {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("id", 0);
			jsonObject.put("method", "get_posts_by_id");
			JSONObject paramsObject = new JSONObject();
			paramsObject.put("board", data.boardName);
			JSONArray jsonArray = new JSONArray();
			jsonArray.put(data.postNumber);
			paramsObject.put("ids", jsonArray);
			jsonObject.put("params", paramsObject);
			entity.setData(jsonObject.toString());
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		JSONObject jsonObject = new HttpRequest(uri, data.holder, data).setPostMethod(entity).read().getJsonObject();
		if (jsonObject == null) {
			throw new InvalidResponseException();
		}
		try {
			jsonObject = jsonObject.getJSONObject("result").optJSONObject(data.postNumber);
			if (jsonObject == null) {
				throw HttpException.createNotFoundException();
			}
			return new ReadSinglePostResult(KurisachModelMapper.createPost(jsonObject, locator, data.boardName));
		} catch (JSONException e) {
			throw new InvalidResponseException(e);
		}
	}

	@Override
	public ReadBoardsResult onReadBoards(ReadBoardsData data) throws HttpException, InvalidResponseException {
		KurisachChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildQuery("api.php", "id", "0", "method", "get_boards");
		JSONObject jsonObject = new HttpRequest(uri, data.holder, data).read().getJsonObject();
		if (jsonObject == null) {
			throw new InvalidResponseException();
		}
		try {
			jsonObject = jsonObject.getJSONObject("result");
			ArrayList<Board> boards = new ArrayList<>();
			for (Iterator<String> keys = jsonObject.keys(); keys.hasNext();) {
				String boardName = keys.next();
				String title = CommonUtils.getJsonString(jsonObject.getJSONObject(boardName), "name");
				boards.add(new Board(boardName, title));
			}
			Collections.sort(boards);
			return new ReadBoardsResult(new BoardCategory(null, boards));
		} catch (JSONException e) {
			throw new InvalidResponseException(e);
		}
	}

	@Override
	public ReadPostsCountResult onReadPostsCount(ReadPostsCountData data) throws HttpException,
			InvalidResponseException {
		KurisachChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildQuery("api.php", "id", "0", "method", "get_thread_ids", "board", data.boardName,
				"thread_id", data.threadNumber);
		JSONObject jsonObject = new HttpRequest(uri, data.holder, data).read().getJsonObject();
		if (jsonObject == null) {
			throw new InvalidResponseException();
		}
		try {
			return new ReadPostsCountResult(jsonObject.getJSONArray("result").length());
		} catch (JSONException e) {
			throw new InvalidResponseException(e);
		}
	}

	private static final ColorMatrixColorFilter CAPTCHA_FILTER = new ColorMatrixColorFilter(new float[]
			{0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f, 0f});

	@Override
	public ReadCaptchaResult onReadCaptcha(ReadCaptchaData data) throws HttpException, InvalidResponseException {
		KurisachChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildQuery("api.php", "id", "0", "method", "get_boards");
		JSONObject jsonObject = new HttpRequest(uri, data.holder, data).read().getJsonObject();
		if (jsonObject == null) {
			throw new InvalidResponseException();
		}
		boolean needCaptcha;
		try {
			needCaptcha = jsonObject.getJSONObject("result").getJSONObject(data.boardName)
					.optInt("captchaenabled") != 0;
		} catch (JSONException e) {
			throw new InvalidResponseException(e);
		}
		if (!needCaptcha) {
			return new ReadCaptchaResult(CaptchaState.SKIP, null);
		}
		uri = locator.buildPath("captcha.php");
		String lang;
		if (KurisachChanConfiguration.CAPTCHA_TYPE_INCH_LATIN.equals(data.captchaType)) {
			lang = "en";
		} else if (KurisachChanConfiguration.CAPTCHA_TYPE_INCH_CYRILLIC.equals(data.captchaType)) {
			lang = "ru";
		} else if (KurisachChanConfiguration.CAPTCHA_TYPE_INCH_NUMERIC.equals(data.captchaType)) {
			lang = "num";
		} else {
			throw new InvalidResponseException();
		}
		Bitmap image = new HttpRequest(uri, data.holder, data).addCookie("captchalang", lang).read().getBitmap();
		if (image != null) {
			Bitmap newImage = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(newImage);
			canvas.drawColor(0xffffffff);
			Paint paint = new Paint();
			paint.setColorFilter(CAPTCHA_FILTER);
			canvas.drawBitmap(image, 0f, 0f, paint);
			image.recycle();
			Bitmap trimmedImage = CommonUtils.trimBitmap(newImage, 0xffffffff);
			if (trimmedImage != null && trimmedImage != newImage) {
				newImage.recycle();
				newImage = trimmedImage;
			}
			String sessionCookie = data.holder.getCookieValue("PHPSESSID");
			CaptchaData captchaData = new CaptchaData();
			captchaData.put(CaptchaData.CHALLENGE, sessionCookie);
			return new ReadCaptchaResult(CaptchaState.CAPTCHA, captchaData).setImage(newImage);
		}
		throw new InvalidResponseException();
	}

	private static final HttpRequest.RedirectHandler POST_REDIRECT_HANDLER =
			(responseCode, requestedUri, redirectedUri, holder) -> responseCode == HttpURLConnection.HTTP_MOVED_PERM
			? HttpRequest.RedirectHandler.Action.RETRANSMIT : HttpRequest.RedirectHandler.Action.CANCEL;

	private static final Pattern PATTERN_POST_ERROR = Pattern.compile("(?s)<h2.*?>(.*?)</h2>");
	private static final Pattern PATTERN_BLACK_LIST_WORD = Pattern.compile("Blacklisted link \\( (.*) \\) detected.");
	private static final Pattern PATTERN_BAN_DATA = Pattern.compile("<strong>(.*?)</strong>");

	private static final SimpleDateFormat DATE_FORMAT_BAN = new SimpleDateFormat("MMMM d, yyyy, KK:mm a", Locale.US);

	static {
		DATE_FORMAT_BAN.setTimeZone(TimeZone.getTimeZone("GMT+3"));
	}

	@Override
	public SendPostResult onSendPost(SendPostData data) throws HttpException, ApiException, InvalidResponseException {
		MultipartEntity entity = new MultipartEntity();
		entity.add("board", data.boardName);
		entity.add("replythread", data.threadNumber == null ? "0" : data.threadNumber);
		entity.add("name", data.name);
		entity.add("em", data.optionSage ? "sage" : data.email);
		entity.add("subject", data.subject);
		entity.add("message", StringUtils.emptyIfNull(data.comment));
		entity.add("postpassword", data.password);
		entity.add("redirecttothread", "1");
		entity.add("embed", ""); // Otherwise there will be a "Please enter an embed ID" error
		if (data.attachments != null) {
			data.attachments[0].addToEntity(entity, "imagefile");
			if (data.attachments[0].optionSpoiler) {
				entity.add("picspoiler", "1");
			}
		}
		String sessionCookie = null;
		if (data.captchaData != null) {
			entity.add("captcha", data.captchaData.get(CaptchaData.INPUT));
			sessionCookie = data.captchaData.get(CaptchaData.CHALLENGE);
		}

		KurisachChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildPath("board.php");
		String responseText;
		try {
			new HttpRequest(uri, data.holder, data).setPostMethod(entity).addCookie("PHPSESSID", sessionCookie)
					.setRedirectHandler(POST_REDIRECT_HANDLER).execute();
			if (data.holder.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP) {
				uri = data.holder.getRedirectedUri();
				String threadNumber = locator.getThreadNumber(uri);
				return new SendPostResult(threadNumber, null);
			}
			responseText = data.holder.read().getString();
		} finally {
			data.holder.disconnect();
		}

		Matcher matcher = PATTERN_POST_ERROR.matcher(responseText);
		if (matcher.find()) {
			String errorMessage = matcher.group(1).trim();
			int errorType = 0;
			Object extra = null;
			if (errorMessage.contains("Капча введена неверно") || errorMessage.contains("Капча протухла")) {
				errorType = ApiException.SEND_ERROR_CAPTCHA;
			} else if (errorMessage.contains("Для ответа нужна картинка, видео или сообщение")) {
				errorType = ApiException.SEND_ERROR_EMPTY_COMMENT;
			} else if (errorMessage.contains("A file is required for a new thread") ||
					errorMessage.contains("Please enter an embed ID")) {
				errorType = ApiException.SEND_ERROR_EMPTY_FILE;
			} else if (errorMessage.contains("Неверный ID треда")) {
				errorType = ApiException.SEND_ERROR_NO_THREAD;
			} else if (errorMessage.contains("Sorry, your message is too long")) {
				errorType = ApiException.SEND_ERROR_FIELD_TOO_LONG;
			} else if (errorMessage.contains("Flood Detected")) {
				errorType = ApiException.SEND_ERROR_TOO_FAST;
			} else if (errorMessage.contains("Убедитесь, что Ваш файл меньше")) {
				errorType = ApiException.SEND_ERROR_FILE_TOO_BIG;
			} else if (errorMessage.contains("Обнаружен дублирующий файл") ||
					errorMessage.contains("Duplicate file entry detected")) {
				errorType = ApiException.SEND_ERROR_FILE_EXISTS;
			} else if (errorMessage.contains("Этот тред закрыт")) {
				errorType = ApiException.SEND_ERROR_CLOSED;
			} else if (errorMessage.contains("Эта доска закрыта")) {
				errorType = ApiException.SEND_ERROR_NO_ACCESS;
			} else if (errorMessage.contains("Вы забанены")) {
				errorType = ApiException.SEND_ERROR_BANNED;
				matcher = PATTERN_BAN_DATA.matcher(responseText);
				ApiException.BanExtra banExtra = new ApiException.BanExtra();
				boolean startDateFound = false;
				while (matcher.find()) {
					String group = matcher.group(1);
					boolean parseSuccess = false;
					long date = 0;
					try {
						date = DATE_FORMAT_BAN.parse(group).getTime();
						parseSuccess = true;
					} catch (java.text.ParseException e) {
						// Ignore exception
					}
					if (parseSuccess || startDateFound) {
						if (startDateFound) {
							if (parseSuccess) {
								banExtra.setExpireDate(date);
							} else if (group.contains("не истечет")) {
								banExtra.setExpireDate(Long.MAX_VALUE);
							}
							extra = banExtra;
							break;
						} else {
							banExtra.setStartDate(date);
							startDateFound = true;
						}
					} else {
						banExtra.setMessage(group);
					}
				}
			} else if (errorMessage.contains("Blacklisted link")) {
				errorType = ApiException.SEND_ERROR_SPAM_LIST;
				matcher = PATTERN_BLACK_LIST_WORD.matcher(errorMessage);
				if (matcher.matches()) {
					extra = new ApiException.WordsExtra().addWord(matcher.group(1));
				}
			}
			if (errorType != 0) {
				throw new ApiException(errorType, extra);
			}
			CommonUtils.writeLog("Kurisach send message", errorMessage);
			throw new ApiException(errorMessage);
		}
		throw new InvalidResponseException();
	}

	@Override
	public SendDeletePostsResult onSendDeletePosts(SendDeletePostsData data) throws HttpException, ApiException,
			InvalidResponseException {
		KurisachChanLocator locator = ChanLocator.get(this);
		UrlEncodedEntity entity = new UrlEncodedEntity("board", data.boardName, "deletepost", "1",
				"postpassword", data.password);
		for (String postNumber : data.postNumbers) {
			entity.add("post[]", postNumber);
		}
		if (data.optionFilesOnly) {
			entity.add("fileonly", "on");
		}
		Uri uri = locator.buildPath("board.php");
		String responseText = new HttpRequest(uri, data.holder, data).setPostMethod(entity).read().getString();
		if (responseText != null) {
			if (responseText.contains("Пост удален") || responseText.contains("Изображение успешно удалено") ||
					responseText.contains("Ваш пост не имеет изображения") ||
					responseText.contains("<meta http-equiv=\"refresh\"")) {
				// Response has message for any post
				// Ignore them, if at least 1 of them was deleted
				return null;
			} else if (responseText.contains("Неверный пароль")) {
				throw new ApiException(ApiException.DELETE_ERROR_PASSWORD);
			}
			CommonUtils.writeLog("Kurisach delete message", responseText);
		}
		throw new InvalidResponseException();
	}

	@Override
	public SendReportPostsResult onSendReportPosts(SendReportPostsData data) throws HttpException, ApiException,
			InvalidResponseException {
		KurisachChanLocator locator = ChanLocator.get(this);
		UrlEncodedEntity entity = new UrlEncodedEntity("board", data.boardName, "reportpost", "1",
				"reportreason", data.comment);
		for (String postNumber : data.postNumbers) {
			entity.add("post[]", postNumber);
		}
		Uri uri = locator.buildPath("board.php");
		String responseText = new HttpRequest(uri, data.holder, data).setPostMethod(entity).read().getString();
		if (responseText != null) {
			if (responseText.contains("Post successfully reported") ||
					responseText.contains("Этот пост уже находится в листе ожидания") ||
					responseText.contains("Этот пост очищен как не нуждающийся в удалении")) {
				// Response has message for any post
				// Ignore them, if at least 1 of them was reported
				return null;
			}
			CommonUtils.writeLog("Kurisach report message", responseText);
		}
		throw new InvalidResponseException();
	}
}