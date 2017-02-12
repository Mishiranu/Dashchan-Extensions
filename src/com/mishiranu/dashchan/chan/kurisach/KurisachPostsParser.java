package com.mishiranu.dashchan.chan.kurisach;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.net.Uri;

import chan.content.ChanConfiguration;
import chan.content.ChanLocator;
import chan.content.model.EmbeddedAttachment;
import chan.content.model.FileAttachment;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.text.GroupParser;
import chan.text.ParseException;
import chan.util.CommonUtils;
import chan.util.StringUtils;

public class KurisachPostsParser implements GroupParser.Callback {
	private final String source;
	private final KurisachChanConfiguration configuration;
	private final KurisachChanLocator locator;
	private final String boardName;

	private String parent;
	private Posts thread;
	private Post post;
	private FileAttachment attachment;
	private ArrayList<Posts> threads;
	private final ArrayList<Post> posts = new ArrayList<>();

	private static final int EXPECT_NONE = 0;
	private static final int EXPECT_FILE_SIZE = 1;
	private static final int EXPECT_SUBJECT = 2;
	private static final int EXPECT_NAME = 3;
	private static final int EXPECT_TRIPCODE = 4;
	private static final int EXPECT_COMMENT = 5;
	private static final int EXPECT_OMITTED = 6;
	private static final int EXPECT_BOARD_TITLE = 7;
	private static final int EXPECT_PAGES_COUNT = 8;

	private int expect = EXPECT_NONE;
	private boolean headerHandling = false;
	private boolean parentFromRefLink = false;

	private static final Pattern FILE_SIZE = Pattern.compile("\\(([\\d\\.]+)(\\w+)(?: *, *(\\d+)x(\\d+))?" +
			"(?: *, *(.+))? *\\) *$");
	private static final Pattern EMBED = Pattern.compile("data-id=\"(.*?)\" data-site=\"(.*?)\"");
	static final Pattern NUMBER = Pattern.compile("(\\d+)");

	public KurisachPostsParser(String source, Object linked, String boardName) {
		this.source = source;
		configuration = ChanConfiguration.get(linked);
		locator = ChanLocator.get(linked);
		this.boardName = boardName;
	}

	public KurisachPostsParser(String source, Object linked, String boardName, String parent) {
		this(source, linked, boardName);
		this.parent = parent;
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
		return threads;
	}

	public ArrayList<Post> convertPosts() throws ParseException {
		GroupParser.parse(source, this);
		return posts;
	}

	public ArrayList<Post> convertSearchPosts() throws ParseException {
		parentFromRefLink = true;
		GroupParser.parse(source, this);
		return posts;
	}

	public Post convertSinglePost() throws ParseException {
		parentFromRefLink = true;
		GroupParser.parse(source, this);
		return posts.size() > 0 ? posts.get(0) : null;
	}

	private String convertUriString(String uriString) {
		if (uriString != null) {
			int index = uriString.indexOf("://");
			if (index > 0) {
				uriString = uriString.substring(uriString.indexOf('/', index + 3));
			}
		}
		return uriString;
	}

