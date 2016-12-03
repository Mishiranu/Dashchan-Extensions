package com.mishiranu.dashchan.chan.tiretirech;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;
import chan.content.ChanConfiguration;
import chan.content.ChanLocator;
import chan.content.model.EmbeddedAttachment;
import chan.content.model.FileAttachment;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.text.ParseException;
import chan.text.TemplateParser;
import chan.util.CommonUtils;
import chan.util.StringUtils;

@SuppressLint("SimpleDateFormat")
public class TiretirechPostsParser {
	private final String source;
	private final TiretirechChanConfiguration configuration;
	private final TiretirechChanLocator locator;
	private final String boardName;

	private String parent;
	private Posts thread;
	private Post post;
	private FileAttachment attachment;
	private ArrayList<Posts> threads;
	private final ArrayList<Post> posts = new ArrayList<>();
	private final ArrayList<FileAttachment> attachments = new ArrayList<>();

	private static final SimpleDateFormat DATE_FORMAT;

	static {
		DateFormatSymbols symbols = new DateFormatSymbols();
		symbols.setShortWeekdays(new String[] {"", "Вс", "Пн", "Вт", "Ср", "Чт", "Пт", "Сб"});
		symbols.setMonths(new String[] {"Января", "Февраля", "Марта", "Апреля", "Мая", "Июня", "Июля", "Августа",
				"Сентября", "Октября", "Ноября", "Декабря"});
		DATE_FORMAT = new SimpleDateFormat("EE dd MMMM yyyy HH:mm:ss", symbols);
		DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT+3"));
	}

	private static final Pattern FILE_SIZE = Pattern.compile("\\(([\\d\\.]+) (\\w+)(?:, *(\\d+)x(\\d+))?");
	private static final Pattern NAME_EMAIL = Pattern.compile("<a href=\"(.*?)\">(.*)</a>");
	private static final Pattern NUMBER = Pattern.compile("\\d+");

	public TiretirechPostsParser(String source, Object linked, String boardName) {
		this.source = source;
		configuration = ChanConfiguration.get(linked);
		locator = ChanLocator.get(linked);
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

	private static final TemplateParser<TiretirechPostsParser> PARSER = new TemplateParser<TiretirechPostsParser>()
			.equals("div", "class", "threadz").open((instance, holder, tagName, attributes) -> {
		String id = attributes.get("id");
		if (id != null) {
			String number = id.substring(1);
			Post post = new Post();
			post.setPostNumber(number);
			holder.parent = number;
			holder.post = post;
			if (holder.threads != null) {
				holder.closeThread();
				holder.thread = new Posts();
			}
		}
		return false;
	}).starts("td", "id", "reply").open((instance, holder, tagName, attributes) -> {
		String number = attributes.get("id").substring(5);
		Post post = new Post();
		post.setParentPostNumber(holder.parent);
		post.setPostNumber(number);
		holder.post = post;
		return false;
	}).equals("span", "class", "filesize").content((instance, holder, text) -> {
		holder.attachment = new FileAttachment();
		holder.attachments.add(holder.attachment);
		text = StringUtils.clearHtml(text);
		Matcher matcher = FILE_SIZE.matcher(text);
		if (matcher.find()) {
			float size = Float.parseFloat(matcher.group(1));
			String dim = matcher.group(2);
			if ("Кб".equals(dim)) {
				size *= 1024;
			} else if ("Мб".equals(dim)) {
				size *= 1024 * 1024;
			}
			holder.attachment.setSize((int) size);
			if (matcher.group(3) != null) {
				holder.attachment.setWidth(Integer.parseInt(matcher.group(3)));
				holder.attachment.setHeight(Integer.parseInt(matcher.group(4)));
			}
		}
	}).name("a").name("source").open((instance, holder, tagName, attributes) -> {
		if (holder.attachment != null) {
			String path = attributes.get("source".equals(tagName) ? "src" : "href");
			holder.attachment.setFileUri(holder.locator, holder.locator.buildPath(path));
		}
		return false;
	}).equals("img", "class", "thumb").open((instance, holder, tagName, attributes) -> {
		String path = attributes.get("src");
		holder.attachment.setThumbnailUri(holder.locator, holder.locator.buildPath(path));
		return false;
	}).name("iframe").open((instance, holder, tagName, attributes) -> {
		EmbeddedAttachment attachment = EmbeddedAttachment.obtain(attributes.get("src"));
		if (attachment != null) {
			holder.post.setAttachments(attachment);
		}
		return false;
	}).equals("span", "class", "filetitle").equals("span", "class", "replytitle").content((i, holder, text) -> {
		holder.post.setSubject(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));
	}).equals("span", "class", "postername").equals("span", "class", "commentpostername").content((i, holder, text) -> {
		text = text.trim();
		Matcher matcher = NAME_EMAIL.matcher(text);
		if (matcher.matches()) {
			String email = matcher.group(1);
			if (email.toLowerCase(Locale.US).equals("sage")) {
				holder.post.setSage(true);
			} else {
				holder.post.setEmail(StringUtils.clearHtml(email));
			}
			text = matcher.group(2);
		}
		if ("<font color=\"#0000FF\">V.</font>".equals(text)) {
			holder.post.setCapcode("Admin");
		} else {
			holder.post.setName(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));
		}
	}).equals("span", "class", "postertrip").content((instance, holder, text) -> {
		holder.post.setTripcode(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim().replace('\u2665', '!')));
	}).equals("span", "class", "postdate").content((instance, holder, text) -> {
		try {
			holder.post.setTimestamp(DATE_FORMAT.parse(text).getTime());
		} catch (java.text.ParseException e) {
			// Ignore exception
		}
	}).equals("img", "src", "/lib/img/close.png").open((instance, holder, tagName, attributes) -> {
		holder.post.setClosed(true);
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
			if (message.contains("USER WAS BANNED FOR THIS POST")) {
				holder.post.setPosterBanned(true);
			}
		}
		text = CommonUtils.restoreCloudFlareProtectedEmails(text);
		holder.post.setComment(text);
		if (holder.attachments.size() > 0) {
			holder.post.setAttachments(holder.attachments);
			holder.attachments.clear();
		}
		holder.attachment = null;
		holder.posts.add(holder.post);
		holder.post = null;
	}).equals("span", "class", "omittedposts").open((i, h, t, a) -> h.threads != null).content((i, holder, text) -> {
		Matcher matcher = NUMBER.matcher(text);
		if (matcher.find()) {
			holder.thread.addPostsCount(Integer.parseInt(matcher.group()));
			if (matcher.find()) {
				holder.thread.addPostsWithFilesCount(Integer.parseInt(matcher.group()));
			}
		}
	}).equals("div", "class", "logo").content((instance, holder, text) -> {
		text = StringUtils.clearHtml(text).trim();
		if (text.startsWith("Тире.ч — ")) {
			text = text.substring(9);
		}
		if (!StringUtils.isEmpty(text)) {
			holder.configuration.storeBoardTitle(holder.boardName, text);
		}
	}).equals("table", "border", "1").content((instance, holder, text) -> {
		String pagesCount = null;
		Matcher matcher = NUMBER.matcher(text);
		while (matcher.find()) {
			pagesCount = matcher.group();
		}
		if (pagesCount != null) {
			try {
				holder.configuration.storePagesCount(holder.boardName, Integer.parseInt(pagesCount));
			} catch (NumberFormatException e) {
				// Ignore exception
			}
		}
	}).prepare();
}