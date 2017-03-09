package com.mishiranu.dashchan.chan.kurisach;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;

import chan.content.model.Attachment;
import chan.content.model.EmbeddedAttachment;
import chan.content.model.FileAttachment;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.util.CommonUtils;
import chan.util.StringUtils;

public class KurisachModelMapper {
	private static final Pattern PATTERN_TAG = Pattern.compile("<.*?>");

	public static Attachment createFileAttachment(JSONObject jsonObject, KurisachChanLocator locator,
			String boardName) throws JSONException {
		String file = CommonUtils.optJsonString(jsonObject, "filename");
		if (StringUtils.isEmpty(file)) {
			return null;
		}
		String extension = StringUtils.emptyIfNull(CommonUtils.optJsonString(jsonObject, "filetype"));
		if ("you".equals(extension)) {
			return EmbeddedAttachment.obtain("https://youtube.com/watch?v=" + file);
		}
		if (!StringUtils.isEmpty(extension)) {
			extension = "." + extension;
		}
		Uri fileUri = locator.buildPath(boardName, "src", file + extension);
		Uri thumbnailUri = locator.isImageExtension(extension)
				? locator.buildPath(boardName, "thumb", file + "s" + extension) : null;
		int size = jsonObject.optInt("filesize");
		int width = jsonObject.optInt("pic_w");
		int height = jsonObject.optInt("pic_h");
		boolean spoiler = jsonObject.optInt("spoiler") != 0;
		return new FileAttachment().setFileUri(locator, fileUri).setThumbnailUri(locator, thumbnailUri)
				.setSize(size).setWidth(width).setHeight(height).setSpoiler(spoiler);
	}

	public static Post createPost(JSONObject jsonObject, KurisachChanLocator locator, String boardName)
			throws JSONException {
		Post post = new Post();
		String thread = CommonUtils.getJsonString(jsonObject, "thread");
		String id = CommonUtils.getJsonString(jsonObject, "id");
		post.setPostNumber(id);
		if (!id.equals(thread) && !"0".equals(thread)) {
			post.setParentPostNumber(thread);
		}
		post.setTimestamp((jsonObject.getLong("datetime") - 3 * 60 * 60) * 1000L);
		String subject = CommonUtils.optJsonString(jsonObject, "subject");
		if (!StringUtils.isEmpty(subject)) {
			post.setSubject(StringUtils.nullIfEmpty(StringUtils.clearHtml(subject).trim()));
		}
		String text = CommonUtils.getJsonString(jsonObject, "text");
		if (!StringUtils.isEmpty(text)) {
			// Add inline pre support, see chan markup implementation
			text = text.replaceAll("<pre class=\"inline-pp.*?>(.*?)</pre>", "<inlinepre>$1</inlinepre>");
			text = text.replace("<span class=\"cut\">Развернуть</span>", "");
			text = removePrettyprintBreaks(text);
			text = StringUtils.replaceAll(text, PATTERN_TAG, matcher ->
					matcher.group().replaceAll("=\\\\\"(.*?)\\\\\"", "=\"$1\""));
		}
		post.setComment(text);
		Attachment attachment = createFileAttachment(jsonObject, locator, boardName);
		if (attachment != null) {
			post.setAttachments(attachment);
		}

		post.setName(CommonUtils.optJsonString(jsonObject, "name"));
		String tripcode = CommonUtils.optJsonString(jsonObject, "tripcode");
		if (!StringUtils.isEmpty(tripcode)) {
			tripcode = "!" + tripcode;
		}
		post.setTripcode(tripcode);
		String email = CommonUtils.optJsonString(jsonObject, "email");
		if (email.toLowerCase(Locale.US).equals("sage")) {
			post.setSage(true);
		} else {
			post.setEmail(email);
		}
		return post;
	}

	private static String removePrettyprintBreaks(String string) {
		// brs inside pre.prettyprint has "display: none" style
		// Also br after pre will get "display: none" with javascript
		// Dashchan doesn't handle css styles and js, so hide these tags manually
		StringBuilder builder = new StringBuilder(string);
		int from = 0;
		while (true) {
			int index1 = builder.indexOf("<pre class=\"prettyprint\"", from);
			int index2 = builder.indexOf("</pre>", from);
			if (index2 > index1 && index1 >= 0) {
				while (true) {
					int brIndex = builder.indexOf("<br", index1 + 1);
					if (brIndex > index1) {
						int brEndIndex = builder.indexOf(">", brIndex) + 1;
						builder.delete(brIndex, brEndIndex);
						if (brIndex >= index2) {
							break;
						}
						index2 -= brEndIndex - brIndex;
					} else {
						break;
					}
				}
				from = index2 + 6;
				if (from >= builder.length()) {
					break;
				}
			} else {
				break;
			}
		}
		return builder.toString();
	}

	public static ArrayList<Post> createPosts(JSONObject jsonObject, KurisachChanLocator locator, String boardName)
			throws JSONException {
		if (jsonObject.length() > 0) {
			ArrayList<Post> posts = new ArrayList<>();
			for (Iterator<String> keys = jsonObject.keys(); keys.hasNext();) {
				posts.add(createPost(jsonObject.getJSONObject(keys.next()), locator, boardName));
			}
			Collections.sort(posts);
			return posts;
		}
		return null;
	}

	public static Posts createThread(JSONObject jsonObject, KurisachChanLocator locator, String boardName)
			throws JSONException {
		int postsCount = jsonObject.optInt("numreplies") + 1;
		int postsWithFilesCount = jsonObject.optInt("numpicreplies");
		ArrayList<Post> posts = new ArrayList<>();
		posts.add(createPost(jsonObject.getJSONObject("op"), locator, boardName));
		jsonObject = jsonObject.optJSONObject("lastreplies");
		if (jsonObject != null && jsonObject.length() > 0) {
			for (Iterator<String> keys = jsonObject.keys(); keys.hasNext();) {
				posts.add(createPost(jsonObject.getJSONObject(keys.next()), locator, boardName));
			}
		}
		Collections.sort(posts);
		if (posts.size() > 0 && posts.get(0).getAttachmentsCount() > 0) {
			postsWithFilesCount++;
		}
		return new Posts(posts).addPostsCount(postsCount).addPostsWithFilesCount(postsWithFilesCount);
	}
}