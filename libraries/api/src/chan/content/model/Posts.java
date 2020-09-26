package chan.content.model;

import android.net.Uri;
import chan.library.api.BuildConfig;
import java.util.Collection;

/**
 * <p>Model containing posts data.</p>
 *
 * <p>This class holds array of posts. Use default constructors to store an array.</p>
 *
 * <h3>Case 1: read posts response</h3>
 *
 * <p>This model contains all posts or only new ones if request is partial.</p>
 *
 * <p>In this case this model may contain unique posters count and original thread URI if your char is archive.
 * You can use the following methods to store this data:</p>
 *
 * <ul>
 * <li>{@link #setArchivedThreadUri(Uri)}</li>
 * <li>{@link #setUniquePosters(int)}</li>
 * </ul>
 *
 * <h3>Case 2: part of read threads response</h3>
 *
 * <p>The first post model is original post. The rest post models are last replies to original one.</p>
 *
 * <p>In this case this model may contain number of posts, files or posts with files. You can use the following
 * method to store this data:</p>
 *
 * <ul>
 * <li>{@link #addPostsCount(int)}</li>
 * <li>{@link #addFilesCount(int)}</li>
 * <li>{@link #addPostsWithFilesCount(int)}</li>
 * </ul>
 *
 * <p>Some chans provides files count in thread. Another chans provides count of posts with files in thread.
 * So, if your chan supports multiple files per post, you can use {@link #addFilesCount(int)} in first case
 * and {@link #addPostsWithFilesCount(int)} in the second case. If your chan supports only one
 * image per post, it's better to use {@link #addPostsWithFilesCount(int)} because it's more usual.</p>
 */
public final class Posts {
	/**
	 * <p>Returns array of post models this model holds.</p>
	 *
	 * @return Array of posts.
	 */
	public Post[] getPosts() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Stores posts in this model.</p>
	 *
	 * @param posts Array of {@link Post}.
	 * @return This model.
	 */
	public Posts setPosts(Post... posts) {
		return BuildConfig.Private.expr(posts);
	}

	/**
	 * <p>Stores posts in this model.</p>
	 *
	 * @param posts Collection of {@link Post}.
	 * @return This model.
	 */
	@SuppressWarnings("unchecked")
	public Posts setPosts(Collection<? extends Post> posts) {
		return BuildConfig.Private.expr(posts);
	}

	/**
	 * <p>Returns archived thread URI.</p>
	 *
	 * @return URI of archived thread.
	 */
	public Uri getArchivedThreadUri() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Stores original URI of archived thread in this model.</p>
	 *
	 * @param uri Original URI of archived thread.
	 * @return This model.
	 */
	public Posts setArchivedThreadUri(Uri uri) {
		return BuildConfig.Private.expr(uri);
	}

	/**
	 * <p>Returns unique posters count.</p>
	 *
	 * @return Unique posters count.
	 */
	public int getUniquePosters() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Stores unique posters count in this model.</p>
	 *
	 * @param uniquePosters Number of unique posters in thread.
	 * @return This model.
	 */
	public Posts setUniquePosters(int uniquePosters) {
		return BuildConfig.Private.expr(uniquePosters);
	}

	/**
	 * <p>Returns posts count in thread.</p>
	 *
	 * @return Posts count.
	 */
	public int getPostsCount() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Stores posts count in thread in this model.</p>
	 *
	 * @param postsCount Number of posts in thread including original post and last replies.
	 * @return This model.
	 */
	public Posts addPostsCount(int postsCount) {
		return BuildConfig.Private.expr(postsCount);
	}

	/**
	 * <p>Returns files count in thread.</p>
	 *
	 * @return Files count.
	 */
	public int getFilesCount() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Stores files count in thread in this model.</p>
	 *
	 * @param filesCount Number of files in thread including original post and last replies.
	 * @return This model.
	 */
	public Posts addFilesCount(int filesCount) {
		return BuildConfig.Private.expr(filesCount);
	}

	/**
	 * <p>Returns posts count with files.</p>
	 *
	 * @return Number of posts with files.
	 */
	public int getPostsWithFilesCount() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Stores posts count with files in this model.</p>
	 *
	 * @param postsWithFilesCount Number of posts with files in thread including original post and last replies.
	 * @return This model.
	 */
	public Posts addPostsWithFilesCount(int postsWithFilesCount) {
		return BuildConfig.Private.expr(postsWithFilesCount);
	}

	/**
	 * <p>Default constructor for {@link Posts}.</p>
	 */
	public Posts() {
		BuildConfig.Private.expr();
	}

	/**
	 * <p>Constructor for {@link Posts} with given {@code posts}.</p>
	 *
	 * @param posts Array of {@link Post}.
	 */
	public Posts(Post... posts) {
		BuildConfig.Private.expr(posts);
	}

	/**
	 * <p>Constructor for {@link Posts} with given {@code posts} that will be transformed to array.</p>
	 *
	 * @param posts Collection of {@link Post}.
	 */
	@SuppressWarnings("unchecked")
	public Posts(Collection<? extends Post> posts) {
		BuildConfig.Private.expr(posts);
	}
}
