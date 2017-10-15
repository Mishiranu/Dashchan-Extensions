package com.mishiranu.dashchan.chan.brchan;

import java.net.HttpURLConnection;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;

import chan.content.ApiException;
import chan.content.ChanPerformer;
import chan.content.InvalidResponseException;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.http.HttpException;
import chan.http.HttpRequest;
import chan.http.HttpResponse;
import chan.http.MultipartEntity;
import chan.http.UrlEncodedEntity;
import chan.text.ParseException;
import chan.util.CommonUtils;
import chan.util.StringUtils;

public class BrchanChanPerformer extends ChanPerformer {
	@Override
	public ReadThreadsResult onReadThreads(ReadThreadsData data) throws HttpException, InvalidResponseException {
		BrchanChanLocator locator = BrchanChanLocator.get(this);
		Uri uri = locator.buildPath(data.boardName, (data.isCatalog() ? "catalog"
				: Integer.toString(data.pageNumber)) + ".json");
		HttpResponse response = new HttpRequest(uri, data).setValidator(data.validator).read();
		JSONObject jsonObject = response.getJsonObject();
		JSONArray jsonArray = response.getJsonArray();
		if (jsonObject != null && data.pageNumber >= 0) {
			try {
				JSONArray threadsArray = jsonObject.getJSONArray("threads");
				Posts[] threads = new Posts[threadsArray.length()];
				for (int i = 0; i < threads.length; i++) {
					threads[i] = BrchanModelMapper.createThread(threadsArray.getJSONObject(i),
							locator, data.boardName, false);
				}
				return new ReadThreadsResult(threads);
			} catch (JSONException e) {
				throw new InvalidResponseException(e);
			}
		} else if (jsonArray != null) {
			if (data.isCatalog()) {
				try {
					if (jsonArray.length() == 1) {
						jsonObject = jsonArray.getJSONObject(0);
						if (!jsonObject.has("threads")) {
							return null;
						}
					}
					ArrayList<Posts> threads = new ArrayList<>();
					for (int i = 0; i < jsonArray.length(); i++) {
						JSONArray threadsArray = jsonArray.getJSONObject(i).getJSONArray("threads");
						for (int j = 0; j < threadsArray.length(); j++) {
							threads.add(BrchanModelMapper.createThread(threadsArray.getJSONObject(j),
									locator, data.boardName, true));
						}
					}
					return new ReadThreadsResult(threads);
				} catch (JSONException e) {
					throw new InvalidResponseException(e);
				}
			} else if (jsonArray.length() == 0) {
				return null;
			}
		}
		throw new InvalidResponseException();
	}

	@Override
	public ReadPostsResult onReadPosts(ReadPostsData data) throws HttpException, InvalidResponseException {
		BrchanChanLocator locator = BrchanChanLocator.get(this);
		Uri uri = locator.buildPath(data.boardName, "res", data.threadNumber + ".json");
		JSONObject jsonObject = new HttpRequest(uri, data).setValidator(data.validator).read().getJsonObject();
		if (jsonObject != null) {
			try {
				JSONArray jsonArray = jsonObject.getJSONArray("posts");
				if (jsonArray.length() > 0) {
					Post[] posts = new Post[jsonArray.length()];
					for (int i = 0; i < posts.length; i++) {
						posts[i] = BrchanModelMapper.createPost(jsonArray.getJSONObject(i),
								locator, data.boardName);
					}
					return new ReadPostsResult(posts);
				}
				return null;
			} catch (JSONException e) {
				throw new InvalidResponseException(e);
			}
		}
		throw new InvalidResponseException();
	}

	@Override
	public ReadBoardsResult onReadBoards(ReadBoardsData data) throws HttpException, InvalidResponseException {
		BrchanChanLocator locator = BrchanChanLocator.get(this);
		Uri uri = locator.buildPath();
		String responseText = new HttpRequest(uri, data).read().getString();
		try {
			return new ReadBoardsResult(new BrchanBoardsParser(responseText).convert());
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		}
	}

