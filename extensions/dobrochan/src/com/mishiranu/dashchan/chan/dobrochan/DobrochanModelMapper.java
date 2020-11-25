package com.mishiranu.dashchan.chan.dobrochan;

import chan.content.ChanLocator;
import chan.content.model.FileAttachment;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.text.JsonSerial;
import chan.text.ParseException;
import chan.util.StringUtils;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class DobrochanModelMapper {
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

	public static class BoardConfiguration {
		public Boolean filesEnabled;
		public Boolean namesEnabled;
		public Boolean tripcodesEnabled;
		public Integer attachmentsCount;

		public boolean handle(JsonSerial.Reader reader, String name) throws IOException, ParseException {
			switch (name) {
				case "allow_files": {
					filesEnabled = reader.nextBoolean();
					return true;
				}
				case "allow_names": {
					namesEnabled = reader.nextBoolean();
					return true;
				}
				case "restrict_trip": {
					tripcodesEnabled = !reader.nextBoolean();
					return true;
				}
				case "files_max_qty": {
					attachmentsCount = reader.nextInt();
					return true;
				}
				default: {
					return false;
				}
			}
		}
	}

	public static FileAttachment createFileAttachment(JsonSerial.Reader reader, ChanLocator locator)
			throws IOException, ParseException {
		FileAttachment attachment = new FileAttachment();
		String thumb = null;

		reader.startObject();
		while (!reader.endStruct()) {
			switch (reader.nextName()) {
				case "src": {
					attachment.setFileUri(locator, locator.buildPath(reader.nextString()));
					break;
				}
				case "thumb": {
					thumb = reader.nextString();
					break;
				}
				case "size": {
					attachment.setSize(reader.nextInt());
					break;
				}
				case "metadata": {
					int width = 0;
					int height = 0;
					int imageWidth = 0;
					int imageHeight = 0;
					int displayWidth = 0;
					int displayHeight = 0;
					reader.startObject();
					while (!reader.endStruct()) {
						switch (reader.nextName()) {
							case "width": {
								width = reader.nextInt();
								break;
							}
							case "height": {
								height = reader.nextInt();
								break;
							}
							case "Image Width": {
								imageWidth = reader.nextInt();
								break;
							}
							case "Image Height": {
								imageHeight = reader.nextInt();
								break;
							}
							case "Display Width": {
								displayWidth = reader.nextInt();
								break;
							}
							case "Display Height": {
								displayHeight = reader.nextInt();
								break;
							}
							default: {
								reader.skip();
								break;
							}
						}
					}
					if (width > 0 && height > 0) {
						attachment.setWidth(width);
						attachment.setHeight(height);
					} else if (imageWidth > 0 && imageHeight > 0) {
						attachment.setWidth(imageWidth);
						attachment.setHeight(imageHeight);
					} else if (displayWidth > 0 && displayHeight > 0) {
						attachment.setWidth(displayWidth);
						attachment.setHeight(displayHeight);
					}
					break;
				}
				default: {
					reader.skip();
					break;
				}
			}
		}

		if (attachment.getWidth() > 0 && attachment.getHeight() > 0 && thumb != null) {
			attachment.setThumbnailUri(locator, locator.buildPath(thumb));
		}
		return attachment;
	}

	public static Post createPost(JsonSerial.Reader reader, ChanLocator locator) throws IOException, ParseException {
		Post post = new Post();
		String message = null;
		boolean messageHtml = false;

		reader.startObject();
		while (!reader.endStruct()) {
			switch (reader.nextName()) {
				case "display_id": {
					post.setPostNumber(reader.nextString());
					break;
				}
				case "name": {
					post.setName(reader.nextString());
					break;
				}
				case "subject": {
					post.setSubject(reader.nextString());
					break;
				}
				case "message_html": {
					message = reader.nextString();
					messageHtml = true;
					break;
				}
				case "message": {
					String rawMessage = reader.nextString();
					if (message == null) {
						message = rawMessage;
						messageHtml = false;
					}
					break;
				}
				case "date": {
					String date = reader.nextString();
					try {
						post.setTimestamp(Objects.requireNonNull(DATE_FORMAT.parse(date)).getTime());
					} catch (java.text.ParseException e) {
						// Ignore
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
					if (attachments != null) {
						post.setAttachments(attachments);
					}
					break;
				}
				default: {
					reader.skip();
					break;
				}
			}
		}

		if (message != null && !messageHtml) {
			// Make simple mark for unparsed message field
			message = message.replace("\r", "");
			message = message.replaceAll(">>(\\d+)", "<a href=\"#i$1\">&gt;&gt;$1</a>");
			message = message.replaceAll("(?:^|\n)(>.*?)(?=\n|$)", "<blockquote>$1</blockquote>");
			message = message.replace("\n", "<br />");
			message = StringUtils.linkify(message);
		}
		post.setComment(message);
		return post;
	}

	public static List<Post> createPosts(JsonSerial.Reader reader, ChanLocator locator, String threadId)
			throws IOException, ParseException {
		ArrayList<Post> posts = new ArrayList<>();
		HashSet<String> postNumbers = new HashSet<>();

		reader.startArray();
		while (!reader.endStruct()) {
			Post post = createPost(reader, locator);
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
			if (!threadId.equals(postNumber)) {
				post.setParentPostNumber(threadId);
			}
			posts.add(post);
		}
		return posts;
	}

	public static Posts createThread(JsonSerial.Reader reader, ChanLocator locator) throws IOException, ParseException {
		String threadId = null;
		int postsCount = 0;
		int filesCount = 0;
		ArrayList<Post> posts = new ArrayList<>();

		reader.startObject();
		while (!reader.endStruct()) {
			switch (reader.nextName()) {
				case "display_id": {
					threadId = reader.nextString();
					break;
				}
				case "posts_count": {
					postsCount = reader.nextInt();
					break;
				}
				case "files_count": {
					filesCount = reader.nextInt();
					break;
				}
				case "posts": {
					reader.startArray();
					while (!reader.endStruct()) {
						posts.add(createPost(reader, locator));
					}
					break;
				}
				default: {
					reader.skip();
					break;
				}
			}
		}

		if (threadId == null) {
			throw new ParseException(new Exception("Thread ID is null"));
		}
		for (Post post : posts) {
			if (!threadId.equals(post.getPostNumber())) {
				post.setParentPostNumber(threadId);
			}
		}
		return new Posts(posts).addPostsCount(postsCount).addPostsWithFilesCount(filesCount);
	}
}
