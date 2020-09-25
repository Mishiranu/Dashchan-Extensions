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
import chan.http.MultipartEntity;
import chan.text.ParseException;
import java.util.List;

public class BunbunmaruChanPerformer extends WakabaChanPerformer {
	@Override
	protected List<Posts> parseThreads(String boardName, String responseText) throws ParseException {
		return new BunbunmaruPostsParser(responseText, this, boardName).convertThreads();
	}

	@Override
	protected List<Post> parsePosts(String boardName, String responseText) throws ParseException {
		return new BunbunmaruPostsParser(responseText, this, boardName).convertPosts();
	}

	@Override
	public ReadSearchPostsResult onReadSearchPosts(ReadSearchPostsData data) throws HttpException,
			InvalidResponseException {
		MultipartEntity entity = new MultipartEntity();
		entity.add("task", "search");
		entity.add("q", data.searchQuery);
		Pair<String, Uri> response = executeWakaba(data.boardName, entity, data.holder, data);
		if (response.first == null) {
			throw new InvalidResponseException();
		}
		try {
			return new ReadSearchPostsResult(parsePosts(data.boardName, response.first));
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		}
	}

	@Override
	public ReadBoardsResult onReadBoards(ReadBoardsData data) {
		return new ReadBoardsResult(new BoardCategory(null, new Board[] {new Board("general", "Shameimaru General"),
				new Board("photos", "Mysterious Photograph Collection")}));
	}
}
