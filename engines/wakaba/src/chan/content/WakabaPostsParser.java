package chan.content;

import chan.content.model.FileAttachment;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.text.ParseException;
import chan.text.TemplateParser;
import chan.util.StringUtils;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class WakabaPostsParser<ChanConfiguration extends WakabaChanConfiguration,
		ChanLocator extends WakabaChanLocator, Holder extends
		WakabaPostsParser<ChanConfiguration, ChanLocator, Holder>> {
	private final TemplateParser<Holder> parser;
	private final String source;

	private final SimpleDateFormat dateFormat;

	protected final ChanConfiguration configuration;
	protected final ChanLocator locator;
	protected final String boardName;

	protected String parent;
	protected Posts thread;
	protected Post post;
	protected FileAttachment attachment;
	protected ArrayList<Posts> threads;
	protected final ArrayList<Post> posts = new ArrayList<>();

	protected boolean headerHandling = false;
	protected boolean originalNameFromLink;

	private static final Pattern FILE_SIZE = Pattern.compile("([\\d.]+) (\\w+), (\\d+)x(\\d+)(?:, (.+))?");
	private static final Pattern NAME_EMAIL = Pattern.compile("<a href=\"(.*?)\">(.*)</a>");
	private static final Pattern NUMBER = Pattern.compile("\\d+");

	public WakabaPostsParser(TemplateParser<Holder> parser, SimpleDateFormat dateFormat,
			String source, Object linked, String boardName) {
		this.parser = parser;
		this.source = source;
		this.dateFormat = dateFormat;
		this.configuration = WakabaChanConfiguration.get(linked);
		this.locator = WakabaChanLocator.get(linked);
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
		parseThis(parser, source);
		closeThread();
		if (threads.size() > 0) {
			updateConfiguration();
			return threads;
		}
		return null;
	}

	public ArrayList<Post> convertPosts() throws ParseException {
		parseThis(parser, source);
		if (posts.size() > 0) {
			updateConfiguration();
			return posts;
		}
		return null;
	}

	protected abstract void parseThis(TemplateParser<Holder> parser, String source) throws ParseException;

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

	private static <Holder extends WakabaPostsParser<?, ?, Holder>> WakabaPostsParser<?, ?, ?> cast(Holder holder) {
		return holder;
	}

	protected static <Holder extends WakabaPostsParser<?, ?, Holder>>
			TemplateParser.InitialBuilder<Holder> createParserBuilder() {
		return TemplateParser.<Holder>builder()
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
								cast(holder).closeThread();
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
					return false;
				})
				.equals("span", "class", "filesize")
				.open((instance, holder, tagName, attributes) -> {
					if (holder.post == null) {
						holder.post = new Post();
					}
					holder.attachment = new FileAttachment();
					return false;
				})
				.name("a")
				.open((instance, holder, tagName, attributes) -> {
					if (holder.attachment != null && holder.attachment.getFileUri(holder.locator) == null) {
						holder.attachment.setFileUri(holder.locator, holder.locator.buildPath(attributes.get("href")));
						return cast(holder).originalNameFromLink;
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
					holder.post.setAttachments(holder.attachment);
					holder.attachment = null;
					return false;
				})
				.equals("div", "class", "nothumb")
				.open((instance, holder, tagName, attributes) -> {
					if (holder.attachment.getSize() > 0 || holder.attachment.getWidth() > 0 ||
							holder.attachment.getHeight() > 0) {
						holder.post.setAttachments(holder.attachment);
					}
					holder.attachment = null;
					return false;
				})
				.name("em")
				.open((instance, holder, tagName, attributes) -> holder.attachment != null)
				.content((instance, holder, text) -> {
					Matcher matcher = FILE_SIZE.matcher(text);
					if (matcher.matches()) {
						float size = Float.parseFloat(matcher.group(1));
						String dim = StringUtils.emptyIfNull(matcher.group(2)).toUpperCase(Locale.US);
						if ("KB".equals(dim)) {
							size *= 1024f;
						} else if ("MB".equals(dim)) {
							size *= 1024f * 1024f;
						}
						int width = Integer.parseInt(matcher.group(3));
						int height = Integer.parseInt(matcher.group(4));
						String originalName = StringUtils.nullIfEmpty(matcher.group(5));
						holder.attachment.setSize((int) size);
						holder.attachment.setWidth(width);
						holder.attachment.setHeight(height);
						if (originalName != null) {
							holder.attachment.setOriginalName(originalName);
						}
					}
				})
				.equals("span", "class", "filetitle")
				.equals("span", "class", "replytitle")
				.content((instance, holder, text) -> holder.post
						.setSubject(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim())))
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
				.equals("span", "class", "postertrip")
				.content((instance, holder, text) -> holder.post
						.setTripcode(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim())))
				.text((instance, holder, source, start, end) -> {
					if (holder.headerHandling) {
						String text = source.substring(start, end).trim();
						if (text.length() > 0) {
							try {
								// noinspection ConstantConditions
								holder.post.setTimestamp(cast(holder).dateFormat.parse(text).getTime());
							} catch (java.text.ParseException e) {
								// Ignore exception
							}
							holder.headerHandling = false;
						}
					}
				})
				.name("blockquote")
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
				.equals("span", "class", "omittedposts")
				.content((instance, holder, text) -> {
					if (holder.threads != null) {
						Matcher matcher = NUMBER.matcher(text);
						if (matcher.find()) {
							holder.thread.addPostsCount(Integer.parseInt(matcher.group()));
							if (matcher.find()) {
								holder.thread.addPostsWithFilesCount(Integer.parseInt(matcher.group()));
							}
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
				});
	}
}
