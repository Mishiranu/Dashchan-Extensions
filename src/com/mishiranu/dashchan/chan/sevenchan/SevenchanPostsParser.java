package com.mishiranu.dashchan.chan.sevenchan;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import chan.content.ChanConfiguration;
import chan.content.ChanLocator;
import chan.content.model.Attachment;
import chan.content.model.EmbeddedAttachment;
import chan.content.model.FileAttachment;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.text.GroupParser;
import chan.text.ParseException;
import chan.util.StringUtils;

public class SevenchanPostsParser implements GroupParser.Callback
{
	private final String mSource;
	private final SevenchanChanConfiguration mConfiguration;
	private final SevenchanChanLocator mLocator;
	private final String mBoardName;
	
	private String mParent;
	private Posts mThread;
	private Post mPost;
	private FileAttachment mAttachment;
	private ArrayList<Posts> mThreads;
	private final ArrayList<Attachment> mAttachments = new ArrayList<>();
	private final ArrayList<Post> mPosts = new ArrayList<>();
	
	private static final int EXPECT_NONE = 0;
	private static final int EXPECT_SUBJECT = 1;
	private static final int EXPECT_NAME = 2;
	private static final int EXPECT_TRIPCODE = 3;
	private static final int EXPECT_CAPCODE = 4;
	private static final int EXPECT_FILE_SIZE = 5;
	private static final int EXPECT_COMMENT = 6;
	private static final int EXPECT_OMITTED = 7;
	private static final int EXPECT_BOARD_TITLE = 8;
	private static final int EXPECT_PAGES_COUNT = 9;
	
	private int mExpect = EXPECT_NONE;
	private boolean mHeaderHandling = false;
	
	private boolean mHasPostBlock = false;
	private boolean mHasPostBlockName = false;
	private int mPostBlockFiles = 0;
	
	private static final SimpleDateFormat DATE_FORMAT;
	private static final SimpleDateFormat DATE_FORMAT_WEEABOO_CLEAN;
	
	static
	{
		DATE_FORMAT = new SimpleDateFormat("yy/MM/dd(EEE)hh:mm", Locale.US);
		DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT+5"));
		DATE_FORMAT_WEEABOO_CLEAN = new SimpleDateFormat("yyyy MM dd ( ) hh mm ss", Locale.JAPANESE);
		DATE_FORMAT_WEEABOO_CLEAN.setTimeZone(TimeZone.getTimeZone("GMT+5"));
	}
	
	private static final Pattern FILE_SIZE = Pattern.compile("\\(([\\d\\.]+)(\\w+) *, *(\\d+)x(\\d+)" +
			"(?: *, *(.+))? *\\)");
	private static final Pattern NAME_EMAIL = Pattern.compile("<a href=\"(.*?)\">(.*)</a>");
	private static final Pattern NUMBER = Pattern.compile("(\\d+)");
	
	public SevenchanPostsParser(String source, Object linked, String boardName)
	{
		mSource = source;
		mConfiguration = ChanConfiguration.get(linked);
		mLocator = ChanLocator.get(linked);
		mBoardName = boardName;
	}
	
