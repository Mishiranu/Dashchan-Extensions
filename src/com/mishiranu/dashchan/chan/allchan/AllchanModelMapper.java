package com.mishiranu.dashchan.chan.allchan;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import chan.content.ChanLocator;
import chan.content.model.Attachment;
import chan.content.model.FileAttachment;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.content.model.Threads;
import chan.util.CommonUtils;
import chan.util.StringUtils;

public class AllchanModelMapper
{
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSS'Z'", Locale.US);
	
	static
	{
		DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
	}
	
	public static FileAttachment createFileAttachment(JSONObject jsonObject, ChanLocator locator, String boardName)
			throws JSONException
	{
		FileAttachment attachment = new FileAttachment();
		String name = CommonUtils.getJsonString(jsonObject, "name");
		attachment.setFileUri(locator, locator.buildPath(boardName, "src", name));
		JSONObject thumbObject = jsonObject.optJSONObject("thumb");
		if (thumbObject != null)
		{
			String thumbName = CommonUtils.getJsonString(thumbObject, "name");
			attachment.setThumbnailUri(locator, locator.buildPath(boardName, "thumb", thumbName));
		}
		JSONObject dimensionsObject = jsonObject.optJSONObject("dimensions");
		attachment.setSize(jsonObject.optInt("size"));
		if (dimensionsObject != null)
		{
			attachment.setWidth(dimensionsObject.optInt("width"));
			attachment.setHeight(dimensionsObject.optInt("height"));
		}
		return attachment;
	}
	
	public static Post createPost(JSONObject jsonObject, ChanLocator locator, String boardName) throws JSONException
	{
		Post post = new Post();
		if (jsonObject.optBoolean("isOp")) post.setOriginalPoster(true);
		String number = CommonUtils.getJsonString(jsonObject, "number");
		String threadNumber = CommonUtils.getJsonString(jsonObject, "threadNumber");
		post.setPostNumber(number);
		if (!number.equals(threadNumber)) post.setParentPostNumber(threadNumber);
		String createdAt = CommonUtils.getJsonString(jsonObject, "createdAt");
		try
		{
			post.setTimestamp(DATE_FORMAT.parse(createdAt).getTime());
		}
		catch (ParseException e)
		{
			
		}
		post.setName(CommonUtils.optJsonString(jsonObject, "name"));
		post.setTripcode(CommonUtils.optJsonString(jsonObject, "tripcode"));
		String email = CommonUtils.optJsonString(jsonObject, "email");
		if (email != null && email.equalsIgnoreCase("sage")) post.setSage(true);
		else post.setEmail(email);
		JSONObject userObject = jsonObject.optJSONObject("user");
		if (userObject != null)
		{
			String level = CommonUtils.optJsonString(userObject, "level");
			if ("ADMIN".equals(level)) post.setCapcode("Admin");
			else if ("MODER".equals(level)) post.setCapcode("Mod");
		}
		String subject = CommonUtils.optJsonString(jsonObject, "subject");
		if (subject != null) post.setSubject(StringUtils.nullIfEmpty(StringUtils.clearHtml(subject).trim()));
		String text = CommonUtils.optJsonString(jsonObject, "text");
		if (text != null)
		{
			text = text.replaceAll("(?s)<a href=\"javascript:void\\(0\\);\" class=\"expandCollapse\".*?>.*?</a>", "");
			text = text.replaceAll("<div class=\"codeBlock.*?>(.*?)</div>", "<pre>$1</pre>");
			post.setComment(text);
		}
		post.setCommentMarkup(CommonUtils.optJsonString(jsonObject, "rawText"));
		JSONArray filesArray = jsonObject.optJSONArray("fileInfos");
		if (filesArray != null && filesArray.length() > 0)
		{
			Attachment[]  attachments = new Attachment[filesArray.length()];
			for (int i = 0; i < attachments.length; i++)
			{
				attachments[i] = createFileAttachment(filesArray.getJSONObject(i), locator, boardName);
			}
			post.setAttachments(attachments);
		}
		return post;
	}
	
	public static Post[] createPosts(JSONObject jsonObject, ChanLocator locator, String boardName) throws JSONException
	{
		Post originalPost = createPost(jsonObject.getJSONObject("opPost"), locator, boardName);
		if (jsonObject.optBoolean("fixed")) originalPost.setSticky(true);
		if (jsonObject.optBoolean("closed")) originalPost.setClosed(true);
		JSONArray jsonArray = jsonObject.optJSONArray("posts");
		if (jsonArray == null) jsonArray = jsonObject.optJSONArray("lastPosts");
		if (jsonArray != null && jsonArray.length() > 0)
		{
			Post[] posts = new Post[1 + jsonArray.length()];
			posts[0] = originalPost;
			for (int i = 0; i < jsonArray.length(); i++)
			{
				posts[i + 1] = createPost(jsonArray.getJSONObject(i), locator, boardName);
			}
			return posts;
		}
		else return new Post[] {originalPost};
	}
	
	public static Posts createThread(JSONObject jsonObject, ChanLocator locator, String boardName) throws JSONException
	{
		Post[] posts = createPosts(jsonObject, locator, boardName);
		Posts thread = new Posts(posts);
		thread.addPostsCount(jsonObject.getInt("postCount"));
		return thread;
	}
	
	public static Threads createThreads(JSONArray jsonArray, ChanLocator locator, String boardName) throws JSONException
	{
		if (jsonArray == null || jsonArray.length() == 0) return null;
		Posts[] threads = new Posts[jsonArray.length()];
		for (int i = 0; i < jsonArray.length(); i++)
		{
			threads[i] = createThread(jsonArray.getJSONObject(i), locator, boardName);
		}
		return new Threads(threads);
	}
}