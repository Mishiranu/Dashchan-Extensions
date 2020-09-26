package com.mishiranu.dashchan.chan.diochan;

import android.text.Html;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import chan.content.model.EmbeddedAttachment;
import chan.content.model.FileAttachment;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.util.CommonUtils;
import chan.util.StringUtils;

public class DiochanModelMapper {
	public static FileAttachment createFileAttachment(JSONObject jsonObject, DiochanChanLocator locator,
			String boardName) throws JSONException {
		FileAttachment attachment = new FileAttachment();
		String tim = CommonUtils.getJsonString(jsonObject, "tim");
		String filename = CommonUtils.getJsonString(jsonObject, "filename");
		String ext = CommonUtils.getJsonString(jsonObject, "ext");
		String thumbnailExt = ".mp4".equalsIgnoreCase(ext.toLowerCase()) || ".webm".equalsIgnoreCase(ext.toLowerCase()) ? ".jpg" : ".png";
		/*
			Temporary fix for .png attachments with .webp thumbnails.
		 */
		if(".png".equalsIgnoreCase(ext.toLowerCase())){
			HttpURLConnection urlConnection = null;
			try {
				URL url = new URL(locator.buildPath(boardName, "thumb", tim + ".webp").toString());
				urlConnection = (HttpURLConnection) url.openConnection();
				urlConnection.setRequestProperty("connection", "close");
				urlConnection.setRequestMethod("HEAD");
				urlConnection.getInputStream().close();
				if(200 == urlConnection.getResponseCode()){
					thumbnailExt = ".webp";
				}
			} catch (MalformedURLException e) {
				// Ignore exception
			} catch (IOException e) {
				// Ignore exception
			} finally {
				if (urlConnection != null) {
					urlConnection.disconnect();
				}
			}
		}
		attachment.setSize(jsonObject.optInt("fsize"));
		attachment.setWidth(jsonObject.optInt("w"));
		attachment.setHeight(jsonObject.optInt("h"));
		attachment.setFileUri(locator, locator.buildPath(boardName, "src", tim + ext));
		attachment.setThumbnailUri(locator, locator.buildPath(boardName, "thumb", tim + thumbnailExt));
		attachment.setOriginalName(filename);
		return attachment;
	}

	public static Post createPost(JSONObject jsonObject, DiochanChanLocator locator, String boardName)
			throws JSONException {
		Post post = new Post();
		if (jsonObject.optInt("sticky") != 0) {
			post.setSticky(true);
		}
		if (jsonObject.optInt("locked") != 0) {
			post.setClosed(true);
		}
		String no = CommonUtils.getJsonString(jsonObject, "no");
		String resto = CommonUtils.getJsonString(jsonObject, "resto");
		post.setPostNumber(no);
		if (!"0".equals(resto)) {
			post.setParentPostNumber(resto);
		}
		post.setTimestamp(jsonObject.getLong("time") * 1000L);
		String name = CommonUtils.optJsonString(jsonObject, "name");
		if (name != null) {
			name = StringUtils.nullIfEmpty(StringUtils.clearHtml(name).trim());
			post.setName(name);
		}
		post.setTripcode(CommonUtils.optJsonString(jsonObject, "trip"));
		post.setIdentifier(CommonUtils.optJsonString(jsonObject, "id"));
		post.setCapcode(CommonUtils.optJsonString(jsonObject, "capcode"));
		String email = CommonUtils.optJsonString(jsonObject, "email");
		if (!StringUtils.isEmpty(email) && (email.equalsIgnoreCase("sage") || email.equalsIgnoreCase("salvia"))) {
			post.setSage(true);
		} else {
			post.setEmail(email);
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
			post.setComment(com);

			while(com.toLowerCase().contains("<span class=\"public_")){
				int spanStartIndex = com.toLowerCase().indexOf("<span class=\"public_");
				int spanEndIndex = com.toLowerCase().indexOf("</span>", spanStartIndex) + "</span>".length();
				String span = com.substring(spanStartIndex, spanEndIndex);
				boolean isWarning = span.toLowerCase().startsWith("<span class=\"public_warning");
				boolean isBan = span.toLowerCase().startsWith("<span class=\"public_ban");
				if(isWarning){
					post.setPosterWarned(true);
				} else if(isBan){
					post.setPosterBanned(true);
				}
				String message = Html.fromHtml(span).toString();
				com = com.replace(span, "<br/><br/><b>" + message + "</b>");
			}
			post.setComment(com);
		}
		try {
			if(jsonObject.has("embed")){
				String embed = CommonUtils.getJsonString(jsonObject, "embed");
				String videoUrl = "";
				if(embed.toLowerCase().contains("www.youtube")){
					int youtubeVideoIdLength = 11;
					int videoIdStartIndex = embed.indexOf("embed/") + "embed/".length();
					int videoIdEndIndex = videoIdStartIndex + youtubeVideoIdLength;
					videoUrl = "https://www.youtube.com/watch?v=" + embed.substring(videoIdStartIndex, videoIdEndIndex);
				}
				if(embed.toLowerCase().contains("vimeo.com")){
					int videoUrlStartIndex = embed.indexOf("https://player.vimeo.com");
					int videoUrlEndIndex = embed.indexOf("\"", videoUrlStartIndex);
					videoUrl = embed.substring(videoUrlStartIndex, videoUrlEndIndex);
				}
				if(embed.toLowerCase().contains("soundcloud.com")){
					String modified = embed.replace("soundcloud.com/oembed", "dummy");
					int videoUrlStartIndex = modified.indexOf("\"url\": \"https://soundcloud.com/") + "\"url\": \"".length();
					int videoUrlEndIndex = modified.indexOf("\"", videoUrlStartIndex);
					videoUrl = modified.substring(videoUrlStartIndex, videoUrlEndIndex);
				}
				if(!StringUtils.isEmpty(videoUrl)){
					EmbeddedAttachment attachment = EmbeddedAttachment.obtain(videoUrl);
					post.setAttachments(attachment);
				}
			} else {
				ArrayList<FileAttachment> attachments = new ArrayList<>();
				attachments.add(createFileAttachment(jsonObject, locator, boardName));
				JSONArray filesArray = jsonObject.optJSONArray("extra_files");
				if (filesArray != null) {
					for (int i = 0; i < filesArray.length(); i++) {
						JSONObject fileObject = filesArray.getJSONObject(i);
						FileAttachment attachment = createFileAttachment(fileObject, locator, boardName);
						attachments.add(attachment);
					}
				}
				post.setAttachments(attachments);
			}
		} catch (JSONException e) {
			// Ignore exception
		}
		return post;
	}

	public static Posts createThread(JSONObject jsonObject, DiochanChanLocator locator, String boardName,
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