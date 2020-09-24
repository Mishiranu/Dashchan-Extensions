package com.mishiranu.dashchan.chan.nowere;

import chan.content.model.FileAttachment;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.text.ParseException;
import chan.text.TemplateParser;
import chan.util.StringUtils;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NowerePostsParser {
	private final String source;
	private final NowereChanConfiguration configuration;
	private final NowereChanLocator locator;
	private final String boardName;

	private String parent;
	private Posts thread;
	private Post post;
	private FileAttachment attachment;
	private ArrayList<Posts> threads;
	private final ArrayList<Post> posts = new ArrayList<>();

	private boolean headerHandling = false;

	private static final SimpleDateFormat DATE_FORMAT;

	static {
		DATE_FORMAT = new SimpleDateFormat("yy/MM/dd(EEE)hh:mm", Locale.US);
		DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT+3"));
	}

	private static final Pattern FILE_SIZE = Pattern.compile("([\\d.]+) (\\w+), (\\d+)x(\\d+)");
	private static final Pattern NAME_EMAIL = Pattern.compile("<a href=\"(.*?)\">(.*)</a>");
	private static final Pattern NUMBER = Pattern.compile("\\d+");

	public NowerePostsParser(String source, Object linked, String boardName) {
		this.source = source;
		this.configuration = NowereChanConfiguration.get(linked);
		this.locator = NowereChanLocator.get(linked);
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
		PARSER.parse(source, this);
		closeThread();
		return threads;
	}

	public ArrayList<Post> convertPosts() throws ParseException {
		PARSER.parse(source, this);
		return posts;
	}

	private static final TemplateParser<NowerePostsParser> PARSER = TemplateParser
			.<NowerePostsParser>builder()
			.equals("input", "name", "delete")
			.open((instance, holder, tagName, attributes) -> {
				if ("checkbox".equals(attributes.get("type"))) {
					holder.headerHandling = true;
					// noinspection ConstantConditions
					if (holder.post == null || holder.post.getPostNumber() == null) {
						String number = attributes.get("value");
						if (holder.post == null) {
							holder.post = new Post();
						}
						holder.post.setPostNumber(number);
						holder.parent = number;
						if (holder.threads != null) {
							holder.closeThread();
							holder.thread = new Posts();
						}
					}
				}
				return false;
			})
			.starts("td", "id", "reply")
			.open((instance, holder, tagName, attributes) -> {
				String number = StringUtils.emptyIfNull(attributes.get("id")).substring(5);
				Post post = new Post();
				post.setParentPostNumber(holder.parent);
				post.setPostNumber(number);
				holder.post = post;
				return false;
			})
			.equals("span", "class", "filesize")
			.open((instance, holder, tagName, attributes) -> {
				if (holder.post == null) {
					holder.post = new Post();
				}
				holder.attachment = new FileAttachment();
				return false;
			})
			.name("a")
			.open((instance, holder, tagName, attributes) -> {
				if (holder.attachment != null) {
					holder.attachment.setFileUri(holder.locator, holder.locator.buildPath(attributes.get("href")));
				}
				return false;
			})
			.equals("img", "class", "thumb")
			.open((instance, holder, tagName, attributes) -> {
				String src = attributes.get("src");
				if (src != null && src.contains("/thumb/")) {
					holder.attachment.setThumbnailUri(holder.locator, holder.locator.buildPath(src));
				}
				holder.post.setAttachments(holder.attachment);
				holder.attachment = null;
				return false;
			})
			.equals("div", "class", "nothumb")
			.open((instance, holder, tagName, attributes) -> {
				if (holder.attachment.getSize() > 0 || holder.attachment.getWidth() > 0 ||
						holder.attachment.getHeight() > 0) {
					holder.post.setAttachments(holder.attachment);
				}
				holder.attachment = null;
				return false;
			})
			.name("em")
			.open((instance, holder, tagName, attributes) -> holder.attachment != null)
			.content((instance, holder, text) -> {
				Matcher matcher = FILE_SIZE.matcher(text);
				if (matcher.matches()) {
					float size = Float.parseFloat(matcher.group(1));
					String dim = matcher.group(2);
					if ("kB".equals(dim)) {
						size *= 1024;
					} else if ("MB".equals(dim)) {
						size *= 1024 * 1024;
					}
					int width = Integer.parseInt(matcher.group(3));
					int height = Integer.parseInt(matcher.group(4));
					holder.attachment.setSize((int) size);
					holder.attachment.setWidth(width);
					holder.attachment.setHeight(height);
				}
			})
			.equals("span", "class", "filetitle")
			.equals("span", "class", "replytitle")
			.content((instance, holder, text) -> holder.post
					.setSubject(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim())))
			.equals("span", "class", "postername").equals("span", "class", "commentpostername")
			.content((instance, holder, text) -> {
				Matcher matcher = NAME_EMAIL.matcher(text);
				if (matcher.matches()) {
					String email = StringUtils.clearHtml(matcher.group(1));
					if (email.toLowerCase(Locale.US).contains("sage")) {
						holder.post.setSage(true);
					} else {
						holder.post.setEmail(email);
					}
					text = matcher.group(2);
				}
				holder.post.setName(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));
			})
			.equals("span", "class", "postertrip")
			.content((instance, holder, text) -> holder.post
					.setTripcode(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim())))
			.text((instance, holder, source, start, end) -> {
				if (holder.headerHandling) {
					String text = source.substring(start, end).trim();
					if (text.length() > 0) {
						try {
							// noinspection ConstantConditions
							holder.post.setTimestamp(DATE_FORMAT.parse(text).getTime());
						} catch (java.text.ParseException e) {
							// Ignore exception
						}
						holder.headerHandling = false;
					}
				}
			})
			.name("blockquote")
			.content((instance, holder, text) -> {
				text = text.trim();
				int index = text.lastIndexOf("<div class=\"abbrev\">");
				if (index >= 0) {
					text = text.substring(0, index).trim();
				}
				holder.post.setComment(text);
				holder.posts.add(holder.post);
				holder.post = null;
			})
			.equals("span", "class", "omittedposts")
			.content((instance, holder, text) -> {
				if (holder.threads != null) {
					Matcher matcher = NUMBER.matcher(text);
					if (matcher.find()) {
						holder.thread.addPostsCount(Integer.parseInt(matcher.group()));
						if (matcher.find()) {
							holder.thread.addPostsWithFilesCount(Integer.parseInt(matcher.group()));
						}
					}
				}
			})
			.equals("div", "class", "logo")
			.content((instance, holder, text) -> {
				text = StringUtils.clearHtml(text).trim();
				if (!StringUtils.isEmpty(text)) {
					holder.configuration.storeBoardTitle(holder.boardName, text);
				}
			})
			.equals("table", "border", "1")
			.content((instance, holder, text) -> {
				text = StringUtils.clearHtml(text);
				int index1 = text.lastIndexOf('[');
				int index2 = text.lastIndexOf(']');
				if (index1 >= 0 && index2 > index1) {
					text = text.substring(index1 + 1, index2);
					try {
						int pagesCount = Integer.parseInt(text) + 1;
						holder.configuration.storePagesCount(holder.boardName, pagesCount);
					} catch (NumberFormatException e) {
						// Ignore exception
					}
				}
			})
			.prepare();
}
