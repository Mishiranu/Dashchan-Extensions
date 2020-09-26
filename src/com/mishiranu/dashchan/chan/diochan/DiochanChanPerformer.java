package com.mishiranu.dashchan.chan.diochan;

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

public class DiochanChanPerformer extends ChanPerformer {
	@Override
	public ReadThreadsResult onReadThreads(ReadThreadsData data) throws HttpException, InvalidResponseException {
		DiochanChanLocator locator = DiochanChanLocator.get(this);
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
					threads[i] = DiochanModelMapper.createThread(threadsArray.getJSONObject(i),
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
							threads.add(DiochanModelMapper.createThread(threadsArray.getJSONObject(j),
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
		DiochanChanLocator locator = DiochanChanLocator.get(this);
		Uri uri = locator.buildPath(data.boardName, "res", data.threadNumber + ".json");
		JSONObject jsonObject = new HttpRequest(uri, data).setValidator(data.validator).read().getJsonObject();
		if (jsonObject != null) {
			try {
				JSONArray jsonArray = jsonObject.getJSONArray("posts");
				if (jsonArray.length() > 0) {
					Post[] posts = new Post[jsonArray.length()];
					for (int i = 0; i < posts.length; i++) {
						posts[i] = DiochanModelMapper.createPost(jsonArray.getJSONObject(i),
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
		DiochanChanLocator locator = DiochanChanLocator.get(this);
		Uri uri = locator.buildPath();
		String responseText = new HttpRequest(uri, data).read().getString();
		try {
			return new ReadBoardsResult(new DiochanBoardsParser(responseText).convert());
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		}
	}

	@Override
	public ReadPostsCountResult onReadPostsCount(ReadPostsCountData data) throws HttpException,
			InvalidResponseException {
		DiochanChanLocator locator = DiochanChanLocator.get(this);
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
	public SendPostResult onSendPost(SendPostData data) throws HttpException, ApiException, InvalidResponseException {
		MultipartEntity entity = new MultipartEntity();
		entity.add("board", data.boardName);
		entity.add("thread", data.threadNumber);
		entity.add("name", data.name);
		entity.add("email", data.optionSage ? "salvia" : data.email);
		entity.add("subject", data.subject);
		entity.add("body", StringUtils.emptyIfNull(data.comment));
		entity.add("password", data.password);
		boolean spoiler = false;
		if (data.attachments != null) {
			for (int i = 0; i < data.attachments.length; i++) {
				SendPostData.Attachment attachment = data.attachments[i];
				attachment.addToEntity(entity, "file" + (i > 0 ? i + 1 : ""));
				if (attachment.optionSpoiler) {
					spoiler = true;
				}
			}
		}
		if (spoiler) {
			entity.add("spoiler", "on");
		}
		entity.add("json_response", "1");

		DiochanChanLocator locator = DiochanChanLocator.get(this);
		Uri contentUri = data.threadNumber != null ? locator.createThreadUri(data.boardName, data.threadNumber)
				: locator.createBoardUri(data.boardName, 0);
		String responseText = new HttpRequest(contentUri, data.holder).read().getString();
		try {
			AntispamFieldsParser.parseAndApply(responseText, entity, "board", "thread", "name", "email",
					"subject", "body", "password", "file", "spoiler", "json_response");
		} catch (ParseException e) {
			throw new InvalidResponseException();
		}
		String refererFix = locator.buildPath().toString().toLowerCase().endsWith("diochan.com") ? "/" + data.boardName + "/" : "";
		Uri uri = locator.buildPath("post.php");
		JSONObject jsonObject = new HttpRequest(uri, data).setPostMethod(entity)
				.addHeader("Referer", "https://www.diochan.com" + refererFix)
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
			if (errorMessage.contains("The body was") || errorMessage.contains("must be at least") || errorMessage.contains("troppo corto o vuoto")) {
				errorType = ApiException.SEND_ERROR_EMPTY_COMMENT;
			} else if (errorMessage.contains("You must upload an image") || errorMessage.contains("Devi inviare")) {
				errorType = ApiException.SEND_ERROR_EMPTY_FILE;
			} else if (errorMessage.contains("was too long") || errorMessage.contains("era troppo lungo")) {
				errorType = ApiException.SEND_ERROR_FIELD_TOO_LONG;
			} else if (errorMessage.contains("The file was too big") || errorMessage.contains("is longer than")
					|| errorMessage.contains("troppo grande") || errorMessage.contains("La webm inviata è")) {
				errorType = ApiException.SEND_ERROR_FILE_TOO_BIG;
			} else if (errorMessage.contains("Thread locked") || errorMessage.contains("Thread bloccato")) {
				errorType = ApiException.SEND_ERROR_CLOSED;
			} else if (errorMessage.contains("Invalid board") || errorMessage.contains("Board non valida")) {
				errorType = ApiException.SEND_ERROR_NO_BOARD;
			} else if (errorMessage.contains("Thread specified does not exist") || errorMessage.contains("Il thread indicato non esiste")) {
				errorType = ApiException.SEND_ERROR_NO_THREAD;
			} else if (errorMessage.contains("Unsupported image format") || errorMessage.contains("Immagine non valida")) {
				errorType = ApiException.SEND_ERROR_FILE_NOT_SUPPORTED;
			} else if (errorMessage.contains("Maximum file size") || errorMessage.contains("Dimensioni massime del file")) {
				errorType = ApiException.SEND_ERROR_FILE_TOO_BIG;
			} else if (errorMessage.contains("Your IP address") || errorMessage.contains("Il tuo indirizzo IP")) {
				errorType = ApiException.SEND_ERROR_BANNED;
			} else if (errorMessage.contains("That file") || errorMessage.contains("esiste gi")) {
				errorType = ApiException.SEND_ERROR_FILE_EXISTS;
			} else if (errorMessage.contains("Flood detected") || errorMessage.contains("Flood individuato") || errorMessage.contains(" Rallenta") ) {
				errorType = ApiException.SEND_ERROR_TOO_FAST;
			}
			/* RESERVED
			else if (errorMessage.contains("URL del video che hai incluso non trovato")) {
				errorType = ApiException.SEND_ERROR_EMBED_ISSUE;
			}
			*/
			if (errorType != 0) {
				throw new ApiException(errorType);
			}
			CommonUtils.writeLog("Diochan send message", errorMessage);
			throw new ApiException(errorMessage);
		}
		throw new InvalidResponseException();
	}

	@Override
	public SendDeletePostsResult onSendDeletePosts(SendDeletePostsData data) throws HttpException, ApiException,
			InvalidResponseException {
		DiochanChanLocator locator = DiochanChanLocator.get(this);
		UrlEncodedEntity entity = new UrlEncodedEntity("delete", "1", "board", data.boardName,
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
			if (errorMessage.contains("Wrong password") || errorMessage.contains("Password sbagliata")) {
				errorType = ApiException.DELETE_ERROR_PASSWORD;
			} else if (errorMessage.contains("before deleting that") || errorMessage.contains("prima di eliminarlo")) {
				errorType = ApiException.DELETE_ERROR_TOO_NEW;
			}
			if (errorType != 0) {
				throw new ApiException(errorType);
			}
			CommonUtils.writeLog("Diochan delete message", errorMessage);
			throw new ApiException(errorMessage);
		}
		throw new InvalidResponseException();
	}

	@Override
	public SendReportPostsResult onSendReportPosts(SendReportPostsData data) throws HttpException, ApiException,
			InvalidResponseException {
		DiochanChanLocator locator = DiochanChanLocator.get(this);
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
			CommonUtils.writeLog("Diochan report message", errorMessage);
			throw new ApiException(errorMessage);
		}
		throw new InvalidResponseException();
	}
}