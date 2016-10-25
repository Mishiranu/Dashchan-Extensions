package com.mishiranu.dashchan.chan.sevenchan;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import chan.content.ChanConfiguration;
import chan.content.ChanLocator;
import chan.content.model.Attachment;
import chan.content.model.EmbeddedAttachment;
import chan.content.model.FileAttachment;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.text.GroupParser;
import chan.text.ParseException;
import chan.util.StringUtils;

public class SevenchanPostsParser implements GroupParser.Callback {
	private final String source;
	private final SevenchanChanConfiguration configuration;
	private final SevenchanChanLocator locator;
	private final String boardName;

	private String parent;
	private Posts thread;
	private Post post;
	private FileAttachment attachment;
	private ArrayList<Posts> threads;
	private final ArrayList<Attachment> attachments = new ArrayList<>();
	private final ArrayList<Post> posts = new ArrayList<>();

	private static final int EXPECT_NONE = 0;
	private static final int EXPECT_SUBJECT = 1;
	private static final int EXPECT_NAME = 2;
	private static final int EXPECT_TRIPCODE = 3;
	private static final int EXPECT_CAPCODE = 4;
	private static final int EXPECT_FILE_SIZE = 5;
	private static final int EXPECT_COMMENT = 6;
	private static final int EXPECT_OMITTED = 7;
	private static final int EXPECT_BOARD_TITLE = 8;
	private static final int EXPECT_PAGES_COUNT = 9;

	private int expect = EXPECT_NONE;
	private boolean headerHandling = false;

	private boolean hasPostBlock = false;
	private boolean hasPostBlockName = false;
	private int postBlockFiles = 0;

	private static final SimpleDateFormat DATE_FORMAT;
	private static final SimpleDateFormat DATE_FORMAT_WEEABOO_CLEAN;

	static {
		DATE_FORMAT = new SimpleDateFormat("yy/MM/dd(EEE)hh:mm", Locale.US);
		DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT+5"));
		DATE_FORMAT_WEEABOO_CLEAN = new SimpleDateFormat("yyyy MM dd ( ) hh mm ss", Locale.JAPANESE);
		DATE_FORMAT_WEEABOO_CLEAN.setTimeZone(TimeZone.getTimeZone("GMT+5"));
	}

	private static final Pattern FILE_SIZE = Pattern.compile("\\(([\\d\\.]+)(\\w+) *, *(\\d+)x(\\d+)" +
			"(?: *, *(.+))? *\\)");
	private static final Pattern NAME_EMAIL = Pattern.compile("<a href=\"(.*?)\">(.*)</a>");
	private static final Pattern NUMBER = Pattern.compile("(\\d+)");

	public SevenchanPostsParser(String source, Object linked, String boardName) {
		this.source = source;
		configuration = ChanConfiguration.get(linked);
		locator = ChanLocator.get(linked);
		this.boardName = boardName;
	}

	public SevenchanPostsParser(String source, Object linked, String boardName, String parent) {
		this(source, linked, boardName);
		this.parent = parent;
	}

	private void closeThread() {
		if (thread != null) {
			thread.setPosts(posts);
			thread.addPostsCount(posts.size());
			int filesCount = 0;
			for (Post post : posts) {
				filesCount += post.getAttachmentsCount();
			}
			thread.addFilesCount(filesCount);
			threads.add(thread);
			posts.clear();
		}
	}

	public ArrayList<Posts> convertThreads() throws ParseException {
		threads = new ArrayList<>();
		GroupParser.parse(source, this);
		closeThread();
		if (threads.size() > 0) {
			updateConfiguration();
			return threads;
		}
		return null;
	}

	public Posts convertPosts() throws ParseException {
		GroupParser.parse(source, this);
		if (posts.size() > 0) {
			updateConfiguration();
			return new Posts(posts);
		}
		return null;
	}

	private void updateConfiguration() {
		if (hasPostBlock) {
			configuration.storeNamesEnabled(boardName, hasPostBlockName);
			if (threads == null) {
				configuration.storeMaxReplyFilesCount(boardName, postBlockFiles);
			}
		}
	}

	private String cutAttachmentUriString(String uriString) {
		int index = uriString.indexOf("//");
		if (index >= 0) {
			index = uriString.indexOf('/', index + 2);
			if (index >= 0) {
				return uriString.substring(index);
			}
			return null;
		}
		return uriString;
	}

