package com.mishiranu.dashchan.chan.ponyach;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;
import chan.content.ChanConfiguration;
import chan.content.ChanLocator;
import chan.content.model.FileAttachment;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.text.ParseException;
import chan.text.TemplateParser;
import chan.util.StringUtils;

@SuppressLint("SimpleDateFormat")
public class PonyachPostsParser
{
	private final String mSource;
	private final PonyachChanConfiguration mConfiguration;
	private final PonyachChanLocator mLocator;
	private final String mBoardName;

	private String mParent;
	private Posts mThread;
	private Post mPost;
	private ArrayList<Posts> mThreads;
	private final ArrayList<Post> mPosts = new ArrayList<>();
	private final ArrayList<FileAttachment> mAttachments = new ArrayList<>();

	private int mAttachmentCount = -1;

	private static final SimpleDateFormat DATE_FORMAT;

	static
	{
		DateFormatSymbols symbols = new DateFormatSymbols();
		symbols.setShortMonths(new String[] {"Янв", "Фев", "Мар", "Апр", "Май", "Июнь", "Июль", "Авг", "Снт", "Окт",
				"Ноя", "Дек"});
		DATE_FORMAT = new SimpleDateFormat("dd MMM yyyy HH:mm:ss", symbols);
		DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT+3"));
	}

	private static final Pattern FILE_SIZE = Pattern.compile("\\(([\\d\\.]+)(\\w+) *, *(\\d+)x(\\d+)" +
			"(?: *, (.*))? *\\)$");
	private static final Pattern NAME_EMAIL = Pattern.compile("<a href=\"(.*?)\">(.*)</a>");
	private static final Pattern NUMBER = Pattern.compile("(\\d+)");

	public PonyachPostsParser(String source, Object linked, String boardName)
	{
		mSource = source;
		mConfiguration = ChanConfiguration.get(linked);
		mLocator = ChanLocator.get(linked);
		mBoardName = boardName;
	}

	private void closeThread()
	{
		if (mThread != null)
		{
			mThread.setPosts(mPosts);
			mThread.addPostsCount(mPosts.size());
			int filesCount = 0;
			for (Post post : mPosts) filesCount += post.getAttachmentsCount();
			mThread.addFilesCount(filesCount);
			mThreads.add(mThread);
			mPosts.clear();
		}
	}

	public ArrayList<Posts> convertThreads() throws ParseException
	{
		mThreads = new ArrayList<>();
		PARSER.parse(mSource, this);
		closeThread();
		if (mAttachmentCount >= 0) mConfiguration.storeAttachmentCount(mBoardName, mAttachmentCount);
		return mThreads;
	}

	public Posts convertPosts() throws ParseException
	{
		PARSER.parse(mSource, this);
		return mPosts.size() > 0 ? new Posts(mPosts) : null;
	}

