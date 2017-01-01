package com.mishiranu.dashchan.chan.taima;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import chan.content.model.FileAttachment;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.text.GroupParser;
import chan.text.ParseException;
import chan.util.StringUtils;

public class TaimaPostsParser implements GroupParser.Callback {
	private final String source;
	private final TaimaChanConfiguration configuration;
	private final TaimaChanLocator locator;
	private final String boardName;

	private String parent;
	private Posts thread;
	private Post post;
	private FileAttachment attachment;
	private ArrayList<Posts> threads;
	private final ArrayList<Post> posts = new ArrayList<>();

	private static final int EXPECT_NONE = 0;
	private static final int EXPECT_SUBJECT = 1;
	private static final int EXPECT_NAME = 2;
	private static final int EXPECT_TRIPCODE = 3;
	private static final int EXPECT_DATE_ID = 4;
	private static final int EXPECT_FILE_DATA = 5;
	private static final int EXPECT_COMMENT = 6;
	private static final int EXPECT_OMITTED = 7;
	private static final int EXPECT_BOARD_TITLE = 8;
	private static final int EXPECT_PAGES_COUNT = 9;

	private int expect = EXPECT_NONE;

	private boolean hasPostBlock = false;
	private boolean hasPostBlockName = false;

	private static final SimpleDateFormat DATE_FORMAT;

	static {
		DATE_FORMAT = new SimpleDateFormat("ccc, dd MMM yy HH:mm:ss 'EST'", Locale.US);
		DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT-4"));
	}

	private static final Pattern FILE_SIZE = Pattern.compile("(?:(\\d+)B / )?([\\d\\.]+) ?(\\w+), (\\d+)x(\\d+)");
	private static final Pattern NUMBER = Pattern.compile("(\\d+)");

	public TaimaPostsParser(String source, Object linked, String boardName) {
		this.source = source;
		configuration = TaimaChanConfiguration.get(linked);
		locator = TaimaChanLocator.get(linked);
		this.boardName = boardName;
	}

	private void closeThread() {
		if (thread != null) {
			thread.setPosts(posts);
			thread.addPostsCount(posts.size());
			int postsWithFilesCount = 0;
			for (Post post : posts) {
				postsWithFilesCount += post.getAttachmentsCount();
			}
			thread.addPostsWithFilesCount(postsWithFilesCount);
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
		}
	}

