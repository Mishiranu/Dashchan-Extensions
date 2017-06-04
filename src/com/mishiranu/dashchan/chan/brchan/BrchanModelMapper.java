package com.mishiranu.dashchan.chan.brchan;

import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;

import chan.content.model.EmbeddedAttachment;
import chan.content.model.FileAttachment;
import chan.content.model.Icon;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.util.CommonUtils;
import chan.util.StringUtils;

public class BrchanModelMapper {
	public static FileAttachment createFileAttachment(JSONObject jsonObject, BrchanChanLocator locator,
			String boardName, long time) throws JSONException {
		FileAttachment attachment = new FileAttachment();
		String tim = CommonUtils.getJsonString(jsonObject, "tim");
		if (StringUtils.isEmpty(tim)) {
			return null;
		}
		String filename = CommonUtils.getJsonString(jsonObject, "filename");
		String ext = CommonUtils.getJsonString(jsonObject, "ext");
		attachment.setSize(jsonObject.optInt("fsize"));
		attachment.setWidth(jsonObject.optInt("w"));
		attachment.setHeight(jsonObject.optInt("h"));
		attachment.setFileUri(locator, locator.buildPath(boardName, "src", tim + ext));
		if (time >= 1475971200000L) {
			// first png 08/10/16 22:53:13 (1475974393000)
			attachment.setThumbnailUri(locator, locator.buildPath(boardName, "thumb",
					tim + (locator.isImageExtension(ext) ? ".png" : ".jpg")));
		} else {
			attachment.setThumbnailUri(locator, locator.buildPath(boardName, "thumb",
					tim + (locator.isImageExtension(ext) ? ext : ".jpg")));
		}
		attachment.setOriginalName(filename);
		return attachment;
	}

	private static final Pattern PATTERN_LINK = Pattern.compile("<a .*?href=\"(.*?)\".*?>");

	public static Post createPost(JSONObject jsonObject, BrchanChanLocator locator, String boardName)
			throws JSONException {
		Post post = new Post();
		if (jsonObject.optInt("sticky") != 0) {
			post.setSticky(true);
		}
		if (jsonObject.optInt("locked") != 0) {
			post.setClosed(true);
		}
		if (jsonObject.optInt("cyclical") != 0) {
			post.setCyclical(true);
		}
		String no = CommonUtils.getJsonString(jsonObject, "no");
		String resto = CommonUtils.getJsonString(jsonObject, "resto");
		post.setPostNumber(no);
		if (!"0".equals(resto)) {
			post.setParentPostNumber(resto);
		}
		long time = jsonObject.getLong("time") * 1000L - 60 * 60 * 1000;
		post.setTimestamp(time);
		String name = CommonUtils.optJsonString(jsonObject, "name");
		if (name != null) {
			name = StringUtils.nullIfEmpty(StringUtils.clearHtml(name).trim());
			post.setName(name);
		}
		post.setTripcode(CommonUtils.optJsonString(jsonObject, "trip"));
		post.setIdentifier(CommonUtils.optJsonString(jsonObject, "id"));
		post.setCapcode(CommonUtils.optJsonString(jsonObject, "capcode"));
		String email = CommonUtils.optJsonString(jsonObject, "email");
		if (email != null) {
			if (email.equalsIgnoreCase("sage")) {
				post.setSage(true);
			} else {
				post.setEmail(StringUtils.nullIfEmpty(StringUtils.clearHtml(email).trim()));
			}
		}
		String country = CommonUtils.optJsonString(jsonObject, "country");
		String countryName = CommonUtils.optJsonString(jsonObject, "country_name");
		if (country != null) {
			Uri uri = locator.buildPath("static", "flags", country.toLowerCase(Locale.US) + ".png");
			String title = countryName == null ? country.toUpperCase(Locale.US) : countryName;
			post.setIcons(new Icon(locator, uri, title));
		}
		String sub = CommonUtils.optJsonString(jsonObject, "sub");
		if (sub != null) {
			sub = StringUtils.nullIfEmpty(StringUtils.clearHtml(sub).trim());
			post.setSubject(sub);
		}
		String com = CommonUtils.optJsonString(jsonObject, "com");
		if (com != null) {
			// Vichan JSON API bug, sometimes comment is broken
			com = com.replace("<a  ", "<a ").replaceAll("href=\"\\?", "href=\"");
			com = StringUtils.replaceAll(com, PATTERN_LINK, matcher -> {
				String uriString = matcher.group(1);
				Uri uri = Uri.parse(StringUtils.clearHtml(uriString));
				if ("privatelink.de".equals(uri.getAuthority())) {
					String query = uri.getQuery();
					if (!StringUtils.isEmpty(query)) {
						uriString = query.replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
					}
				}
				return "<a href=\"" + uriString + "\">";
			});
			post.setComment(com);
		}
		String embed = StringUtils.nullIfEmpty(CommonUtils.optJsonString(jsonObject, "embed"));
		if (embed != null) {
			EmbeddedAttachment attachment = EmbeddedAttachment.obtain(embed);
			if (attachment != null) {
				post.setAttachments(attachment);
			}
		} else {
			try {
				ArrayList<FileAttachment> attachments = new ArrayList<>();
				FileAttachment attachment = createFileAttachment(jsonObject, locator, boardName, time);
				if (attachment != null) {
					attachments.add(attachment);
				}
				JSONArray filesArray = jsonObject.optJSONArray("extra_files");
				if (filesArray != null) {
					for (int i = 0; i < filesArray.length(); i++) {
						JSONObject fileObject = filesArray.getJSONObject(i);
						attachment = createFileAttachment(fileObject, locator, boardName, time);
						if (attachment != null) {
							attachments.add(attachment);
						}
					}
				}
				if (attachments.size() > 0) {
					post.setAttachments(attachments);
				}
			} catch (JSONException e) {
				// Ignore exception
			}
		}
		return post;
	}

	public static Posts createThread(JSONObject jsonObject, BrchanChanLocator locator, String boardName,
			boolean fromCatalog) throws JSONException {
		Post[] posts;
		int postsCount = 0;
		int filesCount = 0;
		if (fromCatalog) {
			Post post = createPost(jsonObject, locator, boardName);
			postsCount = jsonObject.getInt("replies") + 1;
			filesCount = jsonObject.getInt("omitted_images") + jsonObject.getInt("images");
			filesCount += post.getAttachmentsCount();
			posts = new Post[] {post};
		} else {
			JSONArray jsonArray = jsonObject.getJSONArray("posts");
			posts = new Post[jsonArray.length()];
			for (int i = 0; i < posts.length; i++) {
				jsonObject = jsonArray.getJSONObject(i);
				posts[i] = createPost(jsonObject, locator, boardName);
				if (i == 0) {
					postsCount = jsonObject.getInt("replies") + 1;
					filesCount = jsonObject.getInt("omitted_images") + jsonObject.getInt("images");
					filesCount += posts[0].getAttachmentsCount();
				}
			}
		}
		return new Posts(posts).addPostsCount(postsCount).addFilesCount(filesCount);
	}
}
