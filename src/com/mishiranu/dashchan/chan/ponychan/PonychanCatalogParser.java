package com.mishiranu.dashchan.chan.ponychan;

import java.util.ArrayList;
import java.util.regex.Matcher;

import android.net.Uri;

import chan.content.ChanLocator;
import chan.content.model.FileAttachment;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.text.GroupParser;
import chan.text.ParseException;
import chan.util.StringUtils;

public class PonychanCatalogParser implements GroupParser.Callback
{
	private final String mSource;
	private final PonychanChanLocator mLocator;
	
	private Post mPost;
	private final ArrayList<Posts> mThreads = new ArrayList<>();
	
	private static final int EXPECT_NONE = 0;
	private static final int EXPECT_COMMENT = 1;
	private static final int EXPECT_OMITTED = 2;
	
	private int mExpect = EXPECT_NONE;
	
	public PonychanCatalogParser(String source, Object linked)
	{
		mSource = source;
		mLocator = ChanLocator.get(linked);
	}
	
	public ArrayList<Posts> convert() throws ParseException
	{
		GroupParser.parse(mSource, this);
		return mThreads;
	}
	
	@Override
	public boolean onStartElement(GroupParser parser, String tagName, String attrs) throws ParseException
	{
		if ("a".equals(tagName))
		{
			String cssClass = parser.getAttr(attrs, "class");
			if ("catalink".equals(cssClass))
			{
				String href = parser.getAttr(attrs, "href");
				String number = href.substring(href.lastIndexOf('#') + 1);
				Post post = new Post();
				post.setPostNumber(number);
				mPost = post;
				mThreads.add(new Posts(post));
			}
		}
		else if ("img".equals(tagName))
		{
			String cssClass = parser.getAttr(attrs, "class");
			if ("catalogthumb".equals(cssClass))
			{
				String src = parser.getAttr(attrs, "src");
				if ("/static/spoiler.png".equals(src)) src = parser.getAttr(attrs, "data-mature-src");
				if ("/static/deleted.png".equals(src)) src = null;
				if (src != null)
				{
					Uri fileUri = Uri.parse(src.replace("/thumb/", "/src/"));
					Uri thumbnailUri = Uri.parse(src);
					if (fileUri.isRelative())
					{
						fileUri = mLocator.buildSpecificPath(fileUri.getPath());
						thumbnailUri = mLocator.buildSpecificPath(thumbnailUri.getPath());
					}
					FileAttachment attachment = new FileAttachment();
					attachment.setFileUri(mLocator, fileUri);
					attachment.setThumbnailUri(mLocator, thumbnailUri);
					mPost.setAttachments(attachment);
					Matcher matcher = PonychanPostsParser.NUMBER.matcher(src);
					if (matcher.find())
					{
						String timestamp = matcher.group(1);
						if (timestamp.length() >= 12) mPost.setTimestamp(Long.parseLong(timestamp));
					}
				}
			}
		}
		else if ("div".equals(tagName))
		{
			String cssClass = parser.getAttr(attrs, "class");
			if ("catacount".equals(cssClass))
			{
				mExpect = EXPECT_OMITTED;
				return true;
			}
			else if ("catapreview".equals(cssClass))
			{
				mExpect = EXPECT_COMMENT;
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
			case EXPECT_COMMENT:
			{
				if (text.startsWith("<span class=\"catasubject\">"))
				{
					int index = text.indexOf("</span>");
					mPost.setSubject(StringUtils.nullIfEmpty(StringUtils.clearHtml(text.substring(26, index)).trim()));
					text = text.substring(index + 7).trim();
				}
				mPost.setComment(text);
				mPost = null;
				break;
			}
			case EXPECT_OMITTED:
			{
				text = StringUtils.clearHtml(text);
				Matcher matcher = PonychanPostsParser.NUMBER.matcher(text);
				if (matcher.find()) mThreads.get(mThreads.size() - 1).addPostsCount(Integer.parseInt(matcher.group(1)));
				break;
			}
		}
		mExpect = EXPECT_NONE;
	}
}