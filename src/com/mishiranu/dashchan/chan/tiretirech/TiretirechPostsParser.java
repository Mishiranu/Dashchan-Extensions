package com.mishiranu.dashchan.chan.tiretirech;

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
import chan.util.CommonUtils;
import chan.util.StringUtils;

@SuppressLint("SimpleDateFormat")
public class TiretirechPostsParser
{
	private final String mSource;
	private final TiretirechChanConfiguration mConfiguration;
	private final TiretirechChanLocator mLocator;
	private final String mBoardName;
	
	private String mParent;
	private Posts mThread;
	private Post mPost;
	private FileAttachment mAttachment;
	private ArrayList<Posts> mThreads;
	private final ArrayList<Post> mPosts = new ArrayList<>();
	private final ArrayList<FileAttachment> mAttachments = new ArrayList<>();
	
	private static final SimpleDateFormat DATE_FORMAT;
	
	static
	{
		DateFormatSymbols symbols = new DateFormatSymbols();
		symbols.setShortWeekdays(new String[] {"", "Вс", "Пн", "Вт", "Ср", "Чт", "Пт", "Сб"});
		symbols.setMonths(new String[] {"Января", "Февраля", "Марта", "Апреля", "Мая", "Июня", "Июля", "Августа",
				"Сентября", "Октября", "Ноября", "Декабря"});
		DATE_FORMAT = new SimpleDateFormat("EE dd MMMM yyyy HH:mm:ss", symbols);
		DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT+3"));
	}
	
	private static final Pattern FILE_SIZE = Pattern.compile("\\(([\\d\\.]+) (\\w+)(?:, *(\\d+)x(\\d+))?");
	private static final Pattern NAME_EMAIL = Pattern.compile("<a href=\"(.*?)\">(.*)</a>");
	private static final Pattern NUMBER = Pattern.compile("\\d+");
	
	public TiretirechPostsParser(String source, Object linked, String boardName)
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
	
	private static final TemplateParser<TiretirechPostsParser> PARSER = new TemplateParser<TiretirechPostsParser>()
			.equals("div", "class", "threadz").open((instance, holder, tagName, attributes) ->
	{
		String id = attributes.get("id");
		if (id != null)
		{
			String number = id.substring(1);
			Post post = new Post();
			post.setPostNumber(number);
			holder.mParent = number;
			holder.mPost = post;
			if (holder.mThreads != null)
			{
				holder.closeThread();
				holder.mThread = new Posts();
			}
		}
		return false;
		
	}).starts("td", "id", "reply").open((instance, holder, tagName, attributes) ->
	{
		String number = attributes.get("id").substring(5);
		Post post = new Post();
		post.setParentPostNumber(holder.mParent);
		post.setPostNumber(number);
		holder.mPost = post;
		return false;
		
	}).equals("span", "class", "filesize").content((instance, holder, text) ->
	{
		holder.mAttachment = new FileAttachment();
		holder.mAttachments.add(holder.mAttachment);
		text = StringUtils.clearHtml(text);
		Matcher matcher = FILE_SIZE.matcher(text);
		if (matcher.find())
		{
			float size = Float.parseFloat(matcher.group(1));
			String dim = matcher.group(2);
			if ("Кб".equals(dim)) size *= 1024;
			else if ("Мб".equals(dim)) size *= 1024 * 1024;
			holder.mAttachment.setSize((int) size);
			if (matcher.group(3) != null)
			{
				holder.mAttachment.setWidth(Integer.parseInt(matcher.group(3)));
				holder.mAttachment.setHeight(Integer.parseInt(matcher.group(4)));
			}
		}
		
	}).name("a").name("source").open((instance, holder, tagName, attributes) ->
	{
		if (holder.mAttachment != null)
		{
			String path = attributes.get("source".equals(tagName) ? "src" : "href");
			holder.mAttachment.setFileUri(holder.mLocator, holder.mLocator.buildPath(path));
		}
		return false;
		
	}).equals("img", "class", "thumb").open((instance, holder, tagName, attributes) ->
	{
		String path = attributes.get("src");
		holder.mAttachment.setThumbnailUri(holder.mLocator, holder.mLocator.buildPath(path));
		return false;
		
	}).equals("span", "class", "filetitle").equals("span", "class", "replytitle").content((i, holder, text) ->
	{
		holder.mPost.setSubject(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));
		
	}).equals("span", "class", "postername").equals("span", "class", "commentpostername").content((i, holder, text) ->
	{
		text = text.trim();
		Matcher matcher = NAME_EMAIL.matcher(text);
		if (matcher.matches())
		{
			String email = matcher.group(1);
			if (email.toLowerCase(Locale.US).equals("sage")) holder.mPost.setSage(true);
			else holder.mPost.setEmail(StringUtils.clearHtml(email));
			text = matcher.group(2);
		}
		if ("<font color=\"#0000FF\">V.</font>".equals(text)) holder.mPost.setCapcode("Admin");
		else holder.mPost.setName(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));
		
	}).equals("span", "class", "postertrip").content((instance, holder, text) ->
	{
		holder.mPost.setTripcode(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim().replace('\u2665', '!')));
		
	}).equals("span", "class", "postdate").content((instance, holder, text) ->
	{
		try
		{
			holder.mPost.setTimestamp(DATE_FORMAT.parse(text).getTime());
		}
		catch (java.text.ParseException e)
		{
			
		}
		
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
			if (message.contains("USER WAS BANNED FOR THIS POST")) holder.mPost.setPosterBanned(true);
		}
		text = CommonUtils.restoreCloudFlareProtectedEmails(text);
		holder.mPost.setComment(text);
		if (holder.mAttachments.size() > 0)
		{
			holder.mPost.setAttachments(holder.mAttachments);
			holder.mAttachments.clear();
		}
		holder.mAttachment = null;
		holder.mPosts.add(holder.mPost);
		holder.mPost = null;
		
	}).equals("span", "class", "omittedposts").open((i, h, t, a) -> h.mThreads != null).content((i, holder, text) ->
	{
		Matcher matcher = NUMBER.matcher(text);
		if (matcher.find())
		{
			holder.mThread.addPostsCount(Integer.parseInt(matcher.group()));
			if (matcher.find()) holder.mThread.addPostsWithFilesCount(Integer.parseInt(matcher.group()));
		}
		
	}).equals("div", "class", "logo").content((instance, holder, text) ->
	{
		text = StringUtils.clearHtml(text).trim();
		if (text.startsWith("Тире.ч — ")) text = text.substring(9);
		if (!StringUtils.isEmpty(text)) holder.mConfiguration.storeBoardTitle(holder.mBoardName, text);
		
	}).equals("table", "border", "1").content((instance, holder, text) ->
	{
		String pagesCount = null;
		Matcher matcher = NUMBER.matcher(text);
		while (matcher.find()) pagesCount = matcher.group();
		if (pagesCount != null)
		{
			try
			{
				holder.mConfiguration.storePagesCount(holder.mBoardName, Integer.parseInt(pagesCount));
			}
			catch (NumberFormatException e)
			{
				
			}
		}
		
	}).prepare();
}