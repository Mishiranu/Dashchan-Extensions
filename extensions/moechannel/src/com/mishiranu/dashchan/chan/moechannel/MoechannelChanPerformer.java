package com.mishiranu.dashchan.chan.moechannel;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import chan.content.ApiException;
import chan.content.ChanPerformer;
import chan.content.InvalidResponseException;
import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.http.HttpException;
import chan.http.HttpRequest;
import chan.http.HttpResponse;
import chan.http.MultipartEntity;
import chan.text.JsonSerial;
import chan.text.ParseException;
import chan.util.CommonUtils;
import chan.util.StringUtils;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONException;
import org.json.JSONObject;

public class MoechannelChanPerformer extends ChanPerformer {
	private static final String COOKIE_SESSION = "_ssid";

	private static final String[] PREFERRED_BOARDS_ORDER = {"Обсуждения", "Общее", "Хобби", "ОП модерация"};

	@Override
	public ReadThreadsResult onReadThreads(ReadThreadsData data) throws HttpException, InvalidResponseException {
		MoechannelChanLocator locator = MoechannelChanLocator.get(this);
		Uri uri = locator.buildPath(data.boardName, (data.isCatalog() ? "catalog" : data.pageNumber == 0
				? "index" : Integer.toString(data.pageNumber)) + ".json");
		HttpResponse response = new HttpRequest(uri, data).setValidator(data.validator).perform();
		MoechannelChanConfiguration configuration = MoechannelChanConfiguration.get(this);
		MoechannelModelMapper.BoardConfiguration boardConfiguration = new MoechannelModelMapper.BoardConfiguration();
		ArrayList<Posts> threads = new ArrayList<>();
		int boardSpeed = 0;
		try (InputStream input = response.open();
				JsonSerial.Reader reader = JsonSerial.reader(input)) {
			reader.startObject();
			while (!reader.endStruct()) {
				String name = reader.nextName();
				if (!boardConfiguration.handle(reader, name)) {
					switch (name) {
						case "threads": {
							reader.startArray();
							while (!reader.endStruct()) {
								threads.add(MoechannelModelMapper.createThread(reader, locator));
							}
							break;
						}
						case "board_speed": {
							boardSpeed = reader.nextInt();
							break;
						}
						default: {
							reader.skip();
							break;
						}
					}
				}
			}
			configuration.updateFromThreadsPostsJson(data.boardName, boardConfiguration);
			return new ReadThreadsResult(threads).setBoardSpeed(boardSpeed);
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		} catch (IOException e) {
			throw response.fail(e);
		}
	}

	@SuppressWarnings("SwitchStatementWithTooFewBranches")
	@Override
	public ReadPostsResult onReadPosts(ReadPostsData data) throws HttpException, InvalidResponseException {
		MoechannelChanLocator locator = MoechannelChanLocator.get(this);
		MoechannelChanConfiguration configuration = MoechannelChanConfiguration.get(this);
		Uri uri = locator.buildPath(data.boardName, "res", data.threadNumber + ".json");
		HttpResponse response = new HttpRequest(uri, data).setValidator(data.validator).perform();
		try (InputStream input = response.open();
				JsonSerial.Reader reader = JsonSerial.reader(input)) {
			MoechannelModelMapper.BoardConfiguration boardConfiguration =
					new MoechannelModelMapper.BoardConfiguration();
			ArrayList<Post> posts = null;
			int uniquePosters = 0;
			reader.startObject();
			while (!reader.endStruct()) {
				String name = reader.nextName();
				if (!boardConfiguration.handle(reader, name)) {
					switch (name) {
						case "threads": {
							reader.startArray();
							reader.startObject();
							while (!reader.endStruct()) {
								switch (reader.nextName()) {
									case "posts": {
										posts = MoechannelModelMapper.createPosts(reader, locator, null);
										break;
									}
									default: {
										reader.skip();
										break;
									}
								}
							}
							while (!reader.endStruct()) {
								reader.skip();
							}
							break;
						}
						case "counter_posters": {
							uniquePosters = reader.nextInt();
							break;
						}
						default: {
							reader.skip();
							break;
						}
					}
				}
			}
			configuration.updateFromThreadsPostsJson(data.boardName, boardConfiguration);
			return new ReadPostsResult(new Posts(posts).setUniquePosters(uniquePosters));
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		} catch (IOException e) {
			throw response.fail(e);
		}
	}

