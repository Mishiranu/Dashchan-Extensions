package com.mishiranu.dashchan.chan.arhivach;

import android.net.Uri;
import chan.content.ChanLocator;
import chan.content.model.FileAttachment;
import chan.content.model.Icon;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.text.ParseException;
import chan.text.TemplateParser;
import chan.util.StringUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArhivachPostsParser {
	private final String source;
	private final ArhivachChanLocator locator;
	private final String threadNumber;

	private Uri threadUri;
	private String parent;
	private Post post;
	private final ArrayList<Post> posts = new ArrayList<>();
	private final ArrayList<FileAttachment> attachments = new ArrayList<>();
	private boolean nextThumbnail;

	private static final Pattern PATTERN_NAME_SAGE = Pattern.compile("ID:( |\u00a0|&nbsp;?)Heaven");
	private static final Pattern PATTERN_CAPCODE = Pattern.compile("## (.*) ##");
	private static final Pattern PATTERN_ICON = Pattern.compile("<img.+?src=\"(.+?)\".+?(?:title=\"(.+?)\")?.+?/?>");

	static final TimeZone TIMEZONE_GMT = TimeZone.getTimeZone("Etc/GMT");

	private static final Pattern PATTERN_DATE_COMMON = Pattern.compile("(?:(\\d+) +)?(\\w+) +" +
			"(?:(\\d+):(\\d+)|(\\d{4}))");
	private static final Pattern PATTERN_DATE_1 = Pattern.compile("(\\d{2})/(\\d{2})/(\\d{2}) \\w+ " +
			"(\\d{2}):(\\d{2}):(\\d{2})");
	private static final Pattern PATTERN_DATE_2 = Pattern.compile("\\w+ (\\d{2}) (\\w+) (\\d{4}) " +
			"(\\d{2}):(\\d{2}):(\\d{2})");

	static final List<String> MONTHS_1 = Arrays.asList("января", "февраля", "марта", "апреля", "мая", "июня", "июля",
			"августа", "сентября", "октября", "ноября", "декабря");
	static final List<String> MONTHS_2 = Arrays.asList("Янв", "Фев", "Мар", "Апр", "Май", "Июн", "Июл", "Авг",
			"Сен", "Окт", "Ноя", "Дек");

	public ArhivachPostsParser(String source, Object linked, String threadNumber) {
		this.source = source;
		this.locator = ChanLocator.get(linked);
		this.threadNumber = threadNumber;
	}

	public Posts convert() throws ParseException {
		PARSER.parse(source, this);
		return new Posts(posts).setArchivedThreadUri(threadUri);
	}

	static FileAttachment parseExpandImage(TemplateParser.Attributes attributes, ArhivachChanLocator locator) {
		String onclick = attributes.get("onclick");
		if (onclick != null) {
			boolean relative = false;
			int start = onclick.indexOf("'http");
			if (start == -1) {
				start = onclick.indexOf("'/");
				if (start >= 0) {
					start++;
				}
				relative = true;
			} else {
				start++;
			}
			if (start >= 0) {
				int end = onclick.indexOf("'", start);
				if (end >= 0) {
					FileAttachment attachment = new FileAttachment();
					String uriString = onclick.substring(start, end);
					if (relative) {
						attachment.setFileUri(locator, locator.buildPath(uriString));
					} else {
						attachment.setFileUri(locator, Uri.parse(uriString));
					}
					return attachment;
				}
			}
		}
		return null;
	}

	@SuppressWarnings("UnusedReturnValue")
	static boolean parseIframeThumbnail(TemplateParser.Attributes attributes, ArrayList<FileAttachment> attachments,
			ArhivachChanLocator locator) {
		String script = attributes.get("src");
		if (script != null) {
			boolean relative = false;
			int start = script.indexOf("'http");
			if (start == -1) {
				start = script.indexOf("'/");
				if (start >= 0) {
					start++;
				}
				relative = true;
			} else {
				start++;
			}
			if (start >= 0) {
				int end = script.indexOf("\\'", start);
				if (end >= 0) {
					FileAttachment attachment = attachments.get(attachments.size() - 1);
					String uriString = script.substring(start, end);
					if (relative) {
						attachment.setThumbnailUri(locator, locator.buildPath(uriString));
					} else {
						attachment.setThumbnailUri(locator, Uri.parse(uriString));
					}
					return true;
				}
			}
		}
		return false;
	}

	static void parseImageThumbnail(TemplateParser.Attributes attributes, ArrayList<FileAttachment> attachments,
			ArhivachChanLocator locator) {
		String uriString = attributes.get("src");
		if (uriString != null) {
			FileAttachment attachment = attachments.get(attachments.size() - 1);
			if (uriString.startsWith("http")) {
				attachment.setThumbnailUri(locator, Uri.parse(uriString));
			} else {
				attachment.setThumbnailUri(locator, locator.buildPath(uriString));
			}
		}
	}

	static GregorianCalendar parseCommonTime(String date) {
		Matcher matcher = PATTERN_DATE_COMMON.matcher(date);
		if (matcher.matches()) {
			int day;
			int month;
			int year;
			int hour;
			int minute;
			GregorianCalendar calendar = new GregorianCalendar(ArhivachPostsParser.TIMEZONE_GMT);
			String dayString = matcher.group(1);
			String monthString = matcher.group(2);
			if (StringUtils.isEmpty(dayString)) {
				if ("вчера".equals(monthString)) {
					calendar.add(GregorianCalendar.DAY_OF_MONTH, -1);
				}
				day = calendar.get(GregorianCalendar.DAY_OF_MONTH);
				month = calendar.get(GregorianCalendar.MONTH);
			} else {
				day = Integer.parseInt(dayString);
				month = ArhivachPostsParser.MONTHS_1.indexOf(monthString);
			}
			String yearString = matcher.group(5);
			if (!StringUtils.isEmpty(yearString)) {
				hour = 0;
				minute = 0;
				year = Integer.parseInt(yearString);
			} else {
				hour = Integer.parseInt(matcher.group(3));
				minute = Integer.parseInt(matcher.group(4));
				year = calendar.get(GregorianCalendar.YEAR);
			}
			calendar = new GregorianCalendar(year, month, day, hour, minute, 0);
			calendar.setTimeZone(ArhivachPostsParser.TIMEZONE_GMT);
			return calendar;
		}
		return null;
	}

	private static long parseTimestamp(String date) {
		Matcher matcher = PATTERN_DATE_1.matcher(date);
		if (matcher.find()) {
			int day = Integer.parseInt(matcher.group(1));
			int month = Integer.parseInt(matcher.group(2)) - 1;
			int year = Integer.parseInt(matcher.group(3)) + 2000;
			int hour = Integer.parseInt(matcher.group(4));
			int minute = Integer.parseInt(matcher.group(5));
			int second = Integer.parseInt(matcher.group(6));
			GregorianCalendar calendar = new GregorianCalendar(year, month, day, hour, minute, second);
			calendar.setTimeZone(TIMEZONE_GMT);
			calendar.add(GregorianCalendar.HOUR, -3);
			return calendar.getTimeInMillis();
		} else {
			matcher = PATTERN_DATE_2.matcher(date);
			if (matcher.find()) {
				int day = Integer.parseInt(matcher.group(1));
				String monthString = matcher.group(2);
				int month = MONTHS_1.indexOf(monthString);
				if (month == -1) {
					month = MONTHS_2.indexOf(monthString);
				}
				if (month == -1) {
					return 0L;
				}
				int year = Integer.parseInt(matcher.group(3));
				int hour = Integer.parseInt(matcher.group(4));
				int minute = Integer.parseInt(matcher.group(5));
				int second = Integer.parseInt(matcher.group(6));
				GregorianCalendar calendar = new GregorianCalendar(year, month, day, hour, minute, second);
				calendar.setTimeZone(TIMEZONE_GMT);
				calendar.add(GregorianCalendar.HOUR, -3);
				return calendar.getTimeInMillis();
			}
		}
		GregorianCalendar calendar = parseCommonTime(date);
		if (calendar != null) {
			calendar.add(GregorianCalendar.HOUR, -3);
			return calendar.getTimeInMillis();
		}
		return 0L;
	}

	private static final TemplateParser<ArhivachPostsParser> PARSER = TemplateParser
			.<ArhivachPostsParser>builder()
			.equals("div", "class", "span3")
			.content((instance, holder, text) -> holder.threadUri = Uri.parse(StringUtils.clearHtml(text).trim()))
			.equals("div", "class", "post")
			.equals("div", "class", "post post_deleted")
			.open((instance, holder, tagName, attributes) -> {
				String number = attributes.get("postid");
				if (StringUtils.isEmpty(number)) {
					// Sometimes numbers can be empty if moder delete the post
					if (holder.posts.size() > 0) {
						number = holder.posts.get(holder.posts.size() - 1).getPostNumber();
						int index = number.indexOf('.');
						if (index >= 0) {
							number = number.substring(0, index) + "." + (Integer.parseInt
									(number.substring(index + 1)) + 1);
						} else {
							number += ".1";
						}
					} else {
						throw new ParseException();
					}
				}
				holder.post = new Post().setThreadNumber(holder.threadNumber).setPostNumber(number);
				if (holder.parent == null) {
					holder.parent = number;
				} else {
					holder.post.setParentPostNumber(holder.parent);
				}
				holder.attachments.clear();
				return false;
			})
			.equals("a", "class", "expand_image")
			.open((instance, holder, tagName, attributes) -> {
				FileAttachment attachment = ArhivachPostsParser.parseExpandImage(attributes, holder.locator);
				if (attachment != null) {
					holder.attachments.add(attachment);
					holder.nextThumbnail = true;
				}
				return false;
			})
			.name("img")
			.open((instance, holder, tagName, attributes) -> {
				if (holder.post != null && holder.nextThumbnail) {
					parseImageThumbnail(attributes, holder.attachments, holder.locator);
					holder.nextThumbnail = false;
				}
				return false;
			})
			.name("iframe")
			.open((instance, holder, tagName, attributes) -> {
				if (holder.post != null && holder.nextThumbnail) {
					parseIframeThumbnail(attributes, holder.attachments, holder.locator);
					holder.nextThumbnail = false;
				}
				return false;
			})
			.equals("h1", "class", "post_subject")
			.equals("span", "class", "post_subject")
			.content((instance, holder, text) -> holder.post
					.setSubject(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim())))
			.equals("span", "class", "poster_name")
			.content((instance, holder, text) -> {
				int index = text.indexOf("<img");
				if (index >= 0) {
					String icon = text.substring(index);
					text = text.substring(0, index);
					Matcher matcher = PATTERN_ICON.matcher(icon);
					ArrayList<Icon> icons = null;
					while (matcher.find()) {
						if (icons == null) {
							icons = new ArrayList<>();
						}
						String path = matcher.group(1);
						String title = matcher.group(2);
						Uri uri = Uri.parse(path);
						if (StringUtils.isEmpty(title)) {
							title = StringUtils.emptyIfNull(uri.getLastPathSegment());
							title = title.substring(0, title.lastIndexOf('.'));
						}
						title = StringUtils.clearHtml(title);
						icons.add(new Icon(holder.locator, uri, title));
					}
					holder.post.setIcons(icons);
				}
				String name = StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim());
				if (name != null) {
					if (PATTERN_NAME_SAGE.matcher(name).find()) {
						holder.post.setSage(true);
					} else {
						index = name.indexOf(" ID: ");
						if (index >= 0) {
							String identifier = name.substring(index + 5).replaceAll(" +", " ");
							name = name.substring(0, index);
							holder.post.setIdentifier(identifier);
						} else if (name.endsWith(" ID:")) {
							name = name.substring(0, name.length() - 4);
						}
						holder.post.setName(name);
					}
				}
			})
			.equals("span", "class", "poster_trip")
			.content((instance, holder, text) -> {
				String tripcode = StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim());
				if (tripcode != null) {
					Matcher matcher = PATTERN_CAPCODE.matcher(tripcode);
					if (matcher.matches()) {
						holder.post.setCapcode(matcher.group(1));
					} else if (tripcode.startsWith("!")) {
						holder.post.setTripcode(tripcode);
					} else if (holder.post.getIdentifier() == null) {
						holder.post.setIdentifier(tripcode);
					}
				}
			})
			.equals("a", "class", "post_mail")
			.open((instance, holder, tagName, attributes) -> {
				String email = StringUtils.nullIfEmpty(StringUtils.clearHtml(attributes.get("href")));
				if (email != null) {
					if (email.equals("mailto:sage")) {
						holder.post.setSage(true);
					} else {
						holder.post.setEmail(email);
					}
				}
				return false;
			})
			.equals("img", "class", "poster_sage")
			.open((instance, holder, tagName, attributes) -> {
				holder.post.setSage(true);
				return false;
			})
			.equals("span", "class", "post_time")
			.content((instance, holder, text) -> holder.post.setTimestamp(parseTimestamp(text.trim())))
			.equals("span", "class", "label label-success")
			.content((instance, holder, text) -> {
				if ("OP".equals(text)) {
					holder.post.setOriginalPoster(true);
				}
			})
			.equals("div", "class", "post_comment_body")
			.content((instance, holder, text) -> {
				holder.nextThumbnail = false;
				int index = text.indexOf("<span class=\"pomyanem\"");
				if (index >= 0) {
					boolean banned = text.indexOf("Помянем", index) >= 0;
					if (banned) {
						holder.post.setPosterBanned(true);
					} else {
						holder.post.setPosterWarned(true);
					}
					text = text.substring(0, index);
				}
				text = text.replace(" (OP)</a>", "</a>");
				holder.post.setComment(text);
				if (holder.attachments.size() > 0) {
					holder.post.setAttachments(holder.attachments);
				}
				holder.posts.add(holder.post);
				holder.post = null;
			})
			.prepare();
}
