package com.mishiranu.dashchan.chan.exach;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;
import android.net.Uri;

import chan.content.model.FileAttachment;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.text.ParseException;
import chan.text.TemplateParser;
import chan.util.StringUtils;

@SuppressLint("SimpleDateFormat")
public class ExachPostsParser
{
	private final String mSource;
	private final ExachChanConfiguration mConfiguration;
	private final ExachChanLocator mLocator;
	private final String mBoardName;

	private String mParent;
	private Post mPost;
	private FileAttachment mAttachment;
	private HashMap<String, Integer> mReplies;
	private final ArrayList<Post> mPosts = new ArrayList<>();

	private static final SimpleDateFormat DATE_FORMAT;

	static
	{
		DateFormatSymbols symbols = new DateFormatSymbols();
		symbols.setMonths(new String[] {"Января", "Февраля", "Марта", "Апреля", "Мая", "Июня", "Июля", "Августа",
				"Сентября", "Октября", "Ноября", "Декабря"});
		DATE_FORMAT = new SimpleDateFormat("dd MMMM, yyyy HH:mm", symbols);
		DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT+3"));
	}

	private static final Pattern PATTERN_FILE_SIZE = Pattern.compile("(?:(\\d+)(\\w+), )?(\\d+)px × (\\d+)px");
	private static final Pattern PATTERN_NUMBER = Pattern.compile("\\d+");

	public ExachPostsParser(String source, Object linked, String boardName)
	{
		mSource = source;
		mConfiguration = ExachChanConfiguration.get(linked);
		mLocator = ExachChanLocator.get(linked);
		mBoardName = boardName;
	}

	public ArrayList<Posts> convertThreads() throws ParseException
	{
		mReplies = new HashMap<>();
		PARSER.parse(mSource, this);
		fixPostsLinks();
		ArrayList<Posts> threads = new ArrayList<>(mPosts.size());
		for (Post post : mPosts)
		{
			Posts thread = new Posts(post);
			thread.addPostsCount(1);
			if (mReplies.containsKey(post.getPostNumber())) thread.addPostsCount(mReplies.get(post.getPostNumber()));
			threads.add(thread);
		}
		return threads;
	}

	public ArrayList<Post> convertPosts(String parent) throws ParseException
	{
		mParent = parent;
		PARSER.parse(mSource, this);
		fixPostsLinks();
		return mPosts;
	}

	public ArrayList<Post> convertSearch() throws ParseException
	{
		PARSER.parse(mSource, this);
		fixPostsLinks();
		return mPosts;
	}

	private void fixPostsLinks()
	{
		for (Post post : mPosts)
		{
			String originalPostNumber = post.getParentPostNumber();
			if (originalPostNumber == null) originalPostNumber = post.getPostNumber();
			post.setComment(post.getComment().replaceAll("<a href=\"(#c\\d+)\".*?>",
					"<a href=\"?id=" + originalPostNumber + "$1\">"));
		}
	}

	public static int extractPagesCount(String text)
	{
		String pagesCount = null;
		Matcher matcher = PATTERN_NUMBER.matcher(StringUtils.clearHtml(text));
		while (matcher.find()) pagesCount = matcher.group();
		return pagesCount != null ? Integer.parseInt(pagesCount) : -1;
	}

	private String fixCommentQuotes(String comment)
	{
		int index = 0;
		StringBuilder builder = null;
		while (true)
		{
			index = builder != null ? builder.indexOf("<span class=\"quote\">", index)
					: comment.indexOf("<span class=\"quote\">");
			if (index >= 0)
			{
				index += 20;
				if (builder == null) builder = new StringBuilder(comment);
				int end = builder.indexOf("</span>", index);
				if (end < 0) break;
				if (index >= 24 && !builder.subSequence(index - 24, index).equals("<br><span class=\"quote\">"))
				{
					builder.insert(index - 20, "<br>");
					index += 4;
					end += 4;
				}
				String replacement = builder.substring(index, end);
				replacement = replacement.replaceAll("^|(?<=<br>)", "&gt; ");
				builder.replace(index, end, replacement);
				index += replacement.length() + 7;
				if (builder.length() - 1 > index && builder.indexOf("<br>", index) != index)
				{
					builder.insert(index, "<br>");
					index += 4;
				}
			}
			else break;
		}
		if (builder != null) comment = builder.toString();
		return comment;
	}

