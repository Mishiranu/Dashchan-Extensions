package com.mishiranu.dashchan.chan.fourchan;

import android.net.Uri;
import chan.content.model.FileAttachment;
import chan.content.model.Icon;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.text.JsonSerial;
import chan.text.ParseException;
import chan.util.CommonUtils;
import chan.util.StringUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Pattern;

public class FourchanModelMapper {
	private static final Pattern PATTERN_MATH = Pattern.compile("\\[(math|eqn)](.*?)\\[/\\1]");

	public static class Extra {
		public int uniquePosters;

		private ArrayList<Post> lastReplies;
		private int replies;
		private int images;
	}

	public static Post createPost(JsonSerial.Reader reader, FourchanChanLocator locator,
			String boardName, boolean handleMathTags, Extra extra) throws IOException, ParseException {
		Post post = new Post();
		boolean trollFlag = false;
		String country = null;
		String countryName = null;
		String tim = null;
		String filename = null;
		String ext = null;
		int size = -1;
		int width = 0;
		int height = 0;

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
					} else if ("developer".equals(capcode)) {
						post.setCapcode("Developer");
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
					StringBuilder builder = new StringBuilder(reader.nextString());
					while (true) {
						int start = builder.indexOf("<wbr");
						if (start < 0) {
							break;
						}
						int end = builder.indexOf(">", start) + 1;
						if (end > start) {
							builder.delete(start, end);
						} else {
							break;
						}
					}
					int exifAbbr = builder.indexOf("<span class=\"abbr\">[EXIF data available. Click");
					if (exifAbbr >= 0) {
						builder.setLength(exifAbbr);
					}
					String com = StringUtils.linkify(builder.toString());
					if (handleMathTags && (com.contains("[math]") || com.contains("[eqn]"))) {
						com = StringUtils.replaceAll(com, PATTERN_MATH, matcher -> "<a href=\"" +
								locator.buildMathUri(StringUtils.clearHtml(matcher.group(2))).toString()
										.replaceAll("\"", "&quot;") + "\">" + StringUtils.clearHtml(matcher.group(2))
								.replaceAll("<", "&lt;").replaceAll(">", "&gt;") + "</a>");
					}
					post.setComment(com);
					break;
				}
				case "country": {
					country = reader.nextString();
					break;
				}
				case "troll_country": {
					country = reader.nextString();
					trollFlag = true;
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
				case "unique_ips": {
					if (extra != null) {
						extra.uniquePosters = reader.nextInt();
					} else {
						reader.skip();
					}
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
				case "last_replies": {
					if (extra != null && extra.lastReplies != null) {
						reader.startArray();
						while (!reader.endStruct()) {
							extra.lastReplies.add(createPost(reader, locator, boardName, handleMathTags, null));
						}
					} else {
						reader.skip();
					}
					break;
				}
				default: {
					reader.skip();
					break;
				}
			}
		}

		if (CommonUtils.equals(post.getIdentifier(), post.getCapcode())) {
			post.setIdentifier(null);
		}
		if (country != null) {
			Uri uri = locator.createIconUri(country, trollFlag);
			String title = countryName == null ? country.toUpperCase(Locale.US) : countryName;
			post.setIcons(new Icon(locator, uri, title));
		}

		if (tim != null && size >= 0) {
			FileAttachment attachment = new FileAttachment();
			attachment.setSize(size);
			attachment.setWidth(width);
			attachment.setHeight(height);
			attachment.setFileUri(locator, locator.buildAttachmentPath(boardName, tim + ext));
			attachment.setThumbnailUri(locator, locator.buildAttachmentPath(boardName, tim + "s.jpg"));
			attachment.setOriginalName(filename);
			post.setAttachments(attachment);
		}
		return post;
	}

	@SuppressWarnings("SwitchStatementWithTooFewBranches")
	public static Posts createThread(JsonSerial.Reader reader, FourchanChanLocator locator, String boardName,
			boolean handleMathTags, boolean fromCatalog) throws IOException, ParseException {
		ArrayList<Post> posts = new ArrayList<>();
		int postsCount = 0;
		int postsWithFilesCount = 0;
		if (fromCatalog) {
			Extra extra = new Extra();
			extra.lastReplies = new ArrayList<>();
			Post originalPost = createPost(reader, locator, boardName, handleMathTags, extra);
			postsCount = extra.replies + 1;
			postsWithFilesCount = extra.images + originalPost.getAttachmentsCount();
			posts.add(originalPost);
			posts.addAll(extra.lastReplies);
		} else {
			reader.startObject();
			while (!reader.endStruct()) {
				switch (reader.nextName()) {
					case "posts": {
						Extra extra = new Extra();
						reader.startArray();
						while (!reader.endStruct()) {
							Post post = createPost(reader, locator, boardName, handleMathTags, extra);
							posts.add(post);
							if (extra != null) {
								postsCount = extra.replies + 1;
								postsWithFilesCount = extra.images + post.getAttachmentsCount();
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
		return new Posts(posts).addPostsCount(postsCount).addPostsWithFilesCount(postsWithFilesCount);
	}
}
