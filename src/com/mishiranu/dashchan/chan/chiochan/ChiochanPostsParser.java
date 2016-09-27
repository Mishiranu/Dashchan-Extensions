package com.mishiranu.dashchan.chan.chiochan;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import chan.content.model.FileAttachment;
import chan.content.model.Icon;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.text.ParseException;
import chan.text.TemplateParser;
import chan.util.StringUtils;

public class ChiochanPostsParser
{
	private final String mSource;
	private final ChiochanChanConfiguration mConfiguration;
	private final ChiochanChanLocator mLocator;
	private final String mBoardName;

	private String mParent;
	private Posts mThread;
	private Post mPost;
	private FileAttachment mAttachment;
	private ArrayList<Posts> mThreads;
	private final ArrayList<Post> mPosts = new ArrayList<>();
	private boolean mExpandMode = false;

	private boolean mHeaderHandling = false;

	private boolean mHasPostBlock = false;
	private boolean mHasPostBlockName = false;

	private static final SimpleDateFormat DATE_FORMAT;

	static
	{
		DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.US);
		DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT+3"));
	}

	private static final Pattern FILE_SIZE = Pattern.compile("\\(([\\d\\.]+)(\\w+) *, *(\\d+)[x×](\\d+)" +
			"(?: *, *(.+))? *\\) *$");
	private static final Pattern NAME_EMAIL = Pattern.compile("<a href=\"(.*?)\">(.*)</a>");
	static final Pattern NUMBER = Pattern.compile("(\\d+)");
	private static final Pattern BUMP_LIMIT = Pattern.compile("Максимальное количество бампов треда: (\\d+)");

	public ChiochanPostsParser(String source, Object linked, String boardName)
	{
		mSource = source;
		mConfiguration = ChiochanChanConfiguration.get(linked);
		mLocator = ChiochanChanLocator.get(linked);
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
		if (mThreads.size() > 0)
		{
			updateConfiguration();
			return mThreads;
		}
		return null;
	}

	public ArrayList<Post> convertPosts() throws ParseException
	{
		PARSER.parse(mSource, this);
		if (mPosts.size() > 0)
		{
			updateConfiguration();
			return mPosts;
		}
		return null;
	}

	public ArrayList<Post> convertExpand() throws ParseException
	{
		mExpandMode = true;
		PARSER.parse(mSource, this);
		if (mPosts.size() > 0)
		{
			updateConfiguration();
			return mPosts;
		}
		return null;
	}

	private void updateConfiguration()
	{
		if (mHasPostBlock) mConfiguration.storeNamesEnabled(mBoardName, mHasPostBlockName);
	}

	private String convertUriString(String uriString)
	{
		if (uriString != null)
		{
			int index = uriString.indexOf("://");
			if (index > 0) uriString = uriString.substring(uriString.indexOf('/', index + 3));
		}
		return uriString;
	}

	private static final TemplateParser<ChiochanPostsParser> PARSER = new TemplateParser<ChiochanPostsParser>()
			.starts("div", "id", "thread").open((instance, holder, tagName, attributes) ->
	{
		String id = attributes.get("id");
		String number = id.substring(6, id.length() - holder.mBoardName.length());
		holder.mPost = new Post();
		holder.mPost.setPostNumber(number);
		holder.mParent = number;
		if (holder.mThreads != null)
		{
			holder.closeThread();
			holder.mThread = new Posts();
		}
		return false;

	}).starts("div", "id", "reply").starts("td", "id", "reply").open((instance, holder, tagName, attributes) ->
	{
		String number = attributes.get("id").substring(5);
		holder.mPost = new Post();
		holder.mPost.setParentPostNumber(holder.mParent);
		holder.mPost.setPostNumber(number);
		return false;

	}).name("input").open((instance, holder, tagName, attributes) ->
	{
		if (holder.mExpandMode)
		{
			String name = attributes.get("name");
			if ("delete[]".equals(name))
			{
				if (holder.mPost == null) holder.mPost = new Post();
				holder.mPost.setPostNumber(attributes.get("value"));
			}
		}
		return false;

	}).name("label").open((instance, holder, tagName, attributes) ->
	{
		if (holder.mPost != null) holder.mHeaderHandling = true;
		return false;

	}).equals("span", "class", "filesize").content((instance, holder, text) ->
	{
		if (holder.mExpandMode && holder.mPost == null) holder.mPost = new Post();
		holder.mAttachment = new FileAttachment();
		text = StringUtils.clearHtml(text);
		Matcher matcher = FILE_SIZE.matcher(text);
		if (matcher.find())
		{
			float size = Float.parseFloat(matcher.group(1));
			String dim = matcher.group(2);
			if ("KB".equals(dim)) size *= 1024;
			else if ("MB".equals(dim)) size *= 1024 * 1024;
			int width = Integer.parseInt(matcher.group(3));
			int height = Integer.parseInt(matcher.group(4));
			String fileName = matcher.group(5);
			holder.mAttachment.setSize((int) size);
			holder.mAttachment.setWidth(width);
			holder.mAttachment.setHeight(height);
			holder.mAttachment.setOriginalName(StringUtils.isEmptyOrWhitespace(fileName) ? null : fileName.trim());
		}

	}).contains("a", "href", "/src/").open((instance, holder, tagName, attributes) ->
	{
		if (holder.mAttachment != null)
		{
			String path = holder.convertUriString(attributes.get("href"));
			holder.mAttachment.setFileUri(holder.mLocator, holder.mLocator.buildPath(path));
			holder.mPost.setAttachments(holder.mAttachment);
		}
		return false;

	}).equals("img", "class", "thumb").open((instance, holder, tagName, attributes) ->
	{
		String path = holder.convertUriString(attributes.get("src"));
		holder.mAttachment.setThumbnailUri(holder.mLocator, holder.mLocator.buildPath(path));
		return false;

	}).equals("span", "class", "filetitle").content((instance, holder, text) ->
	{
		text = text.trim();
		if (text.endsWith("\u21e9"))
		{
			text = StringUtils.nullIfEmpty(text.substring(0, text.length() - 1));
			holder.mPost.setSage(true);
		}
		holder.mPost.setSubject(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));

	}).equals("span", "class", "postername").content((instance, holder, text) ->
	{
		text = text.trim();
		Matcher matcher = NAME_EMAIL.matcher(text);
		if (matcher.matches())
		{
			String email = matcher.group(1);
			if (email != null && email.toLowerCase(Locale.US).contains("sage"))
			{
				// Old chiochan sage appearance
				holder.mPost.setSage(true);
			}
			text = matcher.group(2);
		}
		holder.mPost.setName(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));

	}).equals("span", "class", "postertrip").content((instance, holder, text) ->
	{
		holder.mPost.setTripcode(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));

	}).contains("img", "src", "/flags/").open((instance, holder, tagName, attributes) ->
	{
		String path = holder.convertUriString(attributes.get("src"));
		String title = StringUtils.clearHtml(attributes.get("alt"));
		holder.mPost.setIcons(new Icon(holder.mLocator, holder.mLocator.buildPath(path), title));
		return false;

	}).text((instance, holder, source, start, end) ->
	{
		if (holder.mHeaderHandling)
		{
			String text = source.substring(start, end).trim();
			if (text.length() > 0)
			{
				int index1 = text.indexOf('(');
				int index2 = text.indexOf(')');
				if (index2 > index1 && index1 > 0)
				{
					// Remove week in brackets
					text = text.substring(0, index1 - 1) + text.substring(index2 + 1);
				}
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

	}).ends("img", "src", "/css/sticky.gif").open((instance, holder, tagName, attributes) ->
	{
		if (holder.mPost != null) holder.mPost.setSticky(true);
		return false;

	}).ends("img", "src", "/css/locked.gif").open((instance, holder, tagName, attributes) ->
	{
		if (holder.mPost != null) holder.mPost.setClosed(true);
		return false;

	}).name("blockquote").content((instance, holder, text) ->
	{
		text = text.trim();
		int index = text.lastIndexOf("<div class=\"abbrev\">");
		if (index >= 0) text = text.substring(0, index).trim();
		index = text.lastIndexOf("<font color=\"#FF0000\">");
		if (index >= 0)
		{
			String message = text.substring(index);
			text = text.substring(0, index);
			if (message.contains("USER WAS BANNED FOR THIS POST") ||
					message.contains("ПОТРЕБИТЕЛЬ БЫЛ ЗАПРЕЩЁН ДЛЯ ЭТОГО СТОЛБА"))
			{
				holder.mPost.setPosterBanned(true);
			}
		}
		holder.mPost.setComment(text);
		holder.mPosts.add(holder.mPost);
		holder.mPost = null;
		holder.mAttachment = null;

	}).equals("span", "class", "omittedposts").content((instance, holder, text) ->
	{
		if (holder.mThread != null)
		{
			Matcher matcher = NUMBER.matcher(text);
			if (matcher.find())
			{
				holder.mThread.addPostsCount(Integer.parseInt(matcher.group(1)));
				if (matcher.find()) holder.mThread.addPostsWithFilesCount(Integer.parseInt(matcher.group(1)));
			}
		}

	}).equals("td", "class", "rules").content((instance, holder, text) ->
	{
		Matcher matcher = BUMP_LIMIT.matcher(text);
		if (matcher.find())
		{
			int bumpLimit = Integer.parseInt(matcher.group(1));
			holder.mConfiguration.storeBumpLimit(holder.mBoardName, bumpLimit);
		}

	}).equals("div", "class", "logo").content((instance, holder, text) ->
	{
		text = StringUtils.clearHtml(text).trim();
		if (!StringUtils.isEmpty(text)) holder.mConfiguration.storeBoardTitle(holder.mBoardName, text);

	}).equals("td", "class", "postblock").content((instance, holder, text) ->
	{
		holder.mHasPostBlock = true;
		if ("Имя".equals(text) || "Name".equals(text)) holder.mHasPostBlockName = true;

	}).equals("div", "class", "pgstbl").content((instance, holder, text) ->
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