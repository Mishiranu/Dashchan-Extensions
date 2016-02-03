package com.mishiranu.dashchan.chan.allchan;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;

import chan.content.ApiException;
import chan.content.ChanConfiguration;
import chan.content.ChanLocator;
import chan.content.ChanPerformer;
import chan.content.InvalidResponseException;
import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.content.model.Post;
import chan.content.model.Threads;
import chan.http.HttpException;
import chan.http.HttpRequest;
import chan.http.HttpResponse;
import chan.http.MultipartEntity;
import chan.http.UrlEncodedEntity;
import chan.util.CommonUtils;
import chan.util.StringUtils;

public class AllchanChanPerformer extends ChanPerformer
{
	@Override
	public ReadThreadsResult onReadThreads(ReadThreadsData data) throws HttpException, InvalidResponseException
	{
		AllchanChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildPath(data.boardName, (data.isCatalog() ? "catalog" : data.pageNumber) + ".json");
		JSONObject jsonObject = new HttpRequest(uri, data.holder, data).setValidator(data.validator).read()
				.getJsonObject();
		if (jsonObject != null)
		{
			AllchanChanConfiguration chanConfiguration = ChanConfiguration.get(this);
			int pagesCount = jsonObject.optInt("pageCount");
			if (pagesCount > 0) chanConfiguration.storePagesCount(data.boardName, pagesCount);
			String boardSpeed = CommonUtils.optJsonString(jsonObject, "postingSpeed");
			int boardSpeedInt = 0;
			if (boardSpeed != null && (boardSpeed.contains("hour") || boardSpeed.contains("час")))
			{
				boardSpeed = boardSpeed.substring(0, boardSpeed.indexOf(' '));
				boardSpeedInt = (int) (Float.parseFloat(boardSpeed) + 0.5f);
			}
			try
			{
				Threads threads = AllchanModelMapper.createThreads(jsonObject.getJSONArray("threads"),
						locator, data.boardName);
				if (threads != null && boardSpeedInt > 0) threads.setBoardSpeed(boardSpeedInt);
				return new ReadThreadsResult(threads);
			}
			catch (JSONException e)
			{
				throw new InvalidResponseException(e);
			}
		}
		throw new InvalidResponseException();
	}
	
	@Override
	public ReadPostsResult onReadPosts(ReadPostsData data) throws HttpException, InvalidResponseException
	{
		AllchanChanLocator locator = ChanLocator.get(this);
		if (data.partialThreadLoading && data.lastPostNumber != null)
		{
			Uri uri = locator.buildQuery("api/threadLastPostNumber.json", "boardName", data.boardName, "threadNumber",
					data.threadNumber);
			HttpResponse response = new HttpRequest(uri, data.holder, data).read();
			JSONObject jsonObject = response.getJsonObject();
			if (jsonObject != null)
			{
				int lastPostNumber;
				try
				{
					lastPostNumber = jsonObject.getInt("lastPostNumber");
				}
				catch (JSONException e)
				{
					throw new InvalidResponseException(e);
				}
				if (lastPostNumber == 0) throw HttpException.createNotFoundException();
				int clientLastPostNumber = Integer.parseInt(data.lastPostNumber);
				if (lastPostNumber <= clientLastPostNumber) return null;
			}
			else throw new InvalidResponseException();
		}
		Uri uri = locator.buildPath(data.boardName, "res", data.threadNumber + ".json");
		JSONObject jsonObject = new HttpRequest(uri, data.holder, data).setValidator(data.validator)
				.read().getJsonObject();
		try
		{
			return new ReadPostsResult(AllchanModelMapper.createPosts(jsonObject.getJSONObject("thread"),
					locator, data.boardName));
		}
		catch (JSONException e)
		{
			String errorDescription = CommonUtils.optJsonString(jsonObject, "errorDescription");
			if (errorDescription != null && errorDescription.contains("no such file or directory"))
			{
				throw HttpException.createNotFoundException();
			}
			throw new InvalidResponseException(e);
		}
	}
	
