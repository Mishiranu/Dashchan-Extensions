package com.mishiranu.dashchan.chan.dollchan;

import android.net.Uri;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import chan.content.ChanConfiguration;
import chan.content.ChanLocator;
import chan.content.WakabaChanConfiguration;
import chan.content.WakabaChanLocator;
import chan.content.WakabaPostsParser;
import chan.content.model.Attachment;
import chan.content.model.FileAttachment;
import chan.content.model.Icon;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.text.ParseException;
import chan.text.TemplateParser;
import chan.util.StringUtils;

public class DollchanPostsParser {
	private boolean reflinkParsing = false;

	private static final SimpleDateFormat DATE_FORMAT;

	static {
		DATE_FORMAT = new SimpleDateFormat("dd.MM.yy EEE hh:mm:ss", Locale.UK);
		DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT+3"));
	}

	protected final DollchanChanConfiguration configuration;
	protected final DollchanChanLocator locator;
	protected final String boardName;

	protected String parent;
	protected Posts thread;
	protected Post post;
	protected ArrayList<FileAttachment> attachments = null;
	protected FileAttachment attachment;
	protected ArrayList<Posts> threads;
	protected final ArrayList<Post> posts = new ArrayList<>();

	protected boolean headerHandling = false;
	protected boolean originalNameFromLink;

	private static final Pattern FILE_SIZE = Pattern.compile("([\\d.]+) (\\w+), (\\d+)x(\\d+)(?:, (.+))?");
	private static final Pattern NAME_EMAIL = Pattern.compile("<a href=\"(.*?)\">(.*)</a>");
	private static final Pattern NUMBER = Pattern.compile("\\d+");

	public DollchanPostsParser(Object linked, String boardName) {
		originalNameFromLink = true;
		this.configuration = DollchanChanConfiguration.get(linked);
		this.locator = DollchanChanLocator.get(linked);
		this.boardName = boardName;
	}

	protected void parseThis(TemplateParser<DollchanPostsParser> parser, InputStream input)
			throws IOException, ParseException {
		parser.parse(new InputStreamReader(input), this);
	}

	private void closeThread() {
		if (thread != null) {
			thread.setPosts(posts);
			thread.addPostsCount(posts.size());
			threads.add(thread);
			posts.clear();
		}
	}

