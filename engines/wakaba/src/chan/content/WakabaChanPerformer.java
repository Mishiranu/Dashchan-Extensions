package chan.content;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;
import android.util.Pair;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.http.HttpException;
import chan.http.HttpRequest;
import chan.http.HttpResponse;
import chan.http.MultipartEntity;
import chan.http.RequestEntity;
import chan.http.UrlEncodedEntity;
import chan.text.ParseException;
import chan.util.CommonUtils;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class WakabaChanPerformer extends ChanPerformer {
	protected abstract List<Posts> parseThreads(String boardName, InputStream input)
			throws IOException, ParseException, InvalidResponseException, RedirectException;

	@Override
	public ReadThreadsResult onReadThreads(ReadThreadsData data) throws HttpException, InvalidResponseException,
			RedirectException {
		WakabaChanLocator locator = WakabaChanLocator.get(this);
		Uri uri = locator.createBoardUri(data.boardName, data.pageNumber);
		HttpResponse response = new HttpRequest(uri, data).setValidator(data.validator).perform();
		try (InputStream input = response.open()) {
			return new ReadThreadsResult(parseThreads(data.boardName, input));
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		} catch (IOException e) {
			throw response.fail(e);
		}
	}

	protected abstract List<Post> parsePosts(String boardName, InputStream input) throws IOException, ParseException;

	protected List<Post> readPosts(ReadPostsData data, Uri uri,
			HttpRequest.RedirectHandler redirectHandler) throws HttpException, InvalidResponseException {
		HttpRequest request = new HttpRequest(uri, data).setValidator(data.validator);
		if (redirectHandler != null) {
			request.setRedirectHandler(redirectHandler);
		}
		HttpResponse response = request.perform();
		try (InputStream input = response.open()) {
			List<Post> posts = parsePosts(data.boardName, input);
			if (posts == null || posts.isEmpty()) {
				throw new InvalidResponseException();
			}
			return posts;
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		} catch (IOException e) {
			throw response.fail(e);
		}
	}

	@Override
	public ReadPostsResult onReadPosts(ReadPostsData data) throws HttpException, InvalidResponseException {
		WakabaChanLocator locator = WakabaChanLocator.get(this);
		Uri uri = locator.createThreadUri(data.boardName, data.threadNumber);
		return new ReadPostsResult(readPosts(data, uri, null));
	}

	private static final ColorMatrixColorFilter CAPTCHA_FILTER = new ColorMatrixColorFilter(new float[]
			{0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f, 1f, 0f});

	protected ReadCaptchaResult readCaptchaResult(ReadCaptchaData data, Uri uri)
			throws HttpException, InvalidResponseException {
		Bitmap image = new HttpRequest(uri, data).perform().readBitmap();
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

	protected ReadCaptchaResult readCaptchaScript(ReadCaptchaData data, String script)
			throws HttpException, InvalidResponseException {
		WakabaChanLocator locator = WakabaChanLocator.get(this);
		Uri uri = locator.createScriptUri(data.boardName, script).buildUpon().appendQueryParameter("key",
				data.threadNumber == null ? "mainpage" : "res" + data.threadNumber).build();
		return readCaptchaResult(data, uri);
	}

	@Override
	public ReadCaptchaResult onReadCaptcha(ReadCaptchaData data) throws HttpException, InvalidResponseException {
		return readCaptchaScript(data, "captcha.pl");
	}

	protected interface SendPostEntityMapper {
		String convert(String field);
	}

	private static final Pattern PATTERN_POST_ERROR = Pattern.compile("<h1 style=\"text-align: center\">(.*?)<br />");
	private static final SendPostEntityMapper DEFAULT_MAPPER = field -> field;

	protected static RequestEntity createSendPostEntity(SendPostData data, SendPostEntityMapper mapper) {
		if (mapper == null) {
			mapper = DEFAULT_MAPPER;
		}
		MultipartEntity entity = new MultipartEntity();
		entity.add(mapper.convert("task"), "post");
		entity.add(mapper.convert("parent"), data.threadNumber);
		entity.add(mapper.convert("field1"), data.name);
		entity.add(mapper.convert("field2"), data.optionSage ? "sage" : data.email);
		entity.add(mapper.convert("field3"), data.subject);
		entity.add(mapper.convert("field4"), data.comment);
		entity.add(mapper.convert("password"), data.password);
		if (data.attachments != null) {
			SendPostData.Attachment attachment = data.attachments[0];
			attachment.addToEntity(entity, mapper.convert("file"));
			if (attachment.optionSpoiler) {
				entity.add(mapper.convert("spoiler"), "on");
			}
		} else {
			entity.add(mapper.convert("nofile"), "1");
		}
		if (data.captchaData != null) {
			entity.add(mapper.convert("captcha"), data.captchaData.get(CaptchaData.INPUT));
		}
		return entity;
	}

	private static final HttpRequest.RedirectHandler POST_REDIRECT_HANDLER = response -> {
		if (response.getResponseCode() == HttpURLConnection.HTTP_SEE_OTHER) {
			return HttpRequest.RedirectHandler.Action.CANCEL;
		}
		return HttpRequest.RedirectHandler.STRICT.onRedirect(response);
	};

	protected Pair<HttpResponse, Uri> executeWakaba(String boardName, RequestEntity entity,
			HttpRequest.Preset preset) throws HttpException {
		WakabaChanLocator locator = WakabaChanLocator.get(this);
		Uri uri = locator.createScriptUri(boardName, "wakaba.pl");
		HttpResponse response = new HttpRequest(uri, preset).setPostMethod(entity)
				.setRedirectHandler(POST_REDIRECT_HANDLER).perform();
		if (response.getResponseCode() == HttpURLConnection.HTTP_SEE_OTHER) {
			return new Pair<>(null, response.getRedirectedUri());
		}
		return new Pair<>(response, null);
	}

	protected enum ErrorSource {POST, DELETE}

	protected void handleError(ErrorSource errorSource, String responseText) throws ApiException {
		Matcher matcher = PATTERN_POST_ERROR.matcher(responseText);
		if (matcher.find()) {
			String errorMessage = matcher.group(1);
			if (errorMessage != null) {
				int errorType = 0;
				int flags = 0;
				switch (errorSource) {
					case POST: {
						if (errorMessage.contains("Wrong verification code entered") ||
								errorMessage.contains("No verification code on record") ||
								errorMessage.contains("Введён неверный код подтверждения") ||
								errorMessage.contains("Код подтверждения не найден в базе")) {
							errorType = ApiException.SEND_ERROR_CAPTCHA;
						} else if (errorMessage.contains("No comment entered") ||
								errorMessage.contains("Пустое поле сообщения")) {
							errorType = ApiException.SEND_ERROR_EMPTY_COMMENT;
							flags |= ApiException.FLAG_KEEP_CAPTCHA;
						} else if (errorMessage.contains("No file selected") ||
								errorMessage.contains("Сообщения без изображений запрещены")) {
							errorType = ApiException.SEND_ERROR_EMPTY_FILE;
							flags |= ApiException.FLAG_KEEP_CAPTCHA;
						} else if (errorMessage.contains("This image is too large") ||
								errorMessage.contains("Изображение слишком большое")) {
							errorType = ApiException.SEND_ERROR_FILE_TOO_BIG;
							flags |= ApiException.FLAG_KEEP_CAPTCHA;
						} else if (errorMessage.contains("Too many characters") ||
								errorMessage.contains("превышает заданный предел")) {
							errorType = ApiException.SEND_ERROR_FIELD_TOO_LONG;
							flags |= ApiException.FLAG_KEEP_CAPTCHA;
						} else if (errorMessage.contains("Этот файл уже был запощен")) {
							errorType = ApiException.SEND_ERROR_FILE_EXISTS;
						} else if (errorMessage.contains("Thread does not exist") ||
								errorMessage.contains("Тред не существует")) {
							errorType = ApiException.SEND_ERROR_NO_THREAD;
						} else if (errorMessage.contains("String refused") ||
								errorMessage.contains("Flood detected, ") ||
								errorMessage.contains("Строка отклонена")) {
							errorType = ApiException.SEND_ERROR_SPAM_LIST;
							flags |= ApiException.FLAG_KEEP_CAPTCHA;
						} else if (errorMessage.contains("Host is banned")) {
							errorType = ApiException.SEND_ERROR_BANNED;
						} else if (errorMessage.contains("Flood detected") ||
								errorMessage.contains("Флуд")) {
							errorType = ApiException.SEND_ERROR_TOO_FAST;
						}
						break;
					}
					case DELETE: {
						if (errorMessage.contains("Incorrect password for deletion") ||
								errorMessage.contains("Введён неверный пароль для удаления")) {
							errorType = ApiException.DELETE_ERROR_PASSWORD;
						}
						break;
					}
				}
				if (errorType != 0) {
					throw new ApiException(errorType, flags);
				}
			}
			CommonUtils.writeLog("Wakaba send message", errorMessage);
			throw new ApiException(errorMessage);
		}
		if (responseText.contains("<h1>Anti-spam filters triggered.</h1>")) {
			throw new ApiException(ApiException.SEND_ERROR_SPAM_LIST, ApiException.FLAG_KEEP_CAPTCHA);
		}
	}

	@Override
	public SendPostResult onSendPost(SendPostData data) throws HttpException, ApiException, InvalidResponseException {
		RequestEntity entity = createSendPostEntity(data, null);
		Pair<HttpResponse, Uri> response = executeWakaba(data.boardName, entity, data);
		if (response.first == null) {
			return null;
		}
		handleError(ErrorSource.POST, response.first.readString());
		throw new InvalidResponseException();
	}

	@Override
	public SendDeletePostsResult onSendDeletePosts(SendDeletePostsData data) throws HttpException, ApiException,
			InvalidResponseException {
		UrlEncodedEntity entity = new UrlEncodedEntity("task", "delete", "password", data.password);
		for (String postNumber : data.postNumbers) {
			entity.add("delete", postNumber);
		}
		if (data.optionFilesOnly) {
			entity.add("fileonly", "on");
		}
		Pair<HttpResponse, Uri> result = executeWakaba(data.boardName, entity, data);
		if (result.first == null) {
			return null;
		}
		handleError(ErrorSource.DELETE, result.first.readString());
		throw new InvalidResponseException();
	}
}
