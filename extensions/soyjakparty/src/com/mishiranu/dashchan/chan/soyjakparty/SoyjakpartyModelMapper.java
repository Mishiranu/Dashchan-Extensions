package com.mishiranu.dashchan.chan.soyjakparty;

import android.net.Uri;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import chan.content.model.Attachment;
import chan.content.model.FileAttachment;
import chan.content.model.Icon;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.text.JsonSerial;
import chan.text.ParseException;
import chan.util.CommonUtils;
import chan.util.StringUtils;

public class SoyjakpartyModelMapper {
	public static class Extra {
		private int replies;
		private int images;
	}

	public static FileAttachment createFileAttachment(SoyjakpartyChanLocator locator, String boardName,
													  String tim, String ext, String filename,
													  int size, int width, int height) {
		FileAttachment attachment = new FileAttachment();
		if (ext.equals("deleted")) {
			attachment.setSize(2733);
			attachment.setWidth(140);
			attachment.setHeight(50);
			attachment.setFileUri(locator, locator.buildPath("static", "deleted.png"));
			attachment.setThumbnailUri(locator, locator.buildPath("static", "deleted.png"));
			attachment.setOriginalName("deleted.png");
		} else {
			attachment.setSize(size);
			attachment.setWidth(width);
			attachment.setHeight(height);
			String thumbnailFile;
			switch (ext) {
				case ".mp4":
				case ".webm": {
					thumbnailFile = tim + ".jpg";
					break;
				}
				case ".pdf": {
					thumbnailFile = "pdf.png";
					break;
				}
				default: {
					thumbnailFile = tim + ext;
					break;
				}
			}
			attachment.setFileUri(locator, locator.buildPath(boardName, "src", tim + ext));
			attachment.setThumbnailUri(locator, locator.buildPath(boardName, "thumb", thumbnailFile));
			attachment.setOriginalName(filename);
		}
		return attachment;
	}

	public static FileAttachment parseExtraFile(JsonSerial.Reader reader, SoyjakpartyChanLocator locator,
												String boardName) throws IOException, ParseException {
		String tim = null;
		String ext = null;
		String filename = null;
		int size = -1;
		int width = 0;
		int height = 0;
		reader.startObject();
		while (!reader.endStruct()) {
			switch (reader.nextName()) {
				case "tim": {
					tim = reader.nextString();
					break;
				}
				case "filename": {
					filename = StringUtils.clearHtml(reader.nextString());
					break;
				}
				case "ext": {
					ext = reader.nextString();
					break;
				}
				case "fsize": {
					size = reader.nextInt();
					break;
				}
				case "w": {
					width = reader.nextInt();
					break;
				}
				case "h": {
					height = reader.nextInt();
					break;
				}
				default: {
					reader.skip();
					break;
				}
			}
		}
		return createFileAttachment(locator, boardName, tim, ext, filename, size, width, height);
	}

