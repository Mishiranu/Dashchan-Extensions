package com.mishiranu.dashchan.chan.shanachan;

import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;

import chan.content.ChanConfiguration;
import chan.content.ChanLocator;
import chan.content.model.FileAttachment;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.text.GroupParser;
import chan.text.ParseException;
import chan.util.StringUtils;

public class ShanachanFlashThreadsParser implements GroupParser.Callback
{
	private final String mSource;
	private final ShanachanChanConfiguration mConfiguration;
	private final ShanachanChanLocator mLocator;
	private final String mBoardName;
	
	private Posts mThread;
	private Post mPost;
	private FileAttachment mAttachment;
	private ArrayList<Posts> mThreads;
	
	private static final int EXPECT_NONE = 0;
	private static final int EXPECT_THREAD_NUMBER = 1;
	private static final int EXPECT_NAME = 2;
	private static final int EXPECT_TRIPCODE = 3;
	private static final int EXPECT_FILE_NAME = 4;
	private static final int EXPECT_SUBJECT = 5;
	private static final int EXPECT_FILE_SIZE = 6;
	private static final int EXPECT_DATE = 7;
	private static final int EXPECT_OMITTED = 8;
	private static final int EXPECT_PAGES_COUNT = 9;
	
	private int mExpect = EXPECT_NONE;
	private int mColumn = -1;
	
	public ShanachanFlashThreadsParser(String source, Object linked, String boardName)
	{
		mSource = source;
		mConfiguration = ChanConfiguration.get(linked);
		mLocator = ChanLocator.get(linked);
		mBoardName = boardName;
	}
	
	public ArrayList<Posts> convertThreads() throws ParseException
	{
		mThreads = new ArrayList<>();
		GroupParser.parse(mSource, this);
		return mThreads;
	}
	
	@Override
	public boolean onStartElement(GroupParser parser, String tagName, String attrs)
	{
		if ("td".equals(tagName))
		{
			if ("width: 1%; white-space: nowrap".equals(parser.getAttr(attrs, "style")))
			{
				String cssClass = parser.getAttr(attrs, "class");
				if ("postblock".equals(cssClass)) return false;
				if (mPost == null)
				{
					mColumn = 0;
					mPost = new Post();
					mThread = new Posts(mPost);
					mThread.addPostsCount(1);
					mExpect = EXPECT_THREAD_NUMBER;
					return true;
				}
				else if (mColumn >= 0)
				{
					mColumn++;
					switch (mColumn)
					{
						case 3:
						{
							mExpect = EXPECT_FILE_SIZE;
							return true;
						}
						case 4:
						{
							mExpect = EXPECT_DATE;
							return true;
						}
						case 5:
						{
							mExpect = EXPECT_OMITTED;
							return true;
						}
					}
				}
			}
			else if (mPost != null && mColumn == 2)
			{
				mExpect = EXPECT_SUBJECT;
				return true;
			}
		}
		else if ("span".equals(tagName))
		{
			if (mPost != null)
			{
				String cssClass = parser.getAttr(attrs, "class");
				if ("postername".equals(cssClass))
				{
					mExpect = EXPECT_NAME;
					return true;
				}
				else if ("postertrip".equals(cssClass))
				{
					mExpect = EXPECT_TRIPCODE;
					return true;
				}
			}
		}
		else if ("a".equals(tagName))
		{
			if (mPost != null && mColumn == 1)
			{
				mAttachment = new FileAttachment();
				mPost.setAttachments(mAttachment);
				String path = parser.getAttr(attrs, "href");
				if (path != null) mAttachment.setFileUri(mLocator, mLocator.buildPath(path));
				mExpect = EXPECT_FILE_NAME;
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
			case EXPECT_THREAD_NUMBER:
			{
				text = text.trim();
				try
				{
					Integer.parseInt(text);
				}
				catch (NumberFormatException e)
				{
					mPost = null;
					mThread = null;
					mColumn = -1;
					break;
				}
				mPost.setPostNumber(text);
				break;
			}
			case EXPECT_NAME:
			{
				Matcher matcher = ShanachanPostsParser.NAME_EMAIL.matcher(text);
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
			case EXPECT_FILE_NAME:
			{
				mAttachment.setOriginalName(StringUtils.emptyIfNull(StringUtils.clearHtml(text).trim()));
				break;
			}
			case EXPECT_SUBJECT:
			{
				mPost.setSubject(StringUtils.emptyIfNull(StringUtils.clearHtml(text).trim()));
				break;
			}
			case EXPECT_FILE_SIZE:
			{
				if (mAttachment != null)
				{
					String[] splitted = text.trim().split(" ");
					float size = Float.parseFloat(splitted[0]);
					String dim = splitted[1];
					if ("kB".equals(dim)) size *= 1024f;
					else if ("MB".equals(dim)) size *= 1024f * 1024f;
					mAttachment.setSize((int) size);
				}
				break;
			}
			case EXPECT_DATE:
			{
				try
				{
					mPost.setTimestamp(ShanachanPostsParser.DATE_FORMAT.parse(text.trim()).getTime());
				}
				catch (java.text.ParseException e)
				{
					
				}
				break;
			}
			case EXPECT_OMITTED:
			{
				Matcher matcher = ShanachanPostsParser.NUMBER.matcher(text);
				if (matcher.find()) mThread.addPostsCount(Integer.parseInt(matcher.group(1)));
				mColumn = -1;
				mThreads.add(mThread);
				mPost = null;
				mThread = null;
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