	@Override
	public ReadPostsCountResult onReadPostsCount(ReadPostsCountData data) throws HttpException,
			InvalidResponseException {
		BrchanChanLocator locator = BrchanChanLocator.get(this);
		Uri uri = locator.buildPath(data.boardName, "res", data.threadNumber + ".json");
		JSONObject jsonObject = new HttpRequest(uri, data).setValidator(data.validator).read().getJsonObject();
		if (jsonObject != null) {
			try {
				return new ReadPostsCountResult(jsonObject.getJSONArray("posts").length());
			} catch (JSONException e) {
				throw new InvalidResponseException(e);
			}
		}
		throw new InvalidResponseException();
	}

	@Override
	public ReadContentResult onReadContent(ReadContentData data) throws HttpException, InvalidResponseException {
		if (data.uri.getPath().contains("/thumb/")) {
			// Try all possible extensions for thumbnails
			String path = data.uri.getEncodedPath();
			Uri.Builder builder = data.uri.buildUpon();
			String[] extensions = {".jpg", ".png", ".gif"};

			for (String extension : extensions) {
				try {
					Uri uri = builder.encodedPath(path + extension).build();
					return new ReadContentResult(new HttpRequest(uri, data).read());
				} catch (HttpException e) {
					if (e.getResponseCode() != HttpURLConnection.HTTP_NOT_FOUND) {
						throw e;
					}
				}
			}
			throw HttpException.createNotFoundException();
		}
		return super.onReadContent(data);
	}

	@Override
	public ReadCaptchaResult onReadCaptcha(ReadCaptchaData data) throws HttpException, InvalidResponseException {
		return new ReadCaptchaResult(CaptchaState.SKIP, null);
	}

	@Override
	public SendPostResult onSendPost(SendPostData data) throws HttpException, ApiException, InvalidResponseException {
		MultipartEntity entity = new MultipartEntity();
		entity.add("board", data.boardName);
		entity.add("thread", data.threadNumber);
		entity.add("name", data.name);
		entity.add("email", data.optionSage ? "sage" : data.email);
		entity.add("subject", data.subject);
		entity.add("body", StringUtils.emptyIfNull(data.comment));
		entity.add("password", data.password);
		if (data.attachments != null) {
			for (int i = 0; i < data.attachments.length; i++) {
				SendPostData.Attachment attachment = data.attachments[i];
				attachment.addToEntity(entity, "file" + (i > 0 ? i + 1 : ""));
			}
		}
		entity.add("json_response", "1");

		BrchanChanLocator locator = BrchanChanLocator.get(this);
		Uri contentUri = data.threadNumber != null ? locator.createThreadUri(data.boardName, data.threadNumber)
				: locator.createBoardUri(data.boardName, 0);
		String responseText = new HttpRequest(contentUri, data.holder).read().getString();
		try {
			AntispamFieldsParser.parseAndApply(responseText, entity, "board", "thread", "name", "email",
					"subject", "body", "password", "file", "json_response");
		} catch (ParseException e) {
			throw new InvalidResponseException();
		}
		Uri uri = locator.buildPath("post.php");
		JSONObject jsonObject = new HttpRequest(uri, data).setPostMethod(entity)
				.addHeader("Referer", (data.threadNumber == null ? locator.createBoardUri(data.boardName, 0)
						: locator.createThreadUri(data.boardName, data.threadNumber)).toString())
				.setRedirectHandler(HttpRequest.RedirectHandler.STRICT).read().getJsonObject();
		if (jsonObject == null) {
			throw new InvalidResponseException();
		}

		String redirect = jsonObject.optString("redirect");
		if (!StringUtils.isEmpty(redirect)) {
			uri = locator.buildPath(redirect);
			String threadNumber = locator.getThreadNumber(uri);
			String postNumber = locator.getPostNumber(uri);
			return new SendPostResult(threadNumber, postNumber);
		}
		String errorMessage = jsonObject.optString("error");
		if (errorMessage != null) {
			int errorType = 0;
			if (errorMessage.contains("CAPTCHA") ||
					errorMessage.contains("Você errou o codigo de verificação")) {
				errorType = ApiException.SEND_ERROR_CAPTCHA;
			} else if (errorMessage.contains("O corpo do texto")) {
				errorType = ApiException.SEND_ERROR_EMPTY_COMMENT;
			} else if (errorMessage.contains("Você deve postar com uma imagem")) {
				errorType = ApiException.SEND_ERROR_EMPTY_FILE;
			} else if (errorMessage.contains("Seu arquivo é grande demais") || errorMessage.contains("é maior que")) {
				errorType = ApiException.SEND_ERROR_FILE_TOO_BIG;
			} else if (errorMessage.contains("Você tentou fazer upload de muitas")) {
				errorType = ApiException.SEND_ERROR_FILES_TOO_MANY;
			} else if (errorMessage.contains("longo demais")) {
				errorType = ApiException.SEND_ERROR_FIELD_TOO_LONG;
			} else if (errorMessage.contains("Tópico trancado")) {
				errorType = ApiException.SEND_ERROR_CLOSED;
			} else if (errorMessage.contains("Board inválida")) {
				errorType = ApiException.SEND_ERROR_NO_BOARD;
			} else if (errorMessage.contains("O tópico especificado não existe")) {
				errorType = ApiException.SEND_ERROR_NO_THREAD;
			} else if (errorMessage.contains("Formato de arquivo não aceito")) {
				errorType = ApiException.SEND_ERROR_FILE_NOT_SUPPORTED;
			} else if (errorMessage.contains("O arquivo")) {
				errorType = ApiException.SEND_ERROR_FILE_EXISTS;
			} else if (errorMessage.contains("Flood detectado")) {
				errorType = ApiException.SEND_ERROR_TOO_FAST;
			}
			if (errorType != 0) {
				throw new ApiException(errorType);
			}
			CommonUtils.writeLog("brchan send message", errorMessage);
			throw new ApiException(errorMessage);
		}
		throw new InvalidResponseException();
	}

