package com.mishiranu.dashchan.chan.nulldvachin;

import java.net.HttpURLConnection;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.http.HttpException;
import chan.http.HttpHolder;
import chan.http.HttpRequest;
import chan.http.MultipartEntity;
import chan.http.UrlEncodedEntity;
import chan.util.CommonUtils;
import chan.util.StringUtils;

public class NulldvachinChanPerformer extends ChanPerformer
{
	@Override
	public ReadThreadsResult onReadThreads(ReadThreadsData data) throws HttpException, InvalidResponseException
	{
		NulldvachinChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildQuery(data.boardName + "/api/threads", "page", Integer.toString(data.pageNumber + 1));
		JSONObject jsonObject = new HttpRequest(uri, data.holder, data).read().getJsonObject();
		if (jsonObject == null) throw new InvalidResponseException();
		handleStatus(jsonObject);
		Posts[] threads;
		try
		{
			threads = NulldvachinModelMapper.createThreads(jsonObject, locator, data.boardName);
		}
		catch (JSONException e)
		{
			throw new InvalidResponseException(e);
		}
		if (threads != null)
		{
			NulldvachinChanConfiguration configuration = ChanConfiguration.get(this);
			configuration.updateFromThreadsPostsJson(data.boardName, jsonObject);
		}
		return new ReadThreadsResult(threads);
	}
	
	@Override
	public ReadPostsResult onReadPosts(ReadPostsData data) throws HttpException, InvalidResponseException
	{
		NulldvachinChanLocator locator = ChanLocator.get(this);
		String lastPostNumber = data.partialThreadLoading ? data.lastPostNumber : null;
		Uri uri;
		if (lastPostNumber != null)
		{
			uri = locator.buildQuery(data.boardName + "/api/newposts", "id", data.threadNumber,
					"after", lastPostNumber);
		}
		else uri = locator.buildQuery(data.boardName + "/api/thread", "id", data.threadNumber);
		JSONObject jsonObject = new HttpRequest(uri, data.holder, data).read().getJsonObject();
		if (jsonObject == null) throw new InvalidResponseException();
		try
		{
			handleStatus(jsonObject);
		}
		catch (HttpException e)
		{
			if (lastPostNumber != null && e.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND)
			{
				// Will throw exception if original post doesn't exist
				onReadSinglePost(data.holder, data, data.boardName, data.threadNumber);
				return new ReadPostsResult((Posts) null); // No new posts
			}
			throw e;
		}
		Post[] posts;
		try
		{
			posts = NulldvachinModelMapper.createPosts(jsonObject, locator, data.boardName);
		}
		catch (JSONException e)
		{
			throw new InvalidResponseException(e);
		}
		if (posts != null)
		{
			NulldvachinChanConfiguration configuration = ChanConfiguration.get(this);
			configuration.updateFromThreadsPostsJson(data.boardName, jsonObject);
		}
		return new ReadPostsResult(posts);
	}
	
	@Override
	public ReadSinglePostResult onReadSinglePost(ReadSinglePostData data) throws HttpException, InvalidResponseException
	{
		return new ReadSinglePostResult(onReadSinglePost(data.holder, data, data.boardName, data.postNumber));
	}
	
	private Post onReadSinglePost(HttpHolder holder, HttpRequest.Preset preset, String boardName, String postNumber)
			throws HttpException, InvalidResponseException
	{
		NulldvachinChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildQuery(boardName + "/api/post", "id", postNumber);
		JSONObject jsonObject = new HttpRequest(uri, holder, preset).read().getJsonObject();
		if (jsonObject == null) throw new InvalidResponseException();
		handleStatus(jsonObject);
		try
		{
			jsonObject = jsonObject.getJSONObject("data").getJSONObject("post");
			return NulldvachinModelMapper.createPost(jsonObject, locator, boardName);
		}
		catch (JSONException e)
		{
			throw new InvalidResponseException(e);
		}
	}
	
	@Override
	public ReadBoardsResult onReadBoards(ReadBoardsData data) throws HttpException, InvalidResponseException
	{
		NulldvachinChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildPath("b", "api", "getboards");
		JSONObject jsonObject = new HttpRequest(uri, data.holder, data).read().getJsonObject();
		if (jsonObject == null) throw new InvalidResponseException();
		handleStatus(jsonObject);
		try
		{
			ArrayList<Board> boards = new ArrayList<>();
			JSONArray jsonArray = jsonObject.getJSONArray("data");
			for (int i = 0; i < jsonArray.length(); i++)
			{
				jsonObject = jsonArray.getJSONObject(i);
				String boardName = CommonUtils.getJsonString(jsonObject, "board_key");
				String title = CommonUtils.getJsonString(jsonObject, "board_name");
				String description = StringUtils.emptyIfNull(CommonUtils.getJsonString(jsonObject, "board_desc"));
				boards.add(new Board(boardName, title, description));
			}
			return new ReadBoardsResult(new BoardCategory(null, boards));
		}
		catch (JSONException e)
		{
			throw new InvalidResponseException(e);
		}
	}
	
