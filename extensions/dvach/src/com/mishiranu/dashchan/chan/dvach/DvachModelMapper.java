package com.mishiranu.dashchan.chan.dvach;

import android.net.Uri;
import chan.content.model.Attachment;
import chan.content.model.EmbeddedAttachment;
import chan.content.model.FileAttachment;
import chan.content.model.Icon;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.content.model.ThreadSummary;
import chan.text.JsonSerial;
import chan.text.ParseException;
import chan.util.StringUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DvachModelMapper {
	private static final Pattern PATTERN_BADGE = Pattern.compile("<img.+?src=\"(.+?)\".+?(?:title=\"(.+?)\")?.+?/?>");
	private static final Pattern PATTERN_CODE = Pattern.compile("\\[code(?:\\s+lang=.+?)?](?:<br ?/?>)*(.+?)" +
			"(?:<br ?/?>)*\\[/code]", Pattern.CASE_INSENSITIVE);
	private static final Pattern PATTERN_HASHLINK = Pattern.compile("<a [^<>]*class=\"hashlink\"[^<>]*>");
	private static final Pattern PATTERN_HASHLINK_TITLE = Pattern.compile("title=\"(.*?)\"");

	public static class Extra {
		public String tags;

		private ArrayList<Post> posts;
		private boolean hasPosts;
		private int postsCount;
		private int postsWithFilesCount;
	}

	public static class BoardConfiguration {
		public String title;
		public String description;
		public String defaultName;
		public int bumpLimit;
		public int maxCommentLength;
		public int pagesCount;
		public String icons;

		public Boolean imagesEnabled;
		public Boolean namesEnabled;
		public Boolean tripcodesEnabled;
		public Boolean subjectsEnabled;
		public Boolean sageEnabled;
		public Boolean flagsEnabled;

		@SuppressWarnings("BooleanMethodIsAlwaysInverted")
		public boolean handle(JsonSerial.Reader reader, String name) throws IOException, ParseException {
			switch (name) {
				case "BoardName": {
					title = reader.nextString();
					return true;
				}
				case "BoardInfoOuter": {
					description = reader.nextString();
					return true;
				}
				case "default_name": {
					defaultName = reader.nextString();
					return true;
				}
				case "bump_limit": {
					bumpLimit = reader.nextInt();
					return true;
				}
				case "max_comment": {
					maxCommentLength = reader.nextInt();
					return true;
				}
				case "pages": {
					int count = 0;
					reader.startArray();
					while (!reader.endStruct()) {
						count++;
						reader.skip();
					}
					pagesCount = count;
					return true;
				}
				case "icons": {
					try (JsonSerial.Writer writer = JsonSerial.writer()) {
						writer.startArray();
						reader.startArray();
						while (!reader.endStruct()) {
							reader.startObject();
							writer.startObject();
							while (!reader.endStruct()) {
								switch (reader.nextName()) {
									case "name": {
										writer.name("name");
										writer.value(reader.nextString());
										break;
									}
									case "num": {
										writer.name("num");
										writer.value(reader.nextString());
										break;
									}
									default: {
										reader.skip();
										break;
									}
								}
							}
							writer.endObject();
						}
						writer.endArray();
						icons = new String(writer.build());
					}
					return true;
				}
				case "enable_images": {
					imagesEnabled = reader.nextBoolean();
					return true;
				}
				case "enable_names": {
					namesEnabled = reader.nextBoolean();
					return true;
				}
				case "enable_trips": {
					tripcodesEnabled = reader.nextBoolean();
					return true;
				}
				case "enable_subject": {
					subjectsEnabled = reader.nextBoolean();
					return true;
				}
				case "enable_sage": {
					sageEnabled = reader.nextBoolean();
					return true;
				}
				case "enable_flags": {
					flagsEnabled = reader.nextBoolean();
					return true;
				}
				default: {
					return false;
				}
			}
		}
	}

	private static String fixAttachmentPath(String boardName, String path) {
		if (!StringUtils.isEmpty(path)) {
			if (!path.startsWith("/")) {
				path = "/" + path;
			}
			if (path.startsWith("/src/") || path.startsWith("/thumb/")) {
				path = "/" + boardName + path;
			}
			return path;
		} else {
			return null;
		}
	}

	private static FileAttachment createFileAttachment(JsonSerial.Reader reader, DvachChanLocator locator,
			String boardName, String archiveDate) throws IOException, ParseException {
		FileAttachment fileAttachment = new FileAttachment();
		reader.startObject();
		while (!reader.endStruct()) {
			switch (reader.nextName()) {
				case "path": {
					String file = fixAttachmentPath(boardName, reader.nextString());
					Uri fileUri = file != null ? locator.buildPath(archiveDate != null
							? file.replace("/src/", "/arch/" + archiveDate + "/src/") : file) : null;
					fileAttachment.setFileUri(locator, fileUri);
					break;
				}
				case "thumbnail": {
					String thumbnail = fixAttachmentPath(boardName, reader.nextString());
					Uri thumbnailUri = thumbnail != null ? locator.buildPath(archiveDate != null
							? thumbnail.replace("/thumb/", "/arch/" + archiveDate + "/thumb/") : thumbnail) : null;
					fileAttachment.setThumbnailUri(locator, thumbnailUri);
					break;
				}
				case "fullname": {
					String originalName = StringUtils.nullIfEmpty(reader.nextString());
					fileAttachment.setOriginalName(originalName);
					break;
				}
				case "size": {
					fileAttachment.setSize(reader.nextInt() * 1024);
					break;
				}
				case "width": {
					fileAttachment.setWidth(reader.nextInt());
					break;
				}
				case "height": {
					fileAttachment.setHeight(reader.nextInt());
					break;
				}
				default: {
					reader.skip();
					break;
				}
			}
		}
		return fileAttachment;
	}

	public static Post createPost(JsonSerial.Reader reader, Object linked, String boardName,
			String archiveDate, boolean sageEnabled, Extra extra) throws IOException, ParseException {
		DvachChanLocator locator = DvachChanLocator.get(linked);
		DvachChanConfiguration configuration = DvachChanConfiguration.get(linked);
		Post post = new Post();
		String subject = null;
		String tags = null;
		String comment = null;
		String name = null;
		String tripcode = null;
		ArrayList<Icon> icons = null;

		reader.startObject();
		while (!reader.endStruct()) {
			switch (reader.nextName()) {
				case "num": {
					post.setPostNumber(reader.nextString());
					break;
				}
				case "parent": {
					String parent = reader.nextString();
					if (!"0".equals(parent)) {
						post.setParentPostNumber(parent);
					}
					break;
				}
				case "op": {
					post.setOriginalPoster(reader.nextBoolean());
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
				case "endless": {
					post.setCyclical(reader.nextBoolean());
					break;
				}
				case "banned": {
					int banned = reader.nextInt();
					if (banned == 1) {
						post.setPosterBanned(true);
					} else if (banned == 2) {
						post.setPosterWarned(true);
					}
					break;
				}
				case "timestamp": {
					post.setTimestamp(reader.nextLong() * 1000L);
					break;
				}
				case "subject": {
					subject = reader.nextString();
					if (!StringUtils.isEmpty(subject)) {
						subject = StringUtils.clearHtml(subject).trim();
					}
					break;
				}
				case "comment": {
					comment = reader.nextString();
					if (!StringUtils.isEmpty(comment)) {
						comment = comment.replace(" (OP)</a>", "</a>");
						comment = comment.replace(" \u2192</a>", "</a>");
						comment = comment.replace("&#47;", "/");
					}
					if (comment.contains("\"hashlink\"")) {
						comment = StringUtils.replaceAll(comment, PATTERN_HASHLINK, matcher -> {
							String title = null;
							Matcher matcher2 = PATTERN_HASHLINK_TITLE.matcher(matcher.group());
							if (matcher2.find()) {
								title = matcher2.group(1);
							}
							if (title != null) {
								Uri uri = locator.createCatalogSearchUri(boardName, title);
								String encodedUri = uri.toString().replace("&", "&amp;").replace("\"", "&quot;");
								return "<a href=\"" + encodedUri + "\">";
							} else {
								return matcher.group();
							}
						});
					}
					if ("pr".equals(boardName) && comment.contains("[code")) {
						comment = PATTERN_CODE.matcher(comment).replaceAll("<fakecode>$1</fakecode>");
					}
					break;
				}
				case "name": {
					name = reader.nextString();
					break;
				}
				case "trip": {
					tripcode = reader.nextString();
					break;
				}
				case "email": {
					String email = reader.nextString();
					boolean sage = sageEnabled && !StringUtils.isEmpty(email) && email.equals("mailto:sage");
					if (sage) {
						post.setSage(true);
					} else {
						post.setEmail(email);
					}
					break;
				}
				case "files": {
					ArrayList<Attachment> attachments = null;
					reader.startArray();
					while (!reader.endStruct()) {
						if (attachments == null) {
							attachments = new ArrayList<>();
						}
						attachments.add(createFileAttachment(reader, locator, boardName, archiveDate));
					}
					post.setAttachments(attachments);
					break;
				}
				case "icon": {
					Matcher matcher = PATTERN_BADGE.matcher(reader.nextString());
					while (matcher.find()) {
						String path = matcher.group(1);
						String title = matcher.group(2);
						Uri uri = locator.buildPath(path);
						if (StringUtils.isEmpty(title)) {
							title = uri.getLastPathSegment();
							title = title.substring(0, title.lastIndexOf('.'));
						}
						if (icons == null) {
							icons = new ArrayList<>();
						}
						title = StringUtils.clearHtml(title);
						icons.add(new Icon(locator, uri, title));
					}
					break;
				}
				case "tags": {
					tags = reader.nextString();
					if (extra != null) {
						extra.tags = tags;
					}
					break;
				}
				case "posts_count": {
					if (extra != null) {
						extra.postsCount = reader.nextInt();
					} else {
						reader.skip();
					}
					break;
				}
				case "files_count":
				case "images_count": {
					if (extra != null) {
						extra.postsWithFilesCount = Math.max(extra.postsWithFilesCount, reader.nextInt());
					} else {
						reader.skip();
					}
					break;
				}
				case "posts": {
					if (extra != null && extra.posts != null) {
						extra.hasPosts = true;
						reader.startArray();
						while (!reader.endStruct()) {
							extra.posts.add(createPost(reader, locator, boardName, null, sageEnabled, null));
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

		// TODO Remove this after server side fix of subjects
		if (post.getParentPostNumber() == null && subject != null) {
			String clearComment = StringUtils.clearHtml(comment).replaceAll("\\s", "");
			if (clearComment.startsWith(subject.replaceAll("\\s", ""))) {
				subject = null;
			}
		}

		if (!StringUtils.isEmpty(tags)) {
			tags = "/" + tags + "/";
			subject = StringUtils.isEmpty(subject) ? tags : subject + " " + tags;
		}
		post.setSubject(subject);
		if (comment != null && !comment.isEmpty() && post.getParentPostNumber() == null && post.isSticky()) {
			comment = comment.replace("\\r\\n", "").replace("\\t", "");
		}
		post.setComment(comment);

		String userAgentData = null;
		String identifier = null;
		if (!StringUtils.isEmpty(name)) {
			Objects.requireNonNull(name);
			int index = "s".equals(boardName) ? name.indexOf("&nbsp;<span style=\"color:rgb(164,164,164);\">") : -1;
			if (index >= 0) {
				userAgentData = name.substring(index + 44);
				name = name.substring(0, index);
			}
			name = StringUtils.clearHtml(name).trim();
			index = name.indexOf(" ID: ");
			if (index >= 0) {
				identifier = name.substring(index + 5).replaceAll(" +", " ");
				name = name.substring(0, index);
				if ("Heaven".equals(identifier)) {
					identifier = null;
					post.setSage(true);
				}
			}
		}
		String capcode = null;
		if (!StringUtils.isEmpty(tripcode)) {
			if ("!!%adm%!!".equals(tripcode)) {
				capcode = "Abu";
			} else if ("!!%mod%!!".equals(tripcode)) {
				capcode = "Mod";
			}
			if (capcode != null) {
				tripcode = null;
			} else {
				tripcode = StringUtils.nullIfEmpty(StringUtils.clearHtml(tripcode).trim());
			}
		}
		post.setName(name);
		post.setIdentifier(identifier);
		post.setTripcode(tripcode);
		post.setCapcode(capcode);

		if (userAgentData != null) {
			int index1 = userAgentData.indexOf('(');
			int index2 = userAgentData.indexOf(')');
			if (index2 > index1 && index1 >= 0) {
				userAgentData = userAgentData.substring(index1 + 1, index2);
				int index = userAgentData.indexOf(':');
				if (index >= 0) {
					String os = StringUtils.clearHtml(userAgentData.substring(0, index));
					String browser = StringUtils.clearHtml(userAgentData.substring(index + 2));
					if (!"Неизвестно".equals(os)) {
						int osIconResId = R.raw.raw_os;
						if (os.contains("Windows")) {
							osIconResId = R.raw.raw_os_windows;
						} else if (os.contains("Linux")) {
							osIconResId = R.raw.raw_os_linux;
						} else if (os.contains("Apple")) {
							osIconResId = R.raw.raw_os_apple;
						} else if (os.contains("Android")) {
							osIconResId = R.raw.raw_os_android;
						}
						if (icons == null) {
							icons = new ArrayList<>();
						}
						icons.add(new Icon(locator, configuration.getResourceUri(osIconResId), os));
					}
					if (!"Неизвестно".equals(browser)) {
						int browserIconResId = R.raw.raw_browser;
						if (browser.contains("Chrom")) {
							browserIconResId = R.raw.raw_browser_chrome;
						} else if (browser.contains("Microsoft Edge")) {
							browserIconResId = R.raw.raw_browser_edge;
						} else if (browser.contains("Internet Explorer")) {
							browserIconResId = R.raw.raw_browser_edge;
						} else if (browser.contains("Firefox")) {
							browserIconResId = R.raw.raw_browser_firefox;
						} else if (browser.contains("Iceweasel")) {
							browserIconResId = R.raw.raw_browser_firefox;
						} else if (browser.contains("Opera")) {
							browserIconResId = R.raw.raw_browser_opera;
						} else if (browser.contains("Safari")) {
							browserIconResId = R.raw.raw_browser_safari;
						}
						if (icons == null) {
							icons = new ArrayList<>();
						}
						icons.add(new Icon(locator, configuration.getResourceUri(browserIconResId), browser));
					}
				}
			}
		}
		post.setIcons(icons);
		return post;
	}

	public static ArrayList<Post> createPosts(JsonSerial.Reader reader, Object linked, String boardName,
			String archiveDate, boolean sageEnabled, Extra extra) throws IOException, ParseException {
		boolean firstPost = true;
		ArrayList<Post> posts = new ArrayList<>();
		reader.startArray();
		while (!reader.endStruct()) {
			posts.add(createPost(reader, linked, boardName, archiveDate, sageEnabled, firstPost ? extra : null));
			firstPost = false;
		}
		if (archiveDate != null && !posts.isEmpty()) {
			posts.get(0).setArchived(true);
		}
		return posts;
	}

	public static Posts createThread(JsonSerial.Reader reader, Object linked, String boardName,
			boolean sageEnabled) throws IOException, ParseException {
		Extra extra = new Extra();
		extra.posts = new ArrayList<>();
		Post post = createPost(reader, linked, boardName, null, sageEnabled, extra);
		// Different data format for thread lists and catalog
		List<Post> posts = extra.hasPosts ? extra.posts : Collections.singletonList(post);
		if (!posts.isEmpty() && posts.get(0).getAttachmentsCount() > 0) {
			extra.postsWithFilesCount++;
		}
		extra.postsCount += posts.size();
		return new Posts(posts).addPostsCount(extra.postsCount).addPostsWithFilesCount(extra.postsWithFilesCount);
	}

	public static Post createWakabaArchivePost(JsonSerial.Reader reader,
			Object linked, String boardName) throws IOException, ParseException {
		DvachChanLocator locator = DvachChanLocator.get(linked);
		Post post = new Post();
		post.setArchived(true);
		String image = null;
		String thumbnail = null;
		int width = 0;
		int height = 0;
		int size = 0;
		String video = null;
		reader.startObject();
		while (!reader.endStruct()) {
			switch (reader.nextName()) {
				case "num": {
					post.setPostNumber(reader.nextString());
					break;
				}
				case "parent": {
					String parent = reader.nextString();
					if (!"0".equals(parent)) {
						post.setParentPostNumber(parent);
					}
					break;
				}
				case "op": {
					post.setOriginalPoster(reader.nextBoolean());
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
				case "banned": {
					int banned = reader.nextInt();
					if (banned == 1) {
						post.setPosterBanned(true);
					} else if (banned == 2) {
						post.setPosterWarned(true);
					}
					break;
				}
				case "comment": {
					post.setComment(reader.nextString());
					break;
				}
				case "name": {
					String name = reader.nextString();
					if (!StringUtils.isEmpty(name)) {
						post.setName(StringUtils.nullIfEmpty(StringUtils.clearHtml(name).trim()));
					}
					break;
				}
				case "subject": {
					String subject = reader.nextString();
					if (!StringUtils.isEmpty(subject)) {
						post.setSubject(StringUtils.nullIfEmpty(StringUtils.clearHtml(subject).trim()));
					}
					break;
				}
				case "timestamp": {
					post.setTimestamp(reader.nextLong() * 1000L);
					break;
				}
				case "image": {
					image = reader.nextString();
					break;
				}
				case "thumbnail": {
					thumbnail = reader.nextString();
					break;
				}
				case "width": {
					width = reader.nextInt();
					break;
				}
				case "height": {
					height = reader.nextInt();
					break;
				}
				case "size": {
					size = reader.nextInt() * 1024;
					break;
				}
				case "video": {
					video = reader.nextString();
					break;
				}
				default: {
					reader.skip();
					break;
				}
			}
		}
		ArrayList<Attachment> attachments = null;
		if (!StringUtils.isEmpty(image)) {
			FileAttachment attachment = new FileAttachment();
			attachment.setFileUri(locator, locator.buildPath(boardName, "arch", "wakaba", image));
			if (!StringUtils.isEmpty(thumbnail)) {
				attachment.setThumbnailUri(locator, locator.buildPath(boardName, "arch", "wakaba", thumbnail));
			}
			attachment.setWidth(width);
			attachment.setHeight(height);
			attachment.setSize(size);
			attachments = new ArrayList<>();
			attachments.add(attachment);
		}
		if (!StringUtils.isEmpty(video)) {
			EmbeddedAttachment attachment = EmbeddedAttachment.obtain(video);
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

	public static List<ThreadSummary> createArchive(JsonSerial.Reader reader, String boardName)
			throws IOException, ParseException {
		ArrayList<ThreadSummary> threadSummaries = new ArrayList<>();
		reader.startArray();
		while (!reader.endStruct()) {
			String threadNumber = null;
			String subject = null;
			reader.startObject();
			while (!reader.endStruct()) {
				switch (reader.nextName()) {
					case "thread": {
						threadNumber = reader.nextString();
						break;
					}
					case "subject": {
						subject = StringUtils.clearHtml(reader.nextString()).trim();
						break;
					}
					default: {
						reader.skip();
						break;
					}
				}
			}
			if (threadNumber != null) {
				if (StringUtils.isEmpty(subject) || "Нет темы".equals(subject)) {
					subject = "#" + threadNumber;
				}
				threadSummaries.add(new ThreadSummary(boardName, threadNumber, subject));
			}
		}
		return threadSummaries;
	}
}
