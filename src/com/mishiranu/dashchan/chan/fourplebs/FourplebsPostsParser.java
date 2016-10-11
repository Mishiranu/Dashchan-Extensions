package com.mishiranu.dashchan.chan.fourplebs;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.net.Uri;

import chan.content.ChanLocator;
import chan.content.model.FileAttachment;
import chan.content.model.Icon;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.text.GroupParser;
import chan.text.ParseException;
import chan.util.StringUtils;

public class FourplebsPostsParser implements GroupParser.Callback
{
	private final String mSource;
	private final FourplebsChanLocator mLocator;

	private boolean mNeedResTo = false;

	private String mResTo;
	private Posts mThread;
	private Post mPost;
	private FileAttachment mAttachment;
	private ArrayList<Posts> mThreads;
	private final ArrayList<Post> mPosts = new ArrayList<>();

	private static final int EXPECT_NONE = 0;
	private static final int EXPECT_FILE_SIZE = 1;
	private static final int EXPECT_SUBJECT = 2;
	private static final int EXPECT_NAME = 3;
	private static final int EXPECT_TRIPCODE = 4;
	private static final int EXPECT_IDENTIFIER = 5;
	private static final int EXPECT_COMMENT = 6;
	private static final int EXPECT_OMITTED_POSTS = 7;
	private static final int EXPECT_OMITTED_IMAGES = 8;

	private int mExpect = EXPECT_NONE;
	private boolean mOriginalPostFileStart = false;

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ssZZZZZ", Locale.US);

	private static final Pattern FILE_SIZE = Pattern.compile("(\\d+)(\\w+), (\\d+)x(\\d+)");
	private static final Pattern FLAG = Pattern.compile("flag-([a-z]+)");

