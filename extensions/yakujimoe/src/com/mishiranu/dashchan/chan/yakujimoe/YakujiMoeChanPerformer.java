package com.mishiranu.dashchan.chan.yakujimoe;

import android.net.Uri;
import android.util.Pair;
import chan.content.ApiException;
import chan.content.InvalidResponseException;
import chan.content.WakabaChanPerformer;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.http.HttpException;
import chan.http.HttpRequest;
import chan.http.RequestEntity;
import chan.text.ParseException;
import java.util.List;

public class YakujiMoeChanPerformer extends WakabaChanPerformer {
	@Override
	protected List<Posts> parseThreads(String boardName, String responseText) throws ParseException {
		return new YakujiMoePostsParser(responseText, this, boardName).convertThreads();
	}

	@Override
	protected List<Post> parsePosts(String boardName, String responseText) throws ParseException {
		return new YakujiMoePostsParser(responseText, this, boardName).convertPosts();
	}

	@Override
	public ReadPostsResult onReadPosts(ReadPostsData data) throws HttpException, InvalidResponseException {
		YakujiMoeChanLocator locator = YakujiMoeChanLocator.get(this);
		Uri uri = locator.createThreadUri(data.boardName, data.threadNumber);
		List<Post> posts = readPosts(data, uri, null);
		if ("!dev".equals(data.boardName)) {
			Posts thread = new Posts(posts);
			thread.setArchivedThreadUri(uri.buildUpon().authority("iichan.hk").build());
			return new ReadPostsResult(thread);
		}
		return new ReadPostsResult(posts);
	}

	@Override
	public ReadBoardsResult onReadBoards(ReadBoardsData data) throws HttpException, InvalidResponseException {
		YakujiMoeChanLocator locator = YakujiMoeChanLocator.get(this);
		Uri uri = locator.buildPath("n", "list.html");
		String responseText = new HttpRequest(uri, data).read().getString();
		try {
			return new ReadBoardsResult(new YakujiMoeBoardsParser(responseText).convert());
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		}
	}

	@Override
	public SendPostResult onSendPost(SendPostData data) throws HttpException, ApiException, InvalidResponseException {
		RequestEntity entity = createSendPostEntity(data, field ->
				field.startsWith("field") ? "nya" + field.substring(5) : field);
		Pair<String, Uri> response = executeWakaba(data.boardName, entity, data.holder, data);
		if (response.first == null) {
			return null;
		}
		handleError(ErrorSource.POST, response.first);
		throw new InvalidResponseException();
	}
}
