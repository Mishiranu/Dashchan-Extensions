package com.mishiranu.dashchan.chan.taima;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import chan.content.ChanConfiguration;
import chan.content.ChanLocator;
import chan.content.model.FileAttachment;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.text.GroupParser;
import chan.text.ParseException;
import chan.util.StringUtils;

public class TaimaPostsParser implements GroupParser.Callback
{
	private final String mSource;
	private final TaimaChanConfiguration mConfiguration;
	private final TaimaChanLocator mLocator;
	private final String mBoardName;
	
	private String mParent;
	private Posts mThread;
	private Post mPost;
	private FileAttachment mAttachment;
	private ArrayList<Posts> mThreads;
	private final ArrayList<Post> mPosts = new ArrayList<>();
	
	private static final int EXPECT_NONE = 0;
	private static final int EXPECT_SUBJECT = 1;
	private static final int EXPECT_NAME = 2;
	private static final int EXPECT_TRIPCODE = 3;
	private static final int EXPECT_DATE_ID = 4;
	private static final int EXPECT_FILE_DATA = 5;
	private static final int EXPECT_COMMENT = 6;
	private static final int EXPECT_OMITTED = 7;
	private static final int EXPECT_BOARD_TITLE = 8;
	private static final int EXPECT_PAGES_COUNT = 9;
	
	private int mExpect = EXPECT_NONE;
	
	private boolean mHasPostBlock = false;
	private boolean mHasPostBlockName = false;
	
	private static final SimpleDateFormat DATE_FORMAT;
	
	static
	{
		DATE_FORMAT = new SimpleDateFormat("ccc, dd MMM yy HH:mm:ss 'EST'", Locale.US);
		DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT-4"));
	}
	
	private static final Pattern FILE_SIZE = Pattern.compile("(?:(\\d+)B / )?([\\d\\.]+) ?(\\w+), (\\d+)x(\\d+)");
	private static final Pattern NUMBER = Pattern.compile("(\\d+)");
	
	public TaimaPostsParser(String source, Object linked, String boardName)
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
	
	public Posts convertPosts() throws ParseException
	{
		GroupParser.parse(mSource, this);
		if (mPosts.size() > 0)
		{
			updateConfiguration();
			return new Posts(mPosts);
		}
		return null;
	}
	
	private void updateConfiguration()
	{
		if (mHasPostBlock)
		{
			mConfiguration.storeNamesEnabled(mBoardName, mHasPostBlockName);
		}
	}
	
