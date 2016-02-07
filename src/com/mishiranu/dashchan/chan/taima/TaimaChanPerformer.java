package com.mishiranu.dashchan.chan.taima;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;
import android.util.SparseArray;

import chan.content.ApiException;
import chan.content.ChanLocator;
import chan.content.ChanPerformer;
import chan.content.InvalidResponseException;
import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.content.model.FileAttachment;
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

public class TaimaChanPerformer extends ChanPerformer
{
	@Override
	public ReadThreadsResult onReadThreads(ReadThreadsData data) throws HttpException, InvalidResponseException
	{
		TaimaChanLocator locator = ChanLocator.get(this);
		Uri uri = data.isCatalog() ? locator.createApiUri(data.boardName, "catalog.json")
				: locator.createBoardUri(data.boardName, data.pageNumber);
		HttpResponse response;
		try
		{
			response = new HttpRequest(uri, data.holder, data).setValidator(data.validator).read();
		}
		catch (HttpException e)
		{
			if (data.isCatalog() && e.getResponseCode() == HttpURLConnection.HTTP_INTERNAL_ERROR)
			{
				throw HttpException.createNotFoundException();
			}
			throw e;
		}
		if (data.isCatalog())
		{
			JSONArray jsonArray = response.getJsonArray();
			if (jsonArray != null)
			{
				ArrayList<Posts> threads = new ArrayList<>();
				try
				{
					for (int i = 0; i < jsonArray.length(); i++)
					{
						JSONArray threadsArray = jsonArray.getJSONObject(i).getJSONArray("threads");
						for (int j = 0; j < threadsArray.length(); j++)
						{
							JSONObject jsonObject = threadsArray.getJSONObject(j);
							Post post = new Post();
							post.setPostNumber(CommonUtils.getJsonString(jsonObject, "no"));
							post.setName(CommonUtils.optJsonString(jsonObject, "name"));
							post.setIdentifier(CommonUtils.optJsonString(jsonObject, "id"));
							post.setTripcode(CommonUtils.optJsonString(jsonObject, "trip"));
							post.setSubject(CommonUtils.optJsonString(jsonObject, "sub"));
							post.setTimestamp(jsonObject.getLong("time"));
							String fileName = CommonUtils.optJsonString(jsonObject, "filename");
							String ext = CommonUtils.optJsonString(jsonObject, "ext");
							if (fileName != null && ext != null)
							{
								FileAttachment attachment = new FileAttachment();
								attachment.setFileUri(locator, locator.createSpecialBoardUri(data.boardName,
										"src", fileName + ext));
								attachment.setThumbnailUri(locator, locator.createSpecialBoardUri(data.boardName,
										"thumb", fileName + "s.jpg"));
								attachment.setWidth(jsonObject.optInt("w"));
								attachment.setHeight(jsonObject.optInt("h"));
								attachment.setSize(jsonObject.optInt("fsize"));
								post.setAttachments(attachment);
							}
							String comment = CommonUtils.optJsonString(jsonObject, "com");
							if (comment != null)
							{
								comment = comment.replaceAll("\r", "");
								comment = comment.replaceAll(">>(\\d+)", "<a href=\"#i$1\">$0</a>");
								comment = comment.replaceAll("(?:^|\n)(>.*?)(?=\n|$)", "<blockquote>$1</blockquote>");
								comment = comment.replace("\n", "<br />");
								comment = StringUtils.linkify(comment);
								post.setComment(comment);
							}
							threads.add(new Posts(post));
						}
					}
					return new ReadThreadsResult(threads);
				}
				catch (JSONException e)
				{
					throw new InvalidResponseException(e);
				}
			}
			throw new InvalidResponseException();
		}
		else
		{
			String responseText = response.getString();
			checkResponse(responseText);
			try
			{
				return new ReadThreadsResult(new TaimaPostsParser(responseText, this, data.boardName).convertThreads());
			}
			catch (ParseException e)
			{
				throw new InvalidResponseException(e);
			}
		}
	}
	
	@Override
	public ReadPostsResult onReadPosts(ReadPostsData data) throws HttpException, InvalidResponseException
	{
		TaimaChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.createThreadUri(data.boardName, data.threadNumber);
		String responseText = new HttpRequest(uri, data.holder, data).setValidator(data.validator).read().getString();
		checkResponse(responseText);
		try
		{
			return new ReadPostsResult(new TaimaPostsParser(responseText, this, data.boardName).convertPosts());
		}
		catch (ParseException e)
		{
			throw new InvalidResponseException(e);
		}
	}
	
	private void checkResponse(String responseText) throws HttpException
	{
		if (responseText != null)
		{
			if (responseText.contains("<title>404 - Not Found!</title>")) throw HttpException.createNotFoundException();
		}
	}
	
	private static class DisplayOrderItem implements Comparable<DisplayOrderItem>
	{
		private final int mDisplayOrder;
		
		public DisplayOrderItem(int displayOrder)
		{
			mDisplayOrder = displayOrder;
		}
		