	public SevenchanPostsParser(String source, Object linked, String boardName, String parent)
	{
		this(source, linked, boardName);
		mParent = parent;
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
			if (mThreads == null) mConfiguration.storeMaxReplyFilesCount(mBoardName, mPostBlockFiles);
		}
	}
	
	private String cutAttachmentUriString(String uriString)
	{
		int index = uriString.indexOf("//");
		if (index >= 0)
		{
			index = uriString.indexOf('/', index + 2);
			if (index >= 0) return uriString.substring(index);
			return null;
		}
		return uriString;
	}
	
	@Override
	public boolean onStartElement(GroupParser parser, String tagName, String attrs)
	{
		if ("div".equals(tagName))
		{
			String cssClass = parser.getAttr(attrs, "class");
			if ("thread".equals(cssClass))
			{
				if (mThreads != null)
				{
					closeThread();
					mThread = new Posts();
					mParent = null;
				}
			}
			else if ("post".equals(cssClass))
			{
				String id = parser.getAttr(attrs, "id");
				Post post = new Post();
				if (mParent == null) mParent = id; else post.setParentPostNumber(mParent);
				post.setPostNumber(id);
				mPost = post;
			}
			else if ("post_header".equals(cssClass))
			{
				mHeaderHandling = true;
			}
			else if ("post_thumb".equals(cssClass))
			{
				mAttachment = new FileAttachment();
			}
			else if (cssClass == null)
			{
				String id = parser.getAttr(attrs, "id");
				if ("paging".equals(id))
				{
					mExpect = EXPECT_PAGES_COUNT;
					return true;
				}
			}
		}
		else if ("span".equals(tagName))
		{
			String cssClass = parser.getAttr(attrs, "class");
			if ("subject".equals(cssClass))
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
			else if ("capcode".equals(cssClass))
			{
				mExpect = EXPECT_CAPCODE;
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
			else if ("title".equals(cssClass))
			{
				mExpect = EXPECT_BOARD_TITLE;
				return true;
			}
		}
		else if ("a".equals(tagName))
		{
			if (mAttachment != null && mAttachment.getFileUri(mLocator) == null)
			{
				String path = cutAttachmentUriString(parser.getAttr(attrs, "href"));
				mAttachment.setFileUri(mLocator, mLocator.buildPath(path));
			}
			else
			{
				String id = parser.getAttr(attrs, "id");
				if (id != null && id.startsWith("expandimg_"))
				{
					String path = cutAttachmentUriString(parser.getAttr(attrs, "href"));
					if (!path.endsWith("/removed.png"))
					{
						mAttachment = new FileAttachment();
						mAttachment.setFileUri(mLocator, mLocator.buildPath(path));
					}
				}
			}
		}
		else if ("img".equals(tagName))
		{
			if (mPost != null)
			{
				String cssClass = parser.getAttr(attrs, "class");
				if ("thumb".equals(cssClass))
				{
					String path = cutAttachmentUriString(parser.getAttr(attrs, "src"));
					mAttachment.setThumbnailUri(mLocator, mLocator.buildPath(path));
				}
				else if ("multithumbfirst".equals(cssClass) || "multithumb".equals(cssClass))
				{
					if (mAttachment != null)
					{
						String path = cutAttachmentUriString(parser.getAttr(attrs, "src"));
						mAttachment.setThumbnailUri(mLocator, mLocator.buildPath(path));
						String title = parser.getAttr(attrs, "title");
						parseFileSize(mAttachment, title);
						mAttachments.add(mAttachment);
						mAttachment = null;
					}
				}
				else if ("stickied".equals(cssClass))
				{
					mPost.setSticky(true);
				}
				else if ("locked".equals(cssClass))
				{
					mPost.setClosed(true);
				}
			}
		}
		else if ("p".equals(tagName))
		{
			String cssClass = parser.getAttr(attrs, "class");
			if ("file_size".equals(cssClass) && mPost != null)
			{
				mExpect = EXPECT_FILE_SIZE;
				return true;
			}
			else if ("message".equals(cssClass))
			{
				mExpect = EXPECT_COMMENT;
				return true;
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
		else if ("form".equals(tagName))
		{
			String name = parser.getAttr(attrs, "name");
			if ("postform".equals(name)) mHasPostBlock = true;
		}
		else if ("input".equals(tagName))
		{
			if (mHasPostBlock)
			{
				String name = parser.getAttr(attrs, "name");
				if ("name".equals(name)) mHasPostBlockName = true;
				else if ("imagefile[]".equals(name)) mPostBlockFiles++;
			}
		}
		return false;
	}
	
	@Override
	public void onEndElement(GroupParser parser, String tagName)
	{
		if (mHeaderHandling && "div".equals(tagName))
		{
			mHeaderHandling = false;
		}
	}
	
	@Override
	public void onText(GroupParser parser, String source, int start, int end)
	{
		if (mHeaderHandling)
		{
			String text = source.substring(start, end).trim();
			if (text.startsWith("ID: "))
			{
				mPost.setIdentifier(StringUtils.clearHtml(text.substring(4)).trim());
			}
			else if (text.contains("("))
			{
				try
				{
					mPost.setTimestamp(DATE_FORMAT.parse(text).getTime());
				}
				catch (java.text.ParseException e1)
				{
					try
					{
						text = text.replaceAll("&#\\d+;", " ");
						mPost.setTimestamp(DATE_FORMAT_WEEABOO_CLEAN.parse(text).getTime());
					}
					catch (java.text.ParseException e2)
					{
						
					}
				}
			}
		}
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
				mPost.setTripcode(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));
				break;
			}
			case EXPECT_CAPCODE:
			{
				mPost.setCapcode(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).replaceAll(" ?## ?", "").trim()));
				break;
			}
			case EXPECT_FILE_SIZE:
			{
				parseFileSize(mAttachment, text);
				mAttachments.add(mAttachment);
				mAttachment = null;
				break;
			}
			case EXPECT_COMMENT:
			{
				text = text.trim();
				if (mThreads != null)
				{
					int index = text.lastIndexOf("<span class=\"abbrev\">");
					if (index >= 0) text = text.substring(0, index).trim();
				}
				if (text.startsWith("<span style=\"float: left;\">"))
				{
					int index = text.indexOf("data=\"");
					if (index >= 0)
					{
						index += 6;
						String url = text.substring(index, text.indexOf('"', index));
						if (url.startsWith("//")) url = "http:" + url;
						EmbeddedAttachment attachment = EmbeddedAttachment.obtain(url);
						if (attachment != null) mAttachments.add(attachment);
					}
					index = text.indexOf("</span></span>");
					if (index >= 0)
					{
						index += 14;
						if (text.indexOf("&nbsp;", index) == index) index += 6;
						text = text.substring(index).trim();
					}
				}
				mPost.setComment(text);
				mPosts.add(mPost);
				if (mAttachments.size() > 0)
				{
					mPost.setAttachments(mAttachments);
					mAttachments.clear();
				}
				mPost = null;
				break;
			}
			case EXPECT_OMITTED:
			{
				text = StringUtils.clearHtml(text);
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
				int index = text.lastIndexOf('\n');
				if (index > 0)
				{
					text = text.substring(index + 1).trim();
					mConfiguration.storeBoardTitle(mBoardName, text);
				}
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
	
	private void parseFileSize(FileAttachment attachment, String text)
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
			if (fileName != null && fileName.endsWith(")")) fileName = fileName.substring(0, fileName.length() - 1);
			attachment.setSize((int) size);
			attachment.setWidth(width);
			attachment.setHeight(height);
			attachment.setOriginalName(StringUtils.isEmptyOrWhitespace(fileName) ? null : fileName.trim());
		}
	}
}