	@Override
	public ReadBoardsResult onReadBoards(ReadBoardsData data) throws HttpException, InvalidResponseException {
		MoechannelChanLocator locator = MoechannelChanLocator.get(this);
		MoechannelChanConfiguration configuration = MoechannelChanConfiguration.get(this);
		Uri uri = locator.buildPath("boards.json");
		HttpResponse response = new HttpRequest(uri, data).perform();
		try (InputStream input = response.open();
				JsonSerial.Reader reader = JsonSerial.reader(input)) {
			HashMap<String, ArrayList<Board>> boardsMap = new HashMap<>();
			reader.startArray();
			while (!reader.endStruct()) {
				String category = null;
				String boardName = null;
				String title = null;
				String description = null;
				String defaultName = null;
				Integer bumpLimit = null;
				reader.startObject();
				while (!reader.endStruct()) {
					switch (reader.nextName()) {
						case "group_name": {
							category = reader.nextString();
							break;
						}
						case "board": {
							boardName = reader.nextString();
							break;
						}
						case "board_name": {
							title = reader.nextString();
							break;
						}
						case "board_subtitle": {
							description = reader.nextString();
							break;
						}
						case "default_name": {
							defaultName = reader.nextString();
							break;
						}
						case "bump_limit": {
							bumpLimit = reader.nextInt();
							break;
						}
						default: {
							reader.skip();
							break;
						}
					}
				}
				if (!StringUtils.isEmpty(category) && !StringUtils.isEmpty(boardName) && !StringUtils.isEmpty(title)) {
					ArrayList<Board> boards = boardsMap.get(category);
					if (boards == null) {
						boards = new ArrayList<>();
						boardsMap.put(category, boards);
					}
					description = configuration.transformBoardDescription(description);
					boards.add(new Board(boardName, title, description));
					configuration.updateFromBoardsJson(boardName, defaultName, bumpLimit);
				}
			}
			ArrayList<BoardCategory> boardCategories = new ArrayList<>();
			for (String title : PREFERRED_BOARDS_ORDER) {
				for (HashMap.Entry<String, ArrayList<Board>> entry : boardsMap.entrySet()) {
					if (title.equals(entry.getKey())) {
						ArrayList<Board> boards = entry.getValue();
						Collections.sort(boards);
						boardCategories.add(new BoardCategory(title, boards));
						break;
					}
				}
			}
			return new ReadBoardsResult(boardCategories);
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		} catch (IOException e) {
			throw response.fail(e);
		}
	}

	private static final byte[] CAPTCHA_ERROR_HASH = {-89, 97, 107, -17, -109, 123, 81, -17, 22, -124,
			48, 90, 125, 77, -26, 73, -90, -23, 98, 103};

