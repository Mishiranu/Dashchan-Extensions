package com.mishiranu.dashchan.chan.alterchan;

import android.net.Uri;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import chan.content.model.FileAttachment;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.text.ParseException;
import chan.text.TemplateParser;
import chan.util.StringUtils;

public class AlterchanPostsParser
{
	private final String mSource;
	private final AlterchanChanConfiguration mConfiguration;
	private final AlterchanChanLocator mLocator;
	
	private String mParent;
	private Posts mThread;
	private Post mPost;
	private FileAttachment mAttachment;
	private ArrayList<Posts> mThreads;
	private final ArrayList<Post> mPosts = new ArrayList<>();
	
	private boolean mParentFromRefLink = false;
	
	private static final SimpleDateFormat DATE_FORMAT;
	
	static
	{
		DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
		DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT+4"));
	}
	
	private static final Pattern POST_REFERENCE = Pattern.compile("<span class=\"ref_id\">(\\w+)</span>");
	private static final Pattern NUMBER = Pattern.compile("\\d+");
	
	public AlterchanPostsParser(String source, Object linked)
	{
		mSource = source;
		mConfiguration = AlterchanChanConfiguration.get(linked);
		mLocator = AlterchanChanLocator.get(linked);
	}
	
	private void closeThread()
	{
		if (mThread != null)
		{
			mThread.setPosts(mPosts);
			mThread.addPostsCount(mPosts.size());
			int postsWithFilesCount = 0;
			for (Post post : mPosts) postsWithFilesCount += post.getAttachmentsCount();
			mThread.addPostsWithFilesCount(postsWithFilesCount);
			mThreads.add(mThread);
			mPosts.clear();
		}
	}
	
	public ArrayList<Posts> convertThreads() throws ParseException
	{
		mThreads = new ArrayList<>();
		PARSER.parse(mSource, this);
		closeThread();
		return mThreads;
	}
	
	public ArrayList<Post> convertPosts() throws ParseException
	{
		PARSER.parse(mSource, this);
		return mPosts;
	}
	
	public Post convertSignlePost() throws ParseException
	{
		mParentFromRefLink = true;
		PARSER.parse(mSource, this);
		return mPosts.isEmpty() ? null : mPosts.get(0);
	}
	
