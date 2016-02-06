package com.mishiranu.dashchan.chan.lainchan;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;
import android.util.Pair;

import chan.content.ApiException;
import chan.content.ChanLocator;
import chan.content.ChanPerformer;
import chan.content.InvalidResponseException;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.http.HttpException;
import chan.http.HttpHolder;
import chan.http.HttpRequest;
import chan.http.HttpResponse;
import chan.http.MultipartEntity;
import chan.http.UrlEncodedEntity;
import chan.text.ParseException;
import chan.util.CommonUtils;
import chan.util.StringUtils;

public class LainchanChanPerformer extends ChanPerformer
{
	@Override
	public ReadThreadsResult onReadThreads(ReadThreadsData data) throws HttpException, InvalidResponseException
	{
		LainchanChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildPath(data.boardName, (data.isCatalog() ? "catalog"
				: Integer.toString(data.pageNumber)) + ".json");
		HttpResponse response = new HttpRequest(uri, data.holder, data).setValidator(data.validator).read();
		JSONObject jsonObject = response.getJsonObject();
		JSONArray jsonArray = response.getJsonArray();
		if (jsonObject != null && data.pageNumber >= 0)
		{
			try
			{
				JSONArray threadsArray = jsonObject.getJSONArray("threads");
				Posts[] threads = new Posts[threadsArray.length()];
				for (int i = 0; i < threads.length; i++)
				{
					threads[i] = LainchanModelMapper.createThread(threadsArray.getJSONObject(i),
							locator, data.boardName, false);
				}
				return new ReadThreadsResult(threads);
			}
			catch (JSONException e)
			{
				throw new InvalidResponseException(e);
			}
		}
		else if (jsonArray != null)
		{
			if (data.isCatalog())
			{
				try
				{
					if (jsonArray.length() == 1)
					{
						jsonObject = jsonArray.getJSONObject(0);
						if (!jsonObject.has("threads")) return null;
					}
					ArrayList<Posts> threads = new ArrayList<>();
					for (int i = 0; i < jsonArray.length(); i++)
					{
						JSONArray threadsArray = jsonArray.getJSONObject(i).getJSONArray("threads");
						for (int j = 0; j < threadsArray.length(); j++)
						{
							threads.add(LainchanModelMapper.createThread(threadsArray.getJSONObject(j),
									locator, data.boardName, true));
						}
					}
					return new ReadThreadsResult(threads);
				}
				catch (JSONException e)
				{
					throw new InvalidResponseException(e);
				}
			}
			else if (jsonArray.length() == 0) return null;
		}
		throw new InvalidResponseException();
	}
	
