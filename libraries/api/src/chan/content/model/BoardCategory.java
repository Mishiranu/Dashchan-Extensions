package chan.content.model;

import chan.library.api.BuildConfig;
import java.util.Collection;
import java.util.Iterator;

/**
 * <p>Model containing board category data: category title and array of {@link Board}.</p>
 */
public final class BoardCategory implements Iterable<Board> {
	/**
	 * <p>Returns board category title.</p>
	 *
	 * @return Title string.
	 */
	public String getTitle() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Return array of {@link Board} under this category.</p>
	 *
	 * @return Array of {@link Board}.
	 */
	public Board[] getBoards() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Constructor for {@link BoardCategory}.</p>
	 *
	 * @param title Board category title.
	 * @param boards Array of {@link Board}.
	 */
	public BoardCategory(String title, Board[] boards) {
		BuildConfig.Private.expr(title, boards);
	}

	/**
	 * <p>Constructor for {@link BoardCategory}. Collection will be transformed to array.</p>
	 *
	 * @param title Board category title.
	 * @param boards Collection of {@link Board}.
	 */
	public BoardCategory(String title, Collection<Board> boards) {
		BuildConfig.Private.expr(title, boards);
	}

	/**
	 * Returns an iterator over {@link Board} elements.
	 *
	 * @return An iterator.
	 */
	@Override
	public Iterator<Board> iterator() {
		return BuildConfig.Private.expr();
	}
}
