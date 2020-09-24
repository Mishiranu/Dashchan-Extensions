package com.mishiranu.dashchan.chan.bunbunmaru;

import chan.content.WakabaChanPerformer;
import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.content.model.Post;
import chan.content.model.Posts;
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
	public ReadBoardsResult onReadBoards(ReadBoardsData data) {
		return new ReadBoardsResult(new BoardCategory(null, new Board[] {new Board("general", "Shameimaru General"),
				new Board("photos", "Mysterious Photograph Collection")}));
	}
}
