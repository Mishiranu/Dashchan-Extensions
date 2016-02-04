package com.mishiranu.dashchan.chan.fourplebs;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.net.Uri;

import chan.content.ChanLocator;
import chan.content.ChanPerformer;
import chan.content.InvalidResponseException;
import chan.content.ThreadRedirectException;
import chan.content.model.Post;
import chan.http.HttpException;
import chan.http.HttpRequest;
import chan.text.ParseException;

public class FourplebsChanPerformer extends ChanPerformer
{
	@Override
	public ReadThreadsResult onReadThreads(ReadThreadsData data) throws HttpException, InvalidResponseException
	{
		FourplebsChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildPath(data.boardName, "page", Integer.toString(data.pageNumber + 1), "");
		String responseText = new HttpRequest(uri, data.holder, data).setValidator(data.validator).read().getString();
		try
		{
			return new ReadThreadsResult(new FourplebsPostsParser(responseText, this).convertThreads());
		}
		catch (ParseException e)
		{
			throw new InvalidResponseException(e);
		}
	}
	
	private static final Pattern PATTERN_REDIRECT = Pattern.compile("You are being redirected to .*?/thread/(\\d+)/#");
	
	@Override
	public ReadPostsResult onReadPosts(ReadPostsData data) throws HttpException, ThreadRedirectException,
			InvalidResponseException
	{
		FourplebsChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.createThreadUri(data.boardName, data.threadNumber);
		String responseText = new HttpRequest(uri, data.holder, data).setValidator(data.validator)
				.setSuccessOnly(false).read().getString();
		if (data.holder.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND)
		{
			uri = locator.buildPath(data.boardName, "post", data.threadNumber, "");
			responseText = new HttpRequest(uri, data.holder, data).read().getString();
			Matcher matcher = PATTERN_REDIRECT.matcher(responseText);
			if (matcher.find()) throw new ThreadRedirectException(matcher.group(1), data.threadNumber);
			throw HttpException.createNotFoundException();
		}
		else data.holder.checkResponseCode();
		try
		{
			Uri threadUri = locator.buildPathWithHost("boards.4chan.org", data.boardName, "thread", data.threadNumber);
			return new ReadPostsResult(new FourplebsPostsParser(responseText, this).convertPosts(threadUri));
		}
		catch (ParseException e)
		{
			throw new InvalidResponseException(e);
		}
	}
	
	@Override
	public ReadSearchPostsResult onReadSearchPosts(ReadSearchPostsData data) throws HttpException,
			InvalidResponseException
	{
		ArrayList<Post> posts = new ArrayList<>();
		FourplebsChanLocator locator = ChanLocator.get(this);
		for (int i = 0; i < 5; i++)
		{
			Uri uri = locator.buildArchivePath(data.boardName, "search", "text").buildUpon()
					.appendPath(data.searchQuery).appendEncodedPath("page/" + (i + 1) + "/").build();
			String responseText = new HttpRequest(uri, data.holder, data).read().getString();
			try
			{
				ArrayList<Post> result = new FourplebsPostsParser(responseText, this).convertSearch();
				if (result == null || result.isEmpty()) break;
				posts.addAll(result);
			}
			catch (ParseException e)
			{
				throw new InvalidResponseException(e);
			}
		}
		return new ReadSearchPostsResult(posts);
	}
	
	@Override
	public ReadBoardsResult onReadBoards(ReadBoardsData data) throws HttpException, InvalidResponseException
	{
		FourplebsChanLocator locator = ChanLocator.get(this);
		String responseText = new HttpRequest(locator.buildArchivePath(), data.holder, data).read().getString();
		try
		{
			return new ReadBoardsResult(new FourplebsBoardsParser(responseText).convert());
		}
		catch (ParseException e)
		{
			throw new InvalidResponseException(e);
		}
	}
}