	@Override
	public SendDeletePostsResult onSendDeletePosts(SendDeletePostsData data) throws HttpException, ApiException,
			InvalidResponseException {
		BrchanChanLocator locator = BrchanChanLocator.get(this);
		UrlEncodedEntity entity = new UrlEncodedEntity("delete", "on", "board", data.boardName,
				"password", data.password, "json_response", "1");
		for (String postNumber : data.postNumbers) {
			entity.add("delete_" + postNumber, "1");
		}
		if (data.optionFilesOnly) {
			entity.add("file", "on");
		}
		Uri uri = locator.buildPath("post.php");
		JSONObject jsonObject = new HttpRequest(uri, data).setPostMethod(entity)
				.setRedirectHandler(HttpRequest.RedirectHandler.STRICT).read().getJsonObject();
		if (jsonObject == null) {
			throw new InvalidResponseException();
		}
		if (jsonObject.optBoolean("success")) {
			return null;
		}
		String errorMessage = jsonObject.optString("error");
		if (errorMessage != null) {
			int errorType = 0;
			if (errorMessage.contains("Senha incorreta")) {
				errorType = ApiException.DELETE_ERROR_PASSWORD;
			} else if (errorMessage.contains("antes de deletar isso")) {
				errorType = ApiException.DELETE_ERROR_TOO_NEW;
			}
			if (errorType != 0) {
				throw new ApiException(errorType);
			}
			CommonUtils.writeLog("brchan delete message", errorMessage);
			throw new ApiException(errorMessage);
		}
		throw new InvalidResponseException();
	}

	@Override
	public SendReportPostsResult onSendReportPosts(SendReportPostsData data) throws HttpException, ApiException,
			InvalidResponseException {
		BrchanChanLocator locator = BrchanChanLocator.get(this);
		UrlEncodedEntity entity = new UrlEncodedEntity("report", "1", "board", data.boardName,
				"reason", StringUtils.emptyIfNull(data.comment), "json_response", "1");
		for (String postNumber : data.postNumbers) {
			entity.add("delete_" + postNumber, "1");
		}
		Uri uri = locator.buildPath("post.php");
		JSONObject jsonObject = new HttpRequest(uri, data).setPostMethod(entity)
				.setRedirectHandler(HttpRequest.RedirectHandler.STRICT).read().getJsonObject();
		if (jsonObject == null) {
			throw new InvalidResponseException();
		}
		if (jsonObject.optBoolean("success")) {
			return null;
		}
		String errorMessage = jsonObject.optString("error");
		if (errorMessage != null) {
			CommonUtils.writeLog("brchan report message", errorMessage);
			throw new ApiException(errorMessage);
		}
		throw new InvalidResponseException();
	}
}
