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
	public boolean onStartElement(GroupParser parser, String tagName, String attrs) throws OriginalPostParsedException {
		if (attrs != null && attrs.contains("data-")) {
			String number = parser.getAttr(attrs, "data-number");
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
			String posterName = parser.getAttr(attrs, "data-name");
			if (posterName != null) {
				post.setName(StringUtils.clearHtml(posterName));
			}
			String identifier = parser.getAttr(attrs, "data-identifier");
			if (identifier != null) {
				post.setIdentifier(StringUtils.clearHtml(identifier));
			}
			String tripcode = parser.getAttr(attrs, "data-tripcode");
			if (tripcode != null) {
				post.setTripcode(StringUtils.clearHtml(tripcode));
			}
			String capcode = parser.getAttr(attrs, "data-capcode");
			if (capcode != null) {
				post.setCapcode(StringUtils.clearHtml(capcode));
			}
			String defaultName = parser.getAttr(attrs, "data-default-name");
			if (defaultName != null) {
				post.setDefaultName(true);
			}
			String email = parser.getAttr(attrs, "data-email");
			if (email != null) {
				post.setEmail(StringUtils.clearHtml(email));
			}
			String timestamp = parser.getAttr(attrs, "data-timestamp");
			if (timestamp != null) {
				post.setTimestamp(Long.parseLong(timestamp));
			}
			String sage = parser.getAttr(attrs, "data-sage");
			if (sage != null) {
				post.setSage(true);
			}
			String op = parser.getAttr(attrs, "data-op");
			if (op != null) {
				post.setOriginalPoster(true);
			}
			String file = parser.getAttr(attrs, "data-file");
			if (file != null) {
				attachment = new FileAttachment();
				attachments.add(attachment);
				attachment.setFileUri(locator, createFileUriLocal(file));
			}
			String thumbnail = parser.getAttr(attrs, "data-thumbnail");
			if (thumbnail != null) {
				attachment.setThumbnailUri(locator, createFileUriLocal(thumbnail));
			}
			String originalName = parser.getAttr(attrs, "data-original-name");
			if (originalName != null) {
				attachment.setOriginalName(StringUtils.clearHtml(originalName));
			}
			String size = parser.getAttr(attrs, "data-size");
			if (size != null) {
				attachment.setSize(Integer.parseInt(size));
			}
			String width = parser.getAttr(attrs, "data-width");
			if (width != null) {
				attachment.setWidth(Integer.parseInt(width));
			}
			String height = parser.getAttr(attrs, "data-height");
			if (height != null) {
				attachment.setHeight(Integer.parseInt(height));
			}
			String icon = parser.getAttr(attrs, "data-icon");
			if (icon != null) {
				String src = parser.getAttr(attrs, "src");
				String title = StringUtils.clearHtml(parser.getAttr(attrs, "title"));
				icons.add(new Icon(locator, Uri.parse(src), title));
			}
			String subject = parser.getAttr(attrs, "data-subject");
			if (subject != null) {
				expect = EXPECT_SUBJECT;
				return true;
			}
			String comment = parser.getAttr(attrs, "data-comment");
			if (comment != null) {
				expect = EXPECT_COMMENT;
				return true;
			}
			String threadUriString = parser.getAttr(attrs, "data-thread-uri");
			if (threadUriString != null) {
				threadUri = Uri.parse(threadUriString);
			}
			String postsCount = parser.getAttr(attrs, "data-posts");
			if (postsCount != null) {
				this.postsCount = Integer.parseInt(postsCount);
			}
			String filesCount = parser.getAttr(attrs, "data-files");
			if (filesCount != null) {
				this.filesCount = Integer.parseInt(filesCount);
			}
		}
		return false;
	}

	@Override
	public void onEndElement(GroupParser parser, String tagName) {}

	@Override
	public void onText(GroupParser parser, String source, int start, int end) {}

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
				if (attachments != null) {
					post.setAttachments(attachments);
					attachments.clear();
				}
				if (icons != null) {
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