	@Override
	public boolean onStartElement(GroupParser parser, String tagName, String attrs)
	{
		if ("div".equals(tagName))
		{
			String cssClass = parser.getAttr(attrs, "class");
			if ("thread_header".equals(cssClass))
			{
				mParent = null;
				mPost = new Post();
				if (mThreads != null)
				{
					closeThread();
					mThread = new Posts();
				}
			}
			else if ("lock".equals(cssClass))
			{
				mPost.setClosed(true);
			}
			else if ("ban".equals(cssClass))
			{
				mPost.setPosterBanned(true);
			}
			else if ("warn".equals(cssClass))
			{
				mPost.setPosterWarned(true);
			}
			else if ("pagelist".equals(cssClass))
			{
				mExpect = EXPECT_PAGES_COUNT;
				return true;
			}
		}
		else if ("td".equals(tagName))
		{
			String id = parser.getAttr(attrs, "id");
			if (id != null && id.startsWith("reply"))
			{
				String number = id.substring(5);
				mPost = new Post();
				mPost.setParentPostNumber(mParent);
				mPost.setPostNumber(number);
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
			else if ("postername".equals(cssClass) || "commentpostername".equals(cssClass))
			{
				mExpect = EXPECT_NAME;
				return true;
			}
			else if ("postertrip".equals(cssClass))
			{
				mExpect = EXPECT_TRIPCODE;
				return true;
			}
			else if ("idhighlight".equals(cssClass))
			{
				mExpect = EXPECT_DATE_ID;
				return true;
			}
			else if ("filesize".equals(cssClass))
			{
				mExpect = EXPECT_FILE_DATA;
				mAttachment = new FileAttachment();
				mPost.setAttachments(mAttachment);
			}
			else if ("board_title".equals(cssClass))
			{
				mExpect = EXPECT_BOARD_TITLE;
				return true;
			}
			else if ("omittedposts".equals(cssClass))
			{
				mExpect = EXPECT_OMITTED;
				return true;
			}
		}
		else if ("em".equals(tagName))
		{
			if (mExpect == EXPECT_FILE_DATA)
			{
				return true;
			}
		}
		else if ("a".equals(tagName))
		{
			if (mExpect == EXPECT_FILE_DATA)
			{
				String path = parser.getAttr(attrs, "href");
				if (path != null) mAttachment.setFileUri(mLocator, mLocator.createSpecialBoardUri(path));
			}
			else if (mParent == null && mPost != null)
			{
				String id = parser.getAttr(attrs, "id");
				if (id != null)
				{
					mParent = id;
					mPost.setPostNumber(id);
				}
			}
		}
		else if ("img".equals(tagName))
		{
			String cssClass = parser.getAttr(attrs, "class");
			if ("thumb".equals(cssClass))
			{
				String path = parser.getAttr(attrs, "src");
				if (path != null && (!path.equals(mAttachment.getFileUri(mLocator).getPath()) ||
						mAttachment.getSize() < 50 * 1024))
				{
					// GIF thumbnails has the same URI as image and can weigh a lot
					if (path != null) mAttachment.setThumbnailUri(mLocator, mLocator.createSpecialBoardUri(path));
				}
			}
		}
		else if ("i".equals(tagName))
		{
			String cssClass = parser.getAttr(attrs, "class");
			if ("gl glyphicon-paperclip".equals(cssClass))
			{
				mPost.setSticky(true);
			}
		}
		else if ("blockquote".equals(tagName))
		{
			mExpect = EXPECT_COMMENT;
			return true;
		}
		else if ("form".equals(tagName))
		{
			String id = parser.getAttr(attrs, "id");
			if ("postform".equals(id)) mHasPostBlock = true;
		}
		else if ("input".equals(tagName))
		{
			if (mHasPostBlock)
			{
				String name = parser.getAttr(attrs, "name");
				if ("field1".equals(name)) mHasPostBlockName = true;
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
				mPost.setName(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));
				break;
			}
			case EXPECT_TRIPCODE:
			{
				mPost.setTripcode(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));
				break;
			}
			case EXPECT_DATE_ID:
			{
				int index = text.indexOf("ID:");
				if (index >= 0) mPost.setIdentifier(text.substring(index + 3));
				index = text.indexOf("EST");
				if (index >= 0)
				{
					try
					{
						mPost.setTimestamp(DATE_FORMAT.parse(text.substring(0, index + 3)).getTime());
					}
					catch (java.text.ParseException e)
					{
						
					}
				}
				break;
			}
			case EXPECT_FILE_DATA:
			{
				Matcher matcher = FILE_SIZE.matcher(text);
				if (matcher.matches())
				{
					String sizebs = matcher.group(1);
					int size;
					if (sizebs != null) size = Integer.parseInt(sizebs); else
					{
						size = Integer.parseInt(matcher.group(2));
						String dim = matcher.group(3);
						if ("KB".equals(dim)) size *= 1024;
						else if ("MB".equals(dim)) size *= 1024 * 1024;
					}
					int width = Integer.parseInt(matcher.group(4));
					int height = Integer.parseInt(matcher.group(5));
					mAttachment.setSize(size);
					mAttachment.setWidth(width);
					mAttachment.setHeight(height);
				}
				break;
			}
			case EXPECT_COMMENT:
			{
				text = text.trim();
				int index = text.lastIndexOf("<div class=\"abbrev\">");
				if (index >= 0) text = text.substring(0, index).trim();
				mPost.setComment(text);
				mPosts.add(mPost);
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
			case EXPECT_PAGES_COUNT:
			{
				text = StringUtils.clearHtml(text);
				String pagesCount = null;
				Matcher matcher = NUMBER.matcher(text);
				while (matcher.find()) pagesCount = matcher.group(1);
				if (pagesCount != null)
				{
					try
					{
						mConfiguration.storePagesCount(mBoardName, Integer.parseInt(pagesCount) + 1);
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