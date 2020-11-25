package com.mishiranu.dashchan.chan.arhivach;

import chan.content.ChanConfiguration;
import chan.content.ChanLocator;
import chan.content.model.FileAttachment;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.text.ParseException;
import chan.text.TemplateParser;
import chan.util.StringUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArhivachThreadsParser {
	private final ArhivachChanConfiguration configuration;
	private final ArhivachChanLocator locator;
	private final boolean handlePagesCount;

	private Post post;
	private final LinkedHashMap<Post, Integer> postHolders = new LinkedHashMap<>();
	private final ArrayList<FileAttachment> attachments = new ArrayList<>();
	private boolean nextThumbnail;

	private static final Pattern PATTERN_BLOCK_TEXT = Pattern.compile("<a style=\"display:block;\".*?>(.*)</a>");
	private static final Pattern PATTERN_SUBJECT = Pattern.compile("^<b>(.*?)</b> &mdash; ");
	private static final Pattern PATTERN_NOT_ARCHIVED = Pattern.compile("<a.*?>\\[.*?] Ожидание обновления</a>");

	public ArhivachThreadsParser(Object linked, boolean handlePagesCount) {
		this.configuration = ChanConfiguration.get(linked);
		this.locator = ChanLocator.get(linked);
		this.handlePagesCount = handlePagesCount;
	}

	public ArrayList<Posts> convertThreads(InputStream input) throws IOException, ParseException {
		PARSER.parse(new InputStreamReader(input), this);
		if (postHolders.size() > 0) {
			ArrayList<Posts> threads = new ArrayList<>(postHolders.size());
			for (LinkedHashMap.Entry<Post, Integer> entry : postHolders.entrySet()) {
				threads.add(new Posts(entry.getKey()).addPostsCount(entry.getValue()));
			}
			return threads;
		}
		return null;
	}

	public ArrayList<Post> convertPosts(InputStream input) throws IOException, ParseException {
		PARSER.parse(new InputStreamReader(input), this);
		if (postHolders.size() > 0) {
			ArrayList<Post> posts = new ArrayList<>(postHolders.size());
			posts.addAll(postHolders.keySet());
			return posts;
		}
		return null;
	}

	private static final TemplateParser<ArhivachThreadsParser> PARSER = TemplateParser
			.<ArhivachThreadsParser>builder()
			.starts("tr", "id", "thread_row_")
			.open((instance, holder, tagName, attributes) -> {
				String number = StringUtils.emptyIfNull(attributes.get("id")).substring(11);
				holder.post = new Post().setThreadNumber(number).setPostNumber(number);
				holder.attachments.clear();
				return false;
			})
			.equals("span", "class", "thread_posts_count")
			.content((instance, holder, text) -> {
				int postsCount = Integer.parseInt(text.trim());
				if (postsCount >= 0) {
					holder.postHolders.put(holder.post, postsCount);
				} else {
					holder.post = null; // Thread is not archived
				}
			})
			.equals("a", "class", "expand_image")
			.open((instance, holder, tagName, attributes) -> {
				if (holder.post != null) {
					FileAttachment attachment = ArhivachPostsParser.parseExpandImage(attributes, holder.locator);
					if (attachment != null) {
						holder.attachments.add(attachment);
						holder.nextThumbnail = true;
					}
				}
				return false;
			})
			.name("img")
			.open((instance, holder, tagName, attributes) -> {
				if (holder.post != null && holder.nextThumbnail) {
					ArhivachPostsParser.parseImageThumbnail(attributes, holder.attachments, holder.locator);
					holder.nextThumbnail = false;
				}
				return false;
			})
			.name("iframe")
			.open((instance, holder, tagName, attributes) -> {
				if (holder.post != null && holder.nextThumbnail) {
					ArhivachPostsParser.parseIframeThumbnail(attributes, holder.attachments, holder.locator);
					holder.nextThumbnail = false;
				}
				return false;
			})
			.equals("div", "class", "thread_text")
			.open((instance, holder, tagName, attributes) -> holder.post != null)
			.content((instance, holder, text) -> {
				holder.nextThumbnail = false;
				text = text.trim();
				if (PATTERN_NOT_ARCHIVED.matcher(text).matches()) {
					holder.postHolders.remove(holder.post);
					holder.post = null; // Thread is not archived
					return;
				}
				Matcher matcher = PATTERN_BLOCK_TEXT.matcher(text);
				if (matcher.matches()) {
					text = StringUtils.emptyIfNull(matcher.group(1));
				}
				matcher = PATTERN_SUBJECT.matcher(text);
				if (matcher.find()) {
					holder.post.setSubject(StringUtils.nullIfEmpty(StringUtils.clearHtml(matcher.group(1)).trim()));
					text = text.substring(StringUtils.emptyIfNull(matcher.group(0)).length());
				}
				if (text.length() > 500 && !text.endsWith(".")) {
					text += '\u2026';
				}
				holder.post.setComment(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));
			})
			.equals("td", "class", "thread_date")
			.open((instance, holder, tagName, attributes) -> holder.post != null)
			.content((instance, holder, text) -> {
				GregorianCalendar calendar = ArhivachPostsParser.parseCommonTime(text);
				if (calendar != null) {
					calendar.add(GregorianCalendar.HOUR, -3);
					holder.post.setTimestamp(calendar.getTimeInMillis());
				}
				if (holder.attachments.size() > 0) {
					holder.post.setAttachments(holder.attachments);
				}
				holder.post = null;
			})
			.equals("a", "title", "Последняя страница")
			.open((instance, holder, tagName, a) -> holder.handlePagesCount)
			.content((instance, holder, text) -> {
				try {
					holder.configuration.storePagesCount(null, Integer.parseInt(text.trim()));
				} catch (NumberFormatException e) {
					// Ignore exception
				}
			})
			.prepare();
}