	@Override
	public ReadSinglePostResult onReadSinglePost(ReadSinglePostData data) throws HttpException, InvalidResponseException
	{
		AllchanChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildQuery("api/post.json", "boardName", data.boardName, "postNumber", data.postNumber);
		HttpResponse response = new HttpRequest(uri, data.holder, data).read();
		JSONObject jsonObject = response.getJsonObject();
		if (jsonObject != null)
		{
			try
			{
				return new ReadSinglePostResult(AllchanModelMapper.createPost(jsonObject, locator, data.boardName));
			}
			catch (JSONException e)
			{
				throw new InvalidResponseException(e);
			}
		}
		if ("null".equals(response.getString())) throw HttpException.createNotFoundException();
		throw new InvalidResponseException();
	}
	
	@Override
	public ReadSearchPostsResult onReadSearchPosts(ReadSearchPostsData data) throws HttpException,
			InvalidResponseException
	{
		AllchanChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildPath("action", "search");
		JSONObject jsonObject = new HttpRequest(uri, data.holder, data).setPostMethod(new UrlEncodedEntity("boardName",
				data.boardName, "query", data.searchQuery)).read().getJsonObject();
		if (jsonObject != null)
		{
			try
			{
				JSONArray jsonArray = jsonObject.getJSONArray("searchResults");
				if (jsonArray == null || jsonArray.length() == 0) return null;
				Post[] posts = new Post[jsonArray.length()];
				for (int i = 0; i < jsonArray.length(); i++)
				{
					JSONObject searchJsonObject = jsonArray.getJSONObject(i);
					String threadNumber = CommonUtils.getJsonString(searchJsonObject, "threadNumber");
					String postNumber = CommonUtils.getJsonString(searchJsonObject, "postNumber");
					String text = CommonUtils.optJsonString(searchJsonObject, "text");
					if (postNumber.equals(threadNumber)) threadNumber = null;
					posts[i] = new Post().setParentPostNumber(threadNumber).setPostNumber(postNumber).setComment(text);
				}
				return new ReadSearchPostsResult(posts);
			}
			catch (JSONException e)
			{
				throw new InvalidResponseException(e);
			}
		}
		throw new InvalidResponseException();
	}
	
	@Override
	public ReadBoardsResult onReadBoards(ReadBoardsData data) throws HttpException, InvalidResponseException
	{
		AllchanChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildPath("misc", "boards.json");
		JSONObject jsonObject = new HttpRequest(uri, data.holder, data).read().getJsonObject();
		if (jsonObject != null)
		{
			try
			{
				JSONArray jsonArray = jsonObject.getJSONArray("boards");
				Board[] boards = new Board[jsonArray.length()];
				for (int i = 0; i < jsonArray.length(); i++)
				{
					JSONObject boardJsonObject = jsonArray.getJSONObject(i);
					String boardName = CommonUtils.getJsonString(boardJsonObject, "name");
					String title = CommonUtils.getJsonString(boardJsonObject, "title");
					boards[i] = new Board(boardName, title);
				}
				AllchanChanConfiguration configuration = ChanConfiguration.get(this);
				configuration.updateFromBoardsJson(jsonObject);
				return new ReadBoardsResult(new BoardCategory("Доски", boards));
			}
			catch (JSONException e)
			{
				throw new InvalidResponseException(e);
			}
		}
		throw new InvalidResponseException();
	}
	
	@Override
	public ReadPostsCountResult onReadPostsCount(ReadPostsCountData data) throws HttpException, InvalidResponseException
	{
		AllchanChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildQuery("api/threadInfo.json", "boardName", data.boardName,
				"threadNumber", data.threadNumber);
		HttpResponse response = new HttpRequest(uri, data.holder, data).setValidator(data.validator).read();
		JSONObject jsonObject = response.getJsonObject();
		if (jsonObject != null)
		{
			try
			{
				return new ReadPostsCountResult(jsonObject.getInt("postCount"));
			}
			catch (JSONException e)
			{
				String errorDescription = CommonUtils.optJsonString(jsonObject, "errorDescription");
				if (errorDescription != null && errorDescription.contains("404"))
				{
					throw HttpException.createNotFoundException();
				}
				throw new InvalidResponseException(e);
			}
		}
		throw new InvalidResponseException();
	}
	