	public FourplebsPostsParser(String source, Object linked)
	{
		mSource = source;
		mLocator = ChanLocator.get(linked);
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

	public Posts convertPosts(Uri threadUri) throws ParseException
	{
		GroupParser.parse(mSource, this);
		return mPosts.size() > 0 ? new Posts(mPosts).setArchivedThreadUri(threadUri) : null;
	}

	public ArrayList<Post> convertSearch() throws ParseException
	{
		mNeedResTo = true;
		GroupParser.parse(mSource, this);
		return mPosts;
	}

	private void ensureFile()
	{
		if (mAttachment == null)
		{
			mAttachment = new FileAttachment();
			mPost.setAttachments(mAttachment);
		}
	}

	private String convertImageSrc(String src)
	{
		int index = src.indexOf("/", src.indexOf("//") + 2);
		return index >= 0 ? src.substring(index) : null;
	}

	@Override
	public boolean onStartElement(GroupParser parser, String tagName, String attrs) throws ParseException
	{
		if ("article".equals(tagName))
		{
			String id = parser.getAttr(attrs, "id");
			if (id != null)
			{
				String cssClass = parser.getAttr(attrs, "class");
				if (cssClass != null)
				{
					if (cssClass.contains("thread"))
					{
						String number = id;
						Post post = new Post();
						post.setPostNumber(number);
						mResTo = number;
						mPost = post;
						mAttachment = null;
						if (mThreads != null)
						{
							closeThread();
							mThread = new Posts();
						}
					}
					else if (cssClass.contains("post"))
					{
						Post post = new Post();
						post.setParentPostNumber(mResTo);
						post.setPostNumber(id);
						mPost = post;
						mAttachment = null;
					}
				}
			}
		}
		else if ("span".equals(tagName))
		{
			String cssClass = parser.getAttr(attrs, "class");
			if ("post_author".equals(cssClass) && mPost != null)
			{
				mExpect = EXPECT_NAME;
				return true;
			}
			else if ("post_tripcode".equals(cssClass) && mPost != null)
			{
				mExpect = EXPECT_TRIPCODE;
				return true;
			}
			else if ("poster_hash".equals(cssClass) && mPost != null)
			{
				mExpect = EXPECT_IDENTIFIER;
				return true;
			}
			else if ("omitted_posts".equals(cssClass))
			{
				mExpect = EXPECT_OMITTED_POSTS;
				return true;
			}
			else if ("omitted_images".equals(cssClass))
			{
				mExpect = EXPECT_OMITTED_IMAGES;
				return true;
			}
			else if ("post_file_metadata".equals(cssClass))
			{
				ensureFile();
				mExpect = EXPECT_FILE_SIZE;
				return true;
			}
			else if (cssClass != null && cssClass.contains("flag"))
			{
				Matcher matcher = FLAG.matcher(cssClass);
				if (matcher.find())
				{
					String country = matcher.group(1);
					Uri uri = mLocator.buildPathWithHost("s.4cdn.org", "image", "country",
							country.toLowerCase(Locale.US) + ".gif");
					String title = StringUtils.clearHtml(parser.getAttr(attrs, "title"));
					mPost.setIcons(new Icon(mLocator, uri, title));
				}
			}
		}
		else if ("h2".equals(tagName))
		{
			String cssClass = parser.getAttr(attrs, "class");
			if ("post_title".equals(cssClass))
			{
				mExpect = EXPECT_SUBJECT;
				return true;
			}
		}
		else if ("time".equals(tagName))
		{
			String datetime = parser.getAttr(attrs, "datetime");
			try
			{
				mPost.setTimestamp(DATE_FORMAT.parse(datetime).getTime());
			}
			catch (java.text.ParseException e)
			{
				throw new RuntimeException(e);
			}
		}
		else if ("div".equals(tagName))
		{
			String cssClass = parser.getAttr(attrs, "class");
			if ("text".equals(cssClass))
			{
				mExpect = EXPECT_COMMENT;
				return true;
			}
			else if ("post_file".equals(cssClass) && mPost.getParentPostNumber() == null)
			{
				ensureFile();
				if (mOriginalPostFileStart)
				{
					mOriginalPostFileStart = false;
					mExpect = EXPECT_FILE_SIZE;
					return true;
				}
				else
				{
					mOriginalPostFileStart = true;
					parser.mark();
				}
			}
		}
		else if ("a".equals(tagName))
		{
			String cssClass = parser.getAttr(attrs, "class");
			if ("thread_image_link".equals(cssClass))
			{
				ensureFile();
				String path = convertImageSrc(parser.getAttr(attrs, "href"));
				mAttachment.setFileUri(mLocator, mLocator.createAttachmentUri(path));
			}
			else if ("post_file_filename".equals(cssClass))
			{
				ensureFile();
				String originalName = parser.getAttr(attrs, "title");
				if (originalName != null) mAttachment.setOriginalName(StringUtils.clearHtml(originalName));
			}
			else if (mNeedResTo)
			{
				String function = parser.getAttr(attrs, "data-function");
				if ("quote".equals(function))
				{
					String href = parser.getAttr(attrs, "href");
					String threadNumber = mLocator.getThreadNumber(Uri.parse(href));
					mPost.setParentPostNumber(threadNumber);
				}
			}
		}
		else if ("img".equals(tagName))
		{
			String cssClass = parser.getAttr(attrs, "class");
			if (cssClass != null && (cssClass.contains("thread_image") || cssClass.contains("post_image")))
			{
				String src = convertImageSrc(parser.getAttr(attrs, "src"));
				mAttachment.setThumbnailUri(mLocator, mLocator.createAttachmentUri(src));
			}
		}
		return false;
	}

	@Override
	public void onEndElement(GroupParser parser, String tagName)
	{
		if ("div".equals(tagName))
		{
			if (mOriginalPostFileStart)
			{
				parser.reset();
			}
		}
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
			case EXPECT_FILE_SIZE:
			{
				Matcher matcher = FILE_SIZE.matcher(text);
				if (matcher.find())
				{
					int size = Integer.parseInt(matcher.group(1));
					String dim = matcher.group(2);
					if ("KiB".equals(dim)) size *= 1024;
					else if ("MiB".equals(dim)) size *= 1024 * 1024;
					int width = Integer.parseInt(matcher.group(3));
					int height = Integer.parseInt(matcher.group(4));
					mAttachment.setSize(size);
					mAttachment.setWidth(width);
					mAttachment.setHeight(height);
				}
				break;
			}
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
			case EXPECT_IDENTIFIER:
			{
				mPost.setIdentifier(StringUtils.clearHtml(text).trim().substring(3));
				break;
			}
			case EXPECT_COMMENT:
			{
				if (text != null) text = text.trim();
				mPost.setComment(text);
				mPosts.add(mPost);
				mPost = null;
				break;
			}
			case EXPECT_OMITTED_POSTS:
			{
				mThread.addPostsCount(Integer.parseInt(text));
				break;
			}
			case EXPECT_OMITTED_IMAGES:
			{
				mThread.addPostsWithFilesCount(Integer.parseInt(text));
				break;
			}
		}
		mExpect = EXPECT_NONE;
	}
}