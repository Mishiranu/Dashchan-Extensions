package com.mishiranu.dashchan.chan.desustorage;

import android.net.Uri;
import chan.content.ChanLocator;
import chan.content.ChanPerformer;
import chan.content.InvalidResponseException;
import chan.content.RedirectException;
import chan.http.HttpException;
import chan.http.HttpRequest;
import chan.text.ParseException;
import java.net.HttpURLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DesustorageChanPerformer extends ChanPerformer {
	@Override
	public ReadThreadsResult onReadThreads(ReadThreadsData data) throws HttpException, InvalidResponseException {
		DesustorageChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildPath(data.boardName, "page", Integer.toString(data.pageNumber + 1), "");
		String responseText = new HttpRequest(uri, data.holder, data).setValidator(data.validator).read().getString();
		try {
			return new ReadThreadsResult(new DesustoragePostsParser(responseText, this).convertThreads());
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		}
	}

	private static final Pattern PATTERN_REDIRECT = Pattern.compile("You are being redirected to .*?/thread/(\\d+)/#");

	@Override
	public ReadPostsResult onReadPosts(ReadPostsData data) throws HttpException, InvalidResponseException,
			RedirectException {
		DesustorageChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.createThreadUri(data.boardName, data.threadNumber);
		String responseText = new HttpRequest(uri, data.holder, data).setValidator(data.validator)
				.setSuccessOnly(false).read().getString();
		if (data.holder.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
			uri = locator.buildPath(data.boardName, "post", data.threadNumber, "");
			responseText = new HttpRequest(uri, data.holder, data).read().getString();
			Matcher matcher = PATTERN_REDIRECT.matcher(responseText);
			if (matcher.find()) {
				throw RedirectException.toThread(data.boardName, matcher.group(1), data.threadNumber);
			}
			throw HttpException.createNotFoundException();
		} else {
			data.holder.checkResponseCode();
		}
		try {
			Uri threadUri = locator.buildPathWithHost("boards.4chan.org", data.boardName, "thread", data.threadNumber);
			return new ReadPostsResult(new DesustoragePostsParser(responseText, this).convertPosts(threadUri));
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		}
	}

	@Override
	public ReadSearchPostsResult onReadSearchPosts(ReadSearchPostsData data) throws HttpException,
			InvalidResponseException {
		DesustorageChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildPath(data.boardName, "search", "text").buildUpon().appendPath(data.searchQuery)
				.appendEncodedPath("page/" + (data.pageNumber + 1) + "/").build();
		String responseText = new HttpRequest(uri, data.holder, data).read().getString();
		try {
			return new ReadSearchPostsResult(new DesustoragePostsParser(responseText, this).convertSearch());
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		}
	}

	@Override
	public ReadBoardsResult onReadBoards(ReadBoardsData data) throws HttpException, InvalidResponseException {
		DesustorageChanLocator locator = ChanLocator.get(this);
		String responseText = new HttpRequest(locator.buildPath(), data.holder, data).read().getString();
		try {
			return new ReadBoardsResult(new DesustorageBoardsParser(responseText).convert());
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		}
	}
}
