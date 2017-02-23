package com.mishiranu.dashchan.chan.onechanca;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;
import android.net.Uri;

import chan.content.model.FileAttachment;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.text.ParseException;
import chan.text.TemplateParser;
import chan.util.StringUtils;

@SuppressLint("SimpleDateFormat")
public class OnechancaPostsParser {
	private final String source;
	private final OnechancaChanLocator locator;

	private String parent;
	private Posts thread;
	private Post post;
	private ArrayList<Posts> threads;
	private String externalLink;
	private final ArrayList<Post> posts = new ArrayList<>();

	private boolean headerHandling = false;
	private boolean replyParsing = false;

	private static final SimpleDateFormat DATE_FORMAT;

	static {
		DateFormatSymbols symbols = new DateFormatSymbols();
		symbols.setMonths(new String[] {"Января", "Февраля", "Марта", "Апреля", "Мая", "Июня", "Июля", "Августа",
				"Сентября", "Октября", "Ноября", "Декабря"});
		DATE_FORMAT = new SimpleDateFormat("dd MMMM yyyy @ HH:mm", symbols);
		DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT+3"));
	}

	private static final Pattern PATTERN_ATTACHMENT = Pattern.compile("(?s)<a class=\"b-image-link\".*?" +
			"href=\"(.*?)\".*?title=\"(.*?)\".*?src=\"(.*?)\".*?</a>");
	private static final Pattern PATTERN_IMAGE = Pattern.compile("(?s)<a.*?<img.*?src=\"(.*?)\".*?/>.*?</a>");
	private static final Pattern PATTERN_TTS = Pattern.compile("(?s)<audio.*?src=\"(.*?tts.voicetech.yandex.net.*?)\"" +
			".*?</audio>");
	private static final Pattern PATTERN_FILE_SIZE = Pattern.compile("(\\d+)x(\\d+), ([\\d\\.]+) (\\w+)");
	private static final Pattern PATTERN_NUMBER = Pattern.compile("\\d+");

	public OnechancaPostsParser(String source, Object linked) {
		this.source = source.replaceAll("(?s)<textarea.*?</textarea>", "");
		locator = OnechancaChanLocator.get(linked);
	}

