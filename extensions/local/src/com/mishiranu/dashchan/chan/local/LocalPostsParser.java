package com.mishiranu.dashchan.chan.local;

import android.net.Uri;
import chan.content.ChanLocator;
import chan.content.model.FileAttachment;
import chan.content.model.Icon;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.text.GroupParser;
import chan.text.ParseException;
import chan.util.StringUtils;
import java.util.ArrayList;

public class LocalPostsParser implements GroupParser.Callback {
	private final String source;
	private final LocalChanLocator locator;
	private final String threadNumber;

	private boolean onlyOriginalPost;
	private String parent;
	private Uri threadUri;
	private int postsCount;
	private int filesCount;

	private Post post;
	private FileAttachment attachment;
	private final ArrayList<Post> posts = new ArrayList<>();
	private final ArrayList<FileAttachment> attachments = new ArrayList<>();
	private final ArrayList<Icon> icons = new ArrayList<>();

	private static final int EXPECT_NONE = 0;
	private static final int EXPECT_SUBJECT = 1;
	private static final int EXPECT_COMMENT = 2;

	private int expect = EXPECT_NONE;

	private static class OriginalPostParsedException extends ParseException {}

	public LocalPostsParser(String source, Object linked, String threadNumber) {
		this.source = source;
		this.locator = ChanLocator.get(linked);
		this.threadNumber = threadNumber;
	}

	public Posts convertPosts() throws ParseException {
		GroupParser.parse(source, this);
		return posts.size() > 0 ? new Posts(posts).setArchivedThreadUri(threadUri) : null;
	}

	public Posts convertThread() throws ParseException {
		onlyOriginalPost = true;
		try {
			GroupParser.parse(source, this);
		} catch (OriginalPostParsedException e) {
			// Ignore
		}
		return new Posts(post).addPostsCount(postsCount).addFilesCount(filesCount);
	}

	@Override
	public boolean onStartElement(GroupParser parser, String tagName,
			GroupParser.Attributes attributes) throws OriginalPostParsedException {
		if (attributes != null && attributes.contains("data-")) {
			String number = attributes.get("data-number");
			if (number != null) {
				if (onlyOriginalPost && post != null) {
					throw new OriginalPostParsedException();
				}
				post = new Post();
				post.setPostNumber(number);
				post.setThreadNumber(threadNumber);
				if (parent == null) {
					parent = number;
				} else {
					post.setParentPostNumber(parent);
				}
			}
			String posterName = attributes.get("data-name");
			if (posterName != null) {
				post.setName(StringUtils.clearHtml(posterName));
			}
			String identifier = attributes.get("data-identifier");
			if (identifier != null) {
				post.setIdentifier(StringUtils.clearHtml(identifier));
			}
			String tripcode = attributes.get("data-tripcode");
			if (tripcode != null) {
				post.setTripcode(StringUtils.clearHtml(tripcode));
			}
			String capcode = attributes.get("data-capcode");
			if (capcode != null) {
				post.setCapcode(StringUtils.clearHtml(capcode));
			}
			String defaultName = attributes.get("data-default-name");
			if (defaultName != null) {
				post.setDefaultName(true);
			}
			String email = attributes.get("data-email");
			if (email != null) {
				post.setEmail(StringUtils.clearHtml(email));
			}
			String timestamp = attributes.get("data-timestamp");
			if (timestamp != null) {
				post.setTimestamp(Long.parseLong(timestamp));
			}
			String sage = attributes.get("data-sage");
			if (sage != null) {
				post.setSage(true);
			}
			String op = attributes.get("data-op");
			if (op != null) {
				post.setOriginalPoster(true);
			}
			String file = attributes.get("data-file");
			if (file != null) {
				attachment = new FileAttachment();
				attachments.add(attachment);
				attachment.setFileUri(locator, createFileUriLocal(file));
			}
			String thumbnail = attributes.get("data-thumbnail");
			if (thumbnail != null) {
				attachment.setThumbnailUri(locator, createFileUriLocal(thumbnail));
			}
			String originalName = attributes.get("data-original-name");
			if (originalName != null) {
				attachment.setOriginalName(StringUtils.clearHtml(originalName));
			}
			String size = attributes.get("data-size");
			if (size != null) {
				attachment.setSize(Integer.parseInt(size));
			}
			String width = attributes.get("data-width");
			if (width != null) {
				attachment.setWidth(Integer.parseInt(width));
			}
			String height = attributes.get("data-height");
			if (height != null) {
				attachment.setHeight(Integer.parseInt(height));
			}
			String icon = attributes.get("data-icon");
			if (icon != null) {
				String src = attributes.get("src");
				String title = StringUtils.clearHtml(attributes.get("title"));
				icons.add(new Icon(locator, Uri.parse(src), title));
			}
			String subject = attributes.get("data-subject");
			if (subject != null) {
				expect = EXPECT_SUBJECT;
				return true;
			}
			String comment = attributes.get("data-comment");
			if (comment != null) {
				expect = EXPECT_COMMENT;
				return true;
			}
			String threadUriString = attributes.get("data-thread-uri");
			if (threadUriString != null) {
				threadUri = Uri.parse(threadUriString);
			}
			String postsCount = attributes.get("data-posts");
			if (postsCount != null) {
				this.postsCount = Integer.parseInt(postsCount);
			}
			String filesCount = attributes.get("data-files");
			if (filesCount != null) {
				this.filesCount = Integer.parseInt(filesCount);
			}
		}
		return false;
	}

	@Override
	public void onEndElement(GroupParser parser, String tagName) {}

	@Override
	public void onText(GroupParser parser, CharSequence text) {}

	@Override
	public void onGroupComplete(GroupParser parser, String text) {
		switch (expect) {
			case EXPECT_SUBJECT: {
				post.setSubject(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));
				break;
			}
			case EXPECT_COMMENT: {
				post.setComment(text);
				posts.add(post);
				if (!attachments.isEmpty()) {
					post.setAttachments(attachments);
					attachments.clear();
				}
				if (!icons.isEmpty()) {
					post.setIcons(icons);
					icons.clear();
				}
				break;
			}
		}
		expect = EXPECT_NONE;
	}

	private Uri createFileUriLocal(String uriString) {
		Uri uri = Uri.parse(uriString);
		if (uri.isRelative()) {
			String path = uri.getPath();
			uri = new Uri.Builder().scheme("http").authority("localhost").path(path).build();
		}
		return uri;
	}
}
