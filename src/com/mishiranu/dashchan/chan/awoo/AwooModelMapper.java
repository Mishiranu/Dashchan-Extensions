package com.mishiranu.dashchan.chan.awoo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Pattern;

import chan.content.model.Post;
import chan.content.model.Posts;
import chan.util.CommonUtils;
import chan.util.StringUtils;

public final class AwooModelMapper {
	private static final Pattern PATTERN_LINK = Pattern.compile(">>(\\d+)");
	private static final Pattern PATTERN_QUOTE = Pattern.compile("(?<=^|<br />)>(.*?)(?=$|<br />)");

	private static Post createPost(JSONObject jsonObject, String boardName) throws JSONException {
		Post post = new Post();
		if (boardName == null) {
			boardName = CommonUtils.getJsonString(jsonObject, "board");
		}
		String parent = String.valueOf(jsonObject.getInt("post_id"));
		post.setSticky(jsonObject.optBoolean("sticky", false));
		post.setClosed(jsonObject.optBoolean("is_locked", false));
		post.setPostNumber(parent);
		if (jsonObject.has("parent")) {
			parent = String.valueOf(jsonObject.getInt("parent"));
			post.setParentPostNumber(parent);
		}
		post.setTimestamp(jsonObject.getLong("date_posted") * 1000L);
		post.setTripcode(CommonUtils.optJsonString(jsonObject, "hash"));
		post.setCapcode(CommonUtils.optJsonString(jsonObject, "capcode"));
		String title = CommonUtils.optJsonString(jsonObject, "title");
		if (!StringUtils.isEmpty(title)) {
			post.setSubject(StringUtils.clearHtml(title).trim());
		}
		final String finalParent = parent;
		final String finalBoardName = boardName;
		String comment = StringUtils.emptyIfNull(CommonUtils.optJsonString(jsonObject, "comment"));
		comment = comment.replace("\n\r", "<br />").replace("\n", "<br />");
		comment = StringUtils.replaceAll(comment, PATTERN_LINK, matcher ->
				"<a href=\"/" + finalBoardName + "/thread/" + finalParent + "#" + matcher.group(1) +"\">&gt;&gt;"  +
				matcher.group(1) + "</a>");
		comment = StringUtils.replaceAll(comment, PATTERN_QUOTE, matcher ->
				"<span class=\"redtext\">&gt;" + matcher.group(1) + "</span>");
		comment = StringUtils.linkify(comment);
		post.setComment(comment);
		return post;
	}

	public static Posts createThreadFromReplies(JSONArray jsonObject) throws JSONException {
		Post[] posts = new Post[jsonObject.length()];
		int postsCount = jsonObject.getJSONObject(0).getInt("number_of_replies");
		String boardName = CommonUtils.getJsonString(jsonObject.getJSONObject(0), "board");
		for (int i = 0; i < posts.length; i++) {
			posts[i] = createPost(jsonObject.getJSONObject(i), boardName);
		}
		return new Posts(posts).addPostsCount(postsCount);
	}

	public static Posts createThread(JSONObject jsonObject) throws JSONException {
		Post[] posts = new Post[1];
		int postsCount = jsonObject.getInt("number_of_replies");
		Post originalPost = createPost(jsonObject, null);
		posts[0] = originalPost;
		return new Posts(posts).addPostsCount(postsCount);
	}
}
