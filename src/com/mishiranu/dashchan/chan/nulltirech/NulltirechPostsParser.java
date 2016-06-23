package com.mishiranu.dashchan.chan.nulltirech;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;
import android.net.Uri;

import chan.content.ChanConfiguration;
import chan.content.ChanLocator;
import chan.content.model.EmbeddedAttachment;
import chan.content.model.FileAttachment;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.text.GroupParser;
import chan.text.ParseException;
import chan.util.StringUtils;

@SuppressLint("SimpleDateFormat")
public class NulltirechPostsParser implements GroupParser.Callback
{
	private final String mSource;
	private final NulltirechChanConfiguration mConfiguration;
	private final NulltirechChanLocator mLocator;
	private final String mBoardName;
	
	private String mParent;
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
	private static final int EXPECT_COMMENT = 5;
	private static final int EXPECT_OMITTED = 6;
	private static final int EXPECT_BOARD_TITLE = 7;
	private static final int EXPECT_PAGES_COUNT = 8;
	
	private int mExpect = EXPECT_NONE;
	private boolean mHeaderHandling = false;
	private boolean mParentFromRefLink = false;
	
	private boolean mHasPostBlock = false;
	private boolean mHasPostBlockName = false;
	
	private static final SimpleDateFormat DATE_FORMAT;
	
	static
	{
		DateFormatSymbols symbols = new DateFormatSymbols();
		symbols.setShortWeekdays(new String[] {"", "Вск", "Пнд", "Втр", "Срд", "Чт", "Птн", "Сбт"});
		symbols.setMonths(new String[] {"Января", "Февраля", "Марта", "Апреля", "Мая", "Июня", "Июля", "Августа",
				"Сентября", "Октября", "Ноября", "Декабря"});
		DATE_FORMAT = new SimpleDateFormat("EE dd MMMM yyyy в HH:mm:ss", symbols);
		DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT+3"));
	}
	
	private static final Pattern FILE_SIZE = Pattern.compile("\\(([\\d\\.]+)(\\w+)(?: *, *(\\d+)x(\\d+))?" +
			"(?: *, *(.+))? *\\) *$");
	private static final Pattern EMBED = Pattern.compile("data-id=\"(.*?)\" data-site=\"(.*?)\"");
	private static final Pattern NUMBER = Pattern.compile("(\\d+)");
	
	public NulltirechPostsParser(String source, Object linked, String boardName)
	{
		mSource = source;
		mConfiguration = ChanConfiguration.get(linked);
		mLocator = ChanLocator.get(linked);
		mBoardName = boardName;
	}
	
	public NulltirechPostsParser(String source, Object linked, String boardName, String parent)
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
	