	@Override
	public boolean onStartElement(GroupParser parser, String tagName, String attrs) {
		if ("div".equals(tagName)) {
			String cssClass = parser.getAttr(attrs, "class");
			if ("thread_header".equals(cssClass)) {
				parent = null;
				post = new Post();
				if (threads != null) {
					closeThread();
					thread = new Posts();
				}
			} else if ("lock".equals(cssClass)) {
				post.setClosed(true);
			} else if ("ban".equals(cssClass)) {
				post.setPosterBanned(true);
			} else if ("warn".equals(cssClass)) {
				post.setPosterWarned(true);
			} else if ("pagelist".equals(cssClass)) {
				expect = EXPECT_PAGES_COUNT;
				return true;
			}
		} else if ("td".equals(tagName)) {
			String id = parser.getAttr(attrs, "id");
			if (id != null && id.startsWith("reply")) {
				String number = id.substring(5);
				post = new Post();
				post.setParentPostNumber(parent);
				post.setPostNumber(number);
			}
		} else if ("span".equals(tagName)) {
			String cssClass = parser.getAttr(attrs, "class");
			if ("filetitle".equals(cssClass)) {
				expect = EXPECT_SUBJECT;
				return true;
			} else if ("postername".equals(cssClass) || "commentpostername".equals(cssClass)) {
				expect = EXPECT_NAME;
				return true;
			} else if ("postertrip".equals(cssClass)) {
				expect = EXPECT_TRIPCODE;
				return true;
			} else if ("idhighlight".equals(cssClass)) {
				expect = EXPECT_DATE_ID;
				return true;
			} else if ("filesize".equals(cssClass)) {
				expect = EXPECT_FILE_DATA;
				attachment = new FileAttachment();
				post.setAttachments(attachment);
			} else if ("board_title".equals(cssClass)) {
				expect = EXPECT_BOARD_TITLE;
				return true;
			} else if ("omittedposts".equals(cssClass)) {
				expect = EXPECT_OMITTED;
				return true;
			}
		} else if ("em".equals(tagName)) {
			if (expect == EXPECT_FILE_DATA) {
				return true;
			}
		} else if ("a".equals(tagName)) {
			if (expect == EXPECT_FILE_DATA) {
				String path = parser.getAttr(attrs, "href");
				if (path != null) {
					attachment.setFileUri(locator, locator.createSpecialBoardUri(path));
				}
			} else if (parent == null && post != null) {
				String id = parser.getAttr(attrs, "id");
				if (id != null) {
					parent = id;
					post.setPostNumber(id);
				}
			}
		} else if ("img".equals(tagName)) {
			String cssClass = parser.getAttr(attrs, "class");
			if ("thumb".equals(cssClass)) {
				String path = parser.getAttr(attrs, "src");
				if (path != null && (!path.equals(attachment.getFileUri(locator).getPath()) ||
						attachment.getSize() < 50 * 1024)) {
					// GIF thumbnails has the same URI as image and can weigh a lot
					if (path != null) {
						attachment.setThumbnailUri(locator, locator.createSpecialBoardUri(path));
					}
				}
			}
		} else if ("i".equals(tagName)) {
			String cssClass = parser.getAttr(attrs, "class");
			if ("gl glyphicon-paperclip".equals(cssClass)) {
				post.setSticky(true);
			}
		} else if ("blockquote".equals(tagName)) {
			expect = EXPECT_COMMENT;
			return true;
		} else if ("form".equals(tagName)) {
			String id = parser.getAttr(attrs, "id");
			if ("postform".equals(id)) {
				hasPostBlock = true;
			}
		} else if ("input".equals(tagName)) {
			if (hasPostBlock) {
				String name = parser.getAttr(attrs, "name");
				if ("field1".equals(name)) {
					hasPostBlockName = true;
				}
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
			case EXPECT_NAME: {
				post.setName(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));
				break;
			}
			case EXPECT_TRIPCODE: {
				post.setTripcode(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));
				break;
			}
			case EXPECT_DATE_ID: {
				int index = text.indexOf("ID:");
				if (index >= 0) {
					post.setIdentifier(text.substring(index + 3));
				}
				index = text.indexOf("EST");
				if (index >= 0) {
					try {
						post.setTimestamp(DATE_FORMAT.parse(text.substring(0, index + 3)).getTime());
					} catch (java.text.ParseException e) {
						// Ignore exception
					}
				}
				break;
			}
			case EXPECT_FILE_DATA: {
				Matcher matcher = FILE_SIZE.matcher(text);
				if (matcher.matches()) {
					String sizebs = matcher.group(1);
					int size;
					if (sizebs != null) {
						size = Integer.parseInt(sizebs);
					} else {
						size = Integer.parseInt(matcher.group(2));
						String dim = matcher.group(3);
						if ("KB".equals(dim)) {
							size *= 1024;
						} else if ("MB".equals(dim)) {
							size *= 1024 * 1024;
						}
					}
					int width = Integer.parseInt(matcher.group(4));
					int height = Integer.parseInt(matcher.group(5));
					attachment.setSize(size);
					attachment.setWidth(width);
					attachment.setHeight(height);
				}
				break;
			}
			case EXPECT_COMMENT: {
				text = text.trim();
				int index = text.lastIndexOf("<div class=\"abbrev\">");
				if (index >= 0) {
					text = text.substring(0, index).trim();
				}
				post.setComment(text);
				posts.add(post);
				break;
			}
			case EXPECT_OMITTED: {
				Matcher matcher = NUMBER.matcher(text);
				if (matcher.find()) {
					thread.addPostsCount(Integer.parseInt(matcher.group(1)));
					if (matcher.find()) {
						thread.addPostsWithFilesCount(Integer.parseInt(matcher.group(1)));
					}
				}
				break;
			}
			case EXPECT_BOARD_TITLE: {
				text = StringUtils.clearHtml(text).trim();
				if (!StringUtils.isEmpty(text)) {
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
					configuration.storePagesCount(boardName, Integer.parseInt(pagesCount) + 1);
				}
				break;
			}
		}
		expect = EXPECT_NONE;
	}
}