	@Override
	public ReadPostsCountResult onReadPostsCount(ReadPostsCountData data) throws HttpException, InvalidResponseException
	{
		NulldvachinChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildQuery(data.boardName + "/api/postcount", "id", data.threadNumber);
		JSONObject jsonObject = new HttpRequest(uri, data.holder, data).read().getJsonObject();
		if (jsonObject != null)
		{
			handleStatus(jsonObject);
			try
			{
				return new ReadPostsCountResult(jsonObject.getJSONObject("data").getInt("postcount"));
			}
			catch (JSONException e)
			{
				throw new InvalidResponseException(e);
			}
		}
		throw new InvalidResponseException();
	}
	
	private void handleStatus(JSONObject jsonObject) throws HttpException, InvalidResponseException
	{
		JSONObject statusObject;
		try
		{
			statusObject = jsonObject.getJSONObject("status");
		}
		catch (JSONException e)
		{
			throw new InvalidResponseException(e);
		}
		int errorCode = statusObject.optInt("error_code");
		if (errorCode != HttpURLConnection.HTTP_OK)
		{
			if (errorCode == HttpURLConnection.HTTP_NOT_FOUND) throw HttpException.createNotFoundException();
			throw new HttpException(errorCode, CommonUtils.optJsonString(statusObject, "error_msg"));
		}
	}
	
	private static final ColorMatrixColorFilter CAPTCHA_FILTER = new ColorMatrixColorFilter(new float[]
			{0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f, 0f});
	
	@Override
	public ReadCaptchaResult onReadCaptcha(ReadCaptchaData data) throws HttpException, InvalidResponseException
	{
		NulldvachinChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildQuery(data.boardName + "/api/checkconfig", "captcha", "");
		JSONObject jsonObject = new HttpRequest(uri, data.holder, data).read().getJsonObject();
		if (jsonObject == null) throw new InvalidResponseException();
		boolean needCaptcha;
		try
		{
			needCaptcha = jsonObject.getInt("captcha") != 0;
		}
		catch (JSONException e)
		{
			throw new InvalidResponseException(e);
		}
		if (needCaptcha)
		{
			uri = locator.buildQuery("captcha.pl", "board", data.boardName, "key",
					data.threadNumber == null ? "mainpage" : "res" + data.threadNumber);
			Bitmap image = new HttpRequest(uri, data.holder, data).read().getBitmap();
			if (image != null)
			{
				Bitmap newImage = Bitmap.createBitmap(image.getWidth(), 32, Bitmap.Config.ARGB_8888);
				Canvas canvas = new Canvas(newImage);
				canvas.drawColor(0xffffffff);
				Paint paint = new Paint();
				paint.setColorFilter(CAPTCHA_FILTER);
				canvas.drawBitmap(image, 0f, (newImage.getHeight() - image.getHeight()) / 2, paint);
				image.recycle();
				return new ReadCaptchaResult(CaptchaState.CAPTCHA, new CaptchaData()).setImage(newImage);
			}
			throw new InvalidResponseException();
		}
		else
		{
			return new ReadCaptchaResult(CaptchaState.SKIP, null)
					.setValidity(ChanConfiguration.Captcha.Validity.IN_BOARD);
		}
	}
	
