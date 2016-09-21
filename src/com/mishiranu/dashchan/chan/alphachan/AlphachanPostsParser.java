package com.mishiranu.dashchan.chan.alphachan;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;
import android.net.Uri;

import chan.content.model.FileAttachment;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.text.ParseException;
import chan.text.TemplateParser;
import chan.util.StringUtils;

@SuppressLint("SimpleDateFormat")
public class AlphachanPostsParser
{
	private final String mSource;
	private final AlphachanChanConfiguration mConfiguration;
	private final AlphachanChanLocator mLocator;
	private final String mBoardName;

	private String mParent;
	private Posts mThread;
	private Post mPost;
	private FileAttachment mAttachment;
	private ArrayList<Posts> mThreads;
	private final ArrayList<Post> mPosts = new ArrayList<>();

	private static final SimpleDateFormat DATE_FORMAT;

	static
	{
		DateFormatSymbols symbols = new DateFormatSymbols();
		symbols.setMonths(new String[] {"Января", "Февраля", "Марта", "Апреля", "Мая", "Июня", "Июля", "Августа",
				"Сентября", "Октября", "Ноября", "Декабря"});
		DATE_FORMAT = new SimpleDateFormat("dd MMMM yyyy в HH:mm:ss", symbols);
		DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT+2"));
	}

	private static final Pattern PATTERN_NUMBER = Pattern.compile("\\d+");
	private static final Pattern PATTERN_LINK = Pattern.compile("(<a.*?>)ID: (\\d+</a>)");

	public AlphachanPostsParser(String source, Object linked, String boardName)
	{
		mSource = source;
		mConfiguration = AlphachanChanConfiguration.get(linked);
		mLocator = AlphachanChanLocator.get(linked);
		mBoardName = boardName;
	}

	private void closeThread()
	{
		if (mThread != null)
		{
			mThread.setPosts(mPosts);
			mThread.addPostsCount(mPosts.size());
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

	private static final TemplateParser<AlphachanPostsParser> PARSER = new TemplateParser<AlphachanPostsParser>()
			.starts("div", "id", "oppost_").open((instance, holder, tagName, attributes) ->
	{
		String number = attributes.get("id").substring(7);
		holder.mPost = new Post();
		holder.mPost.setPostNumber(number);
		holder.mParent = number;
		if (holder.mThreads != null)
		{
			holder.closeThread();
			holder.mThread = new Posts();
		}
		return false;

	}).starts("div", "id", "post_").open((instance, holder, tagName, attributes) ->
	{
		String number = attributes.get("id").substring(5);
		holder.mPost = new Post();
		holder.mPost.setParentPostNumber(holder.mParent);
		holder.mPost.setPostNumber(number);
		return false;

	}).contains("a", "href", "/src/").open((instance, holder, tagName, attributes) ->
	{
		holder.mAttachment = new FileAttachment();
		holder.mAttachment.setFileUri(holder.mLocator, Uri.parse(attributes.get("href")));
		holder.mPost.setAttachments(holder.mAttachment);
		return false;

	}).contains("img", "src", "/thumb/").open((instance, holder, tagName, attributes) ->
	{
		holder.mAttachment.setThumbnailUri(holder.mLocator, Uri.parse(attributes.get("src")));
		return false;

	}).equals("span", "class", "filetitle").content((instance, holder, text) ->
	{
		holder.mPost.setSubject(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));

	}).equals("span", "class", "postername").content((instance, holder, text) ->
	{
		holder.mPost.setName(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));

	}).equals("span", "class", "datetime").content((instance, holder, text) ->
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
		text = StringUtils.replaceAll(text, PATTERN_LINK, m -> m.group(1) + "&gt;&gt;" + m.group(2));
		text = text.replace("<span class=\"quote\">", "<span class=\"quote\">&gt; ");
		holder.mPost.setComment(text);
		holder.mPosts.add(holder.mPost);

	}).equals("a", "class", "ans").content((instance, holder, text) ->
	{
		Matcher matcher = PATTERN_NUMBER.matcher(text);
		if (matcher.find()) holder.mThread.addPostsCount(Integer.parseInt(matcher.group()));

	}).equals("div", "id", "CapTitle").content((instance, holder, text) ->
	{
		text = StringUtils.clearHtml(text).trim();
		int index = text.indexOf(" - ");
		if (index >= 0) text = text.substring(index + 3);
		if (!StringUtils.isEmpty(text)) holder.mConfiguration.storeBoardTitle(holder.mBoardName, text);

	}).equals("div", "id", "CapDescr").content((instance, holder, text) ->
	{
		text = StringUtils.clearHtml(text).trim();
		if (!StringUtils.isEmpty(text)) holder.mConfiguration.storeBoardDescription(holder.mBoardName, text);

	}).equals("div", "class", "paginator").content((instance, holder, text) ->
	{
		String pagesCount = null;
		Matcher matcher = PATTERN_NUMBER.matcher(StringUtils.clearHtml(text));
		while (matcher.find()) pagesCount = matcher.group();
		if (pagesCount != null) holder.mConfiguration.storePagesCount(holder.mBoardName, Integer.parseInt(pagesCount));

	}).prepare();
}