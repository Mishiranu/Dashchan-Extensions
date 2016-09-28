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
import chan.text.GroupParser;
import chan.text.ParseException;
import chan.util.StringUtils;

@SuppressLint("SimpleDateFormat")
public class PonyachPostsParser implements GroupParser.Callback
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
	
	private static final int EXPECT_NONE = 0;
	private static final int EXPECT_SUBJECT = 1;
	private static final int EXPECT_NAME = 2;
	private static final int EXPECT_TRIPCODE = 3;
	private static final int EXPECT_DATE = 4;
	private static final int EXPECT_FILE_SIZE = 5;
	private static final int EXPECT_COMMENT = 6;
	private static final int EXPECT_OMITTED = 7;
	private static final int EXPECT_BOARD_TITLE = 8;
	private static final int EXPECT_PAGES_COUNT = 9;
	
	private int mExpect = EXPECT_NONE;
	
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
		GroupParser.parse(mSource, this);
		closeThread();
		if (mAttachmentCount >= 0) mConfiguration.storeAttachmentCount(mBoardName, mAttachmentCount);
		return mThreads;
	}
	
	public Posts convertPosts() throws ParseException
	{
		GroupParser.parse(mSource, this);
		return mPosts.size() > 0 ? new Posts(mPosts) : null;
	}
	
	@Override
	public boolean onStartElement(GroupParser parser, String tagName, String attrs)
	{
		if ("div".equals(tagName))
		{
			String id = parser.getAttr(attrs, "id");
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
			}
		}
		else if ("td".equals(tagName))
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
		}
		else if ("span".equals(tagName))
		{
			String cssClass = parser.getAttr(attrs, "class");
			if ("filetitle".equals(cssClass))
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
				mExpect = EXPECT_TRIPCODE;
				return true;
			}
			else if ("mobile_date dast-date".equals(cssClass))
			{
				mExpect = EXPECT_DATE;
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
			else if (cssClass != null && cssClass.startsWith("filesize fs_"))
			{
				mExpect = EXPECT_FILE_SIZE;
				return true;
			}
		}
		else if ("img".equals(tagName))
		{
			if (mPost != null)
			{
				String src = parser.getAttr(attrs, "src");
				if (src != null)
				{
					if (src.endsWith("/icons/locked.png")) mPost.setSticky(true);
					else if (src.endsWith("/icons/locked.gif")) mPost.setClosed(true);
				}
			}
		}
		else if ("a".equals(tagName))
		{
			if (mPost != null && mAttachments.size() > 0)
			{
				String onclick = parser.getAttr(attrs, "onclick");
				if (onclick != null && onclick.startsWith("javascript:expandimg"))
				{
					FileAttachment attachment = mAttachments.get(mAttachments.size() - 1);
					int index = onclick.indexOf("/src/");
					if (index > 0)
					{
						String path = onclick.substring(index + 5, onclick.indexOf('\'', index));
						attachment.setFileUri(mLocator, mLocator.buildPath(mBoardName, "src", path));
					}
					index = onclick.indexOf("/thumb/");
					if (index > 0)
					{
						String path = onclick.substring(index + 7, onclick.indexOf('\'', index));
						attachment.setThumbnailUri(mLocator, mLocator.buildPath(mBoardName, "thumb", path));
					}
				}
			}
		}
		else if ("blockquote".equals(tagName))
		{
			mExpect = EXPECT_COMMENT;
			return true;
		}
		else if ("select".equals(tagName))
		{
			if (mThreads != null)
			{
				String name = parser.getAttr(attrs, "name");
				if (name != null && name.startsWith("upload-rating-"))
				{
					mAttachmentCount = Integer.parseInt(name.substring(14));
				}
			}
		}
		else if ("table".equals(tagName))
		{
			String border = parser.getAttr(attrs, "border");
			if (mThreads != null && "1".equals(border))
			{
				mExpect = EXPECT_PAGES_COUNT;
				return true;
			}
		}
		return false;
	}
	
	@Override
	public void onEndElement(GroupParser parser, String tagName)
	{
		
	}
	
	@Override
	public void onText(GroupParser parser, String source, int start, int end) throws ParseException
	{
		
	}
	
	@Override
	public void onGroupComplete(GroupParser parser, String text)
	{
		switch (mExpect)
		{
			case EXPECT_SUBJECT:
			{
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
					if (email.toLowerCase(Locale.US).equals("mailto:sage")) mPost.setSage(true);
					else mPost.setEmail(StringUtils.clearHtml(email));
					text = matcher.group(2);
				}
				mPost.setName(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));
				break;
			}
			case EXPECT_TRIPCODE:
			{
				String tripcode = text.trim();
				if ("[M]".equals(tripcode)) mPost.setCapcode("Mod");
				else mPost.setTripcode(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));
				break;
			}
			case EXPECT_DATE:
			{
				text = text.trim();
				int index = text.indexOf(' ');
				if (index >= 0)
				{
					text = text.substring(index + 1);
					try
					{
						mPost.setTimestamp(DATE_FORMAT.parse(text).getTime());
					}
					catch (java.text.ParseException e)
					{
						
					}
				}
				break;
			}
			case EXPECT_FILE_SIZE:
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
					mAttachments.add(attachment);
				}
				break;
			}
			case EXPECT_COMMENT:
			{
				text = text.trim();
				text = text.replaceAll("<a class=\"irc-reflink.*?</a>", "");
				int index = text.lastIndexOf("<div class=\"abbrev\">");
				if (index >= 0) text = text.substring(0, index).trim();
				if (mAttachments.size() > 0)
				{
					mPost.setAttachments(mAttachments);
					mAttachments.clear();
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
					if (matcher.find()) mThread.addFilesCount(Integer.parseInt(matcher.group(1)));
				}
				break;
			}
			case EXPECT_BOARD_TITLE:
			{
				text = StringUtils.clearHtml(text).trim();
				if (!StringUtils.isEmpty(text))
				{
					mConfiguration.storeBoardTitle(mBoardName, PonyachBoardsParser
							.validateBoardTitle(mBoardName, text));
				}
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