	@Override
	public boolean onStartElement(GroupParser parser, String tagName, String attrs) {
		if ("div".equals(tagName)) {
			String id = parser.getAttr(attrs, "id");
			if (id != null && id.startsWith("thread")) {
				String number = id.substring(6, id.length() - boardName.length());
				Post post = new Post();
				post.setPostNumber(number);
				parent = number;
				this.post = post;
				if (threads != null) {
					closeThread();
					thread = new Posts();
				}
			} else {
				String cssClass = parser.getAttr(attrs, "class");
				if ("logo".equals(cssClass)) {
					expect = EXPECT_BOARD_TITLE;
					return true;
				}
			}
		} else if ("td".equals(tagName)) {
			String id = parser.getAttr(attrs, "id");
			if (id != null && id.startsWith("reply")) {
				String number = id.substring(5);
				Post post = new Post();
				post.setParentPostNumber(parent);
				post.setPostNumber(number);
				this.post = post;
			}
		} else if ("label".equals(tagName)) {
			if (post != null) {
				headerHandling = true;
			}
		} else if ("span".equals(tagName)) {
			String cssClass = parser.getAttr(attrs, "class");
			if ("filesize".equals(cssClass)) {
				attachment = new FileAttachment();
				expect = EXPECT_FILE_SIZE;
				return true;
			} else if ("filetitle".equals(cssClass)) {
				expect = EXPECT_SUBJECT;
				return true;
			} else if ("postername".equals(cssClass) && post.getTimestamp() <= 0) {
				expect = EXPECT_NAME;
				return true;
			} else if ("postertrip".equals(cssClass)) {
				expect = EXPECT_TRIPCODE;
				return true;
			} else if ("admin".equals(cssClass)) {
				post.setCapcode("Admin");
				// Skip this block to parse date correctly
				expect = EXPECT_NONE;
				return true;
			} else if ("mod".equals(cssClass)) {
				post.setCapcode("Mod");
				// Skip this block to parse date correctly
				expect = EXPECT_NONE;
				return true;
			} else if ("omittedposts".equals(cssClass)) {
				if (threads != null) {
					expect = EXPECT_OMITTED;
					return true;
				}
			}
		} else if ("a".equals(tagName)) {
			if (headerHandling) {
				String href = parser.getAttr(attrs, "href");
				if (href != null) {
					String email = href;
					if (email.startsWith("mailto")) {
						email = StringUtils.clearHtml(email);
					} else {
						if (email.startsWith("/cdn-cgi/l/email-protection#")) {
							String string = "<a href=\"" + email + "\"></a>";
							string = CommonUtils.restoreCloudFlareProtectedEmails(string);
							email = string.substring(9, string.length() - 6);
						} else {
							email = null;
						}
					}
					if (email != null) {
						if (email.toLowerCase(Locale.US).equals("mailto:sage")) {
							post.setSage(true);
						} else {
							post.setEmail(email);
						}
					}
				}
			} else if (parentFromRefLink && "shl".equals(parser.getAttr(attrs, "class"))) {
				String href = parser.getAttr(attrs, "href");
				if (href != null) {
					post.setParentPostNumber(locator.getThreadNumber(Uri.parse(href)));
				}
			} else if (attachment != null) {
				String path = convertUriString(parser.getAttr(attrs, "href"));
				if (path != null) {
					attachment.setFileUri(locator, locator.buildPath(path));
				} else {
					attachment = null;
				}
			}
		} else if ("img".equals(tagName)) {
			String cssClass = parser.getAttr(attrs, "class");
			if ("thumb".equals(cssClass)) {
				if (attachment != null) {
					String path = convertUriString(parser.getAttr(attrs, "src"));
					if (path != null) {
						attachment.setThumbnailUri(locator, locator.buildPath(path));
					}
					post.setAttachments(attachment);
					attachment = null;
				}
			} else {
				if (post != null) {
					String src = parser.getAttr(attrs, "src");
					if (src != null) {
						if (src.endsWith("/images/sticky.gif")) {
							post.setSticky(true);
						} else if (src.endsWith("/images/locked.gif")) {
							post.setClosed(true);
						}
					}
				}
			}
		} else if ("source".equals(tagName)) {
			if (attachment != null) {
				attachment.setFileUri(locator, locator.buildPath(convertUriString(parser.getAttr(attrs, "src"))));
				post.setAttachments(attachment);
				attachment = null;
			}
		} else if ("blockquote".equals(tagName)) {
			expect = EXPECT_COMMENT;
			return true;
		} else if ("table".equals(tagName)) {
			String border = parser.getAttr(attrs, "border");
			if (threads != null && "1".equals(border)) {
				expect = EXPECT_PAGES_COUNT;
				return true;
			}
		}
		return false;
	}

	@Override
	public void onEndElement(GroupParser parser, String tagName) {
		if ("label".equals(tagName)) {
			headerHandling = false;
		}
	}

	private static final Pattern DATE = Pattern.compile("(\\d{4}) (\\w+) (\\d{1,2}) (\\d{2}):(\\d{2}):(\\d{2})");

	private static final List<String> MONTHS = Arrays.asList("Янв", "Фев", "Мар",
			"Апр", "Май", "Июн", "Июл", "Авг", "Сен", "Окт", "Ноя", "Дек");

	private static final TimeZone TIMEZONE_GMT = TimeZone.getTimeZone("Etc/GMT");

	@Override
	public void onText(GroupParser parser, String source, int start, int end) {
		if (headerHandling) {
			String text = source.substring(start, end).trim();
			if (text.length() > 0) {
				Matcher matcher = DATE.matcher(text);
				if (matcher.find()) {
					int year = Integer.parseInt(matcher.group(1));
					String monthString = matcher.group(2);
					int month = MONTHS.indexOf(monthString);
					int day = Integer.parseInt(matcher.group(3));
					int hour = Integer.parseInt(matcher.group(4));
					int minute = Integer.parseInt(matcher.group(5));
					int second = Integer.parseInt(matcher.group(6));
					GregorianCalendar calendar = new GregorianCalendar(year, month, day, hour, minute, second);
					calendar.setTimeZone(TIMEZONE_GMT);
					calendar.add(GregorianCalendar.HOUR, -3);
					post.setTimestamp(calendar.getTimeInMillis());
				}
				headerHandling = false;
			}
		}
	}