	@Override
	public SendPostResult onSendPost(SendPostData data) throws HttpException, ApiException, InvalidResponseException
	{
		MultipartEntity entity = new MultipartEntity();
		entity.add("task", "post");
		entity.add("section", data.boardName);
		entity.add("parent", data.threadNumber);
		entity.add("nya1", data.name);
		if (data.optionSage) entity.add("nya2", "sage");
		entity.add("nya3", data.subject);
		entity.add("nya4", data.comment);
		entity.add("password", data.password);
		entity.add("ajax", "1");
		if (data.attachments != null)
		{
			for (SendPostData.Attachment attachment : data.attachments) attachment.addToEntity(entity, "file");
		}
		else entity.add("nofile", "on");
		if (data.captchaData != null) entity.add("captcha", data.captchaData.get(CaptchaData.INPUT));

		NulldvachinChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildPath("wakaba.pl");
		JSONObject jsonObject = new HttpRequest(uri, data.holder, data).setPostMethod(entity)
				.setRedirectHandler(HttpRequest.RedirectHandler.STRICT).read().getJsonObject();
		if (jsonObject == null) throw new InvalidResponseException();
		String parent = CommonUtils.optJsonString(jsonObject, "parent");
		String num = CommonUtils.optJsonString(jsonObject, "num");
		if (parent != null && num != null)
		{
			if ("0".equals(parent))
			{
				parent = num;
				num = null;
			}
			return new SendPostResult(parent, num);
		}
		
		String error = CommonUtils.optJsonString(jsonObject, "error");
		if (error == null) throw new InvalidResponseException();
		int errorType = 0;
		int flags = 0;
		if (error.contains("Неверно введена капча") || error.contains("Капча протухла"))
		{
			errorType = ApiException.SEND_ERROR_CAPTCHA;
		}
		else if (error.contains("Текст не введен"))
		{
			errorType = ApiException.SEND_ERROR_EMPTY_COMMENT;
			flags |= ApiException.FLAG_KEEP_CAPTCHA;
		}
		else if (error.contains("Сообщения без изображений запрещены"))
		{
			errorType = ApiException.SEND_ERROR_EMPTY_FILE;
			flags |= ApiException.FLAG_KEEP_CAPTCHA;
		}
		else if (error.contains("Слишком большой размер файла"))
		{
			errorType = ApiException.SEND_ERROR_FILE_TOO_BIG;
			flags |= ApiException.FLAG_KEEP_CAPTCHA;
		}
		else if (error.contains("Слишком много символов"))
		{
			errorType = ApiException.SEND_ERROR_FIELD_TOO_LONG;
			flags |= ApiException.FLAG_KEEP_CAPTCHA;
		}
		else if (error.contains("Тред не существует"))
		{
			errorType = ApiException.SEND_ERROR_NO_THREAD;
		}
		else if (error.contains("Строка отклонена"))
		{
			errorType = ApiException.SEND_ERROR_SPAM_LIST;
			flags |= ApiException.FLAG_KEEP_CAPTCHA;
		}
		else if (error.contains("Этот файл уже размещен") || error.contains("файл с таким же именем"))
		{
			errorType = ApiException.SEND_ERROR_FILE_EXISTS;
			flags |= ApiException.FLAG_KEEP_CAPTCHA;
		}
		else if (error.contains("Бан"))
		{
			errorType = ApiException.SEND_ERROR_BANNED;
		}
		else if (error.contains("Обнаружен флуд"))
		{
			errorType = ApiException.SEND_ERROR_TOO_FAST;
		}
		if (errorType != 0) throw new ApiException(errorType, flags);
		CommonUtils.writeLog("Nulldvachin send message", error);
		throw new ApiException(error);
	}
	
	@Override
	public SendDeletePostsResult onSendDeletePosts(SendDeletePostsData data) throws HttpException, ApiException,
			InvalidResponseException
	{
		NulldvachinChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildPath("wakaba.pl");
		UrlEncodedEntity entity = new UrlEncodedEntity("task", "delete", "section", data.boardName,
				"parent", data.threadNumber, "password", data.password, "ajax", "1");
		for (String postNumber : data.postNumbers) entity.add("delete", postNumber);
		JSONObject jsonObject = new HttpRequest(uri, data.holder, data).setPostMethod(entity).read().getJsonObject();
		if (jsonObject == null) throw new InvalidResponseException();
		String path = CommonUtils.optJsonString(jsonObject, "redir");
		if (!StringUtils.isEmpty(path)) return null;
		try
		{
			jsonObject = jsonObject.getJSONObject("error");
		}
		catch (JSONException e)
		{
			throw new InvalidResponseException(e);
		}
		int errorType = 0;
		String jsonAsString = jsonObject.toString();
		if (jsonAsString.contains("Период ожидания перед удалением"))
		{
			errorType = ApiException.DELETE_ERROR_TOO_NEW;
		}
		else if (jsonAsString.contains("Неверный пароль"))
		{
			errorType = ApiException.DELETE_ERROR_PASSWORD;
		}
		String firstMessage = null;
		try
		{
			if (jsonObject.length() < data.postNumbers.size()) return null; // At least 1 post was deleted
			firstMessage = CommonUtils.getJsonString(jsonObject, jsonObject.keys().next());
		}
		catch (JSONException e)
		{
			throw new InvalidResponseException(e);
		}
		if (errorType != 0) throw new ApiException(errorType);
		CommonUtils.writeLog("Nulldvachin delete message", jsonAsString, firstMessage);
		throw new ApiException(firstMessage);
	}
}