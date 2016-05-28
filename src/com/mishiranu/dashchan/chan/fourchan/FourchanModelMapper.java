package com.mishiranu.dashchan.chan.fourchan;

import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;

import chan.content.model.FileAttachment;
import chan.content.model.Icon;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.util.CommonUtils;
import chan.util.StringUtils;

public class FourchanModelMapper
{
	public static Post createPost(JSONObject jsonObject, FourchanChanLocator locator, String boardName)
			throws JSONException
	{
		Post post = new Post();
		if (jsonObject.optInt("sticky") != 0) post.setSticky(true);
		if (jsonObject.optInt("closed") != 0) post.setClosed(true);
		if (jsonObject.optInt("archived") != 0) post.setArchived(true);
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
		String capcode = CommonUtils.optJsonString(jsonObject, "capcode");
		if ("admin".equals(capcode) || "admin_highlight".equals(capcode)) post.setCapcode("Admin");
		if ("mod".equals(capcode)) post.setCapcode("Mod");
		if ("developer".equals(capcode)) post.setCapcode("Developer");
		String country = CommonUtils.optJsonString(jsonObject, "country");
		String countryName = CommonUtils.optJsonString(jsonObject, "country_name");
		if (country != null)
		{
			Uri uri = locator.createIconUri(country);
			String title = countryName == null ? country.toUpperCase(Locale.US) : countryName;
			post.setIcons(new Icon(locator, uri, title));
		}
		String sub = CommonUtils.optJsonString(jsonObject, "sub");
		if (sub != null)
		{
			sub = StringUtils.nullIfEmpty(StringUtils.clearHtml(sub).trim());
			post.setSubject(sub);
		}
		String com = CommonUtils.optJsonString(jsonObject, "com");
		if (com != null)
		{
			com = com.replaceAll("<wbr ?/?>", "");
			com = StringUtils.linkify(com);
			post.setComment(com);
		}
		String tim = CommonUtils.optJsonString(jsonObject, "tim");
		if (tim != null)
		{
			FileAttachment attachment = new FileAttachment();
			String filename = CommonUtils.getJsonString(jsonObject, "filename");
			if (filename != null) filename = StringUtils.clearHtml(filename);
			String ext = CommonUtils.getJsonString(jsonObject, "ext");
			attachment.setSize(jsonObject.optInt("fsize"));
			attachment.setWidth(jsonObject.optInt("w"));
			attachment.setHeight(jsonObject.optInt("h"));
			attachment.setFileUri(locator, locator.buildAttachmentPath(boardName, tim + ext));
			attachment.setThumbnailUri(locator, locator.buildAttachmentPath(boardName, tim + "s.jpg"));
			attachment.setOriginalName(filename);
			post.setAttachments(attachment);
		}
		return post;
	}
	
	public static Posts createThread(JSONObject jsonObject, FourchanChanLocator locator, String boardName,
			boolean fromCatalog) throws JSONException
	{
		Post[] posts;
		int postsCount = 0;
		int postsWithFilesCount = 0;
		if (fromCatalog)
		{
			Post originalPost = createPost(jsonObject, locator, boardName);
			postsCount = jsonObject.getInt("replies") + 1;
			postsWithFilesCount = jsonObject.getInt("images") + originalPost.getAttachmentsCount();
			JSONArray jsonArray = jsonObject.optJSONArray("last_replies");
			if (jsonArray != null && jsonArray.length() > 0)
			{
				posts = new Post[jsonArray.length() + 1];
				for (int i = 1; i < posts.length; i++)
				{
					posts[i] = createPost(jsonArray.getJSONObject(i - 1), locator, boardName);
				}
			}
			else posts = new Post[1];
			posts[0] = originalPost;
		}
		else
		{
			JSONArray jsonArray = jsonObject.getJSONArray("posts");
			posts = new Post[jsonArray.length()];
			for (int i = 0; i <posts.length; i++)
			{
				jsonObject = jsonArray.getJSONObject(i);
				posts[i] = createPost(jsonObject, locator, boardName);
				if (i == 0)
				{
					postsCount = jsonObject.getInt("replies") + 1;
					postsWithFilesCount = jsonObject.getInt("images") + posts[0].getAttachmentsCount();
				}
			}
		}
		return new Posts(posts).addPostsCount(postsCount).addPostsWithFilesCount(postsWithFilesCount);
	}
}