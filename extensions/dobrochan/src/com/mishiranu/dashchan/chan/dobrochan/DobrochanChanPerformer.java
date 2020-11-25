package com.mishiranu.dashchan.chan.dobrochan;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;
import chan.content.ApiException;
import chan.content.ChanConfiguration;
import chan.content.ChanLocator;
import chan.content.ChanPerformer;
import chan.content.InvalidResponseException;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.http.CookieBuilder;
import chan.http.HttpException;
import chan.http.HttpRequest;
import chan.http.HttpResponse;
import chan.http.MultipartEntity;
import chan.http.UrlEncodedEntity;
import chan.text.JsonSerial;
import chan.text.ParseException;
import chan.util.CommonUtils;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DobrochanChanPerformer extends ChanPerformer {
	private static final int DELAY = 1000;
	private static final String COOKIE_HANABIRA = "hanabira";
	private static final String COOKIE_HANABIRA_TEMP = "hanabira_temp";

	private synchronized HttpResponse performRepeatable(HttpRequest request) throws HttpException {
		request.addCookie(buildCookies());
		HttpException exception = null;
		for (int i = 0; i < 5; i++) {
			try {
				if (i == 1) {
					request.setDelay(DELAY);
				}
				return request.perform();
			} catch (HttpException e) {
				exception = e;
				if (!e.isHttpException() || e.getResponseCode() != HttpURLConnection.HTTP_UNAVAILABLE) {
					break;
				}
			}
		}
		throw exception;
	}

	private CookieBuilder buildCookies(CaptchaData captchaData) {
		DobrochanChanConfiguration configuration = ChanConfiguration.get(this);
		String hanabira = configuration.getCookie(COOKIE_HANABIRA);
		if (hanabira == null && captchaData != null) {
			hanabira = captchaData.get(COOKIE_HANABIRA);
		}
		String hanabiraTemp = configuration.getCookie(COOKIE_HANABIRA_TEMP);
		if (hanabiraTemp == null && captchaData != null) {
			hanabiraTemp = captchaData.get(COOKIE_HANABIRA_TEMP);
		}
		return new CookieBuilder().append(COOKIE_HANABIRA, hanabira).append(COOKIE_HANABIRA_TEMP, hanabiraTemp);
	}

	private CookieBuilder buildCookies() {
		return buildCookies(null);
	}

	@SuppressWarnings("SwitchStatementWithTooFewBranches")
	@Override
	public ReadThreadsResult onReadThreads(ReadThreadsData data) throws HttpException, InvalidResponseException {
		DobrochanChanLocator locator = ChanLocator.get(this);
		DobrochanChanConfiguration configuration = ChanConfiguration.get(this);
		Uri uri = locator.buildPath(data.boardName, data.pageNumber + ".json");
		HttpResponse response = performRepeatable(new HttpRequest(uri, data).setValidator(data.validator));
		try (InputStream input = response.open();
				JsonSerial.Reader reader = JsonSerial.reader(input)) {
			ArrayList<Posts> threads = new ArrayList<>();
			reader.startObject();
			while (!reader.endStruct()) {
				switch (reader.nextName()) {
					case "boards": {
						reader.startObject();
						while (!reader.endStruct()) {
							if (reader.nextName().equals(data.boardName)) {
								reader.startObject();
								while (!reader.endStruct()) {
									switch (reader.nextName()) {
										case "threads": {
											reader.startArray();
											while (!reader.endStruct()) {
												threads.add(DobrochanModelMapper.createThread(reader, locator));
											}
											break;
										}
										case "pages": {
											int pagesCount = reader.nextInt();
											configuration.storePagesCount(data.boardName, pagesCount);
											break;
										}
										default: {
											reader.skip();
											break;
										}
									}
								}
							} else {
								reader.skip();
							}
						}
						break;
					}
					default: {
						reader.skip();
						break;
					}
				}
			}
			return new ReadThreadsResult(threads);
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		} catch (IOException e) {
			throw response.fail(e);
		}
	}

	@SuppressWarnings({"SwitchStatementWithTooFewBranches", "StatementWithEmptyBody"})
	@Override
	public ReadPostsResult onReadPosts(ReadPostsData data) throws HttpException, InvalidResponseException {
		DobrochanChanLocator locator = ChanLocator.get(this);
		DobrochanChanConfiguration configuration = ChanConfiguration.get(this);
		Uri uri;
		if (data.partialThreadLoading && data.lastPostNumber != null) {
			uri = locator.createApiUri("thread", data.boardName, data.threadNumber + "/new.json",
					"last_post", data.lastPostNumber, "new_format", "1", "message_html", "1", "board", "1");
		} else {
			uri = locator.createApiUri("thread", data.boardName, data.threadNumber + "/all.json",
					"new_format", "1", "message_html", "1", "board", "1");
		}
		HttpResponse response = performRepeatable(new HttpRequest(uri, data).setValidator(data.validator));
		try (InputStream input = response.open();
				JsonSerial.Reader reader = JsonSerial.reader(input)) {
			DobrochanModelMapper.BoardConfiguration boardConfiguration = new DobrochanModelMapper.BoardConfiguration();
			List<Post> posts = null;
			reader.startObject();
			while (!reader.endStruct()) {
				switch (reader.nextName()) {
					case "error": {
						handleMobileApiError(reader);
						break;
					}
					case "result": {
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
													posts = DobrochanModelMapper.createPosts(reader,
															locator, data.threadNumber);
													break;
												}
												default: {
													reader.skip();
													break;
												}
											}
										}
										while (!reader.endStruct()) {}
										break;
									}
									default: {
										reader.skip();
									}
								}
							}
						}
						break;
					}
					default: {
						reader.skip();
						break;
					}
				}
			}
			configuration.updateFromPostsJson(data.boardName, boardConfiguration);
			if (posts == null) {
				return null;
			}
			return new ReadPostsResult(posts);
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		} catch (IOException e) {
			throw response.fail(e);
		}
	}

	@SuppressWarnings({"SwitchStatementWithTooFewBranches", "StatementWithEmptyBody"})
	@Override
	public ReadSinglePostResult onReadSinglePost(ReadSinglePostData data) throws HttpException,
			InvalidResponseException {
		DobrochanChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.createApiUri("post", data.boardName, data.postNumber + ".json",
				"new_format", "1", "message_html", "1", "thread", "1");
		HttpResponse response = performRepeatable(new HttpRequest(uri, data));
		try (InputStream input = response.open();
				JsonSerial.Reader reader = JsonSerial.reader(input)) {
			String threadNumber = null;
			Post post = null;
			reader.startObject();
			while (!reader.endStruct()) {
				switch (reader.nextName()) {
					case "error": {
						handleMobileApiError(reader);
						break;
					}
					case "result": {
						reader.startObject();
						while (!reader.endStruct()) {
							switch (reader.nextName()) {
								case "threads": {
									reader.startArray();
									reader.startObject();
									while (!reader.endStruct()) {
										switch (reader.nextName()) {
											case "display_id": {
												threadNumber = reader.nextString();
												break;
											}
											case "posts": {
												reader.startArray();
												post = DobrochanModelMapper.createPost(reader, locator);
												while (!reader.endStruct()) {}
												break;
											}
											default: {
												reader.skip();
												break;
											}
										}
									}
									while (!reader.endStruct()) {}
									break;
								}
								default: {
									reader.skip();
								}
							}
						}
						break;
					}
					default: {
						reader.skip();
						break;
					}
				}
			}
			if (post == null) {
				throw new InvalidResponseException();
			}
			post.setParentPostNumber(threadNumber);
			return new ReadSinglePostResult(post);
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		} catch (IOException e) {
			throw response.fail(e);
		}
	}

	@Override
	public ReadBoardsResult onReadBoards(ReadBoardsData data) throws HttpException, InvalidResponseException {
		DobrochanChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildPath("frame.xhtml");
		HttpResponse response = new HttpRequest(uri, data).perform();
		try (InputStream input = response.open()) {
			return new ReadBoardsResult(new DobrochanBoardsParser().convert(input));
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		} catch (IOException e) {
			throw response.fail(e);
		}
	}

	@SuppressWarnings("SwitchStatementWithTooFewBranches")
	@Override
	public ReadPostsCountResult onReadPostsCount(ReadPostsCountData data) throws HttpException,
			InvalidResponseException {
		DobrochanChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.createApiUri("thread", data.boardName, data.threadNumber + "/last.json",
				"count", "0", "new_format", "1");
		HttpResponse response = performRepeatable(new HttpRequest(uri, data).setValidator(data.validator));
		try (InputStream input = response.open();
				JsonSerial.Reader reader = JsonSerial.reader(input)) {
			int postCount = 0;
			reader.startObject();
			while (!reader.endStruct()) {
				switch (reader.nextName()) {
					case "error": {
						handleMobileApiError(reader);
						break;
					}
					case "result": {
						reader.startObject();
						while (!reader.endStruct()) {
							switch (reader.nextName()) {
								case "posts_count": {
									postCount = reader.nextInt();
									break;
								}
								default: {
									reader.skip();
									break;
								}
							}
						}
						break;
					}
					default: {
						reader.skip();
						break;
					}
				}
			}
			return new ReadPostsCountResult(postCount);
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		} catch (IOException e) {
			throw response.fail(e);
		}
	}

	@SuppressWarnings("SwitchStatementWithTooFewBranches")
	private void handleMobileApiError(JsonSerial.Reader reader) throws IOException, ParseException,
			HttpException, InvalidResponseException {
		reader.startObject();
		while (!reader.endStruct()) {
			switch (reader.nextName()) {
				case "message": {
					String message = reader.nextString();
					if ("Specified element does not exist.".equals(message) || "Post is deleted.".equals(message)) {
						throw HttpException.createNotFoundException();
					} else {
						throw new HttpException(0, message);
					}
				}
			}
		}
		throw new InvalidResponseException();
	}

	private static final ColorMatrixColorFilter CAPTCHA_FILTER = new ColorMatrixColorFilter(new float[]
			{0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f});

	private final HashSet<String> forceCaptcha = new HashSet<>();

	private boolean isForceCaptcha(String boardName, String threadNumber) {
		return forceCaptcha.contains(boardName + "," + threadNumber);
	}

	private void setForceCaptcha(String boardName, String threadNumber, boolean forceCaptcha) {
		String key = boardName + "," + threadNumber;
		if (forceCaptcha) {
			this.forceCaptcha.add(key);
		} else {
			this.forceCaptcha.remove(key);
		}
	}

	@SuppressWarnings("SwitchStatementWithTooFewBranches")
	@Override
	public ReadCaptchaResult onReadCaptcha(ReadCaptchaData data) throws HttpException, InvalidResponseException {
		DobrochanChanLocator locator = ChanLocator.get(this);
		DobrochanChanConfiguration configuration = ChanConfiguration.get(this);
		if (!configuration.isAlwaysLoadCaptcha()) {
			if (!isForceCaptcha(data.boardName, data.threadNumber)) {
				Uri uri = locator.buildPath("api", "user.json");
				HttpResponse response = performRepeatable(new HttpRequest(uri, data));
				try (InputStream input = response.open();
						JsonSerial.Reader reader = JsonSerial.reader(input)) {
					boolean noCaptcha = false;
					reader.startObject();
					while (!reader.endStruct()) {
						switch (reader.nextName()) {
							case "tokens": {
								reader.startArray();
								while (!reader.endStruct()) {
									reader.startObject();
									while (!reader.endStruct()) {
										switch (reader.nextName()) {
											case "token": {
												String token = reader.nextString();
												noCaptcha |= "no_user_captcha".equals(token);
												break;
											}
											default: {
												reader.skip();
												break;
											}
										}
									}
								}
								break;
							}
							default: {
								reader.skip();
							}
						}
					}
					if (noCaptcha) {
						return new ReadCaptchaResult(CaptchaState.SKIP, new CaptchaData());
					}
				} catch (ParseException e) {
					throw new InvalidResponseException(e);
				} catch (IOException e) {
					throw response.fail(e);
				}
			} else {
				setForceCaptcha(data.boardName, data.threadNumber, false);
			}
		}
		Uri uri = locator.buildPath("captcha", data.boardName, System.currentTimeMillis() + ".png");
		HttpResponse response = performRepeatable(new HttpRequest(uri, data));
		Bitmap image = response.readBitmap();
		Bitmap trimmed = CommonUtils.trimBitmap(image, 0xffffffff);
		if (trimmed == null) {
			trimmed = image;
		}
		Bitmap newImage = Bitmap.createBitmap(trimmed.getWidth(), 32, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(newImage);
		int shift = (newImage.getHeight() - trimmed.getHeight()) / 2;
		canvas.drawColor(0xffffffff);
		Paint paint = new Paint();
		paint.setColorFilter(CAPTCHA_FILTER);
		canvas.drawBitmap(trimmed, 0, shift, paint);
		if (trimmed != image) {
			trimmed.recycle();
		}
		image.recycle();
		CaptchaData captchaData = new CaptchaData();
		String hanabira = response.getCookieValue(COOKIE_HANABIRA);
		if (hanabira != null) {
			configuration.storeCookie(COOKIE_HANABIRA, hanabira, "Hanabira");
			captchaData.put(COOKIE_HANABIRA, hanabira);
		}
		String hanabiraTemp = response.getCookieValue(COOKIE_HANABIRA_TEMP);
		if (hanabiraTemp != null) {
			configuration.storeCookie(COOKIE_HANABIRA_TEMP, hanabiraTemp, "Hanabira Temp");
			captchaData.put(COOKIE_HANABIRA_TEMP, hanabiraTemp);
		}
		return new ReadCaptchaResult(CaptchaState.CAPTCHA, captchaData).setImage(newImage);
	}

	@SuppressWarnings("SwitchStatementWithTooFewBranches")
	public String readThreadId(HttpRequest.Preset preset, String boardName, String threadNumber) throws HttpException,
			InvalidResponseException {
		DobrochanChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.createApiUri("thread", boardName, threadNumber + "/last.json",
				"count", "0", "new_format", "1");
		HttpResponse response = performRepeatable(new HttpRequest(uri, preset));
		try (InputStream input = response.open();
				JsonSerial.Reader reader = JsonSerial.reader(input)) {
			String threadId = null;
			reader.startObject();
			while (!reader.endStruct()) {
				switch (reader.nextName()) {
					case "error": {
						handleMobileApiError(reader);
						break;
					}
					case "result": {
						reader.startObject();
						while (!reader.endStruct()) {
							switch (reader.nextName()) {
								case "thread_id": {
									threadId = reader.nextString();
									break;
								}
								default: {
									reader.skip();
									break;
								}
							}
						}
						break;
					}
					default: {
						reader.skip();
						break;
					}
				}
			}
			if (threadId == null) {
				throw new InvalidResponseException();
			}
			return threadId;
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		} catch (IOException e) {
			throw response.fail(e);
		}
	}

	private static final Pattern PATTERN_POST_ERROR_UNCOMMON = Pattern.compile("<h2>(.*?)</h2>");
	private static final Pattern PATTERN_POST_ERROR = Pattern.compile("<td.*?class='post-error'>(.*?)</td>");

	@Override
	public SendPostResult onSendPost(SendPostData data) throws HttpException, ApiException, InvalidResponseException {
		MultipartEntity entity = new MultipartEntity();
		entity.add("thread_id", data.threadNumber);
		entity.add("name", data.name);
		entity.add("subject", data.subject);
		entity.add("message", data.comment);
		entity.add("password", data.password);
		entity.add("goto", "thread");
		if (data.optionSage) {
			entity.add("sage", "on");
		}
		if (data.attachments != null) {
			for (int i = 0; i < data.attachments.length; i++) {
				SendPostData.Attachment attachment = data.attachments[i];
				attachment.addToEntity(entity, "file_" + (i + 1));
				entity.add("file_" + (i + 1) + "_rating", attachment.rating);
			}
			entity.add("post_files_count", Integer.toString(data.attachments.length));
		}
		if (data.captchaData != null) {
			entity.add("captcha", data.captchaData.get(CaptchaData.INPUT));
		}

		DobrochanChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildPath(data.boardName, "post", "new.xhtml");
		HttpResponse response = new HttpRequest(uri, data).setPostMethod(entity)
				.addCookie(buildCookies(data.captchaData))
				.setRedirectHandler(HttpRequest.RedirectHandler.NONE).perform();
		String responseText;
		if (response.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP) {
			uri = response.getRedirectedUri();
			String path = uri.getPath();
			if (path == null) {
				throw new InvalidResponseException();
			}
			if (!path.startsWith("/error")) {
				String threadNumber = locator.getThreadNumber(uri);
				if (threadNumber == null) {
					throw new InvalidResponseException();
				}
				return new SendPostResult(threadNumber, null);
			}
			responseText = new HttpRequest(uri, data).addCookie(buildCookies(data.captchaData)).perform().readString();
		} else {
			responseText = response.readString();
		}

		String errorMessage = null;
		Matcher matcher = PATTERN_POST_ERROR.matcher(responseText);
		if (matcher.find()) {
			errorMessage = matcher.group(1);
		} else {
			matcher = PATTERN_POST_ERROR_UNCOMMON.matcher(responseText);
			if (matcher.find()) {
				errorMessage = matcher.group(1);
			}
		}
		if (errorMessage != null) {
			int errorType = 0;
			if (errorMessage.contains("Неверная капча") || errorMessage.contains("Нужно включить кукисы")
					|| errorMessage.contains("подтвердите, что вы человек")) {
				setForceCaptcha(data.boardName, data.threadNumber, true);
				errorType = ApiException.SEND_ERROR_CAPTCHA;
			} else if (errorMessage.contains("Вы должны указать тему или написать сообщение")
					|| errorMessage.contains("Вы должны написать текст")) {
				errorType = ApiException.SEND_ERROR_EMPTY_COMMENT;
			} else if (errorMessage.contains("Вы должны прикрепить")) {
				errorType = ApiException.SEND_ERROR_EMPTY_FILE;
			} else if (errorMessage.contains("Сообщение не должно превышать")) {
				errorType = ApiException.SEND_ERROR_FIELD_TOO_LONG;
			} else if (errorMessage.contains("не существует")) {
				errorType = ApiException.SEND_ERROR_NO_THREAD;
			} else if (errorMessage.contains("закрыт")) {
				errorType = ApiException.SEND_ERROR_CLOSED;
			}
			if (errorType != 0) {
				throw new ApiException(errorType);
			} else {
				CommonUtils.writeLog("Dobrochan send message", errorMessage);
				throw new ApiException(errorMessage);
			}
		}
		throw new InvalidResponseException();
	}

	@Override
	public SendDeletePostsResult onSendDeletePosts(SendDeletePostsData data) throws HttpException, ApiException,
			InvalidResponseException {
		String threadId = readThreadId(data, data.boardName, data.threadNumber);
		DobrochanChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildPath(data.boardName, "delete");
		UrlEncodedEntity entity = new UrlEncodedEntity("task", "delete", "password", data.password);
		for (String postNumber : data.postNumbers) {
			entity.add(postNumber, threadId);
		}
		HttpResponse response = new HttpRequest(uri, data).setPostMethod(entity)
				.setRedirectHandler(HttpRequest.RedirectHandler.NONE).perform();
		if (response.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP) {
			return null;
		}
		String responseText = response.readString();
		Matcher matcher = PATTERN_POST_ERROR_UNCOMMON.matcher(responseText);
		if (matcher.find()) {
			String errorMessage = matcher.group(1);
			if (errorMessage != null) {
				int errorType = 0;
				if (errorMessage.contains("Неправильный пароль")) {
					errorType = ApiException.DELETE_ERROR_PASSWORD;
				}
				if (errorType != 0) {
					throw new ApiException(errorType);
				}
			}
			CommonUtils.writeLog("Dobrochan delete message", errorMessage);
			throw new ApiException(errorMessage);
		}
		throw new InvalidResponseException();
	}
}
