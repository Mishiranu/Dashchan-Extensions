package com.mishiranu.dashchan.chan.chiochan;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import chan.content.model.FileAttachment;
import chan.content.model.Icon;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.text.ParseException;
import chan.text.TemplateParser;
import chan.util.StringUtils;

public class ChiochanPostsParser {
	private final String source;
	private final ChiochanChanConfiguration configuration;
	private final ChiochanChanLocator locator;
	private final String boardName;

	private String parent;
	private Posts thread;
	private Post post;
	private FileAttachment attachment;
	private ArrayList<Posts> threads;
	private final ArrayList<Post> posts = new ArrayList<>();
	private boolean expandMode = false;

	private boolean headerHandling = false;

	private boolean hasPostBlock = false;
	private boolean hasPostBlockName = false;

	private static final SimpleDateFormat DATE_FORMAT;

	static {
		DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.US);
		DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT+3"));
	}

	private static final Pattern FILE_SIZE = Pattern.compile("\\(([\\d\\.]+)(\\w+) *, *(\\d+)[x×](\\d+)" +
			"(?: *, *(.+))? *\\) *$");
	private static final Pattern NAME_EMAIL = Pattern.compile("<a href=\"(.*?)\">(.*)</a>");
	static final Pattern NUMBER = Pattern.compile("(\\d+)");
	private static final Pattern BUMP_LIMIT = Pattern.compile("Максимальное количество бампов треда: (\\d+)");

	public ChiochanPostsParser(String source, Object linked, String boardName) {
		this.source = source;
		configuration = ChiochanChanConfiguration.get(linked);
		locator = ChiochanChanLocator.get(linked);
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

	public ArrayList<Post> convertPosts() throws ParseException {
		PARSER.parse(source, this);
		if (posts.size() > 0) {
			updateConfiguration();
			return posts;
		}
		return null;
	}

	public ArrayList<Post> convertExpand() throws ParseException {
		expandMode = true;
		PARSER.parse(source, this);
		if (posts.size() > 0) {
			updateConfiguration();
			return posts;
		}
		return null;
	}

	private void updateConfiguration() {
		if (hasPostBlock) {
			configuration.storeNamesEnabled(boardName, hasPostBlockName);
		}
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

	private static final TemplateParser<ChiochanPostsParser> PARSER = new TemplateParser<ChiochanPostsParser>()
			.starts("div", "id", "thread").open((instance, holder, tagName, attributes) -> {
		String id = attributes.get("id");
		String number = id.substring(6, id.length() - holder.boardName.length());
		holder.post = new Post();
		holder.post.setPostNumber(number);
		holder.parent = number;
		if (holder.threads != null) {
			holder.closeThread();
			holder.thread = new Posts();
		}
		return false;
	}).starts("div", "id", "reply").starts("td", "id", "reply").open((instance, holder, tagName, attributes) -> {
		String number = attributes.get("id").substring(5);
		holder.post = new Post();
		holder.post.setParentPostNumber(holder.parent);
		holder.post.setPostNumber(number);
		return false;
	}).name("input").open((instance, holder, tagName, attributes) -> {
		if (holder.expandMode) {
			String name = attributes.get("name");
			if ("delete[]".equals(name)) {
				if (holder.post == null) {
					holder.post = new Post();
				}
				holder.post.setPostNumber(attributes.get("value"));
			}
		}
		return false;
	}).name("label").open((instance, holder, tagName, attributes) -> {
		if (holder.post != null) {
			holder.headerHandling = true;
		}
		return false;
	}).equals("span", "class", "filesize").content((instance, holder, text) -> {
		if (holder.expandMode && holder.post == null) {
			holder.post = new Post();
		}
		holder.attachment = new FileAttachment();
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
			holder.attachment.setSize((int) size);
			holder.attachment.setWidth(width);
			holder.attachment.setHeight(height);
			holder.attachment.setOriginalName(StringUtils.isEmptyOrWhitespace(fileName) ? null : fileName.trim());
		}
	}).contains("a", "href", "/src/").open((instance, holder, tagName, attributes) -> {
		if (holder.attachment != null) {
			String path = holder.convertUriString(attributes.get("href"));
			holder.attachment.setFileUri(holder.locator, holder.locator.buildPath(path));
			holder.post.setAttachments(holder.attachment);
		}
		return false;
	}).equals("img", "class", "thumb").open((instance, holder, tagName, attributes) -> {
		String path = holder.convertUriString(attributes.get("src"));
		holder.attachment.setThumbnailUri(holder.locator, holder.locator.buildPath(path));
		return false;
	}).equals("span", "class", "filetitle").content((instance, holder, text) -> {
		text = text.trim();
		if (text.endsWith("\u21e9")) {
			text = StringUtils.nullIfEmpty(text.substring(0, text.length() - 1));
			holder.post.setSage(true);
		}
		holder.post.setSubject(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));
	}).equals("span", "class", "postername").content((instance, holder, text) -> {
		text = text.trim();
		Matcher matcher = NAME_EMAIL.matcher(text);
		if (matcher.matches()) {
			String email = matcher.group(1);
			if (email != null && email.toLowerCase(Locale.US).contains("sage")) {
				// Old chiochan sage appearance
				holder.post.setSage(true);
			}
			text = matcher.group(2);
		}
		holder.post.setName(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));
	}).equals("span", "class", "postertrip").content((instance, holder, text) -> {
		holder.post.setTripcode(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));
	}).equals("span", "class", "admin").content((instance, holder, text) -> {
		holder.post.setCapcode("Admin");
	}).contains("img", "src", "/flags/").open((instance, holder, tagName, attributes) -> {
		String path = holder.convertUriString(attributes.get("src"));
		String title = StringUtils.clearHtml(attributes.get("alt"));
		holder.post.setIcons(new Icon(holder.locator, holder.locator.buildPath(path), title));
		return false;
	}).text((instance, holder, source, start, end) -> {
		if (holder.headerHandling) {
			String text = source.substring(start, end).trim();
			if (text.length() > 0) {
				int index1 = text.indexOf('(');
				int index2 = text.indexOf(')');
				if (index2 > index1 && index1 > 0) {
					// Remove week in brackets
					text = text.substring(0, index1 - 1) + text.substring(index2 + 1);
				}
				try {
					holder.post.setTimestamp(DATE_FORMAT.parse(text).getTime());
				} catch (java.text.ParseException e) {
					// Ignore exception
				}
				holder.headerHandling = false;
			}
		}
	}).ends("img", "src", "/css/sticky.gif").open((instance, holder, tagName, attributes) -> {
		if (holder.post != null) {
			holder.post.setSticky(true);
		}
		return false;
	}).ends("img", "src", "/css/locked.gif").open((instance, holder, tagName, attributes) -> {
		if (holder.post != null) {
			holder.post.setClosed(true);
		}
		return false;
	}).name("blockquote").content((instance, holder, text) -> {
		text = text.trim();
		int index = text.lastIndexOf("<div class=\"abbrev\">");
		if (index >= 0) {
			text = text.substring(0, index).trim();
		}
		index = text.lastIndexOf("<font color=\"#FF0000\">");
		if (index >= 0) {
			String message = text.substring(index);
			text = text.substring(0, index);
			if (message.contains("USER WAS BANNED FOR THIS POST") ||
					message.contains("ПОТРЕБИТЕЛЬ БЫЛ ЗАПРЕЩЁН ДЛЯ ЭТОГО СТОЛБА")) {
				holder.post.setPosterBanned(true);
			}
		}
		holder.post.setComment(text);
		holder.posts.add(holder.post);
		holder.post = null;
		holder.attachment = null;
	}).equals("span", "class", "omittedposts").content((instance, holder, text) -> {
		if (holder.thread != null) {
			Matcher matcher = NUMBER.matcher(text);
			if (matcher.find()) {
				holder.thread.addPostsCount(Integer.parseInt(matcher.group(1)));
				if (matcher.find()) {
					holder.thread.addPostsWithFilesCount(Integer.parseInt(matcher.group(1)));
				}
			}
		}
	}).equals("td", "class", "rules").content((instance, holder, text) -> {
		Matcher matcher = BUMP_LIMIT.matcher(text);
		if (matcher.find()) {
			int bumpLimit = Integer.parseInt(matcher.group(1));
			holder.configuration.storeBumpLimit(holder.boardName, bumpLimit);
		}
	}).equals("div", "class", "logo").content((instance, holder, text) -> {
		text = StringUtils.clearHtml(text).trim();
		if (!StringUtils.isEmpty(text)) {
			holder.configuration.storeBoardTitle(holder.boardName, text);
		}
	}).equals("td", "class", "postblock").content((instance, holder, text) -> {
		holder.hasPostBlock = true;
		if ("Имя".equals(text) || "Name".equals(text)) {
			holder.hasPostBlockName = true;
		}
	}).equals("div", "class", "pgstbl").content((instance, holder, text) -> {
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
	}).prepare();
}