package com.mishiranu.dashchan.chan.diochan;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.net.Uri;

import chan.content.ChanConfiguration;
import chan.content.ChanLocator;
import chan.content.model.EmbeddedAttachment;
import chan.content.model.FileAttachment;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.text.GroupParser;
import chan.text.ParseException;
import chan.util.CommonUtils;
import chan.util.StringUtils;

public class DiochanPostsParser implements GroupParser.Callback
{
	private final String mSource;
	private final DiochanChanConfiguration mConfiguration;
	private final DiochanChanLocator mLocator;
	private final String mBoardName;
	
	private String mParent;
	private Posts mThread;
	private Post mPost;
	private ArrayList<Posts> mThreads;
	private final ArrayList<Post> mPosts = new ArrayList<>();
	private final ArrayList<FileAttachment> mAttachments = new ArrayList<FileAttachment>();
	
	private static final int EXPECT_NONE = 0;
	private static final int EXPECT_FILE_SIZE = 1;
	private static final int EXPECT_SUBJECT = 2;
	private static final int EXPECT_NAME = 3;
	private static final int EXPECT_TRIPCODE = 4;
	private static final int EXPECT_MULTIPLE_FILE_NAME = 5;
	private static final int EXPECT_COMMENT = 6;
	private static final int EXPECT_OMITTED = 7;
	private static final int EXPECT_BOARD_TITLE = 8;
	private static final int EXPECT_PAGES_COUNT = 9;
	
	private int mExpect = EXPECT_NONE;
	private boolean mHeaderHandling = false;
	private boolean mParentFromRefLink = false;
	
	private boolean mHasPostBlock = false;
	private boolean mHasPostBlockName = false;
	private boolean mHasSpoilerCheckBox = false;
	
	private static final SimpleDateFormat[] DATE_FORMATS;
	
	static
	{
		TimeZone timeZone = TimeZone.getTimeZone("Europe/Rome");
		DATE_FORMATS = new SimpleDateFormat[3];
		DATE_FORMATS[0] = new SimpleDateFormat("yy/dd/MM(EEE)hh:mm", Locale.ITALY);
		DATE_FORMATS[0].setTimeZone(timeZone);
		DATE_FORMATS[1] = new SimpleDateFormat("(EEE) dd/MM/yyyy hh:mm", Locale.ITALY);
		DATE_FORMATS[1].setTimeZone(timeZone);
		DATE_FORMATS[2] = new SimpleDateFormat("yy/dd/MM(EEE)hh:mm", Locale.US);
		DATE_FORMATS[2].setTimeZone(timeZone);
	}
	
	private static final Pattern FILE_SIZE = Pattern.compile("\\(([\\d\\.]+) ?(\\w+)(?: *, *(\\d+)x(\\d+))?" +
			"(?: *, *(.+))? *\\) *$");
	private static final Pattern FILE_SIZE_MULTIPLE = Pattern.compile("\\( *(\\d+)x(\\d+) *\\) *([\\d\\.]+) (\\w+)");
	private static final Pattern NAME_EMAIL = Pattern.compile("<a href=\"(.*?)\">(.*)</a>");
	private static final Pattern NUMBER = Pattern.compile("(\\d+)");
	
	public DiochanPostsParser(String source, Object linked, String boardName)
	{
		mSource = source;
		mConfiguration = ChanConfiguration.get(linked);
		mLocator = ChanLocator.get(linked);
		mBoardName = boardName;
	}
	
