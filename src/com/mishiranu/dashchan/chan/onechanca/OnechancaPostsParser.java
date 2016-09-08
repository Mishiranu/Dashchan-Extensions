package com.mishiranu.dashchan.chan.onechanca;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;
import android.net.Uri;

import chan.content.model.FileAttachment;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.text.GroupParser;
import chan.text.ParseException;
import chan.util.StringUtils;

@SuppressLint("SimpleDateFormat")
public class OnechancaPostsParser implements GroupParser.Callback
{
	private final String mSource;
	private final OnechancaChanLocator mLocator;
	
	private String mParent;
	private Posts mThread;
	private Post mPost;
	private ArrayList<Posts> mThreads;
	private String mExternalLink;
	private final ArrayList<Post> mPosts = new ArrayList<>();
	
	private static final int EXPECT_NONE = 0;
	private static final int EXPECT_SUBJECT = 1;
	private static final int EXPECT_COMMENT = 2;
	private static final int EXPECT_OMITTED = 3;
	
	private int mExpect = EXPECT_NONE;
	private boolean mHeaderHandling = false;
	private boolean mReplyParsing = false;
	
	private static final SimpleDateFormat DATE_FORMAT;
	
	static
	{
		DateFormatSymbols symbols = new DateFormatSymbols();
		symbols.setMonths(new String[] {"Января", "Февраля", "Марта", "Апреля", "Мая", "Июня", "Июля", "Августа",
				"Сентября", "Октября", "Ноября", "Декабря"});
		DATE_FORMAT = new SimpleDateFormat("dd MMMM yyyy @ HH:mm", symbols);
		DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT+3"));
	}

	private static final Pattern PATTERN_ATTACHMENT = Pattern.compile("(?s)<a class=\"b-image-link\".*?" +
			"href=\"(.*?)\".*?title=\"(.*?)\".*?src=\"(.*?)\".*?</a>");
	private static final Pattern PATTERN_IMAGE = Pattern.compile("(?s)<a.*?<img.*?src=\"(.*?)\".*?/>.*?</a>");
	private static final Pattern PATTERN_TTS = Pattern.compile("(?s)<audio.*?src=\"(.*?tts.voicetech.yandex.net.*?)\"" +
			".*?</audio>");
	private static final Pattern PATTERN_FILE_SIZE = Pattern.compile("(\\d+)x(\\d+), ([\\d\\.]+) (\\w+)");
	private static final Pattern PATTERN_NUMBER = Pattern.compile("\\d+");
	
	public OnechancaPostsParser(String source, Object linked)
	{
		mSource = source.replaceAll("(?s)<textarea.*?</textarea>", "");
		mLocator = OnechancaChanLocator.get(linked);
	}
	
