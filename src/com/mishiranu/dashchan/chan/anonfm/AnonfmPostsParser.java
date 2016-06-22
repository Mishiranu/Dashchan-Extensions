package com.mishiranu.dashchan.chan.anonfm;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import chan.content.model.Post;
import chan.content.model.Posts;
import chan.text.GroupParser;
import chan.text.ParseException;
import chan.util.StringUtils;

public class AnonfmPostsParser implements GroupParser.Callback
{
	private final String mSource;

	private String mThreadNumber;
	private String mParent;
	private Posts mThread;
	private Post mPost;
	private ArrayList<Posts> mThreads;
	private final ArrayList<Post> mPosts = new ArrayList<>();
	
	private static final int EXPECT_NONE = 0;
	private static final int EXPECT_THREAD_NUMBER = 1;
	private static final int EXPECT_TIMESTAMP = 2;
	private static final int EXPECT_COMMENT = 3;
	
	private int mExpect = EXPECT_NONE;
	
	private static final SimpleDateFormat DATE_FORMAT;
	
	static
	{
		DATE_FORMAT = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy", Locale.US);
		DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT+3"));
	}
	
	private static final Pattern THREAD_NUMBER = Pattern.compile("[Тт]ред №(\\d+)");
	
	public static final long START_TIMESTAMP = 1230768000000L;
	
	public static long timestampToPostNumber(long timestamp)
	{
		return (timestamp - START_TIMESTAMP) / 1000L;
	}
	
	public static String quotifyComment(String comment)
	{
		return comment.replaceAll("(?<=^|<br>)&gt;.*?(?=$|<br>)", "<span class=\"unkfunc\">$0</span>");
	}
	
	public AnonfmPostsParser(String source)
	{
		mSource = source;
	}
	
	private void closeThread()
	{
		if (mThread != null)
		{
			mThread.setPosts(mPosts);
			mThreads.add(mThread);
			mPosts.clear();
		}
	}
	
	public ArrayList<Posts> convertThreads() throws ParseException
	{
		mThreads = new ArrayList<>();
		GroupParser.parse(mSource, this);
		closeThread();
		return mThreads;
	}
	
	public ArrayList<Post> convertPosts() throws ParseException
	{
		GroupParser.parse(mSource, this);
		return mPosts;
	}
	
	@Override
	public boolean onStartElement(GroupParser parser, String tagName, String attrs)
	{
		if ("h3".equals(tagName))
		{
			mExpect = EXPECT_THREAD_NUMBER;
			return true;
		}
		else if ("div".equals(tagName))
		{
			String cssClass = parser.getAttr(attrs, "class");
			if ("thread".equals(cssClass))
			{
				if (mThreads != null)
				{
					closeThread();
					mThread = new Posts();
				}
			}
			else if ("time".equals(cssClass))
			{
				mExpect = EXPECT_TIMESTAMP;
				return true;
			}
			else if ("post".equals(cssClass))
			{
				if (mPost != null)
				{
					mExpect = EXPECT_COMMENT;
					return true;
				}
			}
		}
		return false;
	}
	
	@Override
	public void onEndElement(GroupParser parser, String tagName)
	{
		
	}
	
	@Override
	public void onText(GroupParser parser, String source, int start, int end)
	{
		
	}
	
	@Override
	public void onGroupComplete(GroupParser parser, String text) throws ParseException
	{
		switch (mExpect)
		{
			case EXPECT_THREAD_NUMBER:
			{
				String threadNumber = null;
				Matcher matcher = THREAD_NUMBER.matcher(text);
				if (matcher.find()) threadNumber = matcher.group(1);
				mThreadNumber = threadNumber;
				break;
			}
			case EXPECT_TIMESTAMP:
			{
				if (mThreadNumber == null) throw new ParseException();
				try
				{
					long timestamp = DATE_FORMAT.parse(StringUtils.clearHtml(text).trim()).getTime();
					mPost = new Post();
					mPost.setTimestamp(timestamp);
					mPost.setPostNumber(Long.toString(timestampToPostNumber(timestamp)));
					mPost.setParentPostNumber(mParent);
					mPost.setThreadNumber(mThreadNumber);
				}
				catch (java.text.ParseException e)
				{
					
				}
				break;
			}
			case EXPECT_COMMENT:
			{
				text = text.trim();
				text = StringUtils.linkify(text);
				text = quotifyComment(text);
				mPost.setComment(text);
				if (mParent == null)
				{
					mParent = mPost.getPostNumber();
				}
				mPosts.add(mPost);
				mPost = null;
				break;
			}
		}
		mExpect = EXPECT_NONE;
	}
}