	private static final TemplateParser<DollchanPostsParser> PARSER =
		TemplateParser.<DollchanPostsParser>builder()
		.equals("input", "name", "delete")
		.open((instance, holder, tagName, attributes) -> {
			if ("checkbox".equals(attributes.get("type"))) {
				holder.headerHandling = true;
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
			holder.attachments = null;
			return false;
		})
		.equals("span", "class", "filesize")
		.open((instance, holder, tagName, attributes) -> {
			if (holder.post == null) {
				holder.post = new Post();
			}
			if (holder.attachments == null) {
				holder.attachments = new ArrayList<>();
			}
			if (holder.attachment != null) {
				holder.attachments.add(holder.attachment);
			}
			holder.attachment = new FileAttachment();
			return false;
		})
		.name("a")
		.open((instance, holder, tagName, attributes) -> {
			if (holder.attachment != null && holder.attachment.getFileUri(holder.locator) == null) {
				holder.attachment.setFileUri(holder.locator, holder.locator.buildPath(attributes.get("href")));
				return holder.originalNameFromLink;
			}
			return false;
		})
		.content((instance, holder, text) -> holder.attachment
				.setOriginalName(StringUtils.clearHtml(text).trim()))
		.equals("img", "class", "thumb")
		.open((instance, holder, tagName, attributes) -> {
			String src = attributes.get("src");
			if (src != null) {
				if (src.contains("/thumb/")) {
					holder.attachment.setThumbnailUri(holder.locator, holder.locator.buildPath(src));
				}
				if (src.contains("extras/icons/spoiler.png")) {
					holder.attachment.setSpoiler(true);
				}
			}
			if (holder.attachments == null) {
				holder.attachments = new ArrayList<>();
			}
			holder.attachments.add(holder.attachment);
			holder.post.setAttachments(holder.attachments);
			holder.attachment = null;
			return false;
		})
		.equals("video", "class", "thumb")
		.open((instance, holder, tagName, attributes) -> {
			if (holder.attachments == null) {
				holder.attachments = new ArrayList<>();
			}
			holder.attachments.add(holder.attachment);
			holder.post.setAttachments(holder.attachments);
			holder.attachment = null;
			return false;
		})
		.equals("div", "class", "nothumb")
		.open((instance, holder, tagName, attributes) -> {
			if (holder.attachment.getSize() > 0 || holder.attachment.getWidth() > 0 ||
					holder.attachment.getHeight() > 0) {
				if (holder.attachments == null) {
					holder.attachments = new ArrayList<>();
				}
				holder.attachments.add(holder.attachment);
				holder.post.setAttachments(holder.attachments);
			}
			holder.attachment = null;
			return false;
		})
		.equals("span", "class", "filetitle")
		.equals("span", "class", "replytitle")
		.content((instance, holder, text) -> holder.post
				.setSubject(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim())))
		.equals("img", "class", "poster-country")
		.open((instance, holder, tagName, attributes) -> {
			String title = attributes.get("title");
			String src = attributes.get("src");
			if (title != null && src != null) {
				Uri fullSrc = holder.locator.buildPath(src);
				holder.post.setIcons(new Icon(holder.locator, fullSrc, title));
			}
			return false;
		})
		.equals("span", "class", "posteruid")
		.content((instance, holder, text) -> {
			String id = StringUtils.clearHtml(text);
			holder.post.setIdentifier(id);
		})
		.equals("span", "class", "postername")
		.equals("span", "class", "commentpostername")
		.content((instance, holder, text) -> {
			String name = text;
			String email = null;
			Matcher matcher = NAME_EMAIL.matcher(text);
			if (matcher.matches()) {
				name = matcher.group(2);
				email = StringUtils.clearHtml(matcher.group(1));
			}
			holder.setNameEmail(name, email);
		})
		.equals("span", "class", "postername postername-admin")
		.content((instance, holder, text) -> {
			holder.post.setCapcode(StringUtils.clearHtml(text));
		})
		.equals("span", "class", "postertrip")
		.content((instance, holder, text) -> holder.post
				.setTripcode(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim())))
		.text((instance, holder, source) -> {
			if (holder.headerHandling) {
				String text = source.toString().trim();
				if (text.length() > 0) {
					try {
						holder.post.setTimestamp(Objects.requireNonNull(
							DATE_FORMAT.parse(text)).getTime());
					} catch (java.text.ParseException e) {
						// Ignore exception
					}
					holder.headerHandling = false;
				}
			}
		})
		.equals("div", "class", "message")
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
		.equals("div", "class", "omittedposts")
		.content((instance, holder, text) -> {
			if (holder.threads != null) {
				Matcher matcher = NUMBER.matcher(text);
				if (matcher.find()) {
					holder.thread.addPostsCount(Integer.parseInt(matcher.group()));
				}
			}
		})
		.equals("div", "class", "logo")
		.content((instance, holder, text) -> holder.storeBoardTitle(StringUtils.clearHtml(text).trim()))
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
		.name("label")
		.open((instance, holder, tagName, attributes) -> {
			if (holder.post != null) {
				holder.headerHandling = true;
			}
			return false;
		})
		.equals("span", "class", "reflink")
		.open((instance, holder, tagName, attributes) -> {
			holder.reflinkParsing = true;
			return false;
		})
		.name("a")
		.open((instance, holder, tagName, attributes) -> {
			if (holder.reflinkParsing) {
				holder.reflinkParsing = false;
				if (holder.post != null && holder.post.getParentPostNumber() == null) {
					Uri uri = Uri.parse(attributes.get("href"));
					String threadNumber = holder.locator.getThreadNumber(uri);
					if (threadNumber != null && !threadNumber.equals(holder.post.getPostNumber())) {
						holder.post.setParentPostNumber(threadNumber);
					}
				}
			}
			return false;
		})
		.prepare();


	public ArrayList<Posts> convertThreads(InputStream input) throws IOException, ParseException {
		threads = new ArrayList<>();
		parseThis(PARSER, input);
		closeThread();
		if (threads.size() > 0) {
			updateConfiguration();
			return threads;
		}
		return null;
	}

	public ArrayList<Post> convertPosts(InputStream input) throws IOException, ParseException {
		parseThis(PARSER, input);
		if (posts.size() > 0) {
			updateConfiguration();
			return posts;
		}
		return null;
	}

	protected void updateConfiguration() {}

	protected void setNameEmail(String nameHtml, String email) {
		if (email != null) {
			if (email.toLowerCase(Locale.US).contains("sage")) {
				post.setSage(true);
			} else {
				post.setEmail(email);
			}
		}
		post.setName(StringUtils.nullIfEmpty(StringUtils.clearHtml(nameHtml).trim()));
	}

	protected void storeBoardTitle(String title) {
		if (!StringUtils.isEmpty(title)) {
			configuration.storeBoardTitle(boardName, title);
		}
	}
}