	@Override
	public ReadPostsResult onReadPosts(ReadPostsData data) throws HttpException, InvalidResponseException
	{
		LainchanChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildPath(data.boardName, "res", data.threadNumber + ".json");
		JSONObject jsonObject = new HttpRequest(uri, data.holder, data).setValidator(data.validator)
				.read().getJsonObject();
		if (jsonObject != null)
		{
			try
			{
				JSONArray jsonArray = jsonObject.getJSONArray("posts");
				if (jsonArray.length() > 0)
				{
					Post[] posts = new Post[jsonArray.length()];
					for (int i = 0; i < posts.length; i++)
					{
						posts[i] = LainchanModelMapper.createPost(jsonArray.getJSONObject(i),
								locator, data.boardName);
					}
					return new ReadPostsResult(posts);
				}
				return null;
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
		LainchanChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildPath();
		String responseText = new HttpRequest(uri, data.holder, data).read().getString();
		try
		{
			return new ReadBoardsResult(new LainchanBoardsParser(responseText).convert());
		}
		catch (ParseException e)
		{
			throw new InvalidResponseException(e);
		}
	}
	
	@Override
	public ReadPostsCountResult onReadPostsCount(ReadPostsCountData data) throws HttpException, InvalidResponseException
	{
		LainchanChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildPath(data.boardName, "res", data.threadNumber + ".json");
		JSONObject jsonObject = new HttpRequest(uri, data.holder, data).setValidator(data.validator)
				.read().getJsonObject();
		if (jsonObject != null)
		{
			try
			{
				return new ReadPostsCountResult(jsonObject.getJSONArray("posts").length());
			}
			catch (JSONException e)
			{
				throw new InvalidResponseException(e);
			}
		}
		throw new InvalidResponseException();
	}
	
	private void readAndApplyTinyboardAntispamFields(HttpHolder holder, HttpRequest.Preset preset,
			MultipartEntity entity, String boardName, String threadNumber) throws HttpException,
			InvalidResponseException
	{
		LainchanChanLocator locator = ChanLocator.get(this);
		Uri uri = threadNumber != null ? locator.createThreadUri(boardName, threadNumber)
				: locator.createBoardUri(boardName, 0);
		String responseText = new HttpRequest(uri, holder, preset).read().getString();
		int start = responseText.indexOf("<form name=\"post\"");
		if (start == -1) throw new InvalidResponseException();
		responseText = responseText.substring(start, responseText.indexOf("</form>") + 7);
		ArrayList<Pair<String, String>> fields;
		try
		{
			fields = new TinyboardAntispamFieldsParser(responseText).convert();
		}
		catch (ParseException e)
		{
			throw new InvalidResponseException();
		}
		for (Pair<String, String> field : fields) entity.add(field.first, field.second);
	}
	
	@Override
	public SendPostResult onSendPost(SendPostData data) throws HttpException, ApiException, InvalidResponseException
	{
		MultipartEntity entity = new MultipartEntity();
		entity.add("post", "Post"); // Must be "Post"
		entity.add("board", data.boardName);
		entity.add("thread", data.threadNumber);
		entity.add("subject", data.subject);
		entity.add("body", StringUtils.emptyIfNull(data.comment));
		entity.add("name", data.name);
		entity.add("email", data.optionSage ? "sage" : data.email);
		entity.add("password", data.password);
		if (data.attachments != null)
		{
			for (int i = 0; i < data.attachments.length; i++)
			{
				SendPostData.Attachment attachment = data.attachments[i];
				attachment.addToEntity(entity, "file" + (i > 0 ? i + 1 : ""));
			}
		}
		entity.add("json_response", "1");
		readAndApplyTinyboardAntispamFields(data.holder, data, entity, data.boardName, data.threadNumber);

		LainchanChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildPath("post.php");
		JSONObject jsonObject = new HttpRequest(uri, data.holder, data).setPostMethod(entity)
				.addHeader("Referer", locator.buildPath().toString())
				.setRedirectHandler(HttpRequest.RedirectHandler.STRICT).read().getJsonObject();
		if (jsonObject == null) throw new InvalidResponseException();
		
		String redirect = jsonObject.optString("redirect");
		if (!StringUtils.isEmpty(redirect))
		{
			uri = locator.buildPath(redirect);
			String threadNumber = locator.getThreadNumber(uri);
			String postNumber = locator.getPostNumber(uri);
			return new SendPostResult(threadNumber, postNumber);
		}
		String errorMessage = jsonObject.optString("error");
		if (errorMessage != null)
		{
			int errorType = 0;
			if (errorMessage.contains("The body was") || errorMessage.contains("must be at least"))
			{
				errorType = ApiException.SEND_ERROR_EMPTY_COMMENT;
			}
			else if (errorMessage.contains("You must upload an image"))
			{
				errorType = ApiException.SEND_ERROR_EMPTY_FILE;
			}
			else if (errorMessage.contains("was too long"))
			{
				errorType = ApiException.SEND_ERROR_FIELD_TOO_LONG;
			}
			else if (errorMessage.contains("The file was too big") || errorMessage.contains("is longer than"))
			{
				errorType = ApiException.SEND_ERROR_FILE_TOO_BIG;
			}
			else if (errorMessage.contains("Thread locked"))
			{
				errorType = ApiException.SEND_ERROR_CLOSED;
			}
			else if (errorMessage.contains("Invalid board"))
			{
				errorType = ApiException.SEND_ERROR_NO_BOARD;
			}
			else if (errorMessage.contains("Thread specified does not exist"))
			{
				errorType = ApiException.SEND_ERROR_NO_THREAD;
			}
			else if (errorMessage.contains("Unsupported image format"))
			{
				errorType = ApiException.SEND_ERROR_FILE_NOT_SUPPORTED;
			}
			else if (errorMessage.contains("Maximum file size"))
			{
				errorType = ApiException.SEND_ERROR_FILE_TOO_BIG;
			}
			else if (errorMessage.contains("Your IP address"))
			{
				errorType = ApiException.SEND_ERROR_BANNED;
			}
			else if (errorMessage.contains("That file"))
			{
				errorType = ApiException.SEND_ERROR_FILE_EXISTS;
			}
			else if (errorMessage.contains("Flood detected"))
			{
				errorType = ApiException.SEND_ERROR_TOO_FAST;
			}
			if (errorType != 0) throw new ApiException(errorType);
			CommonUtils.writeLog("Lainchan send message", errorMessage);
			throw new ApiException(errorMessage);
		}
		throw new InvalidResponseException();
	}
	
	@Override
	public SendDeletePostsResult onSendDeletePosts(SendDeletePostsData data) throws HttpException, ApiException,
			InvalidResponseException
	{
		LainchanChanLocator locator = ChanLocator.get(this);
		UrlEncodedEntity entity = new UrlEncodedEntity("delete", "1", "board", data.boardName,
				"password", data.password, "json_response", "1");
		for (String postNumber : data.postNumbers) entity.add("delete_" + postNumber, "1");
		if (data.optionFilesOnly) entity.add("file", "on");
		Uri uri = locator.buildPath("post.php");
		JSONObject jsonObject = new HttpRequest(uri, data.holder, data).setPostMethod(entity)
				.setRedirectHandler(HttpRequest.RedirectHandler.STRICT).read().getJsonObject();
		if (jsonObject == null) throw new InvalidResponseException();
		if (jsonObject.optBoolean("success")) return null;
		String errorMessage = jsonObject.optString("error");
		if (errorMessage != null)
		{
			int errorType = 0;
			if (errorMessage.contains("Wrong password"))
			{
				errorType = ApiException.DELETE_ERROR_PASSWORD;
			}
			else if (errorMessage.contains("before deleting that"))
			{
				errorType = ApiException.DELETE_ERROR_TOO_NEW;
			}
			if (errorType != 0) throw new ApiException(errorType);
			CommonUtils.writeLog("Lainchan delete message", errorMessage);
			throw new ApiException(errorMessage);
		}
		throw new InvalidResponseException();
	}
	
	@Override
	public SendReportPostsResult onSendReportPosts(SendReportPostsData data) throws HttpException, ApiException,
			InvalidResponseException
	{
		LainchanChanLocator locator = ChanLocator.get(this);
		UrlEncodedEntity entity = new UrlEncodedEntity("report", "1", "board", data.boardName,
				"reason", StringUtils.emptyIfNull(data.comment), "json_response", "1");
		for (String postNumber : data.postNumbers) entity.add("delete_" + postNumber, "1");
		Uri uri = locator.buildPath("post.php");
		JSONObject jsonObject = new HttpRequest(uri, data.holder, data).setPostMethod(entity)
				.setRedirectHandler(HttpRequest.RedirectHandler.STRICT).read().getJsonObject();
		if (jsonObject == null) throw new InvalidResponseException();
		if (jsonObject.optBoolean("success")) return null;
		String errorMessage = jsonObject.optString("error");
		if (errorMessage != null)
		{
			CommonUtils.writeLog("Lainchan report message", errorMessage);
			throw new ApiException(errorMessage);
		}
		throw new InvalidResponseException();
	}
}