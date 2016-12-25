package com.mishiranu.dashchan.chan.fourplebs;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.net.Uri;

import chan.content.model.FileAttachment;
import chan.content.model.Icon;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.text.ParseException;
import chan.text.TemplateParser;
import chan.util.StringUtils;

public class FourplebsPostsParser {
	private final String source;
	private final FourplebsChanLocator locator;

	private boolean needResTo = false;

	private String resTo;
	private Posts thread;
	private Post post;
	private FileAttachment attachment;
	private ArrayList<Posts> threads;
	private final ArrayList<Post> posts = new ArrayList<>();

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ssZZZZZ", Locale.US);

	private static final Pattern PATTERN_FILE = Pattern.compile("(?:(.*), )?(\\d+)(\\w+), (\\d+)x(\\d+)(?:, (.*))?");

	public FourplebsPostsParser(String source, Object linked) {
		this.source = source;
		locator = FourplebsChanLocator.get(linked);
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

	public Posts convertPosts(Uri threadUri) throws ParseException {
		PARSER.parse(source, this);
		return posts.size() > 0 ? new Posts(posts).setArchivedThreadUri(threadUri) : null;
	}

	public ArrayList<Post> convertSearch() throws ParseException {
		needResTo = true;
		PARSER.parse(source, this);
		return posts;
	}

	private String convertImageUriString(String uriString) {
		int index = uriString.indexOf("//");
		if (index >= 0) {
			index = uriString.indexOf("/", index + 2);
			return index >= 0 ? uriString.substring(index) : null;
		}
		return uriString;
	}

	private static final TemplateParser<FourplebsPostsParser> PARSER = new TemplateParser<FourplebsPostsParser>()
			.contains("article", "class", "thread").contains("article", "class", "post")
			.open((instance, holder, tagName, attributes) -> {
		String id = attributes.get("id");
		if (id != null) {
			if (attributes.get("class").contains("thread")) {
				Post post = new Post();
				post.setPostNumber(id);
				holder.resTo = id;
				holder.post = post;
				if (holder.threads != null) {
					holder.closeThread();
					holder.thread = new Posts();
				}
			} else {
				Post post = new Post();
				post.setParentPostNumber(holder.resTo);
				post.setPostNumber(id);
				holder.post = post;
			}
		}
		return false;
	}).equals("span", "class", "post_author").open((i, h, t, a) -> h.post != null).content((i, holder, text) -> {
		holder.post.setName(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));
	}).equals("span", "class", "post_tripcode").open((i, h, t, a) -> h.post != null).content((i, holder, text) -> {
		holder.post.setTripcode(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));
	}).equals("span", "class", "poster_hash").open((i, h, t, a) -> h.post != null).content((i, holder, text) -> {
		holder.post.setIdentifier(StringUtils.clearHtml(text).trim().substring(3));
	}).equals("h2", "class", "post_title").content((i, holder, text) -> {
		holder.post.setSubject(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));
	}).contains("span", "class", "flag-").open((instance, holder, tagName, attributes) -> {
		String cssClass = attributes.get("class");
		Uri uri = holder.locator.createFlagStubUri(cssClass);
		String title = StringUtils.clearHtml(attributes.get("title"));
		String end = ". Click here to search for posts with this flag";
		if (title.endsWith(end)) {
			title = title.substring(0, title.length() - end.length());
		}
		if (title.isEmpty()) {
			title = cssClass.substring(cssClass.lastIndexOf('-') + 1).toUpperCase(Locale.US);
		}
		holder.post.setIcons(new Icon(holder.locator, uri, title));
		return false;
	}).name("time").open((instance, holder, tagName, attributes) -> {
		try {
			holder.post.setTimestamp(DATE_FORMAT.parse(attributes.get("datetime")).getTime());
		} catch (java.text.ParseException e) {
			// Ignore exception
		}
		return false;
	}).equals("a", "data-function", "quote").open((instance, holder, tagName, attributes) -> {
		if (holder.needResTo) {
			holder.post.setParentPostNumber(holder.locator.getThreadNumber(Uri.parse(attributes.get("href"))));
		}
		return false;
	}).equals("a", "class", "thread_image_link").open((instance, holder, tagName, attributes) -> {
		String path = holder.convertImageUriString(attributes.get("href"));
		if (holder.attachment == null) {
			holder.attachment = new FileAttachment();
		}
		holder.attachment.setFileUri(holder.locator, holder.locator.createAttachmentUri(path));
		return false;
	}).equals("div", "class", "post_file").content((instance, holder, text) -> {
		if (holder.attachment == null) {
			holder.attachment = new FileAttachment();
		}
		if (text.contains("<span class=\"post_file_controls\">")) {
			text = text.substring(text.indexOf("</span>") + 7);
		}
		text = StringUtils.clearHtml(text).trim();
		Matcher matcher = PATTERN_FILE.matcher(text);
		if (matcher.find()) {
			int size = Integer.parseInt(matcher.group(2));
			String dim = matcher.group(3);
			if ("KiB".equals(dim)) {
				size *= 1024;
			} else if ("MiB".equals(dim)) {
				size *= 1024 * 1024;
			}
			int width = Integer.parseInt(matcher.group(4));
			int height = Integer.parseInt(matcher.group(5));
			holder.attachment.setSize(size);
			holder.attachment.setWidth(width);
			holder.attachment.setHeight(height);
			String originalName = matcher.group(1);
			if (originalName == null) {
				originalName = matcher.group(6);
			}
			if (originalName != null) {
				holder.attachment.setOriginalName(originalName);
			}
		}
	}).contains("img", "class", "thread_image").contains("img", "class", "post_image")
			.open((instance, holder, tagName, attributes) -> {
		String src = holder.convertImageUriString(attributes.get("src"));
		holder.attachment.setThumbnailUri(holder.locator, holder.locator.createAttachmentUri(src));
		return false;
	}).equals("div", "class", "text").content((instance, holder, text) -> {
		if (text != null) {
			text = text.trim();
		}
		holder.post.setComment(text);
		if (holder.attachment != null) {
			holder.post.setAttachments(holder.attachment);
		}
		holder.posts.add(holder.post);
		holder.attachment = null;
		holder.post = null;
	}).equals("span", "class", "omitted_posts").content((instance, holder, text) -> {
		holder.thread.addPostsCount(Integer.parseInt(text));
	}).equals("span", "class", "omitted_images").content((instance, holder, text) -> {
		holder.thread.addPostsWithFilesCount(Integer.parseInt(text));
	}).prepare();
}