	public DiochanPostsParser(String source, Object linked, String boardName, String parent)
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
		if (mHasPostBlock) mConfiguration.storeNamesSpoilersEnabled(mBoardName, mHasPostBlockName, mHasSpoilerCheckBox);
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
				if ("file_reply".equals(cssClass))
				{
					mAttachments.add(new FileAttachment());
				}
				else if ("logo".equals(cssClass))
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
				mAttachments.add(new FileAttachment());
				mExpect = EXPECT_FILE_SIZE;
				return true;
			}
			else if ("fileinfo".equals(cssClass))
			{
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
			else if (mAttachments.size() > 0)
			{
				String path = convertUriString(parser.getAttr(attrs, "href"));
				if (path != null && path.contains("/src/"))
				{
					mAttachments.get(mAttachments.size() - 1).setFileUri(mLocator, mLocator.buildPath(path));
					String onclick = parser.getAttr(attrs, "onclick");
					if (onclick != null && onclick.startsWith("javascript:espandipic"))
					{
						mExpect = EXPECT_MULTIPLE_FILE_NAME;
						return true;
					}
				}
			}
		}
		else if ("img".equals(tagName))
		{
			if (mPost != null)
			{
				String src = parser.getAttr(attrs, "src");
				if (src != null)
				{
					if (src.endsWith("/css/sticky.gif")) mPost.setSticky(true);
					else if (src.endsWith("/css/locked.gif")) mPost.setClosed(true);
					else if (mAttachments.size() > 0 && src.contains("/thumb/"))
					{
						String path = convertUriString(parser.getAttr(attrs, "src"));
						if (path != null)
						{
							FileAttachment attachment = mAttachments.get(mAttachments.size() - 1);
							String width = parser.getAttr(attrs, "width");
							String height = parser.getAttr(attrs, "height");
							String originalName = attachment.getOriginalName();
							if ("100".equals(width) && "100".equals(height) && originalName != null &&
									originalName.startsWith("Immagine Spoi"))
							{
								attachment.setSpoiler(true);
								attachment.setOriginalName(null);
							}
							else attachment.setThumbnailUri(mLocator, mLocator.buildPath(path));
						}
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
				else if ("spoiler".equals(name)) mHasSpoilerCheckBox = true;
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
				for (SimpleDateFormat dateFormat : DATE_FORMATS)
				{
					try
					{
						mPost.setTimestamp(dateFormat.parse(text).getTime());
						break;
					}
					catch (java.text.ParseException e)
					{
						
					}
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
				FileAttachment attachment = mAttachments.get(mAttachments.size() - 1);
				text = StringUtils.clearHtml(text);
				Matcher matcher = FILE_SIZE.matcher(text);
				if (matcher.find())
				{
					float size = Float.parseFloat(matcher.group(1));
					String dim = matcher.group(2);
					if ("KB".equals(dim)) size *= 1024;
					else if ("MB".equals(dim)) size *= 1024 * 1024;
					attachment.setSize((int) size);
					if (matcher.group(3) != null)
					{
						attachment.setWidth(Integer.parseInt(matcher.group(3)));
						attachment.setHeight(Integer.parseInt(matcher.group(4)));
					}
					String fileName = matcher.group(5);
					attachment.setOriginalName(StringUtils.isEmptyOrWhitespace(fileName) ? null : fileName.trim());
				}
				else
				{
					matcher = FILE_SIZE_MULTIPLE.matcher(text);
					if (matcher.find())
					{
						attachment.setWidth(Integer.parseInt(matcher.group(1)));
						attachment.setHeight(Integer.parseInt(matcher.group(2)));
						float size = Float.parseFloat(matcher.group(3));
						String dim = matcher.group(4);
						if ("KB".equals(dim)) size *= 1024;
						else if ("MB".equals(dim)) size *= 1024 * 1024;
						attachment.setSize((int) size);
					}
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
				text = text.trim();
				Matcher matcher = NAME_EMAIL.matcher(text);
				if (matcher.matches())
				{
					String email = matcher.group(1);
					if (email.startsWith("/cdn-cgi/l/email-protection#"))
					{
						String string = "<a href=\"" + email + "\"></a>";
						string = CommonUtils.restoreCloudFlareProtectedEmails(string);
						email = string.substring(9, string.length() - 6);
					}
					if (email.toLowerCase(Locale.US).equals("mailto:sage") ||
							email.toLowerCase(Locale.US).equals("mailto:salvia"))
					{
						mPost.setSage(true);
					}
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
			case EXPECT_MULTIPLE_FILE_NAME:
			{
				String fileName = StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim());
				mAttachments.get(mAttachments.size() - 1).setOriginalName(fileName);
				break;
			}
			case EXPECT_COMMENT:
			{
				text = text.trim();
				if (text.startsWith("<span style=\"float: left;"))
				{
					int index = text.indexOf("</span>") + 7;
					String embed = text.substring(0, index);
					text = text.substring(index).trim();
					index = embed.indexOf("youtube-player video");
					if (index >= 0)
					{
						embed = "https://www.youtube.com/watch?v=" + embed.substring(index + 20, index + 31);
					}
					else
					{
						index = embed.indexOf("videohttps://soundcloud.com");
						if (index >= 0) embed = embed.substring(index + 5, embed.indexOf('"', index));
					}
					EmbeddedAttachment attachment = EmbeddedAttachment.obtain(embed);
					if (attachment != null) mPost.setAttachments(attachment);
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
				text = CommonUtils.restoreCloudFlareProtectedEmails(text);
				mPost.setComment(text);
				mPosts.add(mPost);
				if (mAttachments.size() > 0)
				{
					for (int i = mAttachments.size() - 1; i >= 0; i--)
					{
						if (mAttachments.get(i).getFileUri(mLocator) == null) mAttachments.remove(i);
					}
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
					if (matcher.find()) mThread.addPostsWithFilesCount(Integer.parseInt(matcher.group(1)));
				}
				break;
			}
			case EXPECT_BOARD_TITLE:
			{
				text = StringUtils.clearHtml(text).trim();
				int index = text.indexOf("- ");
				if (index >= 0) text = text.substring(index + 2);
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
}