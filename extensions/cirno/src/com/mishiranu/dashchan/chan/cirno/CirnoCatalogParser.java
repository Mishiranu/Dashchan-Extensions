package com.mishiranu.dashchan.chan.cirno;

import android.net.Uri;
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
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CirnoCatalogParser {
	private final CirnoChanLocator locator;

	private Post post;
	private final ArrayList<Posts> threads = new ArrayList<>();

	private static final Pattern LINK_TITLE = Pattern.compile("#(\\d+) \\((.*)\\)");

	public CirnoCatalogParser(Object linked) {
		locator = CirnoChanLocator.get(linked);
	}

	public ArrayList<Posts> convert(InputStream input) throws IOException, ParseException {
		PARSER.parse(new InputStreamReader(input), this);
		return threads;
	}

	private static final TemplateParser<CirnoCatalogParser> PARSER = TemplateParser
			.<CirnoCatalogParser>builder()
			.starts("a", "title", "#")
			.open((instance, holder, tagName, attributes) -> {
				Matcher matcher = LINK_TITLE.matcher(StringUtils.emptyIfNull(attributes.get("title")));
				if (matcher.matches()) {
					String number = Objects.requireNonNull(matcher.group(1));
					String date = Objects.requireNonNull(matcher.group(2));
					Post post = new Post();
					post.setPostNumber(number);
					try {
						post.setTimestamp(Objects.requireNonNull(CirnoPostsParser.DATE_FORMAT.parse(date)).getTime());
					} catch (java.text.ParseException e) {
						// Ignore exception
					}
					holder.post = post;
					holder.threads.add(new Posts(post));
				}
				return false;
			})
			.name("img")
			.open((instance, holder, tagName, attributes) -> {
				if (holder.post != null) {
					String src = attributes.get("src");
					if (src != null) {
						FileAttachment attachment = new FileAttachment();
						Uri thumbnailUri = src.contains("/thumb/") ? holder.locator.buildPath(src) : null;
						attachment.setThumbnailUri(holder.locator, thumbnailUri);
						attachment.setSpoiler(src.contains("extras/icons/spoiler.png"));
						if (thumbnailUri != null) {
							Uri fileUri = holder.locator.buildPath(src.replace("/thumb/", "/src/").replace("s.", "."));
							attachment.setFileUri(holder.locator, fileUri);
						}
						holder.post.setAttachments(attachment);
					}
				}
				return false;
			})
			.equals("span", "class", "filetitle")
			.content((instance, holder, text) -> {
				if (holder.post != null) {
					holder.post.setSubject(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));
				}
			})
			.equals("span", "class", "cattext")
			.content((instance, holder, text) -> {
				if (holder.post != null) {
					text = StringUtils.nullIfEmpty(text);
					if (text != null) {
						text = text.trim() + '\u2026';
					}
					holder.post.setComment(text);
					holder.post = null;
				}
			})
			.prepare();
}