	@Override
	public ReadCaptchaResult onReadCaptcha(ReadCaptchaData data) throws HttpException, InvalidResponseException
	{
		AllchanChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildQuery("api/captchaQuota.json", "boardName", data.boardName);
		JSONObject jsonObject = new HttpRequest(uri, data.holder, data).read().getJsonObject();
		if (jsonObject != null)
		{
			int qouta = jsonObject.optInt("quota");
			if (qouta > 0)
			{
				ReadCaptchaResult result = new ReadCaptchaResult(CaptchaState.SKIP, null);
				result.validity = ChanConfiguration.Captcha.Validity.IN_BOARD;
				return result;
			}
		}
		String captchaType = data.captchaType;
		boolean recaptcha2 = AllchanChanConfiguration.CAPTCHA_TYPE_RECAPTCHA_2.equals(captchaType);
		boolean recaptcha1 = AllchanChanConfiguration.CAPTCHA_TYPE_RECAPTCHA_1.equals(captchaType);
		boolean yandexNumeric = AllchanChanConfiguration.CAPTCHA_TYPE_YANDEX_NUMERIC.equals(captchaType);
		boolean yandexTextual = AllchanChanConfiguration.CAPTCHA_TYPE_YANDEX_TEXTUAL.equals(captchaType);
		if (recaptcha2 || recaptcha1)
		{
			uri = locator.buildPath("misc", "board", data.boardName + ".json");
			jsonObject = new HttpRequest(uri, data.holder, data).read().getJsonObject();
			if (jsonObject == null) throw new InvalidResponseException();
			try
			{
				JSONArray jsonArray = jsonObject.getJSONObject("board").getJSONArray("supportedCaptchaEngines");
				String apiKey = null;
				for (int i = 0; i < jsonArray.length(); i++)
				{
					JSONObject captchaObject = jsonArray.getJSONObject(i);
					String id = StringUtils.emptyIfNull(CommonUtils.getJsonString(captchaObject, "id"));
					if (recaptcha2 && id.equals("google-recaptcha") || recaptcha1 && id.equals("google-recaptcha-v1"))
					{
						apiKey = CommonUtils.getJsonString(captchaObject, "publicKey");
						break;
					}
				}
				if (apiKey == null) throw new InvalidResponseException();
				CaptchaData captchaData = new CaptchaData();
				captchaData.put(CaptchaData.API_KEY, apiKey);
				ReadCaptchaResult result = new ReadCaptchaResult(CaptchaState.CAPTCHA, captchaData);
				result.validity = ChanConfiguration.Captcha.Validity.IN_BOARD;
				return result;
			}
			catch (JSONException e)
			{
				throw new InvalidResponseException(e);
			}
		}
		else if (yandexNumeric || yandexTextual)
		{
			uri = locator.buildQuery("api/yandexCaptchaImage.json", "type", yandexTextual ? "elatm" : "estd");
			jsonObject = new HttpRequest(uri, data.holder, data).addCookie("captchaEngine", yandexTextual
					? "yandex-captcha-elatm" : "yandex-captcha-estd").read().getJsonObject();
			if (jsonObject == null) throw new InvalidResponseException();
			try
			{
				String challenge = CommonUtils.getJsonString(jsonObject, "challenge");
				CaptchaData captchaData = new CaptchaData();
				captchaData.put(CaptchaData.CHALLENGE, challenge);
				ReadCaptchaResult result = new ReadCaptchaResult(CaptchaState.CAPTCHA, captchaData);
				result.validity = ChanConfiguration.Captcha.Validity.IN_BOARD;
				return result;
			}
			catch (JSONException e)
			{
				throw new InvalidResponseException(e);
			}
		}
		else throw new InvalidResponseException();
	}
	
