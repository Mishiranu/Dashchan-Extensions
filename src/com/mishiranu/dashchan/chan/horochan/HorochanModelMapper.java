package com.mishiranu.dashchan.chan.horochan;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import chan.content.model.Attachment;
import chan.content.model.EmbeddedAttachment;
import chan.content.model.FileAttachment;
import chan.content.model.Post;
import chan.util.CommonUtils;
import chan.util.StringUtils;

public class HorochanModelMapper {
	private static final Pattern PATTERN_LINK = Pattern.compile("<a href=\"(.*?)\">");
	private static final Pattern PATTERN_LINK_1 = Pattern.compile("/(\\d+)");
	private static final Pattern PATTERN_LINK_2 = Pattern.compile("/b/res/(\\d+)/?(?:#p?(\\d+))");

	public static FileAttachment createFileAttachment(JSONObject jsonObject, HorochanChanLocator locator)
			throws JSONException {
		FileAttachment attachment = new FileAttachment();
		String name = CommonUtils.getJsonString(jsonObject, "name");
		String ext = CommonUtils.getJsonString(jsonObject, "ext");
		String storage = CommonUtils.getJsonString(jsonObject, "storage");
		attachment.setFileUri(locator, locator.buildStaticPath(storage, "src", name + "." + ext));
		attachment.setThumbnailUri(locator, locator.buildStaticPath(storage, "thumb", "t" + name + ".jpeg"));
		attachment.setSize(jsonObject.optInt("size"));
		attachment.setWidth(jsonObject.optInt("width"));
		attachment.setHeight(jsonObject.optInt("height"));
		return attachment;
	}

	public static Post createPost(JSONObject jsonObject, HorochanChanLocator locator, HashSet<String> postNumbers)
			throws JSONException {
		Post post = new Post();
		String id = CommonUtils.getJsonString(jsonObject, "id");
		String parent = CommonUtils.optJsonString(jsonObject, "parent");
		post.setPostNumber(id);
		post.setParentPostNumber(parent);
		String originalPostNumber = parent != null ? parent : id;
		String subject = CommonUtils.optJsonString(jsonObject, "subject");
		if (subject != null) {
			subject = StringUtils.nullIfEmpty(StringUtils.clearHtml(subject).trim());
			post.setSubject(subject);
		}
		String message = CommonUtils.getJsonString(jsonObject, "message");
		if (message != null) {
			StringBuffer buffer = null;
			Matcher matcher = PATTERN_LINK.matcher(message);
			while (matcher.find()) {
				if (buffer == null) {
					buffer = new StringBuffer();
				}
				String url = matcher.group(1);
				String threadNumber = null;
				String postNumber = null;
				Matcher urlMatcher = PATTERN_LINK_1.matcher(url);
				if (urlMatcher.matches()) {
					threadNumber = urlMatcher.group(1);
					if (postNumbers != null && postNumbers.contains(threadNumber)) {
						postNumber = threadNumber;
						threadNumber = originalPostNumber;
					}
				} else {
					urlMatcher = PATTERN_LINK_2.matcher(url);
					if (urlMatcher.matches()) {
						threadNumber = urlMatcher.group(1);
						postNumber = urlMatcher.group(2);
					}
				}
				if (threadNumber != null) {
					matcher.appendReplacement(buffer, "<a href=\"/b/thread/" + threadNumber +
							(postNumber != null ? "#" + postNumber : "") + "\">");
				} else {
					matcher.appendReplacement(buffer, "$0");
				}
			}
			if (buffer != null) {
				matcher.appendTail(buffer);
				message = buffer.toString();
			}
			message = message.replaceAll("<blockquote><p>(?!&gt;|>)", "<blockquote><p>&gt;");
		}
		post.setComment(message);
		post.setTimestamp(jsonObject.optLong("timestamp") * 1000L);
		ArrayList<Attachment> attachments = null;
		try {
			JSONArray filesArray = jsonObject.optJSONArray("files");
			if (filesArray != null && filesArray.length() > 0) {
				attachments = new ArrayList<>();
				for (int i = 0; i < filesArray.length(); i++) {
					attachments.add(createFileAttachment(filesArray.getJSONObject(i), locator));
				}
			}
		} catch (JSONException e) {
			attachments = null;
		}
		String embed = CommonUtils.optJsonString(jsonObject, "embed");
		if (!StringUtils.isEmpty(embed)) {
			EmbeddedAttachment attachment = EmbeddedAttachment.obtain("http://www.youtube.com/watch?v=" + embed);
			if (attachment != null) {
				if (attachments == null) {
					attachments = new ArrayList<>();
				}
				attachments.add(attachment);
			}
		}
		if (attachments != null) {
			post.setAttachments(attachments);
		}
		return post;
	}

	private static void fillPosts(JSONArray jsonArray, HorochanChanLocator locator, Post[] posts, int from,
			HashSet<String> postNumbers) throws JSONException {
		if (postNumbers == null) {
			postNumbers = new HashSet<>();
		}
		for (int i = 0; i < jsonArray.length(); i++) {
			Post post = createPost(jsonArray.getJSONObject(i), locator, postNumbers);
			postNumbers.add(post.getPostNumber());
			posts[i + from] = post;
		}
	}

	public static Post[] createPosts(JSONObject jsonObject, HorochanChanLocator locator, HashSet<String> postNumbers)
			throws JSONException {
		Post originalPost = createPost(jsonObject, locator, null);
		JSONArray jsonArray = jsonObject.optJSONArray("replies");
		Post[] posts;
		if (jsonArray != null && jsonArray.length() > 0) {
			if (postNumbers == null) {
				postNumbers = new HashSet<>();
			}
			postNumbers.add(originalPost.getPostNumber());
			posts = new Post[jsonArray.length() + 1];
			fillPosts(jsonArray, locator, posts, 1, postNumbers);
		} else {
			posts = new Post[1];
		}
		posts[0] = originalPost;
		return posts;
	}

	public static Post[] createPosts(JSONArray jsonArray, HorochanChanLocator locator, HashSet<String> postNumbers)
			throws JSONException {
		if (jsonArray != null && jsonArray.length() > 0) {
			Post[] posts = new Post[jsonArray.length()];
			fillPosts(jsonArray, locator, posts, 0, postNumbers);
			return posts;
		}
		return null;
	}
}