package com.mishiranu.dashchan.chan.archiverbt;

import android.net.Uri;
import chan.content.model.FileAttachment;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.text.GroupParser;
import chan.text.ParseException;
import chan.util.StringUtils;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArchiveRbtPostsParser implements GroupParser.Callback {
	private final String source;
	private final ArchiveRbtChanLocator locator;

	private String resTo;
	private Posts thread;
	private Post post;
	private FileAttachment attachment;
	private ArrayList<Posts> threads;
	private final ArrayList<Post> posts = new ArrayList<>();

	private static final int EXPECT_NONE = 0;
	private static final int EXPECT_SUBJECT = 1;
	private static final int EXPECT_NAME = 2;
	private static final int EXPECT_TRIP = 3;
	private static final int EXPECT_COMMENT = 4;
	private static final int EXPECT_OMITTED = 5;

	private int expect = EXPECT_NONE;
	private boolean spanStarted = false;

	private static final Pattern FILE_SIZE = Pattern.compile("File: ([\\d\\.]+) (\\w+), (\\d+)x(\\d+)(?:, (.*))?");
	private static final Pattern NUMBER = Pattern.compile("(\\d+)");

	public ArchiveRbtPostsParser(String source, Object linked) {
		this.source = source;
		locator = ArchiveRbtChanLocator.get(linked);
	}

	private void closeThread() {
		if (thread != null) {
			thread.setPosts(posts);
			thread.addPostsCount(posts.size());
			threads.add(thread);
			posts.clear();
		}
	}

	public ArrayList<Posts> convertThreads() throws ParseException {
		threads = new ArrayList<>();
		GroupParser.parse(source, this);
		closeThread();
		return threads.isEmpty() ? null : threads;
	}

	public Posts convertPosts(Uri threadUri) throws ParseException {
		GroupParser.parse(source, this);
		return posts.size() > 0 ? new Posts(posts).setArchivedThreadUri(threadUri) : null;
	}

	public ArrayList<Post> convertSearch() throws ParseException {
		GroupParser.parse(source, this);
		return posts;
	}

	@Override
	public boolean onStartElement(GroupParser parser, String tagName, String attrs) throws ParseException {
		if ("div".equals(tagName)) {
			String id = parser.getAttr(attrs, "id");
			if (id != null) {
				String number = id.substring(1);
				boolean isNumber;
				try {
					Integer.parseInt(number);
					isNumber = true;
				} catch (Exception e) {
					isNumber = false;
				}
				if (isNumber) {
					Post post = new Post();
					post.setPostNumber(number);
					resTo = number;
					this.post = post;
					if (threads != null) {
						closeThread();
						thread = new Posts();
					}
				}
			}
		} else if ("td".equals(tagName)) {
			String cssClass = parser.getAttr(attrs, "class");
			if (cssClass != null && cssClass.contains("reply")) {
				String id = parser.getAttr(attrs, "id");
				if (id != null) {
					String number = id.substring(1).replace('_', '.');
					Post post = new Post();
					post.setParentPostNumber(resTo);
					post.setPostNumber(number);
					this.post = post;
				}
			}
		} else if ("span".equals(tagName)) {
			String cssClass = parser.getAttr(attrs, "class");
			if ("filetitle".equals(cssClass)) {
				expect = EXPECT_SUBJECT;
				return true;
			} else if (cssClass != null && cssClass.startsWith("postername")) {
				expect = EXPECT_NAME;
				return true;
			} else if ("postertrip".equals(cssClass)) {
				expect = EXPECT_TRIP;
				return true;
			} else if ("posttime".equals(cssClass)) {
				String timestamp = parser.getAttr(attrs, "title");
				if (timestamp != null) {
					post.setTimestamp(Long.parseLong(timestamp));
				}
			} else if ("omittedposts".equals(cssClass)) {
				expect = EXPECT_OMITTED;
				return true;
			} else {
				spanStarted = true;
			}
		} else if ("a".equals(tagName)) {
			boolean resToHandled = false;
			if (post != null && resTo == null) {
				String cssClass = parser.getAttr(attrs, "class");
				if ("js".equals(cssClass)) {
					String onclick = parser.getAttr(attrs, "onclick");
					if (onclick != null && onclick.startsWith("replyhighlight")) {
						String href = parser.getAttr(attrs, "href");
						post.setParentPostNumber(locator.getThreadNumber(Uri.parse(href)));
						resToHandled = true;
					}
				}
			}
			if (!resToHandled && attachment != null) {
				String href = parser.getAttr(attrs, "href");
				if (href.startsWith("/boards/") && href.contains("/img/")) {
					attachment.setFileUri(locator, locator.buildPath(href));
				}
			}
		} else if ("img".equals(tagName)) {
			String cssClass = parser.getAttr(attrs, "class");
			if (attachment != null && "file thumb".equals(cssClass)) {
				attachment.setThumbnailUri(locator, locator.buildPath(parser.getAttr(attrs, "src")));
			}
		} else if ("p".equals(tagName)) {
			if (post != null) {
				expect = EXPECT_COMMENT;
				return true;
			}
		}
		return false;
	}

	@Override
	public void onEndElement(GroupParser parser, String tagName) {}

	@Override
	public void onText(GroupParser parser, String source, int start, int end) throws ParseException {
		if (spanStarted) {
			spanStarted = false;
			if (post != null) {
				String text = StringUtils.clearHtml(source.substring(start, end)).trim();
				Matcher matcher = FILE_SIZE.matcher(text);
				if (matcher.matches()) {
					float size = Float.parseFloat(matcher.group(1));
					String dim = matcher.group(2);
					if ("KB".equals(dim)) {
						size *= 1024f;
					} else if ("MB".equals(dim)) {
						size *= 1024f * 1024f;
					}
					int width = Integer.parseInt(matcher.group(3));
					int height = Integer.parseInt(matcher.group(4));
					String originalName = matcher.group(5);
					attachment = new FileAttachment();
					post.setAttachments(attachment);
					attachment.setSize((int) size);
					attachment.setWidth(width);
					attachment.setHeight(height);
					attachment.setOriginalName(originalName);
				}
			}
		}
	}

	@Override
	public void onGroupComplete(GroupParser parser, String text) {
		switch (expect) {
			case EXPECT_SUBJECT: {
				post.setSubject(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));
				break;
			}
			case EXPECT_NAME: {
				text = StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim());
				if (text != null && text.startsWith("##")) {
					post.setCapcode(text.substring(2));
				} else {
					post.setName(text);
				}
				break;
			}
			case EXPECT_TRIP: {
				post.setTripcode(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));
				break;
			}
			case EXPECT_COMMENT: {
				post.setComment(text);
				posts.add(post);
				post = null;
				attachment = null;
				break;
			}
			case EXPECT_OMITTED: {
				Matcher matcher = NUMBER.matcher(text);
				if (matcher.find()) {
					thread.addPostsCount(Integer.parseInt(matcher.group(1)));
				}
				break;
			}
		}
		expect = EXPECT_NONE;
	}
}
