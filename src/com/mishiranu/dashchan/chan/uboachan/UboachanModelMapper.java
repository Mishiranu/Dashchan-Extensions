package com.mishiranu.dashchan.chan.uboachan;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import chan.content.model.EmbeddedAttachment;
import chan.content.model.FileAttachment;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.util.CommonUtils;
import chan.util.StringUtils;

public class UboachanModelMapper
{
	public static FileAttachment createFileAttachment(JSONObject jsonObject, UboachanChanLocator locator,
			String boardName) throws JSONException
	{
		FileAttachment attachment = new FileAttachment();
		String tim = CommonUtils.getJsonString(jsonObject, "tim");
		String filename = CommonUtils.getJsonString(jsonObject, "filename");
		String ext = CommonUtils.getJsonString(jsonObject, "ext");
		attachment.setSize(jsonObject.optInt("fsize"));
		attachment.setWidth(jsonObject.optInt("w"));
		attachment.setHeight(jsonObject.optInt("h"));
		attachment.setFileUri(locator, locator.buildPath(boardName, "src", tim + ext));
		attachment.setThumbnailUri(locator, locator.buildPath(boardName, "thumb", tim + ".png"));
		attachment.setOriginalName(filename);
		return attachment;
	}

	public static Post createPost(JSONObject jsonObject, UboachanChanLocator locator, String boardName)
			throws JSONException
	{
		Post post = new Post();
		if (jsonObject.optInt("sticky") != 0) post.setSticky(true);
		if (jsonObject.optInt("locked") != 0) post.setClosed(true);
		if (jsonObject.optInt("cyclical") != 0) post.setCyclical(true);
		String no = CommonUtils.getJsonString(jsonObject, "no");
		String resto = CommonUtils.getJsonString(jsonObject, "resto");
		post.setPostNumber(no);
		if (!"0".equals(resto)) post.setParentPostNumber(resto);
		post.setTimestamp(jsonObject.getLong("time") * 1000L);
		String name = CommonUtils.optJsonString(jsonObject, "name");
		if (name != null)
		{
			name = StringUtils.nullIfEmpty(StringUtils.clearHtml(name).trim());
			post.setName(name);
		}
		post.setTripcode(CommonUtils.optJsonString(jsonObject, "trip"));
		post.setIdentifier(CommonUtils.optJsonString(jsonObject, "id"));
		post.setCapcode(CommonUtils.optJsonString(jsonObject, "capcode"));
		String email = CommonUtils.optJsonString(jsonObject, "email");
		if (!StringUtils.isEmpty(email) && email.equalsIgnoreCase("sage")) post.setSage(true);
		else post.setEmail(email);
		String sub = CommonUtils.optJsonString(jsonObject, "sub");
		if (sub != null)
		{
			sub = StringUtils.nullIfEmpty(StringUtils.clearHtml(sub).trim());
			post.setSubject(sub);
		}
		String com = CommonUtils.optJsonString(jsonObject, "com");
		if (com != null)
		{
			// Vichan JSON API bug, sometimes comment is broken
			com = com.replace("<a  ", "<a ").replaceAll("href=\"\\?", "href=\"");
			post.setComment(com);
		}
		String embed = StringUtils.nullIfEmpty(CommonUtils.optJsonString(jsonObject, "embed"));
		if (embed != null)
		{
			EmbeddedAttachment attachment = EmbeddedAttachment.obtain(embed);
			if (attachment != null) post.setAttachments(attachment);
		}
		else
		{
			try
			{
				ArrayList<FileAttachment> attachments = new ArrayList<>();
				attachments.add(createFileAttachment(jsonObject, locator, boardName));
				JSONArray filesArray = jsonObject.optJSONArray("extra_files");
				if (filesArray != null)
				{
					for (int i = 0; i < filesArray.length(); i++)
					{
						JSONObject fileObject = filesArray.getJSONObject(i);
						FileAttachment attachment = createFileAttachment(fileObject, locator, boardName);
						attachments.add(attachment);
					}
				}
				post.setAttachments(attachments);
			}
			catch (JSONException e)
			{

			}
		}
		return post;
	}

	public static Posts createThread(JSONObject jsonObject, UboachanChanLocator locator, String boardName,
			boolean fromCatalog) throws JSONException
	{
		Post[] posts;
		int postsCount = 0;
		int filesCount = 0;
		if (fromCatalog)
		{
			Post post = createPost(jsonObject, locator, boardName);
			postsCount = jsonObject.getInt("replies") + 1;
			filesCount = jsonObject.getInt("omitted_images") + jsonObject.getInt("images");
			filesCount += post.getAttachmentsCount();
			posts = new Post[] {post};
		}
		else
		{
			JSONArray jsonArray = jsonObject.getJSONArray("posts");
			posts = new Post[jsonArray.length()];
			for (int i = 0; i < posts.length; i++)
			{
				jsonObject = jsonArray.getJSONObject(i);
				posts[i] = createPost(jsonObject, locator, boardName);
				if (i == 0)
				{
					postsCount = jsonObject.getInt("replies") + 1;
					filesCount = jsonObject.getInt("omitted_images") + jsonObject.getInt("images");
					filesCount += posts[0].getAttachmentsCount();
				}
			}
		}
		return new Posts(posts).addPostsCount(postsCount).addFilesCount(filesCount);
	}
}