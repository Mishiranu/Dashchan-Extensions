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

public class BrchanSearchParser
{
	private final String mSource;
	private final BrchanChanLocator mLocator;

	private Post mPost;
	private final ArrayList<Post> mPosts = new ArrayList<>();

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);

	static
	{
		DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT-2"));
	}

	public BrchanSearchParser(String source, Object linked)
	{
		mSource = source;
		mLocator = BrchanChanLocator.get(linked);
	}

	public ArrayList<Post> convertPosts() throws ParseException
	{
		PARSER.parse(mSource, this);
		return mPosts;
	}

	private static final TemplateParser<BrchanSearchParser> PARSER = new TemplateParser<BrchanSearchParser>()
			.starts("div", "id", "reply_").starts("div", "id", "op_").open((instance, holder, tagName, attributes) ->
	{
		String id = attributes.get("id");
		holder.mPost = new Post().setPostNumber(id.substring(id.indexOf('_') + 1, id.length()));
		return false;

	}).equals("a", "class", "post_no").open((instance, holder, tagName, attributes) ->
	{
		String resto = holder.mLocator.getThreadNumber(Uri.parse(attributes.get("href")));
		if (!holder.mPost.getPostNumber().equals(resto)) holder.mPost.setParentPostNumber(resto);
		return false;

	}).equals("span", "class", "subject").content((instance, holder, text) ->
	{
		holder.mPost.setSubject(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));

	}).equals("span", "class", "name").content((instance, holder, text) ->
	{
		holder.mPost.setName(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));

	}).equals("span", "class", "tripcode").content((instance, holder, text) ->
	{
		holder.mPost.setTripcode(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));

	}).equals("span", "class", "capcode").content((instance, holder, text) ->
	{
		String capcode = StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim());
		if (capcode != null && capcode.startsWith("## ")) holder.mPost.setCapcode(capcode.substring(3));

	}).equals("a", "class", "email").open((instance, holder, tagName, attributes) ->
	{
		String email = attributes.get("href");
		if (email != null)
		{
			email = StringUtils.clearHtml(email);
			email = CommonUtils.restoreCloudFlareProtectedEmails("<a href=\"" + email + "\"></a>");
			email = email.substring(9, email.length() - 6);
			if (email.startsWith("mailto:")) email = email.substring(7);
			if (email.equalsIgnoreCase("sage")) holder.mPost.setSage(true); else holder.mPost.setEmail(email);
		}
		return false;

	}).contains("time", "datetime", "").open((instance, holder, tagName, attributes) ->
	{
		if (holder.mPost != null)
		{
			try
			{
				holder.mPost.setTimestamp(DATE_FORMAT.parse(attributes.get("datetime")).getTime());
			}
			catch (java.text.ParseException e)
			{

			}
		}
		return false;

	}).equals("div", "class", "body").content((instance, holder, text) ->
	{
		text = CommonUtils.restoreCloudFlareProtectedEmails(text);
		holder.mPost.setComment(text);
		holder.mPosts.add(holder.mPost);
		holder.mPost = null;

	}).prepare();
}