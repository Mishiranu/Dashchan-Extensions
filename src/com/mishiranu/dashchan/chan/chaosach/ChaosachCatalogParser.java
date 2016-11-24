package com.mishiranu.dashchan.chan.chaosach;

import android.net.Uri;

import java.util.ArrayList;
import java.util.regex.Matcher;

import chan.content.model.FileAttachment;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.text.ParseException;
import chan.text.TemplateParser;
import chan.util.StringUtils;

public class ChaosachCatalogParser {
	private final String source;
	private final ChaosachChanLocator locator;

	private Post post;
	private final ArrayList<Posts> threads = new ArrayList<>();
	private String lastHref;

	public ChaosachCatalogParser(String source, Object linked) {
		this.source = source;
		locator = ChaosachChanLocator.get(linked);
	}

	public ArrayList<Posts> convert() throws ParseException {
		PARSER.parse(source, this);
		return threads;
	}

	private static final TemplateParser<ChaosachCatalogParser> PARSER = new TemplateParser<ChaosachCatalogParser>()
			.name("a").open((instance, holder, tagName, attributes) -> {
		holder.lastHref = attributes.get("href");
		return false;
	}).equals("div", "class", "post").open((instance, holder, tagName, attributes) -> {
		holder.post = new Post();
		holder.post.setPostNumber(holder.locator.getThreadNumber(Uri.parse(holder.lastHref)));
		holder.lastHref = null;
		return false;
	}).name("img").open((instance, holder, tagName, attributes) -> {
		if (holder.post != null) {
			FileAttachment attachment = new FileAttachment();
			attachment.setFileUri(holder.locator, holder.locator.buildPath(attributes.get("data-url")));
			attachment.setThumbnailUri(holder.locator, holder.locator.buildPath(attributes.get("src")));
			ChaosachPostsParser.mapFileInfo(attachment, attributes.get("title"));
			holder.post.setAttachments(attachment);
		}
		return false;
	}).equals("div", "class", "reply-title").content((instance, holder, text) -> {
		holder.post.setSubject(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));
	}).equals("div", "class", "message").content((instance, holder, text) -> {
		holder.post.setComment(text);
	}).text((instance, holder, source, start, end) -> {
		if (holder.post != null) {
			int index = source.indexOf("Replies", start);
			if (index >= start && index < end) {
				Posts thread = new Posts();
				thread.setPosts(holder.post);
				thread.addPostsCount(1);
				Matcher matcher = ChaosachPostsParser.PATTERN_NUMBER.matcher(source);
				if (matcher.find(start)) {
					thread.addPostsCount(Integer.parseInt(matcher.group()));
				}
				holder.threads.add(thread);
				holder.post = null;
			}
		}
	}).prepare();
}