	public static Post createPost(JsonSerial.Reader reader, SoyjakpartyChanLocator locator, String boardName, Extra extra)
			throws IOException, ParseException {
		Post post = new Post();
		String country = null;
		String countryName = null;
		String tim = null;
		String filename = null;
		String ext = null;
		int size = -1;
		int width = 0;
		int height = 0;
		ArrayList<Attachment> attachments = new ArrayList<>();

		reader.startObject();
		while (!reader.endStruct()) {
			switch (reader.nextName()) {
				case "no": {
					post.setPostNumber(reader.nextString());
					break;
				}
				case "resto": {
					String resto = reader.nextString();
					if (!"0".equals(resto)) {
						post.setParentPostNumber(resto);
					}
					break;
				}
				case "time": {
					post.setTimestamp(reader.nextLong() * 1000L);
					break;
				}
				case "sticky": {
					post.setSticky(reader.nextBoolean());
					break;
				}
				case "closed": {
					post.setClosed(reader.nextBoolean());
					break;
				}
				case "archived": {
					post.setArchived(reader.nextBoolean());
					break;
				}
				case "name": {
					post.setName(StringUtils.clearHtml(reader.nextString()).trim());
					break;
				}
				case "trip": {
					post.setTripcode(reader.nextString());
					break;
				}
				case "email": {
					String email = reader.nextString();
					if (email.equals("sage")) {
						post.setSage(true);
					} else {
						post.setEmail(email);
					}
					break;
				}
				case "id": {
					post.setIdentifier(reader.nextString());
					break;
				}
				case "capcode": {
					String capcode = reader.nextString();
					if ("admin".equals(capcode) || "admin_highlight".equals(capcode)) {
						post.setCapcode("Admin");
					} else if ("mod".equals(capcode)) {
						post.setCapcode("Mod");
					} else if (!"none".equals(capcode)) {
						post.setCapcode(capcode);
					}
					break;
				}
				case "sub": {
					post.setSubject(StringUtils.clearHtml(reader.nextString()).trim());
					break;
				}
				case "com": {
					post.setComment(reader.nextString());
					break;
				}
				case "country": {
					country = reader.nextString();
					break;
				}
				case "country_name": {
					countryName = reader.nextString();
					break;
				}
				case "tim": {
					tim = reader.nextString();
					break;
				}
				case "filename": {
					filename = StringUtils.clearHtml(reader.nextString());
					break;
				}
				case "ext": {
					ext = reader.nextString();
					break;
				}
				case "fsize": {
					size = reader.nextInt();
					break;
				}
				case "w": {
					width = reader.nextInt();
					break;
				}
				case "h": {
					height = reader.nextInt();
					break;
				}
				case "replies": {
					if (extra != null) {
						extra.replies = reader.nextInt();
					} else {
						reader.skip();
					}
					break;
				}
				case "images": {
					if (extra != null) {
						extra.images = reader.nextInt();
					} else {
						reader.skip();
					}
					break;
				}
				case "extra_files": {
					reader.startArray();
					while (!reader.endStruct()) {
						attachments.add(parseExtraFile(reader, locator, boardName));
					}
					break;
				}
				default: {
					reader.skip();
					break;
				}
			}
		}
		if (tim != null && size >= 0) {
			attachments.add(0, createFileAttachment(locator, boardName, tim, ext, filename, size, width, height));
			post.setAttachments(attachments);
		}
		if (CommonUtils.equals(post.getIdentifier(), post.getCapcode())) {
			post.setIdentifier(null);
		}
		if (country != null) {
			ArrayList<Icon> icons = new ArrayList<>(1);
			Uri uri = locator.createCountryIconUri(country);
			String title = countryName == null ? country.toUpperCase(Locale.US) : countryName;
			icons.add(new Icon(locator, uri, title));
			post.setIcons(icons);
		}
		return post;
	}

	public static Posts createThread(JsonSerial.Reader reader, SoyjakpartyChanLocator locator, String boardName,
                                     boolean fromCatalog) throws IOException, ParseException {
		ArrayList<Post> posts = new ArrayList<>();
		int postsCount = 0;
		int filesCount = 0;
		if (fromCatalog) {
			Extra extra = new Extra();
			Post originalPost = createPost(reader, locator, boardName, extra);
			postsCount = extra.replies + 1;
			filesCount = extra.images + originalPost.getAttachmentsCount();
			posts.add(originalPost);
		} else {
			reader.startObject();
			while (!reader.endStruct()) {
				switch (reader.nextName()) {
					case "posts": {
						Extra extra = new Extra();
						reader.startArray();
						while (!reader.endStruct()) {
							Post post = createPost(reader, locator, boardName, extra);
							posts.add(post);
							if (extra != null) {
								postsCount = extra.replies + 1;
								filesCount = extra.images + post.getAttachmentsCount();
								extra = null;
							}
						}
						break;
					}
					default: {
						reader.skip();
						break;
					}
				}
			}
		}
		return new Posts(posts).addPostsCount(postsCount).addFilesCount(filesCount);
	}
}