	private static String createCaptchaChallenge(String id, String session) {
		try {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("id", id);
			jsonObject.put("session", session);
			return jsonObject.toString();
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	private String updateAndGetSession(HttpResponse response, String session) {
		String newSession = response.getCookieValue(COOKIE_SESSION);
		if (newSession != null && !newSession.equals(session)) {
			MoechannelChanConfiguration configuration = MoechannelChanConfiguration.get(this);
			configuration.storeCookie(COOKIE_SESSION, newSession, "Session");
			return newSession;
		}
		return session;
	}

	@Override
	public ReadCaptchaResult onReadCaptcha(ReadCaptchaData data) throws HttpException, InvalidResponseException {
		MoechannelChanLocator locator = MoechannelChanLocator.get(this);
		MoechannelChanConfiguration configuration = MoechannelChanConfiguration.get(this);
		String session = configuration.getCookie(COOKIE_SESSION);
		Uri uri = data.threadNumber != null
				? locator.buildQuery("api/captcha/service_id", "board", data.boardName, "thread", data.threadNumber)
				: locator.buildQuery("api/captcha/service_id", "board", data.boardName);
		HttpResponse response = new HttpRequest(uri, data).addCookie(COOKIE_SESSION, session).perform();
		session = updateAndGetSession(response, session);
		JSONObject jsonObject;
		try {
			jsonObject = new JSONObject(response.readString());
		} catch (JSONException e) {
			throw new InvalidResponseException(e);
		}
		int result = jsonObject.optInt("result");
		if (result == 2) {
			CaptchaData captchaData = new CaptchaData();
			captchaData.put(CaptchaData.CHALLENGE, createCaptchaChallenge(null, session));
			return new ReadCaptchaResult(CaptchaState.SKIP, captchaData);
		} else if (result == 1) {
			String id;
			try {
				id = jsonObject.getString("id");
			} catch (JSONException e) {
				throw new InvalidResponseException(e);
			}
			CaptchaData captchaData = new CaptchaData();
			captchaData.put(CaptchaData.CHALLENGE, createCaptchaChallenge(id, session));
			uri = locator.buildPath("api", "captcha", "image", id);
			response = new HttpRequest(uri, data).addCookie(COOKIE_SESSION, session).perform();
			byte[] bytes = response.readBytes();
			MessageDigest digest;
			try {
				digest = MessageDigest.getInstance("SHA-1");
			} catch (NoSuchAlgorithmException e) {
				throw new RuntimeException(e);
			}
			byte[] sha1sum = digest.digest(bytes);
			if (Arrays.equals(CAPTCHA_ERROR_HASH, sha1sum)) {
				throw new InvalidResponseException();
			}
			Bitmap image = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
			return new ReadCaptchaResult(CaptchaState.CAPTCHA, captchaData).setImage(image);
		} else {
			throw new InvalidResponseException();
		}
	}

	private static final Pattern PATTERN_BAN = Pattern.compile("([^ ]*?): (.*?)(?:\\.|$)");

	private static final SimpleDateFormat DATE_FORMAT_BAN;

	static {
		DATE_FORMAT_BAN = new SimpleDateFormat("d/M/yy HH:mm:ss", Locale.US);
		DATE_FORMAT_BAN.setTimeZone(TimeZone.getTimeZone("GMT+3"));
	}

	@Override
	public SendPostResult onSendPost(SendPostData data) throws HttpException, ApiException, InvalidResponseException {
		MultipartEntity entity = new MultipartEntity();
		entity.add("task", "post");
		entity.add("board", data.boardName);
		entity.add("thread", data.threadNumber != null ? data.threadNumber : "0");
		entity.add("subject", data.subject);
		entity.add("comment", data.comment);
		entity.add("name", data.name);
		entity.add("email", data.email);
		if (data.optionSage) {
			entity.add("sage", "on");
		}
		if (data.optionOriginalPoster) {
			entity.add("opmark", "on");
		}
		if (data.attachments != null) {
			for (int i = 0; i < data.attachments.length; i++) {
				data.attachments[i].addToEntity(entity, "formimages[]");
			}
		}

		String session = null;
		if (data.captchaData != null) {
			String challenge = data.captchaData.get(CaptchaData.CHALLENGE);
			String input = StringUtils.emptyIfNull(data.captchaData.get(CaptchaData.INPUT));
			JSONObject jsonObject;
			String id;
			try {
				jsonObject = new JSONObject(challenge);
				id = StringUtils.nullIfEmpty(jsonObject.optString("id"));
				session = jsonObject.getString("session");
			} catch (JSONException e) {
				throw new RuntimeException(e);
			}
			entity.add("captcha_id", id);
			entity.add("captcha_value", input);
		}

		MoechannelChanLocator locator = MoechannelChanLocator.get(this);
		Uri uri = locator.buildPath("api", "posting");
		HttpResponse response = new HttpRequest(uri, data).setPostMethod(entity).addCookie(COOKIE_SESSION, session)
				.setRedirectHandler(HttpRequest.RedirectHandler.STRICT).perform();
		updateAndGetSession(response, session);
		JSONObject jsonObject;
		try {
			jsonObject = new JSONObject(response.readString());
		} catch (JSONException e) {
			throw new InvalidResponseException(e);
		}
		String postNumber = jsonObject.optString("Num");
		if (!StringUtils.isEmpty(postNumber)) {
			return new SendPostResult(data.threadNumber, postNumber);
		}
		String threadNumber = CommonUtils.optJsonString(jsonObject, "Target");
		if (!StringUtils.isEmpty(threadNumber)) {
			return new SendPostResult(threadNumber, null);
		}

		int error = Math.abs(jsonObject.optInt("Error", Integer.MAX_VALUE));
		String reason = jsonObject.optString("Reason");
		int errorType = 0;
		Object extra = null;
		switch (error) {
			case 2: {
				errorType = ApiException.SEND_ERROR_NO_BOARD;
				break;
			}
			case 3: {
				errorType = ApiException.SEND_ERROR_NO_THREAD;
				break;
			}
			case 4: {
				errorType = ApiException.SEND_ERROR_NO_ACCESS;
				break;
			}
			case 5: {
				errorType = ApiException.SEND_ERROR_CAPTCHA;
				break;
			}
			case 6: {
				errorType = ApiException.SEND_ERROR_BANNED;
				break;
			}
			case 7: {
				errorType = ApiException.SEND_ERROR_CLOSED;
				break;
			}
			case 8: {
				errorType = ApiException.SEND_ERROR_TOO_FAST;
				break;
			}
			case 9: {
				errorType = ApiException.SEND_ERROR_FIELD_TOO_LONG;
				break;
			}
			case 11: {
				errorType = ApiException.SEND_ERROR_FILE_NOT_SUPPORTED;
				break;
			}
			case 13: {
				errorType = ApiException.SEND_ERROR_FILES_TOO_MANY;
				break;
			}
			case 19: {
				errorType = ApiException.SEND_ERROR_EMPTY_FILE;
				break;
			}
			case 20: {
				errorType = ApiException.SEND_ERROR_EMPTY_COMMENT;
				break;
			}
		}
		if (error == 6) {
			ApiException.BanExtra banExtra = new ApiException.BanExtra();
			Matcher matcher = PATTERN_BAN.matcher(reason);
			while (matcher.find()) {
				String name = StringUtils.emptyIfNull(matcher.group(1));
				String value = StringUtils.emptyIfNull(matcher.group(2));
				if ("Бан".equals(name)) {
					banExtra.setId(value);
				} else if ("Причина".equals(name)) {
					banExtra.setMessage(value);
				} else if ("Истекает".equals(name)) {
					try {
						long date = Objects.requireNonNull(DATE_FORMAT_BAN.parse(value)).getTime();
						banExtra.setExpireDate(date);
					} catch (java.text.ParseException e) {
						// Ignore exception
					}
				}
			}
			extra = banExtra;
		}
		if (errorType != 0) {
			throw new ApiException(errorType, extra);
		}
		CommonUtils.writeLog("Moechannel send message", error, reason);
		if (!StringUtils.isEmpty(reason)) {
			throw new ApiException(reason);
		}
		throw new InvalidResponseException();
	}

	@Override
	public SendDeletePostsResult onSendDeletePosts(SendDeletePostsData data)
			throws HttpException, ApiException, InvalidResponseException {
		MoechannelChanLocator locator = MoechannelChanLocator.get(this);
		MoechannelChanConfiguration configuration = MoechannelChanConfiguration.get(this);
		String session = configuration.getCookie(COOKIE_SESSION);
		Uri uri = locator.buildPath("api", "posting");
		MultipartEntity entity = new MultipartEntity("task", "delete", "board", data.boardName,
				"thread", data.threadNumber, "postnum", data.postNumbers.get(0));
		HttpResponse response = new HttpRequest(uri, data).setPostMethod(entity).addCookie(COOKIE_SESSION, session)
				.setRedirectHandler(HttpRequest.RedirectHandler.STRICT).perform();
		updateAndGetSession(response, session);
		JSONObject jsonObject;
		try {
			jsonObject = new JSONObject(response.readString());
			int error = Math.abs(jsonObject.optInt("Error", Integer.MAX_VALUE));
			String reason = jsonObject.optString("Reason");
			if (StringUtils.isEmpty(reason)) {
				return null;
			}
			int errorType = 0;
			switch (error) {
				case 2:
				case 21: {
					errorType = ApiException.DELETE_ERROR_NO_ACCESS;
				}
			}
			if (errorType != 0) {
				throw new ApiException(errorType);
			}
			CommonUtils.writeLog("Moechannel delete message", error, reason);
			throw new ApiException(reason);
		} catch (JSONException e) {
			throw new InvalidResponseException(e);
		}
	}

	@Override
	public SendReportPostsResult onSendReportPosts(SendReportPostsData data) throws HttpException, ApiException,
			InvalidResponseException {
		MoechannelChanLocator locator = MoechannelChanLocator.get(this);
		Uri uri = locator.buildPath("api", "posting");
		StringBuilder postsBuilder = new StringBuilder();
		for (String postNumber : data.postNumbers) {
			postsBuilder.append(postNumber).append(", ");
		}
		MultipartEntity entity = new MultipartEntity("task", "report", "board", data.boardName,
				"thread", data.threadNumber, "posts", postsBuilder.toString(), "comment", data.comment);
		JSONObject jsonObject;
		try {
			jsonObject = new JSONObject(new HttpRequest(uri, data).setPostMethod(entity)
					.setRedirectHandler(HttpRequest.RedirectHandler.STRICT).perform().readString());
			int error = Math.abs(jsonObject.optInt("Error", Integer.MAX_VALUE));
			String reason = jsonObject.optString("Reason");
			if (StringUtils.isEmpty(reason) || "Reported".equals(reason)) {
				return null;
			}
			CommonUtils.writeLog("Moechannel report message", error, reason);
			throw new ApiException(reason);
		} catch (JSONException e) {
			throw new InvalidResponseException(e);
		}
	}
}
