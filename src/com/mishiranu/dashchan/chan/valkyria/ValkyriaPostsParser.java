package com.mishiranu.dashchan.chan.valkyria;

import android.net.Uri;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import chan.content.model.EmbeddedAttachment;
import chan.content.model.FileAttachment;
import chan.content.model.Icon;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.text.ParseException;
import chan.text.TemplateParser;
import chan.util.CommonUtils;
import chan.util.StringUtils;

public class ValkyriaPostsParser
{
	private final String mSource;
	private final ValkyriaChanConfiguration mConfiguration;
	private final ValkyriaChanLocator mLocator;
	private final String mBoardName;

	private String mParent;
	private Posts mThread;
	private Post mPost;
	private FileAttachment mAttachment;
	private ArrayList<Posts> mThreads;
	private final ArrayList<Post> mPosts = new ArrayList<>();
	private final ArrayList<FileAttachment> mAttachments = new ArrayList<>();

	private boolean mFileHandling = false;
	private boolean mHeaderHandling = false;

	private static final SimpleDateFormat DATE_FORMAT;

	static
	{
		DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy(EEE)hh:mm:ss", Locale.US);
		DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT-5"));
	}

	private static final Pattern FILE_SIZE = Pattern.compile("([\\d\\.]+)(\\w+), (\\d+)x(\\d+)");
	private static final Pattern NAME_EMAIL = Pattern.compile("<a href='(.*?)'>(.*)</a>");
	private static final Pattern NUMBER = Pattern.compile("(\\d+)");

