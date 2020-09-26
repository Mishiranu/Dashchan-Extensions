package chan.text;

import chan.content.ChanMarkup;
import chan.library.api.BuildConfig;

/**
 * <p>This class is used to handle comment input when user writes new post.</p>
 */
public class CommentEditor {
	/**
	 * <p>Flag for tags that work only being in one line.</p>
	 */
	public static final int FLAG_ONE_LINE = BuildConfig.Private.expr();

	/**
	 * <p>Add a new tag to handle. {@code what} argument must be one of tag constants from {@link ChanMarkup}.</p>
	 *
	 * @param what Tag to handle.
	 * @param open Open tag string.
	 * @param close Close tag string.
	 */
	public final void addTag(int what, String open, String close) {
		BuildConfig.Private.expr(what, open, close);
	}

	/**
	 * <p>Add a new tag to handle. {@code what} argument must be one of tag constants from {@link ChanMarkup}.</p>
	 *
	 * @param what Tag to handle.
	 * @param open Open tag string.
	 * @param close Close tag string.
	 * @param flags Tag handling flags.
	 */
	public final void addTag(int what, String open, String close, int flags) {
		BuildConfig.Private.expr(what, open, close, flags);
	}

	/**
	 * <p>Sets a unordered list mark. This mark is using in the beginning of line for transformation to unordered list.
	 * By default it equals {@code "- "}.</p>
	 *
	 * @param mark Unordered list mark.
	 */
	public final void setUnorderedListMark(String mark) {
		BuildConfig.Private.expr(mark);
	}

	/**
	 * <p>Sets a ordered list mark. This mark is using in the beginning of line for transformation to ordered list.
	 * By default it equals {@code null}. That means all lists will begin with number and dot: "1. ", "2. ", etc.</p>
	 *
	 * @param mark Ordered list mark.
	 */
	public final void setOrderedListMark(String mark) {
		BuildConfig.Private.expr(mark);
	}

	/**
	 * <p>Implementation of {@link CommentEditor}.</p>
	 *
	 * <p>This editor has the following configuration:</p>
	 *
	 * <table summary="">
	 * <tr><th>What</th><th>Open</th><th>Close</th></tr>
	 * <tr><td>{@link ChanMarkup#TAG_BOLD}</td><td>{@code [b]}</td><td>{@code [/b]}</td></tr>
	 * <tr><td>{@link ChanMarkup#TAG_ITALIC}</td><td>{@code [i]}</td><td>{@code [/i]}</td></tr>
	 * <tr><td>{@link ChanMarkup#TAG_UNDERLINE}</td><td>{@code [u]}</td><td>{@code [/u]}</td></tr>
	 * <tr><td>{@link ChanMarkup#TAG_OVERLINE}</td><td>{@code [o]}</td><td>{@code [/o]}</td></tr>
	 * <tr><td>{@link ChanMarkup#TAG_STRIKE}</td><td>{@code [s]}</td><td>{@code [/s]}</td></tr>
	 * <tr><td>{@link ChanMarkup#TAG_SUBSCRIPT}</td><td>{@code [sub]}</td><td>{@code [/sub]}</td></tr>
	 * <tr><td>{@link ChanMarkup#TAG_SUPERSCRIPT}</td><td>{@code [sup]}</td><td>{@code [/sup]}</td></tr>
	 * <tr><td>{@link ChanMarkup#TAG_SPOILER}</td><td>{@code [spoiler]}</td><td>{@code [/spoiler]}</td></tr>
	 * <tr><td>{@link ChanMarkup#TAG_CODE}</td><td>{@code [code]}</td><td>{@code [/code]}</td></tr>
	 * <tr><td>{@link ChanMarkup#TAG_ASCII_ART}</td><td>{@code [aa]}</td><td>{@code [/aa]}</td></tr>
	 * </table>
	 */
	public static class BulletinBoardCodeCommentEditor extends CommentEditor {
		public BulletinBoardCodeCommentEditor() {
			BuildConfig.Private.expr();
		}
	}

	/**
	 * <p>Implementation of {@link CommentEditor}.</p>
	 *
	 * <p>This editor has the following configuration:</p>
	 *
	 * <table summary="">
	 * <tr><th>What</th><th>Open</th><th>Close</th><th>Flags</th></tr>
	 * <tr><td>{@link ChanMarkup#TAG_BOLD}</td><td>{@code **}</td><td>{@code **}</td><td>{@code one line}</td></tr>
	 * <tr><td>{@link ChanMarkup#TAG_ITALIC}</td><td>{@code *}</td><td>{@code *}</td><td>{@code one line}</td></tr>
	 * <tr><td>{@link ChanMarkup#TAG_SPOILER}</td><td>{@code %%}</td><td>{@code %%}</td><td>{@code one line}</td></tr>
	 * <tr><td>{@link ChanMarkup#TAG_CODE}</td><td>{@code `}</td><td>{@code `}</td><td>{@code one line}</td></tr>
	 * </table>
	 *
	 * <p>Also this editor can handle {@link ChanMarkup#TAG_STRIKE} with appending multiple {@code ^H}
	 * after end selection position.</p>
	 */
	public static class WakabaMarkCommentEditor extends CommentEditor {
		public WakabaMarkCommentEditor() {
			BuildConfig.Private.expr();
		}
	}
}