		@Override
		public int compareTo(DisplayOrderItem another)
		{
			return mDisplayOrder - another.mDisplayOrder;
		}
	}
	
	private static class BoardCategoryItem extends DisplayOrderItem
	{
		public String title;
		public final ArrayList<BoardItem> boardItems = new ArrayList<>();
		
		public BoardCategoryItem(String title, int displayOrder)
		{
			super(displayOrder);
			this.title = title;
		}
	}
	
	private static class BoardItem extends DisplayOrderItem
	{
		public String boardName;
		public String title;
		
		public BoardItem(String boardName, String title, int displayOrder)
		{
			super(displayOrder);
			this.boardName = boardName;
			this.title = title;
		}
	}
	
	@Override
	public ReadBoardsResult onReadBoards(ReadBoardsData data) throws HttpException, InvalidResponseException
	{
		TaimaChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.createApiUri("categories.json");
		JSONObject categoriesObject = new HttpRequest(uri, data.holder, data).read().getJsonObject();
		uri = locator.createApiUri("boards.json");
		JSONObject boardsObject = new HttpRequest(uri, data.holder, data).read().getJsonObject();
		if (categoriesObject != null && boardsObject != null)
		{
			try
			{
				JSONArray categoriesArray = categoriesObject.getJSONArray("categories");
				SparseArray<BoardCategoryItem> categories = new SparseArray<>();
				for (int i = 0; i < categoriesArray.length(); i++)
				{
					JSONObject jsonObject = categoriesArray.getJSONObject(i);
					int id = jsonObject.getInt("id");
					String title = StringUtils.clearHtml(CommonUtils.getJsonString(jsonObject, "title"));
					int displayOrder = jsonObject.getInt("display_order");
					categories.put(id, new BoardCategoryItem(title, displayOrder));
				}
				JSONArray boardsArray = boardsObject.getJSONArray("boards");
				for (int i = 0; i < boardsArray.length(); i++)
				{
					JSONObject jsonObject = boardsArray.getJSONObject(i);
					int category = jsonObject.getInt("category");
					String boardName = CommonUtils.getJsonString(jsonObject, "board");
					String title = StringUtils.clearHtml(CommonUtils.getJsonString(jsonObject, "title"));
					int displayOrder = jsonObject.getInt("display_order");
					BoardCategoryItem boardCategoryItem = categories.get(category);
					boardCategoryItem.boardItems.add(new BoardItem(boardName, title, displayOrder));
				}
				ArrayList<BoardCategoryItem> boardCategoryItems = new ArrayList<>(categories.size());
				for (int i = 0; i < categories.size(); i++)
				{
					BoardCategoryItem boardCategoryItem = categories.get(categories.keyAt(i));
					Collections.sort(boardCategoryItem.boardItems);
					boardCategoryItems.add(boardCategoryItem);
				}
				Collections.sort(boardCategoryItems);
				BoardCategory[] boardCategories = new BoardCategory[boardCategoryItems.size()];
				for (int i = 0; i < boardCategoryItems.size(); i++)
				{
					BoardCategoryItem boardCategoryItem = boardCategoryItems.get(i);
					Board[] boards = new Board[boardCategoryItem.boardItems.size()];
					for (int j = 0; j < boardCategoryItem.boardItems.size(); j++)
					{
						BoardItem boardItem = boardCategoryItem.boardItems.get(j); 
						boards[j] = new Board(boardItem.boardName, boardItem.title);
					}
					boardCategories[i] = new BoardCategory(boardCategoryItem.title, boards);
				}
				return new ReadBoardsResult(boardCategories);
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
		TaimaChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.createApiUri(data.boardName, "res", data.threadNumber + ".json");
		JSONObject jsonObject = new HttpRequest(uri, data.holder, data).setValidator(data.validator)
				.read().getJsonObject();
		if (jsonObject != null)
		{
			try
			{
				int count = jsonObject.getJSONArray("posts").length();
				if (count == 0) throw HttpException.createNotFoundException();
				return new ReadPostsCountResult(count);
			}
			catch (JSONException e)
			{
				throw new InvalidResponseException(e);
			}
		}
		throw new InvalidResponseException();
	}
	
	@Override
	public ReadContentResult onReadContent(ReadContentData data) throws HttpException, InvalidResponseException
	{
		HttpResponse response = new HttpRequest(data.uri, data.holder, data).read();
		checkResponse(response.getString());
		return new ReadContentResult(response);
	}
	
	private static final Pattern PATTERN_POST_ERROR = Pattern.compile("<pre.*?>([\\s\\S]*?)</pre>");
	
	@Override
	public SendPostResult onSendPost(SendPostData data) throws HttpException, ApiException, InvalidResponseException
	{
		TaimaChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.createSpecialBoardUri("bunker", "");
		JSONObject jsonObject = new HttpRequest(uri, data.holder, data).setPostMethod(new UrlEncodedEntity("b",
				"0")).addHeader("X-Requested-With", "XMLHttpRequest").read().getJsonObject();
		String banana = jsonObject != null ? CommonUtils.optJsonString(jsonObject, "response") : null;
		if (banana == null) throw new ApiException(ApiException.SEND_ERROR_NO_ACCESS);
		
		MultipartEntity entity = new MultipartEntity();
		entity.add("task", "post");
		entity.add("board", data.boardName);
		entity.add("parent", data.threadNumber);
		entity.add("password", data.password);
		entity.add("field1", data.name);
		entity.add("field3", data.subject);
		entity.add("field4", data.comment);
		if (data.optionSage) entity.add("sage", "on");
		if (data.attachments != null)
		{
			SendPostData.Attachment attachment = data.attachments[0];
			attachment.addToEntity(entity, "file");
		}
		else entity.add("nofile", "on");
		entity.add("banana", banana);
		
		uri = locator.createSpecialBoardUri(data.boardName, "taimaba.pl");
		String responseText;
		try
		{
			new HttpRequest(uri, data.holder, data).setPostMethod(entity)
					.setRedirectHandler(HttpRequest.RedirectHandler.NONE).execute();
			if (data.holder.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP)
			{
				// Success
				return null;
			}
			responseText = data.holder.read().getString();
		}
		finally
		{
			data.holder.disconnect();
		}
		
		Matcher matcher = PATTERN_POST_ERROR.matcher(responseText);
		if (matcher.find())
		{
			String errorMessage = matcher.group(1);
			if (errorMessage != null)
			{
				int errorType = 0;
				int flags = 0;
				if (errorMessage.contains("No comment entered"))
				{
					errorType = ApiException.SEND_ERROR_EMPTY_COMMENT;
					flags |= ApiException.FLAG_KEEP_CAPTCHA;
				}
				else if (errorMessage.contains("No file selected"))
				{
					errorType = ApiException.SEND_ERROR_EMPTY_FILE;
					flags |= ApiException.FLAG_KEEP_CAPTCHA;
				}
				else if (errorMessage.contains("This image is too large"))
				{
					errorType = ApiException.SEND_ERROR_FILE_TOO_BIG;
					flags |= ApiException.FLAG_KEEP_CAPTCHA;
				}
				else if (errorMessage.contains("Too many characters"))
				{
					errorType = ApiException.SEND_ERROR_FIELD_TOO_LONG;
					flags |= ApiException.FLAG_KEEP_CAPTCHA;
				}
				else if (errorMessage.contains("Thread does not exist"))
				{
					errorType = ApiException.SEND_ERROR_NO_THREAD;
				}
				else if (errorMessage.contains("String refused") || errorMessage.contains("Flood detected, "))
				{
					errorType = ApiException.SEND_ERROR_SPAM_LIST;
					flags |= ApiException.FLAG_KEEP_CAPTCHA;
				}
				else if (errorMessage.contains("Host is banned"))
				{
					errorType = ApiException.SEND_ERROR_BANNED;
				}
				else if (errorMessage.contains("Flood detected"))
				{
					errorType = ApiException.SEND_ERROR_TOO_FAST;
				}
				if (errorType != 0) throw new ApiException(errorType, flags);
			}
			CommonUtils.writeLog("Taima send message", errorMessage);
			throw new ApiException(errorMessage);
		}
		throw new InvalidResponseException();
	}
	
	private static final Pattern PATTERN_REPORT = Pattern.compile("text: '([\\s\\S]*?)'");
	
	@Override
	public SendReportPostsResult onSendReportPosts(SendReportPostsData data) throws HttpException, ApiException,
			InvalidResponseException
	{
		String postNumber = data.postNumbers.get(0);
		TaimaChanLocator locator = ChanLocator.get(this);
		Uri.Builder uriBuilder = Uri.parse("http://boards.420chan.org:8080/narcbot/ajaxReport.jsp").buildUpon();
		uriBuilder.appendQueryParameter("reason", data.type);
		uriBuilder.appendQueryParameter("postId", postNumber);
		if (!postNumber.equals(data.threadNumber)) uriBuilder.appendQueryParameter("parentId", data.threadNumber);
		uriBuilder.appendQueryParameter("note", data.comment);
		uriBuilder.appendQueryParameter("location", locator.createThreadUri(data.boardName,
				data.threadNumber).toString());
		Uri uri = uriBuilder.build();
		String responseText = new HttpRequest(uri, data.holder, data).read().getString();
		if (responseText != null)
		{
			Matcher matcher = PATTERN_REPORT.matcher(responseText);
			if (matcher.find())
			{
				String message = matcher.group(1);
				if (message.contains("reported"))
				{
					return null;
				}
				if (message.contains("Wait longer"))
				{
					throw new ApiException(ApiException.REPORT_ERROR_TOO_OFTEN);
				}
				throw new ApiException(message);
			}
		}
		throw new InvalidResponseException();
	}
}