	public ValkyriaPostsParser(String source, Object linked, String boardName)
	{
		mSource = source;
		mConfiguration = ValkyriaChanConfiguration.get(linked);
		mLocator = ValkyriaChanLocator.get(linked);
		mBoardName = boardName;
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

	private static final TemplateParser<ValkyriaPostsParser> PARSER = new TemplateParser<ValkyriaPostsParser>()
			.starts("input", "name", "post_").open((instance, holder, tagName, attributes) ->
	{
		if ("checkbox".equals(attributes.get("type")))
		{
			holder.mHeaderHandling = true;
			if (holder.mPost == null || holder.mPost.getPostNumber() == null)
			{
				String number = attributes.get("name").substring(5);
				if (holder.mPost == null) holder.mPost = new Post();
				holder.mPost.setPostNumber(number);
				holder.mParent = number;
				if (holder.mThreads != null)
				{
					holder.closeThread();
					holder.mThread = new Posts();
				}
			}
		}
		return false;

	}).starts("td", "id", "replybox_").open((instance, holder, tagName, attributes) ->
	{
		String number = attributes.get("id").substring(9);
		Post post = new Post();
		post.setParentPostNumber(holder.mParent);
		post.setPostNumber(number);
		holder.mPost = post;
		return false;

	}).equals("div", "class", "FileDetails").equals("span", "class", "FileDetails")
			.open((instance, holder, tagName, attributes) ->
	{
		holder.mFileHandling = true;
		if (holder.mPost == null) holder.mPost = new Post();
		return false;

	}).name("a").open((instance, holder, tagName, attributes) ->
	{
		if (holder.mFileHandling)
		{
			holder.mAttachment = new FileAttachment();
			holder.mAttachment.setFileUri(holder.mLocator, Uri.parse(attributes.get("href")));
			holder.mAttachments.add(holder.mAttachment);
		}
		return false;

	}).text((instance, holder, source, start, end) ->
	{
		if (holder.mFileHandling)
		{
			String text = StringUtils.clearHtml(source.substring(start, end));
			Matcher matcher = FILE_SIZE.matcher(text);
			if (matcher.find())
			{
				float size = Float.parseFloat(matcher.group(1));
				String dim = matcher.group(2);
				if ("kb".equals(dim)) size *= 1024f;
				else if ("mb".equals(dim)) size *= 1024f * 1024f;
				int width = Integer.parseInt(matcher.group(3));
				int height = Integer.parseInt(matcher.group(4));
				holder.mAttachment.setSize((int) size);
				holder.mAttachment.setWidth(width);
				holder.mAttachment.setHeight(height);
				holder.mFileHandling = false;
			}
		}

	}).name("div").name("span").close((instance, holder, tagName) ->
	{
		holder.mFileHandling = false;

	}).contains("img", "class", "ThumbnailImage").open((instance, holder, tagName, attributes) ->
	{
		String uriString = attributes.get("data-original");
		if (uriString == null) uriString = attributes.get("src");
		if (uriString != null)
		{
			if (uriString.endsWith("/board/spoiler.png")) holder.mAttachment.setSpoiler(true);
			else holder.mAttachment.setThumbnailUri(holder.mLocator, Uri.parse(uriString));
		}
		return false;

	}).equals("object", "class", "MediaEmbed").content((instance, holder, text) ->
	{
		EmbeddedAttachment attachment = EmbeddedAttachment.obtain(text);
		if (attachment != null) holder.mPost.setAttachments(attachment);

	}).equals("span", "class", "Subject").content((instance, holder, text) ->
	{
		holder.mPost.setSubject(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));

	}).equals("span", "class", "UserName").content((instance, holder, text) ->
	{
		text = CommonUtils.restoreCloudFlareProtectedEmails(text);
		Matcher matcher = NAME_EMAIL.matcher(text);
		if (matcher.matches())
		{
			String email = StringUtils.clearHtml(matcher.group(1));
			if ("mailto:sage".equals(email)) holder.mPost.setSage(true); else holder.mPost.setEmail(email);
			text = matcher.group(2);
		}
		holder.mPost.setName(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));

	}).equals("span", "class", "UserNameTripcode").content((instance, holder, text) ->
	{
		holder.mPost.setTripcode(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));

	}).contains("img", "src", "/images/flags/").open((instance, holder, tagName, attributes) ->
	{
		String title = StringUtils.clearHtml(attributes.get("title"));
		Uri uri = Uri.parse(attributes.get("src"));
		holder.mPost.setIcons(new Icon(holder.mLocator, uri, title));
		return false;

	}).text((instance, holder, source, start, end) ->
	{
		if (holder.mHeaderHandling)
		{
			String text = source.substring(start, end).trim();
			if (text.length() > 0)
			{
				try
				{
					holder.mPost.setTimestamp(DATE_FORMAT.parse(text).getTime());
				}
				catch (java.text.ParseException e)
				{

				}
				holder.mHeaderHandling = false;
			}
		}

	}).ends("span", "id", "_message_div").content((instance, holder, text) ->
	{
		text = CommonUtils.restoreCloudFlareProtectedEmails(text);
		text = text.replaceAll(">\\s+(&gt;&gt;\\d+)\\s+</a>", ">$1</a>"); // Simplify post links to fix internal parser
		text = text.replaceAll("<span class=\"spoilerBox\".*?</span>", ""); // Remove spoiler buttons
		text = text.replaceAll("<span id=\"SpoilerBox_\\d+\"", "<span class=\"SpoilerBox");
		text = StringUtils.linkify(text);
		holder.mPost.setComment(text);
		if (holder.mAttachments.size() > 0)
		{
			holder.mPost.setAttachments(holder.mAttachments);
			holder.mAttachments.clear();
		}
		holder.mPosts.add(holder.mPost);
		holder.mAttachment = null;
		holder.mPost = null;

	}).equals("span", "class", "OmissionText").content((instance, holder, text) ->
	{
		if (holder.mThreads != null)
		{
			Matcher matcher = NUMBER.matcher(text);
			if (matcher.find())
			{
				int postsCount = Integer.parseInt(matcher.group(1));
				int postsWithFilesCount = 0;
				if (matcher.find())
				{
					postsWithFilesCount = Integer.parseInt(matcher.group(1));
					postsCount += postsWithFilesCount;
				}
				else if (text.contains("image reply(s)")) postsWithFilesCount = postsCount;
				holder.mThread.addPostsCount(postsCount).addPostsWithFilesCount(postsWithFilesCount);
			}
		}

	}).equals("div", "class", "Title").content((instance, holder, text) ->
	{
		text = StringUtils.clearHtml(text).trim();
		int index = text.indexOf("- ");
		if (index >= 0) text = text.substring(index + 2);
		if (!StringUtils.isEmpty(text)) holder.mConfiguration.storeBoardTitle(holder.mBoardName, text);

	}).equals("table", "class", "PagingTable").content((instance, holder, text) ->
	{
		text = StringUtils.clearHtml(text);
		int index1 = text.lastIndexOf('[');
		int index2 = text.lastIndexOf(']');
		if (index1 >= 0 && index2 > index1)
		{
			text = text.substring(index1 + 1, index2);
			try
			{
				int pagesCount = Integer.parseInt(text) + 1;
				holder.mConfiguration.storePagesCount(holder.mBoardName, pagesCount);
			}
			catch (NumberFormatException e)
			{

			}
		}

	}).prepare();
}