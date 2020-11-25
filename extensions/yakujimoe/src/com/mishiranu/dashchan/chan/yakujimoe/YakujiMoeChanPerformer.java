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
import chan.http.HttpResponse;
import chan.http.RequestEntity;
import chan.text.ParseException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class YakujiMoeChanPerformer extends WakabaChanPerformer {
	@Override
	protected List<Posts> parseThreads(String boardName, InputStream input) throws IOException, ParseException {
		return new YakujiMoePostsParser(this, boardName).convertThreads(input);
	}

	@Override
	protected List<Post> parsePosts(String boardName, InputStream input) throws IOException, ParseException {
		return new YakujiMoePostsParser(this, boardName).convertPosts(input);
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
		HttpResponse response = new HttpRequest(uri, data).perform();
		try (InputStream input = response.open()) {
			return new ReadBoardsResult(new YakujiMoeBoardsParser().convert(input));
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		} catch (IOException e) {
			throw response.fail(e);
		}
	}

	@Override
	public SendPostResult onSendPost(SendPostData data) throws HttpException, ApiException, InvalidResponseException {
		RequestEntity entity = createSendPostEntity(data, field ->
				field.startsWith("field") ? "nya" + field.substring(5) : field);
		Pair<HttpResponse, Uri> response = executeWakaba(data.boardName, entity, data);
		if (response.first == null) {
			return null;
		}
		handleError(ErrorSource.POST, response.first.readString());
		throw new InvalidResponseException();
	}
}
