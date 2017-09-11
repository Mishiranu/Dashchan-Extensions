package com.mishiranu.dashchan.chan.awoo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import chan.content.model.Post;
import chan.content.model.Posts;
import chan.util.CommonUtils;
import chan.util.StringUtils;

public final class AwooModelMapper {
	public static Post createPost(JSONObject jsonObject) throws JSONException {
		Post post = new Post();
		post.setSticky(jsonObject.optBoolean("is_sticky", false));
		post.setClosed(jsonObject.optBoolean("locked", false));
		post.setPostNumber(String.valueOf(jsonObject.getInt("post_id")));
		if (jsonObject.has("parent")) {
			post.setParentPostNumber(String.valueOf(jsonObject.getInt("parent")));
		}
		post.setTimestamp(jsonObject.getLong("date_posted"));
		post.setTripcode(CommonUtils.optJsonString(jsonObject, "hash"));
		post.setCapcode(CommonUtils.optJsonString(jsonObject, "capcode"));
		String title = CommonUtils.optJsonString(jsonObject, "title");
		if (!StringUtils.isEmpty(title)) {
			post.setSubject(StringUtils.clearHtml(title).trim());
		}
		post.setComment(StringUtils.linkify(CommonUtils.optJsonString(jsonObject, "comment")));
		return post;
	}

	public static Posts createThreadFromReplies(JSONArray jsonObject) throws JSONException {
		Post[] posts = new Post[jsonObject.length()];
		int postsWithFilesCount = 0;
		int postsCount = jsonObject.getJSONObject(0).getInt("number_of_replies");
		for (int i = 0; i < posts.length; i++) {
			posts[i] = createPost(jsonObject.getJSONObject(i));
		}
		return new Posts(posts).addPostsCount(postsCount).addPostsWithFilesCount(postsWithFilesCount);
	}

	public static Posts createThreadFromCatalog(JSONObject jsonObject) throws JSONException {
		Post[] posts = new Post[1];
		int postsWithFilesCount = 0;
		int postsCount = jsonObject.getInt("number_of_replies");
		Post originalPost = createPost(jsonObject);
		posts[0] = originalPost;
		return new Posts(posts).addPostsCount(postsCount).addPostsWithFilesCount(postsWithFilesCount);
	}
}