	private static final TemplateParser<AlterchanPostsParser> PARSER = new TemplateParser<AlterchanPostsParser>()
			.starts("a", "id", "oppost_").open((instance, holder, tagName, attributes) ->
	{
		String number = attributes.get("id").substring(7);
		if (StringUtils.isEmpty(number)) return false;
		number = holder.mLocator.convertToDecimalNumber(number);
		holder.mPost = new Post();
		holder.mPost.setPostNumber(number);
		holder.mParent = number;
		if (holder.mThreads != null)
		{
			holder.closeThread();
			holder.mThread = new Posts();
		}
		return false;
		
	}).starts("a", "id", "subpost_").open((instance, holder, tagName, attributes) ->
	{
		String number = attributes.get("id").substring(8);
		if (StringUtils.isEmpty(number)) return false;
		number = holder.mLocator.convertToDecimalNumber(number);
		holder.mPost = new Post();
		holder.mPost.setPostNumber(number);
		holder.mPost.setParentPostNumber(holder.mParent);
		return false;
		
	}).equals("input", "name", "post_pid").open((instance, holder, tagName, attributes) ->
	{
		if (holder.mPost != null)
		{
			String number = holder.mLocator.convertToDecimalNumber(attributes.get("value"));
			holder.mPost.setParentPostNumber(number);
		}
		return false;
		
	}).equals("a", "class", "post_file_name").open((instance, holder, tagName, attributes) ->
	{
		holder.mAttachment = new FileAttachment();
		holder.mAttachment.setFileUri(holder.mLocator, holder.mLocator.buildPath(attributes.get("href")));
		holder.mPost.setAttachments(holder.mAttachment);
		return false;
		
	}).equals("span", "class", null).open((instance, holder, tagName, attributes) -> holder.mAttachment != null)
			.content((instance, holder, text) ->
	{
		String[] splitted = text.split(" +");
		if (splitted.length == 2)
		{
			float size = Float.parseFloat(splitted[0]);
			if ("KB".equals(splitted[1])) size *= 1024f;
			else if ("MB".equals(splitted[1])) size *= 1024f * 1024f;
			holder.mAttachment.setSize((int) size);
		}
		
	}).equals("span", "class", "img_width").open((instance, holder, s, attributes) -> holder.mAttachment != null)
			.content((instance, holder, text) -> holder.mAttachment.setWidth(Integer.parseInt(text)))
			.equals("span", "class", "img_height").open((instance, holder, s, attributes) -> holder.mAttachment != null)
			.content((instance, holder, text) -> holder.mAttachment.setHeight(Integer.parseInt(text)))
			.equals("img", "class", "thumb").open((instance, holder, s, attributes) ->
	{
		if (holder.mAttachment != null)
		{
			holder.mAttachment.setThumbnailUri(holder.mLocator, holder.mLocator.buildPath(attributes.get("src")));
			holder.mAttachment = null;
		}
		return false;
		
	}).name("table").close((instance, holder, tagName) ->
	{
		holder.mAttachment = null;
		
	}).equals("span", "class", "post_title").content((instance, holder, text) ->
	{
		if (holder.mPost != null) holder.mPost.setSubject(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));
		
	}).equals("a", "class", "post_email").open((instance, holder, tagName, attributes) ->
	{
		if (holder.mPost != null)
		{
			String href = attributes.get("href");
			if (href != null && href.startsWith("mailto:"))
			{
				if ("mailto:sage".equals(href)) holder.mPost.setSage(true);
				else holder.mPost.setEmail(StringUtils.clearHtml(href));
			}
		}
		return false;
		
	}).equals("span", "class", "post_name").content((instance, holder, text) ->
	{
		if (holder.mPost != null)
		{
			String name = StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim());
			String email = holder.mPost.getEmail();
			if (email == null && holder.mPost.isSage()) email = "mailto:sage";
			// Name can be copied from email
			if (email == null || !email.substring(7).equals(name)) holder.mPost.setName(name);
		}
		
	}).equals("a", "class", "reflink").open((instance, holder, tagName, attributes) ->
	{
		if (holder.mPost != null && holder.mParentFromRefLink)
		{
			String threadNumber = holder.mLocator.getThreadNumber(Uri.parse(attributes.get("href")));
			holder.mPost.setParentPostNumber(threadNumber);
		}
		return false;
		
	}).contains("a", "class", "post_date").content((instance, holder, text) ->
	{
		if (holder.mPost != null)
		{
			try
			{
				holder.mPost.setTimestamp(DATE_FORMAT.parse(text.trim()).getTime());
			}
			catch (java.text.ParseException e)
			{
				
			}
		}
		
	}).name("blockquote").content((instance, holder, text) ->
	{
		if (holder.mPost != null)
		{
			// Fix links
			text = StringUtils.replaceAll(text, POST_REFERENCE, matcher -> holder.mLocator
					.convertToDecimalNumber(matcher.group(1)));
			holder.mPost.setComment(text);
			holder.mPosts.add(holder.mPost);
			holder.mPost = null;
		}
		
	}).equals("div", "class", "omittedposts").content((instance, holder, text) ->
	{
		if (holder.mThreads != null)
		{
			text = StringUtils.clearHtml(text);
			Matcher matcher = NUMBER.matcher(text);
			if (matcher.find())
			{
				holder.mThread.addPostsCount(Integer.parseInt(matcher.group()));
				if (matcher.find()) holder.mThread.addPostsWithFilesCount(Integer.parseInt(matcher.group()));
			}
		}
		
	}).equals("div", "class", "pagination").content((instance, holder, text) ->
	{
		text = StringUtils.clearHtml(text);
		String pagesCount = null;
		Matcher matcher = NUMBER.matcher(text);
		while (matcher.find()) pagesCount = matcher.group();
		if (pagesCount != null) holder.mConfiguration.storePagesCount(null, Integer.parseInt(pagesCount));
		
	}).prepare();
}