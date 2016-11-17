package com.mishiranu.dashchan.chan.randomarchive;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.net.Uri;

import chan.content.model.FileAttachment;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.text.ParseException;
import chan.text.TemplateParser;
import chan.util.StringUtils;

public class RandomArchivePostsParser {
	private final String source;
	private final RandomArchiveChanLocator locator;
	private final RandomArchiveChanConfiguration configuration;
	private final String boardName;

	private String parent;
	private Posts thread;
	private Post post;
	private FileAttachment attachment;
	private ArrayList<Posts> threads;
	private final ArrayList<Post> posts = new ArrayList<>();

	private static final SimpleDateFormat DATE_FORMAT;

	static {
		DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.US);
		DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT+1"));
	}

	private static final Pattern FILE_SIZE = Pattern.compile("File: (.*) \\((\\d+) (\\w+), (\\d+)x(\\d+)\\).*");
	private static final Pattern NUMBER = Pattern.compile("\\d+");

	public RandomArchivePostsParser(String source, Object linked, String boardName) {
		this.source = source.replace("<img src= \"", "<img src=\""); // Fix links for parser
		locator = RandomArchiveChanLocator.get(linked);
		configuration = RandomArchiveChanConfiguration.get(linked);
		this.boardName = boardName;
	}

	private void closeThread() {
		if (thread != null) {
			thread.setPosts(posts);
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

	public Posts convertPosts(Uri threadUri) throws ParseException {
		PARSER.parse(source, this);
		return posts.size() > 0 ? new Posts(posts).setArchivedThreadUri(threadUri) : null;
	}

	private static final TemplateParser<RandomArchivePostsParser> PARSER =
			new TemplateParser<RandomArchivePostsParser>()
			.equals("div", "class", "post op").equals("div", "class", "post reply")
			.open((instance, holder, tagName, attributes) -> {
		boolean originalPost = "post op".equals(attributes.get("class"));
		if (!originalPost && holder.thread != null) {
			// Ignore replies in threads list because threads list contains
			// first replies instead of last which is not usual
			return true;
		}
		String postNumber = attributes.get("id").substring(1);
		holder.post = new Post();
		holder.post.setPostNumber(postNumber);
		if (originalPost) {
			holder.parent = postNumber;
		} else {
			holder.post.setParentPostNumber(holder.parent);
		}
		if (holder.threads != null) {
			holder.closeThread();
			holder.thread = new Posts();
		}
		return false;
	}).contains("div", "class", "postInfoM").open((i, h, t, a) -> true) // Ignore mobile block
			.equals("span", "class", "name").content((instance, holder, text) -> {
		holder.post.setName(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));
	}).equals("span", "class", "dateTime").content((instance, holder, text) -> {
		try {
			holder.post.setTimestamp(DATE_FORMAT.parse(text).getTime());
		} catch (java.text.ParseException e) {
			// Ignore exception
		}
	}).equals("div", "class", "fileText").content((instance, holder, text) -> {
		Matcher matcher = FILE_SIZE.matcher(StringUtils.clearHtml(text).trim());
		if (matcher.matches()) {
			float size = Float.parseFloat(matcher.group(2));
			String dim = matcher.group(3);
			if ("KB".equals(dim)) {
				size *= 1024f;
			} else if ("MB".equals(dim)) {
				size *= 1024f * 1024f;
			}
			int width = Integer.parseInt(matcher.group(4));
			int height = Integer.parseInt(matcher.group(5));
			if (holder.attachment == null) {
				holder.attachment = new FileAttachment();
			}
			holder.attachment.setOriginalName(matcher.group(1));
			holder.attachment.setSize((int) size);
			holder.attachment.setWidth(width);
			holder.attachment.setHeight(height);
		}
	}).equals("a", "class", "fileThumb").open((instance, holder, tagName, attributes) -> {
		if (holder.attachment == null) {
			holder.attachment = new FileAttachment();
		}
		holder.attachment.setFileUri(holder.locator, Uri.parse(attributes.get("href")));
		return false;
	}).contains("img", "src", "/thumb/").open((instance, holder, tagName, attributes) -> {
		holder.attachment.setThumbnailUri(holder.locator, Uri.parse(attributes.get("src")));
		return false;
	}).name("blockquote").content((instance, holder, text) -> {
		text = StringUtils.linkify(text);
		holder.post.setComment(text);
		if (holder.attachment != null) {
			holder.post.setAttachments(holder.attachment);
			holder.attachment = null;
		}
		holder.posts.add(holder.post);
		holder.post = null;
	}).equals("span", "class", "summary desktop").content((instance, holder, text) -> {
		if (holder.threads != null) {
			Matcher matcher = NUMBER.matcher(text);
			if (matcher.find()) {
				holder.thread.addPostsCount(Integer.parseInt(matcher.group()));
				if (matcher.find()) {
					holder.thread.addPostsWithFilesCount(Integer.parseInt(matcher.group()));
				}
			}
		}
	}).equals("div", "class", "pages").content((instance, holder, text) -> {
		text = StringUtils.clearHtml(text);
		int index1 = text.lastIndexOf('[');
		int index2 = text.lastIndexOf(']');
		if (index1 >= 0 && index2 > index1) {
			text = text.substring(index1 + 1, index2);
			try {
				holder.configuration.storePagesCount(holder.boardName, Integer.parseInt(text));
			} catch (NumberFormatException e) {
				// Ignore exception
			}
		}
	}).prepare();
}