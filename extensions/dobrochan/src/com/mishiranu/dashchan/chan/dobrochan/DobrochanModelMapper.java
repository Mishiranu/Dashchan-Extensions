package com.mishiranu.dashchan.chan.dobrochan;

import chan.content.ChanLocator;
import chan.content.model.FileAttachment;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.util.CommonUtils;
import chan.util.StringUtils;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DobrochanModelMapper {
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

	public static FileAttachment createFileAttachment(JSONObject jsonObject, ChanLocator locator) throws JSONException {
		FileAttachment attachment = new FileAttachment();
		String src = CommonUtils.getJsonString(jsonObject, "src");
		attachment.setFileUri(locator, locator.buildPath(src));
		attachment.setSize(jsonObject.optInt("size"));
		JSONObject metadata = jsonObject.optJSONObject("metadata");
		if (metadata != null) {
			int width = metadata.optInt("width");
			int height = metadata.optInt("height");
			if (width == 0 || height == 0) {
				width = metadata.optInt("Image Width");
				height = metadata.optInt("Image Height");
			}
			if (width == 0 || height == 0) {
				width = metadata.optInt("Display Width");
				height = metadata.optInt("Display Height");
			}
			if (width > 0 && height > 0) {
				attachment.setWidth(width);
				attachment.setHeight(height);
				String thumb = CommonUtils.optJsonString(jsonObject, "thumb");
				attachment.setThumbnailUri(locator, locator.buildPath(thumb));
			}
		}
		return attachment;
	}

	public static Post createPost(JSONObject jsonObject, ChanLocator locator, String threadId) throws JSONException {
		Post post = new Post();
		String displayId = CommonUtils.getJsonString(jsonObject, "display_id");
		if (threadId != null && !threadId.equals(displayId)) {
			post.setParentPostNumber(threadId);
		}
		post.setPostNumber(displayId);
		post.setName(CommonUtils.optJsonString(jsonObject, "name"));
		post.setSubject(CommonUtils.optJsonString(jsonObject, "subject"));
		String message;
		try {
			message = CommonUtils.getJsonString(jsonObject, "message_html");
		} catch (JSONException e) {
			message = CommonUtils.getJsonString(jsonObject, "message");
			if (message != null) {
				// Make simple mark for unparsed message field
				message = message.replaceAll("\r", "");
				message = message.replaceAll(">>(\\d+)", "<a href=\"#i$1\">&gt;&gt;$1</a>");
				message = message.replaceAll("(?:^|\n)(>.*?)(?=\n|$)", "<blockquote>$1</blockquote>");
				message = message.replaceAll("\n", "<br />");
				message = StringUtils.linkify(message);
			}
		}
		post.setComment(message);
		String date = CommonUtils.getJsonString(jsonObject, "date");
		try {
			post.setTimestamp(Objects.requireNonNull(DATE_FORMAT.parse(date)).getTime());
		} catch (ParseException e) {
			// Ignore exception
		}
		try {
			JSONArray filesArray = jsonObject.getJSONArray("files");
			if (filesArray.length() > 0) {
				FileAttachment[]  attachments = new FileAttachment[filesArray.length()];
				for (int i = 0; i < attachments.length; i++) {
					attachments[i] = createFileAttachment(filesArray.getJSONObject(i), locator);
				}
				post.setAttachments(attachments);
			}
		} catch (JSONException e) {
			// Ignore exception
		}
		return post;
	}

	public static Post[] createPosts(JSONArray jsonArray, ChanLocator locator, String threadId) throws JSONException {
		if (jsonArray.length() > 0) {
			HashSet<String> postNumbers = new HashSet<>();
			Post[] posts = new Post[jsonArray.length()];
			for (int i = 0; i < posts.length; i++) {
				Post post = createPost(jsonArray.getJSONObject(i), locator, threadId);
				String postNumber = post.getPostNumber();
				if (postNumbers.contains(postNumber)) {
					for (int j = 1;; j++) {
						String newPostNumber = postNumber + '.' + j;
						if (!postNumbers.contains(newPostNumber)) {
							postNumbers.add(newPostNumber);
							post.setPostNumber(newPostNumber);
							break;
						}
					}
				} else {
					postNumbers.add(postNumber);
				}
				posts[i] = post;
			}
			return posts;
		}
		return null;
	}

	public static Posts createThread(JSONObject jsonObject, ChanLocator locator) throws JSONException {
		String threadId = CommonUtils.getJsonString(jsonObject, "display_id");
		int postsCount = jsonObject.getInt("posts_count");
		int filesCount = jsonObject.getInt("files_count");
		JSONArray postsArray = jsonObject.getJSONArray("posts");
		Post[] posts = new Post[postsArray.length()];
		for (int i = 0; i < posts.length; i++) {
			posts[i] = createPost(postsArray.getJSONObject(i), locator, threadId);
		}
		return new Posts(posts).addPostsCount(postsCount).addPostsWithFilesCount(filesCount);
	}
}
