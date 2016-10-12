package com.mishiranu.dashchan.chan.fourplebs;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.net.Uri;

import chan.content.ChanLocator;
import chan.content.model.FileAttachment;
import chan.content.model.Icon;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.text.ParseException;
import chan.text.TemplateParser;
import chan.util.StringUtils;

public class FourplebsPostsParser
{
	private final String mSource;
	private final FourplebsChanLocator mLocator;

	private boolean mNeedResTo = false;

	private String mResTo;
	private Posts mThread;
	private Post mPost;
	private FileAttachment mAttachment;
	private ArrayList<Posts> mThreads;
	private final ArrayList<Post> mPosts = new ArrayList<>();

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ssZZZZZ", Locale.US);

	private static final Pattern PATTERN_FILE = Pattern.compile("(?:(.*), )?(\\d+)(\\w+), (\\d+)x(\\d+)(?:, (.*))?");

	public FourplebsPostsParser(String source, Object linked)
	{
		mSource = source;
		mLocator = ChanLocator.get(linked);
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

	public Posts convertPosts(Uri threadUri) throws ParseException
	{
		PARSER.parse(mSource, this);
		return mPosts.size() > 0 ? new Posts(mPosts).setArchivedThreadUri(threadUri) : null;
	}

	public ArrayList<Post> convertSearch() throws ParseException
	{
		mNeedResTo = true;
		PARSER.parse(mSource, this);
		return mPosts;
	}

	private String convertImageUriString(String uriString)
	{
		int index = uriString.indexOf("//");
		if (index >= 0)
		{
			index = uriString.indexOf("/", index + 2);
			return index >= 0 ? uriString.substring(index) : null;
		}
		return uriString;
	}

	private static final TemplateParser<FourplebsPostsParser> PARSER = new TemplateParser<FourplebsPostsParser>()
			.contains("article", "class", "thread").contains("article", "class", "post")
			.open((instance, holder, tagName, attributes) ->
	{
		String id = attributes.get("id");
		if (id != null)
		{
			if (attributes.get("class").contains("thread"))
			{
				Post post = new Post();
				post.setPostNumber(id);
				holder.mResTo = id;
				holder.mPost = post;
				if (holder.mThreads != null)
				{
					holder.closeThread();
					holder.mThread = new Posts();
				}
			}
			else
			{
				Post post = new Post();
				post.setParentPostNumber(holder.mResTo);
				post.setPostNumber(id);
				holder.mPost = post;
			}
		}
		return false;

	}).equals("span", "class", "post_author").open((i, h, t, a) -> h.mPost != null).content((i, holder, text) ->
	{
		holder.mPost.setName(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));

	}).equals("span", "class", "post_tripcode").open((i, h, t, a) -> h.mPost != null).content((i, holder, text) ->
	{
		holder.mPost.setTripcode(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));

	}).equals("span", "class", "poster_hash").open((i, h, t, a) -> h.mPost != null).content((i, holder, text) ->
	{
		holder.mPost.setIdentifier(StringUtils.clearHtml(text).trim().substring(3));

	}).equals("h2", "class", "post_title").content((i, holder, text) ->
	{
		holder.mPost.setSubject(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));

	}).contains("span", "class", "flag-").open((instance, holder, tagName, attributes) ->
	{
		String cssClass = attributes.get("class");
		Uri uri = holder.mLocator.createFlagStubUri(cssClass);
		String title = StringUtils.clearHtml(attributes.get("title"));
		String end = ". Click here to search for posts with this flag";
		if (title.endsWith(end)) title = title.substring(0, title.length() - end.length());
		if (title.isEmpty()) title = cssClass.substring(cssClass.lastIndexOf('-') + 1).toUpperCase(Locale.US);
		holder.mPost.setIcons(new Icon(holder.mLocator, uri, title));
		return false;

	}).name("time").open((instance, holder, tagName, attributes) ->
	{
		try
		{
			holder.mPost.setTimestamp(DATE_FORMAT.parse(attributes.get("datetime")).getTime());
		}
		catch (java.text.ParseException e)
		{

		}
		return false;

	}).equals("a", "data-function", "quote").open((instance, holder, tagName, attributes) ->
	{
		if (holder.mNeedResTo)
		{
			holder.mPost.setParentPostNumber(holder.mLocator.getThreadNumber(Uri.parse(attributes.get("href"))));
		}
		return false;

	}).equals("a", "class", "thread_image_link").open((instance, holder, tagName, attributes) ->
	{
		String path = holder.convertImageUriString(attributes.get("href"));
		if (holder.mAttachment == null) holder.mAttachment = new FileAttachment();
		holder.mAttachment.setFileUri(holder.mLocator, holder.mLocator.createAttachmentUri(path));
		return false;

	}).equals("div", "class", "post_file").content((instance, holder, text) ->
	{
		if (holder.mAttachment == null) holder.mAttachment = new FileAttachment();
		if (text.contains("<span class=\"post_file_controls\">")) text = text.substring(text.indexOf("</span>") + 7);
		text = StringUtils.clearHtml(text).trim();
		Matcher matcher = PATTERN_FILE.matcher(text);
		if (matcher.find())
		{
			int size = Integer.parseInt(matcher.group(2));
			String dim = matcher.group(3);
			if ("KiB".equals(dim)) size *= 1024;
			else if ("MiB".equals(dim)) size *= 1024 * 1024;
			int width = Integer.parseInt(matcher.group(4));
			int height = Integer.parseInt(matcher.group(5));
			holder.mAttachment.setSize(size);
			holder.mAttachment.setWidth(width);
			holder.mAttachment.setHeight(height);
			String originalName = matcher.group(1);
			if (originalName == null) originalName = matcher.group(6);
			if (originalName != null) holder.mAttachment.setOriginalName(originalName);
		}

	}).contains("img", "class", "thread_image").contains("img", "class", "post_image")
			.open((instance, holder, tagName, attributes) ->
	{
		String src = holder.convertImageUriString(attributes.get("src"));
		holder.mAttachment.setThumbnailUri(holder.mLocator, holder.mLocator.createAttachmentUri(src));
		return false;

	}).equals("div", "class", "text").content((instance, holder, text) ->
	{
		if (text != null) text = text.trim();
		holder.mPost.setComment(text);
		if (holder.mAttachment != null) holder.mPost.setAttachments(holder.mAttachment);
		holder.mPosts.add(holder.mPost);
		holder.mAttachment = null;
		holder.mPost = null;

	}).equals("span", "class", "omitted_posts").content((instance, holder, text) ->
	{
		holder.mThread.addPostsCount(Integer.parseInt(text));

	}).equals("span", "class", "omitted_images").content((instance, holder, text) ->
	{
		holder.mThread.addPostsWithFilesCount(Integer.parseInt(text));

	}).prepare();
}