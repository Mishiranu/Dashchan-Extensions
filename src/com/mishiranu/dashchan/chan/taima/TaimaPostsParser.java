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
import chan.text.TemplateParser;
import chan.util.StringUtils;

public class TaimaPostsParser {
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
		PARSER.parse(source, this);
		closeThread();
		if (threads.size() > 0) {
			updateConfiguration();
			return threads;
		}
		return null;
	}

	public Posts convertPosts() throws ParseException {
		PARSER.parse(source, this);
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

	private static final TemplateParser<TaimaPostsParser> PARSER = new TemplateParser<TaimaPostsParser>()
			.equals("div", "class", "thread_header").open((instance, holder, tagName, attributes) -> {
		holder.parent = null;
		holder.post = new Post();
		if (holder.threads != null) {
			holder.closeThread();
			holder.thread = new Posts();
		}
		return false;
	}).starts("td", "id", "reply").open((instance, holder, tagName, attributes) -> {
		String number = attributes.get("id").substring(5);
		holder.post = new Post();
		holder.post.setParentPostNumber(holder.parent);
		holder.post.setPostNumber(number);
		return false;
	}).contains("a", "id", "").open((instance, holder, tagName, attributes) -> {
		if (holder.parent == null && holder.post != null) {
			String id = attributes.get("id");
			if (id != null) {
				holder.parent = id;
				holder.post.setPostNumber(id);
			}
		}
		return false;
	}).name("em").content((instance, holder, text) -> {
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
			if (holder.attachment == null) {
				holder.attachment = new FileAttachment();
				holder.post.setAttachments(holder.attachment);
			}
			holder.attachment.setSize(size);
			holder.attachment.setWidth(width);
			holder.attachment.setHeight(height);
		}
	}).contains("a", "href", "/src/").open((instance, holder, tagName, attributes) -> {
		String path = attributes.get("href");
		if (path != null) {
			if (holder.attachment == null) {
				holder.attachment = new FileAttachment();
				holder.post.setAttachments(holder.attachment);
			}
			holder.attachment.setFileUri(holder.locator, holder.locator.createSpecialBoardUri(path));
		}
		return false;
	}).equals("img", "class", "thumb").open((instance, holder, tagName, attributes) -> {
		String path = attributes.get("src");
		if (path != null && (!path.contains("/src/") || holder.attachment.getSize() < 50 * 1024)) {
			// GIF thumbnails has the same URI as image and can weigh a lot
			if (path != null) {
				holder.attachment.setThumbnailUri(holder.locator, holder.locator.createSpecialBoardUri(path));
			}
		}
		return false;
	}).equals("div", "class", "lock").open((i, h, t, a) -> !h.post.setClosed(true).isClosed())
			.equals("div", "class", "ban").open((i, h, t, a) -> !h.post.setPosterBanned(true).isPosterBanned())
			.equals("div", "class", "warn").open((i, h, t, a) -> !h.post.setPosterWarned(true).isPosterWarned())
			.equals("i", "class", "gl glyphicon-paperclip").open((i, h, t, a) -> !h.post.setSticky(true).isSticky())
			.equals("span", "class", "filetitle").content((instance, holder, text) -> {
		holder.post.setSubject(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));
	}).equals("span", "class", "postername").equals("span", "class", "commentpostername")
			.content((instance, holder, text) -> {
		holder.post.setName(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));
	}).equals("span", "class", "postertrip").content((instance, holder, text) -> {
		holder.post.setTripcode(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));
	}).equals("span", "class", "idhighlight").content((instance, holder, text) -> {
		int index = text.indexOf("ID:");
		if (index >= 0) {
			holder.post.setIdentifier(text.substring(index + 3));
		}
		index = text.indexOf("EST");
		if (index >= 0) {
			try {
				holder.post.setTimestamp(DATE_FORMAT.parse(text.substring(0, index + 3)).getTime());
			} catch (java.text.ParseException e) {
				// Ignore exception
			}
		}
	}).equals("span", "class", "omittedposts").content((instance, holder, text) -> {
		Matcher matcher = NUMBER.matcher(text);
		if (matcher.find()) {
			holder.thread.addPostsCount(Integer.parseInt(matcher.group(1)));
			if (matcher.find()) {
				holder.thread.addPostsWithFilesCount(Integer.parseInt(matcher.group(1)));
			}
		}
	}).name("blockquote").content((instance, holder, text) -> {
		text = text.trim();
		int index = text.lastIndexOf("<div class=\"abbrev\">");
		if (index >= 0) {
			text = text.substring(0, index).trim();
		}
		holder.post.setComment(text);
		holder.posts.add(holder.post);
		holder.attachment = null;
	}).equals("span", "class", "board_title").content((instance, holder, text) -> {
		text = StringUtils.clearHtml(text).trim();
		if (!StringUtils.isEmpty(text)) {
			holder.configuration.storeBoardTitle(holder.boardName, text);
		}
	}).equals("div", "class", "pagelist").content((instance, holder, text) -> {
		text = StringUtils.clearHtml(text);
		String pagesCount = null;
		Matcher matcher = NUMBER.matcher(text);
		while (matcher.find()) {
			pagesCount = matcher.group(1);
		}
		if (pagesCount != null) {
			holder.configuration.storePagesCount(holder.boardName, Integer.parseInt(pagesCount) + 1);
		}
	}).equals("form", "id", "postform").open((i, h, t, a) -> h.hasPostBlock = true)
			.equals("input", "name", "field1").open((i, h, t, a) -> h.hasPostBlockName = true).prepare();
}