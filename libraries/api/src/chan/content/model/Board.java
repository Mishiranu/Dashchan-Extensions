package chan.content.model;

import chan.library.api.BuildConfig;

/**
 * <p>Model containing board data: board name, title and description.</p>
 */
public final class Board implements Comparable<Board> {
	/**
	 * <p>Returns name of this board. For example {@code b}.</p>
	 *
	 * @return Board name.
	 */
	public String getBoardName() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Returns title of this board. For example {@code Random}.</p>
	 *
	 * @return Board title.
	 */
	public String getTitle() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Returns description of this board.</p>
	 *
	 * @return Board description.
	 */
	public String getDescription() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Constructor for {@link Board}.</p>
	 *
	 * @param boardName Board name.
	 * @param title Board title.
	 */
	public Board(String boardName, String title) {
		BuildConfig.Private.expr(boardName, title);
	}

	/**
	 * <p>Constructor for {@link Board}.</p>
	 *
	 * @param boardName Board name.
	 * @param title Board title.
	 * @param description Board description.
	 */
	public Board(String boardName, String title, String description) {
		BuildConfig.Private.expr(boardName, title, description);
	}

	@SuppressWarnings("NullableProblems")
	@Override
	public int compareTo(Board another) {
		return BuildConfig.Private.expr(another);
	}
}