	@Override
	public boolean onStartElement(GroupParser parser, String tagName, String attrs) {
		if ("div".equals(tagName)) {
			String cssClass = parser.getAttr(attrs, "class");
			if ("thread".equals(cssClass)) {
				if (threads != null) {
					closeThread();
					thread = new Posts();
					parent = null;
				}
			} else if ("post".equals(cssClass)) {
				String id = parser.getAttr(attrs, "id");
				Post post = new Post();
				if (parent == null) {
					parent = id;
				} else {
					post.setParentPostNumber(parent);
				}
				post.setPostNumber(id);
				this.post = post;
			} else if ("post_header".equals(cssClass)) {
				headerHandling = true;
			} else if ("post_thumb".equals(cssClass)) {
				attachment = new FileAttachment();
			} else if (cssClass == null) {
				String id = parser.getAttr(attrs, "id");
				if ("paging".equals(id)) {
					expect = EXPECT_PAGES_COUNT;
					return true;
				}
			}
		} else if ("span".equals(tagName)) {
			String cssClass = parser.getAttr(attrs, "class");
			if ("subject".equals(cssClass)) {
				expect = EXPECT_SUBJECT;
				return true;
			} else if ("postername".equals(cssClass)) {
				expect = EXPECT_NAME;
				return true;
			} else if ("postertrip".equals(cssClass)) {
				expect = EXPECT_TRIPCODE;
				return true;
			} else if ("capcode".equals(cssClass)) {
				expect = EXPECT_CAPCODE;
				return true;
			} else if ("omittedposts".equals(cssClass)) {
				if (threads != null) {
					expect = EXPECT_OMITTED;
					return true;
				}
			} else if ("title".equals(cssClass)) {
				expect = EXPECT_BOARD_TITLE;
				return true;
			}
		} else if ("a".equals(tagName)) {
			if (attachment != null && attachment.getFileUri(locator) == null) {
				String path = cutAttachmentUriString(parser.getAttr(attrs, "href"));
				attachment.setFileUri(locator, locator.buildPath(path));
			} else {
				String id = parser.getAttr(attrs, "id");
				if (id != null && id.startsWith("expandimg_")) {
					String path = cutAttachmentUriString(parser.getAttr(attrs, "href"));
					if (!path.endsWith("/removed.png")) {
						attachment = new FileAttachment();
						attachment.setFileUri(locator, locator.buildPath(path));
					}
				}
			}
		} else if ("img".equals(tagName)) {
			if (post != null) {
				String cssClass = parser.getAttr(attrs, "class");
				if ("thumb".equals(cssClass)) {
					String path = cutAttachmentUriString(parser.getAttr(attrs, "src"));
					attachment.setThumbnailUri(locator, locator.buildPath(path));
				} else if ("multithumbfirst".equals(cssClass) || "multithumb".equals(cssClass)) {
					if (attachment != null) {
						String path = cutAttachmentUriString(parser.getAttr(attrs, "src"));
						attachment.setThumbnailUri(locator, locator.buildPath(path));
						String title = parser.getAttr(attrs, "title");
						parseFileSize(attachment, title);
						attachments.add(attachment);
						attachment = null;
					}
				} else if ("stickied".equals(cssClass)) {
					post.setSticky(true);
				} else if ("locked".equals(cssClass)) {
					post.setClosed(true);
				}
			}
		} else if ("p".equals(tagName)) {
			String cssClass = parser.getAttr(attrs, "class");
			if ("file_size".equals(cssClass) && post != null) {
				expect = EXPECT_FILE_SIZE;
				return true;
			} else if ("message".equals(cssClass)) {
				expect = EXPECT_COMMENT;
				return true;
			}
		} else if ("table".equals(tagName)) {
			String border = parser.getAttr(attrs, "border");
			if (threads != null && "1".equals(border)) {
				expect = EXPECT_PAGES_COUNT;
				return true;
			}
		} else if ("form".equals(tagName)) {
			String name = parser.getAttr(attrs, "name");
			if ("postform".equals(name)) {
				hasPostBlock = true;
			}
		} else if ("input".equals(tagName)) {
			if (hasPostBlock) {
				String name = parser.getAttr(attrs, "name");
				if ("name".equals(name)) {
					hasPostBlockName = true;
				} else if ("imagefile[]".equals(name)) {
					postBlockFiles++;
				}
			}
		}
		return false;
	}

	@Override
	public void onEndElement(GroupParser parser, String tagName) {
		if (headerHandling && "div".equals(tagName)) {
			headerHandling = false;
		}
	}

