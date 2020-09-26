package chan.content.model;

import chan.library.api.BuildConfig;

/**
 * <p>Model containing thread summary: board name, thread number and short description.
 * This model is used in archived threads page, for example.</p>
 */
public final class ThreadSummary {
	/**
	 * <p>Constructor for {@link ThreadSummary}.</p>
	 *
	 * @param boardName Board name.
	 * @param threadNumber Thread number.
	 * @param description Short description, may be the subject or some first sentences of the comment.
	 */
	public ThreadSummary(String boardName, String threadNumber, String description) {
		BuildConfig.Private.expr(boardName, threadNumber, description);
	}

	/**
	 * <p>Returns board name.</p>
	 *
	 * @return Board name.
	 */
	public String getBoardName() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Returns thread number.</p>
	 *
	 * @return Thread number.
	 */
	public String getThreadNumber() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Returns thread short description.</p>
	 *
	 * @return Thread description.
	 */
	public String getDescription() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Returns posts count.</p>
	 *
	 * @return Posts count.
	 */
	public int getPostsCount() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Stores posts count in this model.</p>
	 *
	 * @param postsCount Number of posts in the thread.
	 * @return This model.
	 */
	public ThreadSummary setPostsCount(int postsCount) {
		return BuildConfig.Private.expr(postsCount);
	}
}
