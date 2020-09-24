package chan.content.model;

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
		throw new IllegalAccessError();
	}

	/**
	 * <p>Returns board name.</p>
	 *
	 * @return Board name.
	 */
	public String getBoardName() {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Returns thread number.</p>
	 *
	 * @return Thread number.
	 */
	public String getThreadNumber() {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Returns thread short description.</p>
	 *
	 * @return Thread description.
	 */
	public String getDescription() {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Returns posts count.</p>
	 *
	 * @return Posts count.
	 */
	public int getPostsCount() {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Stores posts count in this model.</p>
	 *
	 * @param postsCount Number of posts in the thread.
	 * @return This model.
	 */
	public ThreadSummary setPostsCount(int postsCount) {
		throw new IllegalAccessError();
	}
}