	private void closeThread()
	{
		if (mThread != null)
		{
			mThread.setPosts(mPosts);
			mThread.addPostsCount(1);
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
		if ("div".equals(tagName))
		{
			String id = parser.getAttr(attrs, "id");
			if (id != null && id.startsWith("post_") && !id.endsWith("_info") && !id.equals("post_notify"))
			{
				String number = id.substring(5);
				int index = number.indexOf('_');
				if (index >= 0) number = number.substring(index + 1);
				Post post = new Post();
				post.setPostNumber(number);
				mParent = number;
				mPost = post;
				mReplyParsing = false;
				if (mThreads != null)
				{
					closeThread();
					mThread = new Posts();
				}
			}
			else if (id != null && id.startsWith("comment_"))
			{
				String number = id.substring(8);
				int index = number.indexOf('_');
				if (index >= 0) number = number.substring(index + 1);
				Post post = new Post();
				post.setParentPostNumber(mParent);
				post.setPostNumber(number);
				mPost = post;
				mReplyParsing = true;
			}
			else if (mPost != null)
			{
				String cssClass = parser.getAttr(attrs, "class");
				if ("b-blog-entry_b-header".equals(cssClass))
				{
					mHeaderHandling = true;
				}
				else if (cssClass != null && (cssClass.contains("b-blog-entry_b-body")
						|| cssClass.contains("b-comment_b-body")))
				{
					mExpect = EXPECT_COMMENT;
					return true;
				}
			}
		}
		else if ("a".equals(tagName))
		{
			if (mHeaderHandling)
			{
				String cssClass = parser.getAttr(attrs, "class");
				if ("m-external".equals(cssClass))
				{
					mExternalLink = StringUtils.clearHtml(parser.getAttr(attrs, "href"));
				}
				if (!"b-blog-entry_b-header_m-category".equals(cssClass))
				{
					mExpect = EXPECT_SUBJECT;
					return true;
				}
			}
		}
		else if ("span".equals(tagName))
		{
			String cssClass = parser.getAttr(attrs, "class");
			if (cssClass != null && cssClass.contains("js-comments"))
			{
				mExpect = EXPECT_OMITTED;
				return true;
			}
		}
		return false;
	}
	
	@Override
	public void onEndElement(GroupParser parser, String tagName)
	{
		if ("div".equals(tagName))
		{
			mHeaderHandling = false;
			if (mPosts.size() == 1 && !mReplyParsing) mPost = null;
		}
	}
	
	@Override
	public void onText(GroupParser parser, String source, int start, int end)
	{
		if (mPost != null && source.indexOf('@', start) < end)
		{
			String text = source.substring(start, end).trim();
			String[] splitted = text.split(" ");
			if (splitted.length > 2 && splitted[2].equals("@"))
			{
				Calendar calendar = Calendar.getInstance();
				calendar.setTimeZone(DATE_FORMAT.getTimeZone());
				text = text.replace(" @", " " + calendar.get(Calendar.YEAR) + " @");
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
			case EXPECT_COMMENT:
			{
				text = text.trim();
				if (mExternalLink != null)
				{
					mExternalLink = mExternalLink.replace("\"", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
					text = "<p><a href=\"" + mExternalLink + "\">" + mExternalLink + "</a></p>" + text;
				}
				text = text.replaceAll("(?s)<blockquote>.*?<p>", "$0&gt; ");
				ArrayList<FileAttachment> attachments = new ArrayList<>();
				{
					Matcher matcher = PATTERN_ATTACHMENT.matcher(text);
					if (matcher.find())
					{
						text = text.replace(matcher.group(), "");
						String href = matcher.group(1);
						String title = matcher.group(2);
						String src = matcher.group(3);
						if (!title.startsWith("x"))
						{
							FileAttachment attachment = new FileAttachment();
							attachment.setFileUri(mLocator, Uri.parse(href));
							attachment.setThumbnailUri(mLocator, Uri.parse(src));
							matcher = PATTERN_FILE_SIZE.matcher(title);
							if (matcher.matches())
							{
								int width = Integer.parseInt(matcher.group(1));
								int height = Integer.parseInt(matcher.group(2));
								float size = Float.parseFloat(matcher.group(3));
								String dim = matcher.group(4);
								if ("KB".equals(dim)) size *= 1024;
								else if ("MB".equals(dim)) size *= 1024 * 1024;
								attachment.setWidth(width);
								attachment.setHeight(height);
								attachment.setSize((int) size);
							}
							attachments.add(attachment);
						}
					}
				}
				// Display smilies as text
				text = text.replaceAll("(?s)<img src=\".*?/img/(.*?).gif\".*?>", ":$1:");
				text = StringUtils.replaceAll(text, PATTERN_IMAGE, matcher ->
				{
					String uriString = matcher.group(1);
					Uri uri = Uri.parse(uriString);
					if ("i.imgur.com".equals(uri.getHost()))
					{
						FileAttachment attachment = new FileAttachment();
						attachment.setFileUri(mLocator, uri);
						attachment.setThumbnailUri(mLocator, uri.buildUpon()
								.path(uri.getPath().replace(".", "m.")).build());
						attachments.add(attachment);
					}
					return "<a href=\"" + uriString + "\">" + uriString + "</a>";
				});
				text = StringUtils.replaceAll(text, PATTERN_TTS, matcher ->
				{
					String uriString = matcher.group(1);
					Uri uri = Uri.parse(uriString);
					String ttsText = uri.getQueryParameter("text");
					return ttsText != null ? "#%" + ttsText.replaceAll("<", "&lt;") + "%#" : "";
				});
				text = text.replaceAll("<p><a href=\".*?\">Читать дальше</a></p>", "");
				mPost.setComment(text);
				mPost.setAttachments(attachments);
				mPosts.add(mPost);
				if (mReplyParsing) mPost = null;
				mExternalLink = null;
				break;
			}
			case EXPECT_OMITTED:
			{
				Matcher matcher = PATTERN_NUMBER.matcher(text);
				if (matcher.matches()) mThread.addPostsCount(Integer.parseInt(matcher.group()));
				break;
			}
		}
		mExpect = EXPECT_NONE;
	}
}