	private static final TemplateParser<PonyachPostsParser> PARSER = new TemplateParser<PonyachPostsParser>()
			.starts("div", "id", "thread").open((instance, holder, tagName, attributes) ->
	{
		String id = attributes.get("id");
		String number = id.substring(6, id.length() - holder.mBoardName.length());
		Post post = new Post();
		post.setPostNumber(number);
		holder.mParent = number;
		holder.mPost = post;
		if (holder.mThreads != null)
		{
			holder.closeThread();
			holder.mThread = new Posts();
		}
		return false;

	}).starts("td", "id", "reply").open((instance, holder, tagName, attributes) ->
	{
		String id = attributes.get("id");
		String number = id.substring(5);
		Post post = new Post();
		post.setParentPostNumber(holder.mParent);
		post.setPostNumber(number);
		holder.mPost = post;
		return false;

	}).equals("span", "class", "filetitle").content((instance, holder, text) ->
	{
		holder.mPost.setSubject(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));

	}).equals("span", "class", "postername").content((instance, holder, text) ->
	{
		text = text.trim();
		Matcher matcher = NAME_EMAIL.matcher(text);
		if (matcher.matches())
		{
			String email = matcher.group(1);
			if (email.toLowerCase(Locale.US).equals("mailto:sage")) holder.mPost.setSage(true);
			else holder.mPost.setEmail(StringUtils.clearHtml(email));
			text = matcher.group(2);
		}
		holder.mPost.setName(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));

	}).equals("span", "class", "postertrip").content((instance, holder, text) ->
	{
		String tripcode = text.trim();
		if ("[M]".equals(tripcode)) holder.mPost.setCapcode("Mod");
		else holder.mPost.setTripcode(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));

	}).equals("span", "class", "mobile_date dast-date").content((instance, holder, text) ->
	{
		text = text.trim();
		int index = text.indexOf(' ');
		if (index >= 0)
		{
			text = text.substring(index + 1);
			try
			{
				holder.mPost.setTimestamp(DATE_FORMAT.parse(text).getTime());
			}
			catch (java.text.ParseException e)
			{

			}
		}

	}).ends("img", "src", "/icons/sticky.png").open((i, h, t, a) -> !h.mPost.setSticky(true).isSticky())
			.ends("img", "src", "/icons/locked.png").open((i, h, t, a) -> !h.mPost.setClosed(true).isClosed())
			.starts("span", "class", "filesize fs_").content((instance, holder, text) ->
	{
		text = StringUtils.clearHtml(text).trim();
		Matcher matcher = FILE_SIZE.matcher(text);
		if (matcher.find())
		{
			FileAttachment attachment = new FileAttachment();
			float size = Float.parseFloat(matcher.group(1));
			String dim = matcher.group(2);
			if ("KB".equals(dim)) size *= 1024;
			else if ("MB".equals(dim)) size *= 1024 * 1024;
			int width = Integer.parseInt(matcher.group(3));
			int height = Integer.parseInt(matcher.group(4));
			attachment.setSize((int) size);
			attachment.setWidth(width);
			attachment.setHeight(height);
			attachment.setOriginalName(matcher.group(5));
			holder.mAttachments.add(attachment);
		}

	}).name("a").open((instance, holder, tagName, attributes) ->
	{
		if (holder.mPost != null && holder.mAttachments.size() > 0)
		{
			String onclick = attributes.get("onclick");
			if (onclick != null && onclick.startsWith("javascript:expandimg"))
			{
				FileAttachment attachment = holder.mAttachments.get(holder.mAttachments.size() - 1);
				int index = onclick.indexOf("/src/");
				if (index > 0)
				{
					String path = onclick.substring(index + 5, onclick.indexOf('\'', index));
					attachment.setFileUri(holder.mLocator,
							holder.mLocator.buildPath(holder.mBoardName, "src", path));
				}
				index = onclick.indexOf("/thumb/");
				if (index > 0)
				{
					String path = onclick.substring(index + 7, onclick.indexOf('\'', index));
					attachment.setThumbnailUri(holder.mLocator,
							holder.mLocator.buildPath(holder.mBoardName, "thumb", path));
				}
			}
		}
		return false;

	}).name("blockquote").content((instance, holder, text) ->
	{
		text = text.trim();
		text = text.replaceAll("<a class=\"irc-reflink.*?</a>", "");
		int index = text.lastIndexOf("<div class=\"abbrev\">");
		if (index >= 0) text = text.substring(0, index).trim();
		if (holder.mAttachments.size() > 0)
		{
			holder.mPost.setAttachments(holder.mAttachments);
			holder.mAttachments.clear();
		}
		holder.mPost.setComment(text);
		holder.mPosts.add(holder.mPost);
		holder.mPost = null;

	}).equals("span", "class", "omittedposts").open((i, h, t, a) -> h.mThreads != null)
			.content((instance, holder, text) ->
	{
		Matcher matcher = NUMBER.matcher(text);
		if (matcher.find())
		{
			holder.mThread.addPostsCount(Integer.parseInt(matcher.group(1)));
			if (matcher.find()) holder.mThread.addFilesCount(Integer.parseInt(matcher.group(1)));
		}

	}).starts("select", "name", "upload-rating-").open((instance, holder, tagName, attributes) ->
	{
		if (holder.mThreads != null) holder.mAttachmentCount = Integer.parseInt(attributes.get("name").substring(14));
		return false;

	}).equals("div", "class", "logo").content((instance, holder, text) ->
	{
		text = StringUtils.clearHtml(text).trim();
		if (!StringUtils.isEmpty(text))
		{
			holder.mConfiguration.storeBoardTitle(holder.mBoardName,
					PonyachBoardsParser.validateBoardTitle(holder.mBoardName, text));
		}

	}).equals("table", "border", "1").content((instance, holder, text) ->
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