	@Override
	public void onText(GroupParser parser, String source, int start, int end) {
		if (headerHandling) {
			String text = source.substring(start, end).trim();
			if (text.startsWith("ID: ")) {
				post.setIdentifier(StringUtils.clearHtml(text.substring(4)).trim());
			} else if (text.contains("(")) {
				try {
					post.setTimestamp(DATE_FORMAT.parse(text).getTime());
				} catch (java.text.ParseException e1) {
					try {
						text = text.replaceAll("&#\\d+;", " ");
						post.setTimestamp(DATE_FORMAT_WEEABOO_CLEAN.parse(text).getTime());
					} catch (java.text.ParseException e2) {
						// Ignore exception
					}
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
				text = text.trim();
				Matcher matcher = NAME_EMAIL.matcher(text);
				if (matcher.matches()) {
					String email = matcher.group(1);
					if (email.toLowerCase(Locale.US).equals("mailto:sage")) {
						post.setSage(true);
					} else {
						post.setEmail(StringUtils.clearHtml(email));
					}
					text = matcher.group(2);
				}
				post.setName(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));
				break;
			}
			case EXPECT_TRIPCODE: {
				post.setTripcode(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));
				break;
			}
			case EXPECT_CAPCODE: {
				post.setCapcode(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).replaceAll(" ?## ?", "").trim()));
				break;
			}
			case EXPECT_FILE_SIZE: {
				parseFileSize(attachment, text);
				attachments.add(attachment);
				attachment = null;
				break;
			}
			case EXPECT_COMMENT: {
				text = text.trim();
				if (threads != null) {
					int index = text.lastIndexOf("<span class=\"abbrev\">");
					if (index >= 0) {
						text = text.substring(0, index).trim();
					}
				}
				if (text.startsWith("<span style=\"float: left;\">")) {
					int index = text.indexOf("data=\"");
					if (index >= 0) {
						index += 6;
						String url = text.substring(index, text.indexOf('"', index));
						if (url.startsWith("//")) {
							url = "http:" + url;
						}
						EmbeddedAttachment attachment = EmbeddedAttachment.obtain(url);
						if (attachment != null) {
							attachments.add(attachment);
						}
					}
					index = text.indexOf("</span></span>");
					if (index >= 0) {
						index += 14;
						if (text.indexOf("&nbsp;", index) == index) {
							index += 6;
						}
						text = text.substring(index).trim();
					}
				}
				post.setComment(text);
				posts.add(post);
				if (attachments.size() > 0) {
					post.setAttachments(attachments);
					attachments.clear();
				}
				post = null;
				break;
			}
			case EXPECT_OMITTED: {
				text = StringUtils.clearHtml(text);
				Matcher matcher = NUMBER.matcher(text);
				if (matcher.find()) {
					thread.addPostsCount(Integer.parseInt(matcher.group(1)));
					if (matcher.find()) {
						thread.addFilesCount(Integer.parseInt(matcher.group(1)));
					}
				}
				break;
			}
			case EXPECT_BOARD_TITLE: {
				text = StringUtils.clearHtml(text).trim();
				int index = text.lastIndexOf('\n');
				if (index > 0) {
					text = text.substring(index + 1).trim();
					configuration.storeBoardTitle(boardName, text);
				}
				break;
			}
			case EXPECT_PAGES_COUNT: {
				text = StringUtils.clearHtml(text);
				String pagesCount = null;
				Matcher matcher = NUMBER.matcher(text);
				while (matcher.find()) {
					pagesCount = matcher.group(1);
				}
				if (pagesCount != null) {
					try {
						configuration.storePagesCount(boardName, Integer.parseInt(pagesCount) + 1);
					} catch (NumberFormatException e) {
						// Ignore exception
					}
				}
				break;
			}
		}
		expect = EXPECT_NONE;
	}

	private void parseFileSize(FileAttachment attachment, String text) {
		text = StringUtils.clearHtml(text);
		Matcher matcher = FILE_SIZE.matcher(text);
		if (matcher.find()) {
			float size = Float.parseFloat(matcher.group(1));
			String dim = matcher.group(2);
			if ("KB".equals(dim)) {
				size *= 1024;
			} else if ("MB".equals(dim)) {
				size *= 1024 * 1024;
			}
			int width = Integer.parseInt(matcher.group(3));
			int height = Integer.parseInt(matcher.group(4));
			String fileName = matcher.group(5);
			if (fileName != null && fileName.endsWith(")")) {
				fileName = fileName.substring(0, fileName.length() - 1);
			}
			attachment.setSize((int) size);
			attachment.setWidth(width);
			attachment.setHeight(height);
			attachment.setOriginalName(StringUtils.isEmptyOrWhitespace(fileName) ? null : fileName.trim());
		}
	}
}