	@Override
	public SendPostResult onSendPost(SendPostData data) throws HttpException, ApiException, InvalidResponseException
	{
		MultipartEntity entity = new MultipartEntity();
		entity.add("boardName", data.boardName);
		entity.add("threadNumber", data.threadNumber);
		entity.add("subject", data.subject);
		entity.add("text", data.comment);
		entity.add("name", data.name);
		entity.add("email", data.optionSage ? "sage" : data.email);
		entity.add("password", data.password);
		entity.add("markupMode", "EXTENDED_WAKABA_MARK,BB_CODE");
		if (data.optionOriginalPoster) entity.add("signAsOp", "true");
		if (data.attachments != null)
		{
			for (int i = 0; i < data.attachments.length; i++)
			{
				SendPostData.Attachment attachment = data.attachments[i];
				attachment.addToEntity(entity, "file_" + i);
				entity.add("file_" + i + "_rating", attachment.rating);
			}
		}
		if (data.captchaData != null)
		{
			if (AllchanChanConfiguration.CAPTCHA_TYPE_RECAPTCHA_2.equals(data.captchaType))
			{
				entity.add("captchaEngine", "google-recaptcha");
				entity.add("g-recaptcha-response", data.captchaData.get(CaptchaData.INPUT));
			}
			else if (AllchanChanConfiguration.CAPTCHA_TYPE_RECAPTCHA_1.equals(data.captchaType))
			{
				entity.add("captchaEngine", "google-recaptcha-v1");
				entity.add("recaptcha_challenge_field", data.captchaData.get(CaptchaData.CHALLENGE));
				entity.add("recaptcha_response_field", data.captchaData.get(CaptchaData.INPUT));
			}
			else if (AllchanChanConfiguration.CAPTCHA_TYPE_YANDEX_NUMERIC.equals(data.captchaType))
			{
				entity.add("captchaEngine", "yandex-captcha-estd");
				entity.add("yandexCaptchaChallenge", data.captchaData.get(CaptchaData.CHALLENGE));
				entity.add("yandexCaptchaResponse", data.captchaData.get(CaptchaData.INPUT));
			}
			else if (AllchanChanConfiguration.CAPTCHA_TYPE_YANDEX_TEXTUAL.equals(data.captchaType))
			{
				entity.add("captchaEngine", "yandex-captcha-elatm");
				entity.add("yandexCaptchaChallenge", data.captchaData.get(CaptchaData.CHALLENGE));
				entity.add("yandexCaptchaResponse", data.captchaData.get(CaptchaData.INPUT));
			}
		}

		AllchanChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildPath("action", data.threadNumber != null ? "createPost" : "createThread");
		JSONObject jsonObject = new HttpRequest(uri, data.holder, data).setPostMethod(entity)
				.setRedirectHandler(HttpRequest.RedirectHandler.STRICT).read().getJsonObject();
		if (jsonObject == null) throw new InvalidResponseException();

		String threadNumber = CommonUtils.optJsonString(jsonObject, "threadNumber");
		String postNumber = CommonUtils.optJsonString(jsonObject, "postNumber");
		if (!StringUtils.isEmpty(threadNumber) || !StringUtils.isEmpty(postNumber))
		{
			return new SendPostResult(data.threadNumber == null ? threadNumber : data.threadNumber, postNumber);
		}
		String errorDescription = CommonUtils.optJsonString(jsonObject, "errorDescription");
		if (!StringUtils.isEmpty(errorDescription))
		{
			int errorType = 0;
			if (errorDescription.contains("Капча пуста") || errorDescription.contains("Недействительная капча"))
			{
				errorType = ApiException.SEND_ERROR_CAPTCHA;
			}
			else if (errorDescription.contains("Отсутствуют и файл, и комментарий"))
			{
				errorType = ApiException.SEND_ERROR_EMPTY_COMMENT;
			}
			else if (errorDescription.contains("без прикрепления файла"))
			{
				errorType = ApiException.SEND_ERROR_EMPTY_FILE;
			}
			else if (errorDescription.contains("Файл слишком большой"))
			{
				errorType = ApiException.SEND_ERROR_FILE_TOO_BIG;
			}
			else if (errorDescription.contains("Тип файла не поддерживается"))
			{
				errorType = ApiException.SEND_ERROR_FILE_NOT_SUPPORTED;
			}
			else if (errorDescription.contains("Posting is disabled in this thread"))
			{
				errorType = ApiException.SEND_ERROR_CLOSED;
			}
			else if (errorDescription.contains("Недействительная доска"))
			{
				errorType = ApiException.SEND_ERROR_NO_BOARD;
			}
			else if (errorDescription.contains("Недействительный тред"))
			{
				errorType = ApiException.SEND_ERROR_NO_THREAD;
			}
			if (errorType != 0) throw new ApiException(errorType);
			CommonUtils.writeLog("Allchan post message", errorDescription);
			throw new ApiException(errorDescription);
		}
		throw new InvalidResponseException();
	}
	