	@Override
	public void onGroupComplete(GroupParser parser, String text) {
		switch (expect) {
			case EXPECT_FILE_SIZE: {
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
					attachment.setSize((int) size);
					if (matcher.group(3) != null) {
						attachment.setWidth(Integer.parseInt(matcher.group(3)));
						attachment.setHeight(Integer.parseInt(matcher.group(4)));
					}
					String fileName = matcher.group(5);
					attachment.setOriginalName(StringUtils.isEmptyOrWhitespace(fileName) ? null : fileName.trim());
				}
				break;
			}
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
			case EXPECT_COMMENT: {
				text = text.trim();
				if (text.startsWith("<span style=\"float: left;\">")) {
					int index = text.indexOf("</span>") + 7;
					String embed = text.substring(0, index);
					if (index + 6 <= text.length()) {
						index += 6;
					}
					text = text.substring(index).trim();
					Matcher matcher = EMBED.matcher(embed);
					if (matcher.find()) {
						String uriString = null;
						String id = matcher.group(1);
						String site = matcher.group(2);
						if ("youtube".equals(site)) {
							uriString = "https://www.youtube.com/watch?v=" + id;
						} else if ("vimeo".equals(site)) {
							uriString = "https://vimeo.com/" + id;
						} else if ("coub".equals(site)) {
							uriString = "https://coub.com/view/" + id;
						}
						if (uriString != null) {
							EmbeddedAttachment attachment = EmbeddedAttachment.obtain(uriString);
							if (attachment == null) {
								if ("coub".equals(site)) {
									attachment = new EmbeddedAttachment(Uri.parse(uriString), null, "COUB",
											EmbeddedAttachment.ContentType.VIDEO, false, null);
								}
							}
							post.setAttachments(attachment);
						}
					}
				}
				int index = text.lastIndexOf("<div class=\"abbrev\">");
				if (index >= 0) {
					text = text.substring(0, index).trim();
				}
				index = text.lastIndexOf("<font color=\"#FF0000\">");
				if (index >= 0) {
					String message = text.substring(index);
					text = text.substring(0, index);
					if (message.contains("USER WAS BANNED FOR THIS POST")) {
						post.setPosterBanned(true);
					}
				}
				// Add inline pre support, see chan markup implementation
				text = text.replaceAll("<pre class=\"inline-pp.*?>(.*?)</pre>", "<inlinepre>$1</inlinepre>");
				text = CommonUtils.restoreCloudFlareProtectedEmails(text);
				text = removePrettyprintBreaks(text);
				text = text.replace("<span class=\"cut\">Развернуть</span>", "");
				// Fix "posts edited" message
				text = text.replaceAll("<br.*?/>", "<br />").replaceAll(" *\r(?!\n)", "\n").replaceAll(" {2,}", " ");
				post.setComment(text);
				posts.add(post);
				post = null;
				break;
			}
			case EXPECT_OMITTED: {
				text = StringUtils.clearHtml(text);
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
				text = text.substring(5 + boardName.length()); // Skip "/boardname/ - "
				configuration.storeBoardTitle(boardName, text);
				break;
			}
			case EXPECT_PAGES_COUNT: {
				text = StringUtils.clearHtml(text);
				int index1 = text.lastIndexOf('[');
				int index2 = text.lastIndexOf(']');
				if (index1 >= 0 && index2 > index1) {
					text = text.substring(index1 + 1, index2);
					try {
						int pagesCount = Integer.parseInt(text) + 1;
						configuration.storePagesCount(boardName, pagesCount);
					} catch (NumberFormatException e) {
						// Ignore exception
					}
				}
				break;
			}
		}
		expect = EXPECT_NONE;
	}

	private String removePrettyprintBreaks(String string) {
		// brs inside pre.prettyprint has "display: none" style
		// Also br after pre will get "display: none" with javascript
		// Dashchan doesn't handle css styles and js, so hide these tags manually
		StringBuilder builder = new StringBuilder(string);
		int from = 0;
		while (true) {
			int index1 = builder.indexOf("<pre class=\"prettyprint\"", from);
			int index2 = builder.indexOf("</pre>", from);
			if (index2 > index1 && index1 >= 0) {
				while (true) {
					int brIndex = builder.indexOf("<br", index1 + 1);
					if (brIndex > index1) {
						int brEndIndex = builder.indexOf(">", brIndex) + 1;
						builder.delete(brIndex, brEndIndex);
						if (brIndex >= index2) {
							break;
						}
						index2 -= brEndIndex - brIndex;
					} else {
						break;
					}
				}
				from = index2 + 6;
				if (from >= builder.length()) {
					break;
				}
			} else {
				break;
			}
		}
		return builder.toString();
	}
}