	private static final TemplateParser<ExachPostsParser> PARSER = new TemplateParser<ExachPostsParser>()
			.starts("div", "id", "c").open((instance, holder, tagName, attributes) ->
	{
		String number = attributes.get("id").substring(1);
		holder.mPost = new Post();
		holder.mPost.setPostNumber(number);
		holder.mPost.setParentPostNumber(holder.mParent);
		return false;

	}).equals("a", "class", "text_image").open((instance, holder, tagName, attributes) ->
	{
		holder.mAttachment = new FileAttachment();
		holder.mAttachment.setFileUri(holder.mLocator, Uri.parse(attributes.get("href")));
		holder.mPost.setAttachments(holder.mAttachment);
		return false;

	}).starts("img", "onclick", "show_image").open((instance, holder, tagName, attributes) ->
	{
		holder.mAttachment.setThumbnailUri(holder.mLocator, Uri.parse(attributes.get("src")));
		String title = attributes.get("title");
		if (title != null)
		{
			title = StringUtils.clearHtml(title);
			Matcher matcher = PATTERN_FILE_SIZE.matcher(title);
			if (matcher.find())
			{
				String dim = matcher.group(2);
				if (dim != null)
				{
					int size = Integer.parseInt(matcher.group(1));
					if ("KB".equals(dim)) size *= 1024;
					else if ("MB".equals(dim)) size *= 1024f * 1024;
					holder.mAttachment.setSize(size);
				}
				holder.mAttachment.setWidth(Integer.parseInt(matcher.group(3)));
				holder.mAttachment.setHeight(Integer.parseInt(matcher.group(4)));
			}
		}
		return false;

	}).name("h2").open((i, h, t, a) -> h.mPost != null).content((instance, holder, text) ->
	{
		holder.mPost.setSubject(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));

	}).name("p").open((instance, holder, tagName, attributes) ->
	{
		if (attributes.get("class") == null && holder.mPost != null && holder.mPost.getComment() == null)
		{
			String style = attributes.get("style");
			if (style != null && style.contains("styles/op.png")) holder.mPost.setOriginalPoster(true);
			return true;
		}
		return false;

	}).content((instance, holder, text) ->
	{
		text = text.trim();
		text = text.replaceAll("<img src=\".*?/img/smiles/(\\d+)\\..*?\"(?: alt=\".*?\")?>", " [smile]$1[/smile] ");
		text = text.replaceAll(" ?<a href=\"post.php\\?id=.*?\">Далее</a>(?:&#8230;)?$", "");
		if (text.startsWith("<h2>Этот тред удален ОПом</h2>"))
		{
			text = text.substring(30);
			holder.mPost.setClosed(true);
		}
		text = holder.fixCommentQuotes(text);
		holder.mPost.setComment(text);
		holder.mPosts.add(holder.mPost);

	}).text((instance, holder, source, start, end) ->
	{
		if (holder.mPost != null && holder.mPost.getComment() != null)
		{
			String text = source.substring(start, end).trim();
			String endsWith = "->Комментарий \u2116" + holder.mPost.getPostNumber();
			if (text.endsWith(endsWith))
			{
				text = text.substring(0, text.length() - endsWith.length());
				holder.mPost.setName(StringUtils.nullIfEmpty(StringUtils.clearHtml(text).trim()));
			}
			else if (holder.mPost.getTimestamp() == 0L)
			{
				if (text.startsWith(")")) text = text.substring(1).trim();
				if (!text.isEmpty())
				{
					try
					{
						holder.mPost.setTimestamp(DATE_FORMAT.parse(text).getTime());
					}
					catch (java.text.ParseException e)
					{

					}
				}
			}
		}

	}).starts("a", "onmouseover", "preview_tread").open((instance, holder, tagName, attributes) ->
	{
		if (holder.mParent == null)
		{
			String onmouseover = attributes.get("onmouseover");
			Matcher matcher = PATTERN_NUMBER.matcher(onmouseover);
			if (matcher.find()) holder.mPost.setParentPostNumber(matcher.group());
		}
		return false;

	}).contains("a", "href", "post.php?id=").open((i, h, t, a) -> h.mReplies != null).content((i, holder, text) ->
	{
		if (text.startsWith("Ответов:"))
		{
			Matcher matcher = PATTERN_NUMBER.matcher(text);
			if (matcher.find()) holder.mReplies.put(holder.mPost.getPostNumber(), Integer.parseInt(matcher.group()));
		}

	}).equals("div", "class", "pages").open((i, h, t, a) -> h.mReplies != null).content((instance, holder, text) ->
	{
		int pagesCount = extractPagesCount(text);
		if (pagesCount >= 0) holder.mConfiguration.storePagesCount(holder.mBoardName, 0);

	}).prepare();
}