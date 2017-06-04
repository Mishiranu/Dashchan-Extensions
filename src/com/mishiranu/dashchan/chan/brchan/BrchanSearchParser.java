package com.mishiranu.dashchan.chan.brchan;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TimeZone;

import android.net.Uri;

import chan.content.model.Post;
import chan.text.ParseException;
import chan.text.TemplateParser;
import chan.util.CommonUtils;
import chan.util.StringUtils;

public class BrchanSearchParser {
	private final String source;
	private final BrchanChanLocator locator;

	private Post post;
	private final ArrayList<Post> posts = new ArrayList<>();

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);

	static {
		DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT-2"));
	}

	public BrchanSearchParser(String source, Object linked) {
		this.source = source;
		locator = BrchanChanLocator.get(linked);
	}

	public ArrayList<Post> convertPosts() throws ParseException {
		PARSER.parse(source, this);
		return posts;
	}

	private static final TemplateParser<BrchanSearchParser> PARSER = new TemplateParser<BrchanSearchParser>()
			.starts("div", "id", "reply_").starts("div", "id", "op_").open((instance, holder, tagName, attributes) -> {
		String id = attributes.get("id");
		holder.post = new Post().setPostNumber(id.substring(id.indexOf('_') + 1, id.length()));
		return false;
	}).equals("a", "class", "post_no").open((instance, holder, tagName, attributes) -> {
		String resto = holder.locator.getThreadNumber(Uri.parse(attributes.get("href")));
		if (!holder.post.getPostNumber().equals(resto)) {
			holder.post.setParentPostNumber(resto);
		}
		return false;
	}).equals("span", "class", "subject").content((instance, holder, text) -> {
		holder.post.setSubject(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));
	}).equals("span", "class", "name").content((instance, holder, text) -> {
		holder.post.setName(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));
	}).equals("span", "class", "tripcode").content((instance, holder, text) -> {
		holder.post.setTripcode(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));
	}).equals("span", "class", "capcode").content((instance, holder, text) -> {
		String capcode = StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim());
		if (capcode != null && capcode.startsWith("## ")) {
			holder.post.setCapcode(capcode.substring(3));
		}
	}).equals("a", "class", "email").open((instance, holder, tagName, attributes) -> {
		String email = attributes.get("href");
		if (email != null) {
			email = StringUtils.clearHtml(email);
			email = CommonUtils.restoreCloudFlareProtectedEmails("<a href=\"" + email + "\"></a>");
			email = email.substring(9, email.length() - 6);
			if (email.startsWith("mailto:")) {
				email = email.substring(7);
			}
			if (email.equalsIgnoreCase("sage")) {
				holder.post.setSage(true);
			} else {
				holder.post.setEmail(email);
			}
		}
		return false;
	}).contains("time", "datetime", "").open((instance, holder, tagName, attributes) -> {
		if (holder.post != null) {
			try {
				holder.post.setTimestamp(DATE_FORMAT.parse(attributes.get("datetime")).getTime());
			} catch (java.text.ParseException e) {
				// Ignore exception
			}
		}
		return false;
	}).equals("div", "class", "body").content((instance, holder, text) -> {
		text = CommonUtils.restoreCloudFlareProtectedEmails(text);
		holder.post.setComment(text);
		holder.posts.add(holder.post);
		holder.post = null;
	}).prepare();
}
