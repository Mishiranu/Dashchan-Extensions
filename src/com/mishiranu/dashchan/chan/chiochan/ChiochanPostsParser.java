package com.mishiranu.dashchan.chan.chiochan;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import chan.content.ChanConfiguration;
import chan.content.ChanLocator;
import chan.content.model.FileAttachment;
import chan.content.model.Icon;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.text.GroupParser;
import chan.text.ParseException;
import chan.util.StringUtils;

public class ChiochanPostsParser implements GroupParser.Callback
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
	
	private static final int EXPECT_NONE = 0;
	private static final int EXPECT_FILE_SIZE = 1;
	private static final int EXPECT_SUBJECT = 2;
	private static final int EXPECT_COUNTRY = 3;
	private static final int EXPECT_NAME = 4;
	private static final int EXPECT_TRIP = 5;
	private static final int EXPECT_COMMENT = 6;
	private static final int EXPECT_OMITTED = 7;
	private static final int EXPECT_BOARD_TITLE = 8;
	private static final int EXPECT_POST_BLOCK = 9;
	private static final int EXPECT_PAGES_COUNT = 10;
	
	private int mExpect = EXPECT_NONE;
	private boolean mHeaderHandling = false;
	
	private boolean mHasPostBlock = false;
	private boolean mHasPostBlockName = false;
	
	private static final SimpleDateFormat DATE_FORMAT;
	
	static
	{
		DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.US);
		DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT+3"));
	}
	
	private static final Pattern FILE_SIZE = Pattern.compile("\\(([\\d\\.]+)(\\w+) *, *(\\d+)x(\\d+)" +
			"(?: *, *(.+))? *\\) *$");
	private static final Pattern NAME_EMAIL = Pattern.compile("<a href=\"(.*?)\">(.*)</a>");
	static final Pattern NUMBER = Pattern.compile("(\\d+)");
	
	public ChiochanPostsParser(String source, Object linked, String boardName)
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
		GroupParser.parse(mSource, this);
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
		GroupParser.parse(mSource, this);
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
		GroupParser.parse(mSource, this);
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
	
	@Override
	public boolean onStartElement(GroupParser parser, String tagName, String attrs)
	{
		boolean isDiv = "div".equals(tagName);
		boolean isTd = "td".equals(tagName);
		if (isDiv || isTd)
		{
			String id = parser.getAttr(attrs, "id");
			if (id != null && id.startsWith("reply"))
			{
				String number = id.substring(5);
				Post post = new Post();
				post.setParentPostNumber(mParent);
				post.setPostNumber(number);
				mPost = post;
			}
			else if (isDiv)
			{
				if (id != null && id.startsWith("thread"))
				{
					String number = id.substring(6, id.length() - mBoardName.length());
					Post post = new Post();
					post.setPostNumber(number);
					mParent = number;
					mPost = post;
					if (mThreads != null)
					{
						closeThread();
						mThread = new Posts();
					}
				}
				else
				{
					String cssClass = parser.getAttr(attrs, "class");
					if ("logo".equals(cssClass))
					{
						mExpect = EXPECT_BOARD_TITLE;
						return true;
					}
					else if ("pgstbl".equals(cssClass))
					{
						mExpect = EXPECT_PAGES_COUNT;
						return true;
					}
				}
			}
			else if (isTd)
			{
				String cssClass = parser.getAttr(attrs, "class");
				if ("postblock".equals(cssClass))
				{
					mHasPostBlock = true;
					mExpect = EXPECT_POST_BLOCK;
					return true;
				}
			}
		}
		else if ("label".equals(tagName))
		{
			if (mPost != null)
			{
				mHeaderHandling = true;
			}
		}
		else if ("span".equals(tagName))
		{
			String cssClass = parser.getAttr(attrs, "class");
			if ("country".equals(cssClass))
			{
				mExpect = EXPECT_COUNTRY;
			}
			else if ("filesize".equals(cssClass))
			{
				if (mExpandMode && mPost == null) mPost = new Post();
				mAttachment = new FileAttachment();
				mExpect = EXPECT_FILE_SIZE;
				return true;
			}
			else if ("filetitle".equals(cssClass))
			{
				mExpect = EXPECT_SUBJECT;
				return true;
			}
			else if ("postername".equals(cssClass))
			{
				mExpect = EXPECT_NAME;
				return true;
			}
			else if ("postertrip".equals(cssClass))
			{
				mExpect = EXPECT_TRIP;
				return true;
			}
			else if ("omittedposts".equals(cssClass))
			{
				if (mThreads != null)
				{
					mExpect = EXPECT_OMITTED;
					return true;
				}
			}
		}
		else if ("a".equals(tagName))
		{
			if (mAttachment != null)
			{
				String path = convertUriString(parser.getAttr(attrs, "href"));
				mAttachment.setFileUri(mLocator, mLocator.buildPath(path));
			}
		}
		else if ("img".equals(tagName))
		{
			String cssClass = parser.getAttr(attrs, "class");
			if ("thumb".equals(cssClass))
			{
				String path = convertUriString(parser.getAttr(attrs, "src"));
				mAttachment.setThumbnailUri(mLocator, mLocator.buildPath(path));
				mPost.setAttachments(mAttachment);
				mAttachment = null;
			}
			else if (mExpect == EXPECT_COUNTRY)
			{
				String path = convertUriString(parser.getAttr(attrs, "src"));
				String title = StringUtils.clearHtml(parser.getAttr(attrs, "alt"));
				mPost.setIcons(new Icon(mLocator, mLocator.buildPath(path), title));
			}
			else
			{
				if (mPost != null)
				{
					String src = parser.getAttr(attrs, "src");
					if (src != null)
					{
						if (src.endsWith("/css/sticky.gif")) mPost.setSticky(true);
						else if (src.endsWith("/css/locked.gif")) mPost.setClosed(true);
					}
				}
			}
		}
		else if ("blockquote".equals(tagName))
		{
			mExpect = EXPECT_COMMENT;
			return true;
		}
		else if ("input".equals(tagName))
		{
			if (mExpandMode)
			{
				String name = parser.getAttr(attrs, "name");
				if ("delete[]".equals(name))
				{
					if (mPost == null) mPost = new Post();
					mPost.setPostNumber(parser.getAttr(attrs, "value"));
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
		if (mHeaderHandling)
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
					mPost.setTimestamp(DATE_FORMAT.parse(text).getTime());
				}
				catch (java.text.ParseException e)
				{
					
				}
				mHeaderHandling = false;
			}
		}
	}
	
	@Override
	public void onGroupComplete(GroupParser parser, String text)
	{
		switch (mExpect)
		{
			case EXPECT_FILE_SIZE:
			{
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
					mAttachment.setSize((int) size);
					mAttachment.setWidth(width);
					mAttachment.setHeight(height);
					mAttachment.setOriginalName(StringUtils.isEmptyOrWhitespace(fileName) ? null : fileName.trim());
				}
				break;
			}
			case EXPECT_SUBJECT:
			{
				text = text.trim();
				if (text.endsWith("\u21e9"))
				{
					text = StringUtils.nullIfEmpty(text.substring(0, text.length() - 1));
					mPost.setSage(true);
				}
				mPost.setSubject(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));
				break;
			}
			case EXPECT_NAME:
			{
				text = text.trim();
				Matcher matcher = NAME_EMAIL.matcher(text);
				if (matcher.matches())
				{
					String email = matcher.group(1);
					if (email != null && email.toLowerCase(Locale.US).contains("sage"))
					{
						// Old chiochan sage appearance
						mPost.setSage(true);
					}
					text = matcher.group(2);
				}
				mPost.setName(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));
				break;
			}
			case EXPECT_TRIP:
			{
				mPost.setTripcode(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));
				break;
			}
			case EXPECT_COMMENT:
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
						mPost.setPosterBanned(true);
					}
				}
				mPost.setComment(text);
				mPosts.add(mPost);
				mPost = null;
				break;
			}
			case EXPECT_OMITTED:
			{
				Matcher matcher = NUMBER.matcher(text);
				if (matcher.find())
				{
					mThread.addPostsCount(Integer.parseInt(matcher.group(1)));
					if (matcher.find()) mThread.addPostsWithFilesCount(Integer.parseInt(matcher.group(1)));
				}
				break;
			}
			case EXPECT_BOARD_TITLE:
			{
				text = StringUtils.clearHtml(text).trim();
				if (!StringUtils.isEmpty(text)) mConfiguration.storeBoardTitle(mBoardName, text);
				break;
			}
			case EXPECT_POST_BLOCK:
			{
				if ("Имя".equals(text) || "Name".equals(text)) mHasPostBlockName = true;
				break;
			}
			case EXPECT_PAGES_COUNT:
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
						mConfiguration.storePagesCount(mBoardName, pagesCount);
					}
					catch (NumberFormatException e)
					{
						
					}
				}
				break;
			}
		}
		mExpect = EXPECT_NONE;
	}
}