package com.mishiranu.dashchan.chan.moechannel;

import chan.content.model.FileAttachment;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.text.JsonSerial;
import chan.text.ParseException;
import chan.util.StringUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MoechannelModelMapper {
	public static class Extra {
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

		public Boolean filesEnabled;
		public Boolean namesEnabled;
		public Boolean postingEnabled;
		public Boolean deletingEnabled;

		@SuppressWarnings("BooleanMethodIsAlwaysInverted")
		public boolean handle(JsonSerial.Reader reader, String name) throws IOException, ParseException {
			switch (name) {
				case "board_name": {
					title = reader.nextString();
					return true;
				}
				case "board_subtitle": {
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
				case "enable_files": {
					filesEnabled = reader.nextBoolean();
					return true;
				}
				case "enable_names": {
					namesEnabled = reader.nextBoolean();
					return true;
				}
				case "enable_posting": {
					postingEnabled = reader.nextBoolean();
					return true;
				}
				case "enable_delete": {
					deletingEnabled = reader.nextBoolean();
					return true;
				}
				default: {
					return false;
				}
			}
		}
	}

	private static FileAttachment createFileAttachment(JsonSerial.Reader reader, MoechannelChanLocator locator)
			throws IOException, ParseException {
		FileAttachment fileAttachment = new FileAttachment();
		reader.startObject();
		while (!reader.endStruct()) {
			switch (reader.nextName()) {
				case "path": {
					String path = reader.nextString();
					if (!StringUtils.isEmpty(path)) {
						fileAttachment.setFileUri(locator, locator.buildPath(path));
					}
					break;
				}
				case "thumbnail": {
					String thumbnail = reader.nextString();
					if (!StringUtils.isEmpty(thumbnail)) {
						fileAttachment.setThumbnailUri(locator, locator.buildPath(thumbnail));
					}
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

	public static Post createPost(JsonSerial.Reader reader, Object linked, Extra extra)
			throws IOException, ParseException {
		MoechannelChanLocator locator = MoechannelChanLocator.get(linked);
		Post post = new Post();
		String name = null;
		String tripcode = null;

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
				case "timestamp": {
					post.setTimestamp(reader.nextLong() * 1000L);
					break;
				}
				case "subject": {
					post.setSubject(StringUtils.clearHtml(reader.nextString()).trim());
					break;
				}
				case "comment": {
					post.setComment(reader.nextString().replace(" (OP)</a>", "</a>"));
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
					if ("sage".equals(email)) {
						post.setSage(true);
					} else {
						post.setEmail(email);
					}
					break;
				}
				case "files": {
					ArrayList<FileAttachment> attachments = null;
					reader.startArray();
					while (!reader.endStruct()) {
						if (attachments == null) {
							attachments = new ArrayList<>();
						}
						attachments.add(createFileAttachment(reader, locator));
					}
					post.setAttachments(attachments);
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
				case "files_count": {
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
							extra.posts.add(createPost(reader, locator, null));
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

		String capcode = null;
		if (!StringUtils.isEmpty(tripcode)) {
			if ("!!%adm%!!".equals(tripcode)) {
				capcode = "Admin";
			} else if ("!!%sys%!!".equals(tripcode)) {
				capcode = "System";
				if ("system".equals(name)) {
					name = null;
				}
			}
			if (capcode != null) {
				tripcode = null;
			} else {
				tripcode = StringUtils.nullIfEmpty(StringUtils.clearHtml(tripcode).trim());
			}
		}
		post.setName(name);
		post.setTripcode(tripcode);
		post.setCapcode(capcode);
		return post;
	}

	public static ArrayList<Post> createPosts(JsonSerial.Reader reader, Object linked, Extra extra)
			throws IOException, ParseException {
		boolean firstPost = true;
		ArrayList<Post> posts = new ArrayList<>();
		reader.startArray();
		while (!reader.endStruct()) {
			posts.add(createPost(reader, linked, firstPost ? extra : null));
			firstPost = false;
		}
		return posts;
	}

	public static Posts createThread(JsonSerial.Reader reader, Object linked) throws IOException, ParseException {
		Extra extra = new Extra();
		extra.posts = new ArrayList<>();
		Post post = createPost(reader, linked, extra);
		// Different data format for thread lists and catalog
		List<Post> posts = extra.hasPosts ? extra.posts : Collections.singletonList(post);
		if (!posts.isEmpty() && posts.get(0).getAttachmentsCount() > 0) {
			extra.postsWithFilesCount++;
		}
		extra.postsCount += posts.size();
		return new Posts(posts).addPostsCount(extra.postsCount).addPostsWithFilesCount(extra.postsWithFilesCount);
	}
}
