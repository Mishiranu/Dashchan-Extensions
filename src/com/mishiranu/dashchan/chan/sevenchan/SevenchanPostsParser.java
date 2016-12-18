package com.mishiranu.dashchan.chan.sevenchan;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.net.Uri;

import chan.content.ChanConfiguration;
import chan.content.ChanLocator;
import chan.content.model.Attachment;
import chan.content.model.EmbeddedAttachment;
import chan.content.model.FileAttachment;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.text.ParseException;
import chan.text.TemplateParser;
import chan.util.StringUtils;

public class SevenchanPostsParser {
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
			if (threads == null) {
				configuration.storeMaxReplyFilesCount(boardName, postBlockFiles);
			}
		}
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

	private static final TemplateParser<SevenchanPostsParser> PARSER = new TemplateParser<SevenchanPostsParser>()
			.equals("div", "class", "thread").open((instance, holder, tagName, attributes) -> {
		if (holder.threads != null) {
			holder.closeThread();
			holder.thread = new Posts();
			holder.parent = null;
		}
		return false;
	}).equals("div", "class", "post").open((instance, holder, tagName, attributes) -> {
		String id = attributes.get("id");
		Post post = new Post();
		if (holder.parent == null) {
			holder.parent = id;
		} else {
			post.setParentPostNumber(holder.parent);
		}
		post.setPostNumber(id);
		holder.post = post;
		return false;
	}).equals("div", "class", "post_header").open((i, h, t, a) -> !(h.headerHandling = true))
			.equals("span", "class", "subject").content((instance, holder, text) -> {
		holder.post.setSubject(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));
	}).equals("span", "class", "postername").content((instance, holder, text) -> {
		text = text.trim();
		Matcher matcher = NAME_EMAIL.matcher(text);
		if (matcher.matches()) {
			String email = matcher.group(1);
			if (email.toLowerCase(Locale.US).equals("mailto:sage")) {
				holder.post.setSage(true);
			} else {
				holder.post.setEmail(StringUtils.clearHtml(email));
			}
			text = matcher.group(2);
		}
		holder.post.setName(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));
	}).equals("span", "class", "postertrip").content((instance, holder, text) -> {
		holder.post.setTripcode(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));
	}).equals("span", "class", "capcode").content((instance, holder, text) -> {
		holder.post.setCapcode(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).replaceAll(" ?## ?", "").trim()));
	}).text((instance, holder, source, start, end) -> {
		if (holder.headerHandling) {
			String text = source.substring(start, end).trim();
			if (text.startsWith("ID: ")) {
				holder.post.setIdentifier(StringUtils.clearHtml(text.substring(4)).trim());
			} else if (text.contains("(")) {
				try {
					holder.post.setTimestamp(DATE_FORMAT.parse(text).getTime());
				} catch (java.text.ParseException e1) {
					try {
						text = text.replaceAll("&#\\d+;", " ");
						holder.post.setTimestamp(DATE_FORMAT_WEEABOO_CLEAN.parse(text).getTime());
					} catch (java.text.ParseException e2) {
						// Ignore exception
					}
				}
			}
		}
	}).name("div").close((instance, holder, tagName) -> holder.headerHandling = false)
			.equals("img", "class", "stickied").open((i, h, t, a) -> !h.post.setSticky(true).isSticky())
			.equals("img", "class", "locked").open((i, h, t, a) -> !h.post.setClosed(true).isClosed())
			.contains("a", "href", "/src/").open((instance, holder, tagName, attributes) -> {
		if ("_blank".equals(attributes.get("target"))) {
			holder.attachment = new FileAttachment();
			holder.attachment.setFileUri(holder.locator, Uri.parse(attributes.get("href")));
			holder.attachments.add(holder.attachment);
		}
		return false;
	}).contains("img", "src", "/thumb/").open((instance, holder, tagName, attributes) -> {
		holder.attachment.setThumbnailUri(holder.locator, Uri.parse(attributes.get("src")));
		String title = attributes.get("title");
		if (title != null) {
			holder.parseFileSize(holder.attachment, title);
		}
		return false;
	}).equals("p", "class", "file_size").content((i, h, t) -> h.parseFileSize(h.attachment, t))
			.equals("p", "class", "message").content((instance, holder, text) -> {
		text = text.trim();
		if (holder.threads != null) {
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
					holder.attachments.add(attachment);
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
		holder.post.setComment(text);
		holder.posts.add(holder.post);
		if (holder.attachments.size() > 0) {
			holder.post.setAttachments(holder.attachments);
			holder.attachments.clear();
		}
		holder.post = null;
	}).equals("span", "class", "omittedposts").content((instance, holder, text) -> {
		if (holder.threads != null) {
			text = StringUtils.clearHtml(text);
			Matcher matcher = NUMBER.matcher(text);
			if (matcher.find()) {
				holder.thread.addPostsCount(Integer.parseInt(matcher.group(1)));
				if (matcher.find()) {
					holder.thread.addFilesCount(Integer.parseInt(matcher.group(1)));
				}
			}
		}
	}).equals("form", "name", "postform").open((i, h, t, a) -> !(h.hasPostBlock = true))
			.equals("input", "name", "name").open((i, h, t, a) -> !(h.hasPostBlockName = true))
			.equals("input", "name", "imagefile[]").open((i, h, t, a) -> ++h.postBlockFiles < 0)
			.equals("span", "class", "title").content((instance, holder, text) -> {
		text = StringUtils.clearHtml(text).trim();
		int index = text.lastIndexOf('\n');
		if (index > 0) {
			text = text.substring(index + 1).trim();
			holder.configuration.storeBoardTitle(holder.boardName, text);
		}
	}).equals("div", "id", "paging").content((instance, holder, text) -> {
		text = StringUtils.clearHtml(text);
		String pagesCount = null;
		Matcher matcher = NUMBER.matcher(text);
		while (matcher.find()) {
			pagesCount = matcher.group(1);
		}
		if (pagesCount != null) {
			try {
				holder.configuration.storePagesCount(holder.boardName, Integer.parseInt(pagesCount) + 1);
			} catch (NumberFormatException e) {
				// Ignore exception
			}
		}
	}).prepare();
}