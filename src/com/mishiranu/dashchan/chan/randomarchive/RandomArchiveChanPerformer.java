package com.mishiranu.dashchan.chan.randomarchive;

import android.net.Uri;

import chan.content.ChanPerformer;
import chan.content.InvalidResponseException;
import chan.http.HttpException;
import chan.http.HttpRequest;
import chan.text.ParseException;

public class RandomArchiveChanPerformer extends ChanPerformer {
	@Override
	public ReadThreadsResult onReadThreads(ReadThreadsData data) throws HttpException, InvalidResponseException {
		RandomArchiveChanLocator locator = RandomArchiveChanLocator.get(this);
		Uri uri = locator.createBoardUri(data.boardName, data.pageNumber);
		String responseText = new HttpRequest(uri, data.holder, data).setValidator(data.validator).read().getString();
		try {
			return new ReadThreadsResult(new RandomArchivePostsParser(responseText, this, data.boardName)
					.convertThreads());
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		}
	}

	@Override
	public ReadPostsResult onReadPosts(ReadPostsData data) throws HttpException, InvalidResponseException {
		RandomArchiveChanLocator locator = RandomArchiveChanLocator.get(this);
		Uri uri = locator.createThreadUri(data.boardName, data.threadNumber);
		String responseText = new HttpRequest(uri, data.holder, data).setValidator(data.validator).read().getString();
		try {
			Uri threadUri = locator.buildPathWithHost("boards.4chan.org", data.boardName, "thread", data.threadNumber);
			return new ReadPostsResult(new RandomArchivePostsParser(responseText, this, data.boardName)
					.convertPosts(threadUri));
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		}
	}
}