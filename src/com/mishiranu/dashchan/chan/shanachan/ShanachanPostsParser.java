package com.mishiranu.dashchan.chan.shanachan;

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

public class ShanachanPostsParser implements GroupParser.Callback
{
	private final String mSource;
	private final ShanachanChanConfiguration mConfiguration;
	private final ShanachanChanLocator mLocator;
	private final String mBoardName;
	
	private String mParent;
	private Posts mThread;
	private Post mPost;
	private FileAttachment mAttachment;
	private ArrayList<Posts> mThreads;
	private final ArrayList<Post> mPosts = new ArrayList<>();
	
	private static final int EXPECT_NONE = 0;
	private static final int EXPECT_FILE_DATA = 1;
	private static final int EXPECT_FILE_SIZE = 2;
	private static final int EXPECT_SUBJECT = 3;
	private static final int EXPECT_NAME = 4;
	private static final int EXPECT_TRIPCODE = 5;
	private static final int EXPECT_COMMENT = 6;
	private static final int EXPECT_OMITTED = 7;
	private static final int EXPECT_BOARD_TITLE = 8;
	private static final int EXPECT_PAGES_COUNT = 9;
	
	private int mExpect = EXPECT_NONE;
	private boolean mHeaderHandling = false; // True when parser is inside post's <label>. Used to parse date.
	
	static final SimpleDateFormat DATE_FORMAT;
	
	static
	{
		DATE_FORMAT = new SimpleDateFormat("yy/MM/dd(EEE)hh:mm", Locale.US);
		DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT+1"));
	}
	
	private static final Pattern FILE_SIZE = Pattern.compile("([\\d\\.]+) (\\w+), (\\d+)x(\\d+)(?:, (.+))?");
	static final Pattern NAME_EMAIL = Pattern.compile("<a href=\"(.*?)\">(.*)</a>");
	static final Pattern NUMBER = Pattern.compile("(\\d+)");
	
	public ShanachanPostsParser(String source, Object linked, String boardName)
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
		return mThreads;
	}
	
	public ArrayList<Post> convertPosts() throws ParseException
	{
		GroupParser.parse(mSource, this);
		return mPosts.size() > 0 ? mPosts : null;
	}
	
	@Override
	public boolean onStartElement(GroupParser parser, String tagName, String attrs)
	{
		/*
		 * For original posts:
		 * 1) Post has image -> create Post when 'span.filesize' reached
		 * 2) Post has no image -> create Post when 'input[type=checkbox][name=delete]' reached
		 * Create Thread when 'input[type=checkbox][name=delete]' reached
		 * 
		 * For replies:
		 * Create Post when td[id^=reply] reached
		 */
		
		if ("input".equals(tagName))
		{
			if ("checkbox".equals(parser.getAttr(attrs, "type")) && "delete".equals(parser.getAttr(attrs, "name")))
			{
				mHeaderHandling = true;
				if (mPost == null || mPost.getPostNumber() == null)
				{
					String number = parser.getAttr(attrs, "value");
					if (mPost == null) mPost = new Post();
					mPost.setPostNumber(number);
					mParent = number;
					if (mThreads != null)
					{
						closeThread();
						mThread = new Posts();
					}
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
			if ("filesize".equals(cssClass))
			{
				if (mPost == null)
				{
					// New thread with image
					// Thread will be created later when parser reach deleting checkbox
					mPost = new Post();
				}
				mAttachment = new FileAttachment();
				mExpect = EXPECT_FILE_DATA;
			}
			else if ("filetitle".equals(cssClass) || "replytitle".equals(cssClass))
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
			else if ("omittedposts".equals(cssClass))
			{
				if (mThreads != null)
				{
					mExpect = EXPECT_OMITTED;
					return true;
				}
			}
		}
		else if ("img".equals(tagName))
		{
			String cssClass = parser.getAttr(attrs, "class");
			if ("thumb".equals(cssClass))
			{
				String src = parser.getAttr(attrs, "src");
				if (src != null && src.contains("/thumb/"))
				{
					mAttachment.setThumbnailUri(mLocator, mLocator.buildPath(src));
				}
				mPost.setAttachments(mAttachment);
			}
		}
		else if ("div".equals(tagName))
		{
			String cssClass = parser.getAttr(attrs, "class");
			if ("nothumb".equals(cssClass))
			{
				if (mAttachment.getSize() > 0) mPost.setAttachments(mAttachment);
				mExpect = EXPECT_NONE;
			}
			else if ("logo".equals(cssClass))
			{
				mExpect = EXPECT_BOARD_TITLE;
				return true;
			}
		}
		else if ("a".equals(tagName))
		{
			if (mExpect == EXPECT_FILE_DATA)
			{
				mAttachment.setFileUri(mLocator, mLocator.buildPath(parser.getAttr(attrs, "href")));
			}
		}
		else if ("em".equals(tagName))
		{
			if (mExpect == EXPECT_FILE_DATA)
			{
				mExpect = EXPECT_FILE_SIZE;
				return true;
			}
		}
		else if ("blockquote".equals(tagName))
		{
			mExpect = EXPECT_COMMENT;
			return true;
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
	public void onText(GroupParser parser, String source, int start, int end)
	{
		if (mHeaderHandling)
		{
			String text = source.substring(start, end).trim();
			if (text.length() > 0)
			{
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
				if (matcher.matches())
				{
					float size = Float.parseFloat(matcher.group(1));
					String dim = matcher.group(2);
					if ("kB".equals(dim)) size *= 1024f;
					else if ("MB".equals(dim)) size *= 1024f * 1024f;
					int width = Integer.parseInt(matcher.group(3));
					int height = Integer.parseInt(matcher.group(4));
					String originalName = matcher.group(5);
					mAttachment.setSize((int) size);
					mAttachment.setWidth(width);
					mAttachment.setHeight(height);
					mAttachment.setOriginalName(StringUtils.isEmptyOrWhitespace(originalName) ? null : originalName);
				}
				break;
			}
			case EXPECT_SUBJECT:
			{
				text = StringUtils.emptyIfNull(StringUtils.clearHtml(text).trim());
				if (text != null)
				{
					if ("f".equals(mBoardName)) text = text.replaceAll("^\\[.*?\\] *", ""); // Remove tag from subject
					mPost.setSubject(text);
				}
				break;
			}
			case EXPECT_NAME:
			{
				Matcher matcher = NAME_EMAIL.matcher(text);
				if (matcher.matches())
				{
					String email = StringUtils.clearHtml(matcher.group(1));
					if (email.toLowerCase(Locale.US).contains("sage")) mPost.setSage(true);
					else mPost.setEmail(email);
					text = matcher.group(2);
				}
				mPost.setName(StringUtils.emptyIfNull(StringUtils.clearHtml(text).trim()));
				break;
			}
			case EXPECT_TRIPCODE:
			{
				mPost.setTripcode(StringUtils.emptyIfNull(StringUtils.clearHtml(text).trim()));
				break;
			}
			case EXPECT_COMMENT:
			{
				text = text.trim();
				int index = text.lastIndexOf("<div class=\"abbrev\">");
				if (index >= 0) text = text.substring(0, index).trim();
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
				if (!"b".equals(mBoardName) && !"fetish".equals(mBoardName) && !"v".equals(mBoardName))
				{
					text = StringUtils.clearHtml(text).trim();
					if (text.startsWith("/" + mBoardName + "/ - ")) text = text.substring(mBoardName.length() + 5);
					if (!StringUtils.isEmpty(text)) mConfiguration.storeBoardTitle(mBoardName, text);
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