	public Post convertSinglePost() throws ParseException
	{
		mParentFromRefLink = true;
		GroupParser.parse(mSource, this);
		return mPosts.size() > 0 ? mPosts.get(0) : null;
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
			if ("filesize".equals(cssClass))
			{
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
				mExpect = EXPECT_TRIPCODE;
				return true;
			}
			else if ("admin".equals(cssClass))
			{
				mPost.setCapcode("Admin");
				// Skip this block to parse date correctly
				mExpect = EXPECT_NONE;
				return true;
			}
			else if ("mod".equals(cssClass))
			{
				mPost.setCapcode("Mod");
				// Skip this block to parse date correctly
				mExpect = EXPECT_NONE;
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
			if (mParentFromRefLink && mPost != null && ("return highlight('" + mPost.getPostNumber() + "');")
					.equals(parser.getAttr(attrs, "onclick")))
			{
				String href = parser.getAttr(attrs, "href");
				if (href != null)
				{
					mPost.setParentPostNumber(ChanLocator.get(mConfiguration).getThreadNumber(Uri.parse(href)));
				}
			}
			else if (mAttachment != null)
			{
				String path = convertUriString(parser.getAttr(attrs, "href"));
				if (path != null) mAttachment.setFileUri(mLocator, mLocator.buildPath(path)); else mAttachment = null;
			}
		}
		else if ("img".equals(tagName))
		{
			String cssClass = parser.getAttr(attrs, "class");
			if ("thumb".equals(cssClass))
			{
				if (mAttachment != null)
				{
					String path = convertUriString(parser.getAttr(attrs, "src"));
					if (path != null) mAttachment.setThumbnailUri(mLocator, mLocator.buildPath(path));
					mPost.setAttachments(mAttachment);
					mAttachment = null;
				}
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
		else if ("table".equals(tagName))
		{
			String cssClass = parser.getAttr(attrs, "class");
			if ("postform".equals(cssClass))
			{
				mHasPostBlock = true;
			}
			else
			{
				String border = parser.getAttr(attrs, "border");
				if (mThreads != null && "1".equals(border))
				{
					mExpect = EXPECT_PAGES_COUNT;
					return true;
				}
			}
		}
		else if ("input".equals(tagName))
		{
			if (mHasPostBlock)
			{
				String name = parser.getAttr(attrs, "name");
				if ("name".equals(name)) mHasPostBlockName = true;
			}
		}
		return false;
	}
	
	@Override
	public void onEndElement(GroupParser parser, String tagName)
	{
		if ("label".equals(tagName))
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
				if (matcher.find())
				{
					float size = Float.parseFloat(matcher.group(1));
					String dim = matcher.group(2);
					if ("KB".equals(dim)) size *= 1024;
					else if ("MB".equals(dim)) size *= 1024 * 1024;
					mAttachment.setSize((int) size);
					if (matcher.group(3) != null)
					{
						mAttachment.setWidth(Integer.parseInt(matcher.group(3)));
						mAttachment.setHeight(Integer.parseInt(matcher.group(4)));
					}
					String fileName = matcher.group(5);
					mAttachment.setOriginalName(StringUtils.isEmptyOrWhitespace(fileName) ? null : fileName.trim());
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
			case EXPECT_COMMENT:
			{
				text = text.trim();
				if (text.startsWith("<span style=\"float: left;\">"))
				{
					int index = text.indexOf("</span>") + 7;
					String embed = text.substring(0, index);
					if (index + 6 <= text.length()) index += 6;
					text = text.substring(index).trim();
					Matcher matcher = EMBED.matcher(embed);
					if (matcher.find())
					{
						String uriString = null;
						String id = matcher.group(1);
						String site = matcher.group(2);
						if ("youtube".equals(site)) uriString = "https://www.youtube.com/watch?v=" + id;
						else if ("vimeo".equals(site)) uriString = "https://vimeo.com/" + id;
						else if ("coub".equals(site)) uriString = "https://coub.com/view/" + id;
						if (uriString != null)
						{
							EmbeddedAttachment attachment = EmbeddedAttachment.obtain(uriString);
							if (attachment == null)
							{
								if ("coub".equals(site))
								{
									attachment = new EmbeddedAttachment(Uri.parse(uriString), null, "COUB",
											EmbeddedAttachment.ContentType.VIDEO, false, null);
								}
							}
							mPost.setAttachments(attachment);
						}
					}
				}
				int index = text.lastIndexOf("<div class=\"abbrev\">");
				if (index >= 0) text = text.substring(0, index).trim();
				index = text.lastIndexOf("<font color=\"#FF0000\">");
				if (index >= 0)
				{
					String message = text.substring(index);
					text = text.substring(0, index);
					if (message.contains("USER WAS BANNED FOR THIS POST")) mPost.setPosterBanned(true);
				}
				text = removePrettyprintBreaks(text);
				mPost.setComment(text);
				mPosts.add(mPost);
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
					if (matcher.find()) mThread.addPostsWithFilesCount(Integer.parseInt(matcher.group(1)));
				}
				break;
			}
			case EXPECT_BOARD_TITLE:
			{
				text = StringUtils.clearHtml(text).trim();
				text = text.substring(5 + mBoardName.length()); // Skip "/boardname/ - "
				mConfiguration.storeBoardTitle(mBoardName, text);
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
	
	private String removePrettyprintBreaks(String string)
	{
		// brs inside pre.prettyprint has "display: none" style
		// Also br after pre will get "display: none" with javascript
		// Dashchan doesn't handle css styles and js, so hide these tags manually
		StringBuilder builder = new StringBuilder(string);
		int from = 0;
		while (true)
		{
			int index1 = builder.indexOf("<pre class=\"prettyprint\"", from);
			int index2 = builder.indexOf("</pre>", from);
			if (index2 > index1 && index1 >= 0)
			{
				while (true)
				{
					int brIndex = builder.indexOf("<br", index1 + 1);
					if (brIndex > index1)
					{
						int brEndIndex = builder.indexOf(">", brIndex) + 1;
						builder.delete(brIndex, brEndIndex);
						if (brIndex >= index2) break;
						index2 -= brEndIndex - brIndex;
					}
					else break;
				}
				from = index2 + 6;
				if (from >= builder.length()) break;
			}
			else break;
		}
		return builder.toString();
	}
}