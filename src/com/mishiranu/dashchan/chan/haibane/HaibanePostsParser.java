package com.mishiranu.dashchan.chan.haibane;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;

import chan.content.model.FileAttachment;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.text.ParseException;
import chan.text.TemplateParser;
import chan.util.StringUtils;

@SuppressLint("SimpleDateFormat")
public class HaibanePostsParser {
	private final String source;
	private final HaibaneChanConfiguration configuration;
	private final HaibaneChanLocator locator;
	private final String boardName;

	private Posts thread;
	private Post post;
	private FileAttachment attachment;
	private ArrayList<Posts> threads;
	private final ArrayList<Post> posts = new ArrayList<>();
	private final ArrayList<FileAttachment> attachments = new ArrayList<>();

	private boolean hasPostBlock = false;
	private boolean hasPostBlockName = false;

	static final SimpleDateFormat DATE_FORMAT;

	static {
		DATE_FORMAT = new SimpleDateFormat("dd MMMM yyyy (EEE) HH:mm:ss", Locale.US);
		DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT+3"));
	}

	static final Pattern PATTERN_NUMBER = Pattern.compile("\\d+");
	private static final Pattern PATTERN_FILE_SIZE = Pattern.compile(".*?, ([\\d\\.]+) (\\w+)(?:, (\\d+)x(\\d+))?");
	private static final Pattern PATTERN_BUMP_LIMIT = Pattern.compile("Bump limit is (\\d+)");

	public HaibanePostsParser(String source, Object linked, String boardName) {
		this.source = source;
		configuration = HaibaneChanConfiguration.get(linked);
		locator = HaibaneChanLocator.get(linked);
		this.boardName = boardName;
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
		PARSER.parse(source, this);
		closeThread();
		if (threads.size() > 0) {
			updateConfiguration();
			return threads;
		}
		return null;
	}

	public ArrayList<Post> convertPosts() throws ParseException {
		PARSER.parse(source, this);
		if (posts.size() > 0) {
			updateConfiguration();
			return posts;
		}
		return null;
	}

	public Post convertSinglePost() throws ParseException {
		PARSER.parse(source, this);
		if (posts.size() == 1) {
			return posts.get(0);
		}
		return null;
	}

	private void updateConfiguration() {
		if (hasPostBlock) {
			configuration.storeNamesEnabled(boardName, hasPostBlockName);
		}
	}

	static void mapFileInfo(FileAttachment attachment, String text) {
		Matcher matcher = PATTERN_FILE_SIZE.matcher(StringUtils.clearHtml(text));
		if (matcher.find()) {
			float size = Float.parseFloat(matcher.group(1));
			String dim = matcher.group(2);
			if ("KB".equals(dim)) {
				size *= 1024f;
			} else if ("MB".equals(dim)) {
				size *= 1024f * 1024f;
			}
			attachment.setSize((int) size);
			if (matcher.group(3) != null) {
				attachment.setWidth(Integer.parseInt(matcher.group(3)));
				attachment.setHeight(Integer.parseInt(matcher.group(4)));
			}
		}
	}

	private static final TemplateParser<HaibanePostsParser> PARSER = new TemplateParser<HaibanePostsParser>()
			.contains("div", "data-post-local-id", "").open((instance, holder, tagName, attributes) -> {
		String threadNumber = attributes.get("data-thread-local-id");
		String postNumber = attributes.get("data-post-local-id");
		holder.post = new Post();
		holder.post.setPostNumber(postNumber);
		if ("0".equals(threadNumber)) {
			if (holder.threads != null) {
				holder.closeThread();
				holder.thread = new Posts();
			}
		} else {
			holder.post.setParentPostNumber(threadNumber);
		}
		return false;
	}).equals("span", "class", "reply-title").content((instance, holder, text) -> {
		holder.post.setSubject(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));
	}).equals("span", "class", "poster-name").content((instance, holder, text) -> {
		holder.post.setName(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));
	}).equals("span", "class", "time").content((instance, holder, text) -> {
		try {
			holder.post.setTimestamp(DATE_FORMAT.parse(text).getTime());
		} catch (java.text.ParseException e) {
			// Ignore exception
		}
	}).equals("div", "class", "file-name").open((instance, holder, tagName, attributes) -> {
		holder.attachment = null;
		return false;
	}).contains("a", "href", "/upload/").open((instance, holder, tagName, attributes) -> {
		if (holder.attachment == null) {
			holder.attachment = new FileAttachment();
			holder.attachment.setFileUri(holder.locator, holder.locator.buildPath(attributes.get("href")));
			holder.attachments.add(holder.attachment);
		}
		return false;
	}).equals("div", "class", "file-info").content((instance, holder, text) -> {
		mapFileInfo(holder.attachment, text);
	}).contains("img", "src", "/thumb/").open((instance, holder, tagName, attributes) -> {
		holder.attachment.setThumbnailUri(holder.locator, holder.locator.buildPath(attributes.get("src")));
		return false;
	}).equals("div", "class", "message").content((instance, holder, text) -> {
		// Fix post links
		text = StringUtils.replaceAll(text, "(<a .*?#)p\\d+(.*?>>>.*?(\\d+)</a>)",
				matcher -> matcher.group(1) + matcher.group(3) + matcher.group(2));
		holder.post.setComment(text);
		holder.posts.add(holder.post);
		if (!holder.attachments.isEmpty()) {
			holder.post.setAttachments(holder.attachments);
			holder.attachments.clear();
		}
		holder.post = null;
	}).equals("div", "class", "omitted").content((instance, holder, text) -> {
		Matcher matcher = PATTERN_NUMBER.matcher(text);
		if (matcher.find()) {
			holder.thread.addPostsCount(Integer.parseInt(matcher.group()));
		}
	}).equals("div", "id", "board-info").content((instance, holder, text) -> {
		Matcher matcher = PATTERN_BUMP_LIMIT.matcher(text);
		if (matcher.find()) {
			int bumpLimit = Integer.parseInt(matcher.group(1));
			holder.configuration.storeBumpLimit(holder.boardName, bumpLimit);
		}
	}).equals("div", "id", "board-header").open((i, h, t, a) -> h.threads != null).content((instance, holder, text) -> {
		text = StringUtils.clearHtml(text).trim();
		int index = text.indexOf("— ");
		if (index >= 0) {
			text = text.substring(index + 2);
		}
		if (!StringUtils.isEmpty(text)) {
			holder.configuration.storeBoardTitle(holder.boardName, text);
		}
	}).starts("input", "id", "hident").open((instance, holder, tagName, attributes) -> {
		holder.hasPostBlock = true;
		if ("Name".equals(attributes.get("placeholder"))) {
			holder.hasPostBlockName = true;
		}
		return false;
	}).equals("div", "class", "pagination").content((instance, holder, text) -> {
		Matcher matcher = PATTERN_NUMBER.matcher(StringUtils.clearHtml(text));
		String pagesCount = null;
		while (matcher.find()) {
			pagesCount = matcher.group();
		}
		if (pagesCount != null) {
			holder.configuration.storePagesCount(holder.boardName, Integer.parseInt(pagesCount) + 1);
		}
	}).prepare();
}