	@Override
	public SendDeletePostsResult onSendDeletePosts(SendDeletePostsData data) throws HttpException, ApiException,
			InvalidResponseException
	{
		AllchanChanLocator locator = ChanLocator.get(this);
		String errorDescription = null;
		if (data.optionFilesOnly)
		{
			Uri uri = locator.buildQuery("api/post.json", "boardName", data.boardName, "postNumber",
					data.postNumbers.get(0));
			HttpResponse response = new HttpRequest(uri, data.holder, data).read();
			JSONObject jsonObject = response.getJsonObject();
			if (jsonObject == null)
			{
				if ("null".equals(response.getString())) throw new ApiException(ApiException.DELETE_ERROR_NOT_FOUND);
				throw new InvalidResponseException();
			}
			ArrayList<String> fileNames = null;
			try
			{
				JSONArray jsonArray = jsonObject.getJSONArray("fileInfos");
				if (jsonArray == null || jsonArray.length() == 0) return null;
				fileNames = new ArrayList<>(jsonArray.length());
				for (int i = 0; i < jsonArray.length(); i++)
				{
					jsonObject = jsonArray.getJSONObject(i);
					String fileName = CommonUtils.getJsonString(jsonObject, "name");
					fileNames.add(fileName);
				}
			}
			catch (JSONException e)
			{
				throw new InvalidResponseException(e);
			}
			boolean wasDeleted = false;
			for (String fileName : fileNames)
			{
				uri = locator.buildPath("action", "deleteFile");
				jsonObject = new HttpRequest(uri, data.holder, data).setPostMethod(new UrlEncodedEntity("fileName",
						fileName, "password", data.password)).read().getJsonObject();
				if (jsonObject == null) throw new InvalidResponseException();
				String currentErrorDescription = CommonUtils.optJsonString(jsonObject, "errorDescription");
				if (currentErrorDescription == null) wasDeleted = true; else errorDescription = currentErrorDescription;
			}
			if (wasDeleted) return null; // Ignore message if at least 1 image was deleted
		}
		else
		{
			Uri uri = locator.buildPath("action", "deletePost");
			JSONObject jsonObject = new HttpRequest(uri, data.holder, data).setPostMethod(new UrlEncodedEntity
					("boardName", data.boardName, "postNumber", data.postNumbers.get(0), "password", data.password))
					.setRedirectHandler(HttpRequest.RedirectHandler.STRICT).read().getJsonObject();
			if (jsonObject == null) throw new InvalidResponseException();
			errorDescription = CommonUtils.optJsonString(jsonObject, "errorDescription");
		}
		if (errorDescription == null) return null;
		int errorType = 0;
		if (errorDescription.contains("Недостаточно прав"))
		{
			errorType = ApiException.DELETE_ERROR_PASSWORD;
		}
		else if (errorDescription.contains("Both file and comment are missing"))
		{
			errorType = ApiException.DELETE_ERROR_NO_ACCESS;
		}
		else if (errorDescription.contains("Cannot read property"))
		{
			errorType = ApiException.DELETE_ERROR_NOT_FOUND;
		}
		if (errorType != 0) throw new ApiException(errorType);
		CommonUtils.writeLog("Allchan delete message", errorDescription);
		throw new ApiException(errorDescription);
	}
}