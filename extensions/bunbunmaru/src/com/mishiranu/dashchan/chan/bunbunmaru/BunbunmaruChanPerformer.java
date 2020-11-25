package com.mishiranu.dashchan.chan.bunbunmaru;

import android.net.Uri;
import android.util.Pair;
import chan.content.InvalidResponseException;
import chan.content.WakabaChanPerformer;
import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.http.HttpException;
import chan.http.HttpResponse;
import chan.http.MultipartEntity;
import chan.text.ParseException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class BunbunmaruChanPerformer extends WakabaChanPerformer {
	@Override
	protected List<Posts> parseThreads(String boardName, InputStream input) throws IOException, ParseException {
		return new BunbunmaruPostsParser(this, boardName).convertThreads(input);
	}

	@Override
	protected List<Post> parsePosts(String boardName, InputStream input) throws IOException, ParseException {
		return new BunbunmaruPostsParser(this, boardName).convertPosts(input);
	}

	@Override
	public ReadSearchPostsResult onReadSearchPosts(ReadSearchPostsData data) throws HttpException,
			InvalidResponseException {
		MultipartEntity entity = new MultipartEntity();
		entity.add("task", "search");
		entity.add("q", data.searchQuery);
		Pair<HttpResponse, Uri> response = executeWakaba(data.boardName, entity, data);
		if (response.first == null) {
			throw new InvalidResponseException();
		}
		try (InputStream input = response.first.open()) {
			return new ReadSearchPostsResult(parsePosts(data.boardName, input));
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		} catch (IOException e) {
			throw response.first.fail(e);
		}
	}

	@Override
	public ReadBoardsResult onReadBoards(ReadBoardsData data) {
		return new ReadBoardsResult(new BoardCategory(null, new Board[] {new Board("general", "Shameimaru General"),
				new Board("photos", "Mysterious Photograph Collection")}));
	}
}