	private void closeThread() {
		if (thread != null) {
			thread.setPosts(posts);
			thread.addPostsCount(1);
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

	private static final TemplateParser<OnechancaPostsParser> PARSER = new TemplateParser<OnechancaPostsParser>()
			.starts("div", "id", "post_").open((instance, holder, tagName, attributes) -> {
		String id = attributes.get("id");
		if (!id.endsWith("_info") && !id.equals("post_notify")) {
			String number = id.substring(5);
			int index = number.indexOf('_');
			if (index >= 0) {
				number = number.substring(index + 1);
			}
			Post post = new Post();
			post.setPostNumber(number);
			holder.parent = number;
			holder.post = post;
			holder.replyParsing = false;
			if (holder.threads != null) {
				holder.closeThread();
				holder.thread = new Posts();
			}
		}
		return false;
	}).starts("div", "id", "comment_").open((instance, holder, tagName, attributes) -> {
		String number = attributes.get("id").substring(8);
		int index = number.indexOf('_');
		if (index >= 0) {
			number = number.substring(index + 1);
		}
		Post post = new Post();
		post.setParentPostNumber(holder.parent);
		post.setPostNumber(number);
		holder.post = post;
		holder.replyParsing = true;
		return false;
	}).equals("div", "class", "b-blog-entry_b-header").open((instance, holder, tagName, attributes) -> {
		holder.headerHandling = holder.post != null;
		return false;
	}).name("div").close((instance, holder, tagName) -> {
		holder.headerHandling = false;
		if (holder.posts.size() == 1 && !holder.replyParsing) {
			holder.post = null;
		}
	}).equals("a", "class", "m-external").open((instance, holder, tagName, attributes) -> {
		if (holder.headerHandling) {
			holder.externalLink = StringUtils.clearHtml(attributes.get("href"));
		}
		return false;
	}).equals("a", "class", "b-blog-entry_b-header_m-category").open((instance, holder, tagName, attributes) -> false)
			.name("a").open((i, h, t, a) -> h.headerHandling).content((instance, holder, text) -> {
		holder.post.setSubject(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));
	}).contains("div", "class", "b-blog-entry_b-body").contains("div", "class", "b-comment_b-body")
			.open((i, h, t, a) -> h.post != null).content((instance, holder, text) -> {
		text = text.trim();
		if (holder.externalLink != null) {
			holder.externalLink = holder.externalLink.replace("\"", "&amp;")
					.replace("<", "&lt;").replace(">", "&gt;");
			text = "<p><a href=\"" + holder.externalLink + "\">" + holder.externalLink + "</a></p>" + text;
		}
		text = text.replaceAll("(?s)<blockquote>.*?<p>", "$0&gt; ");
		ArrayList<FileAttachment> attachments = new ArrayList<>();{
			Matcher matcher = PATTERN_ATTACHMENT.matcher(text);
			if (matcher.find()) {
				text = text.replace(matcher.group(), "");
				String href = matcher.group(1);
				String title = matcher.group(2);
				String src = matcher.group(3);
				if (!title.startsWith("x")) {
					FileAttachment attachment = new FileAttachment();
					attachment.setFileUri(holder.locator, Uri.parse(href));
					attachment.setThumbnailUri(holder.locator, Uri.parse(src));
					matcher = PATTERN_FILE_SIZE.matcher(title);
					if (matcher.matches()) {
						int width = Integer.parseInt(matcher.group(1));
						int height = Integer.parseInt(matcher.group(2));
						float size = Float.parseFloat(matcher.group(3));
						String dim = matcher.group(4);
						if ("KB".equals(dim)) {
							size *= 1024;
						} else if ("MB".equals(dim)) {
							size *= 1024 * 1024;
						}
						attachment.setWidth(width);
						attachment.setHeight(height);
						attachment.setSize((int) size);
					}
					attachments.add(attachment);
				}
			}
		}
		// Display smilies as text
		text = text.replaceAll("(?s)<img src=\".*?/img/(.*?)\\.\\w+\".*?>", ":$1:");
		text = StringUtils.replaceAll(text, PATTERN_IMAGE, matcher -> {
			String uriString = matcher.group(1);
			Uri uri = Uri.parse(uriString);
			if ("i.imgur.com".equals(uri.getHost())) {
				FileAttachment attachment = new FileAttachment();
				attachment.setFileUri(holder.locator, uri);
				attachment.setThumbnailUri(holder.locator, uri.buildUpon()
						.path(uri.getPath().replace(".", "m.")).build());
				attachments.add(attachment);
			}
			return "<a href=\"" + uriString + "\">" + uriString + "</a>";
		});
		text = StringUtils.replaceAll(text, PATTERN_TTS, matcher -> {
			String uriString = matcher.group(1);
			Uri uri = Uri.parse(uriString);
			String ttsText = uri.getQueryParameter("text");
			return ttsText != null ? "#%" + ttsText.replaceAll("<", "&lt;").replaceAll(">", "&gt;") + "%#" : "";
		});
		text = text.replaceAll("<p><a href=\".*?\">Читать дальше</a></p>", "");
		holder.post.setComment(text);
		holder.post.setAttachments(attachments);
		holder.posts.add(holder.post);
		if (holder.replyParsing) {
			holder.post = null;
		}
		holder.externalLink = null;
	}).text((instance, holder, source, start, end) -> {
		if (holder.post != null && source.indexOf('@', start) < end) {
			String text = source.substring(start, end).trim();
			String[] splitted = text.split(" ");
			if (splitted.length > 2 && splitted[2].equals("@")) {
				Calendar calendar = Calendar.getInstance();
				calendar.setTimeZone(DATE_FORMAT.getTimeZone());
				text = text.replace(" @", " " + calendar.get(Calendar.YEAR) + " @");
			}
			try {
				holder.post.setTimestamp(DATE_FORMAT.parse(text).getTime());
			} catch (java.text.ParseException e) {
				// Ignore exception
			}
			holder.headerHandling = false;
		}
	}).contains("span", "class", "js-comments").content((instance, holder, text) -> {
		Matcher matcher = PATTERN_NUMBER.matcher(text);
		if (matcher.matches()) {
			holder.thread.addPostsCount(Integer.parseInt(matcher.group()));
		}
	}).prepare();
}