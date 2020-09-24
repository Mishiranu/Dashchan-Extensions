package chan.content;

import android.util.Pair;
import chan.text.CommentEditor;

/**
 * <p>Provides HTML posts handling and post editing.</p>
 *
 * <p>If your chan supports posting, you must implement {@link ChanMarkup#obtainCommentEditor(String)}.</p>
 *
 * <p>You can configure post markup handling using these methods:<p>
 *
 * <ul>
 * <li>{@link ChanMarkup#addTag(String, int)}</li>
 * <li>{@link ChanMarkup#addTag(String, String, int)}</li>
 * <li>{@link ChanMarkup#addTag(String, String, String, int)}</li>
 * <li>{@link ChanMarkup#addBlock(String, boolean, boolean)}</li>
 * <li>{@link ChanMarkup#addBlock(String, String, boolean, boolean)}</li>
 * <li>{@link ChanMarkup#addBlock(String, String, String, boolean, boolean)}</li>
 * <li>{@link ChanMarkup#addPreformatted(String, boolean)}</li>
 * <li>{@link ChanMarkup#addPreformatted(String, String, boolean)}</li>
 * <li>{@link ChanMarkup#addPreformatted(String, String, String, boolean)}</li>
 * <li>{@link ChanMarkup#addColorable(String)}</li>
 * <li>{@link ChanMarkup#addColorable(String, String)}</li>
 * <li>{@link ChanMarkup#addColorable(String, String, String)}</li>
 * </ul>
 */
public abstract class ChanMarkup {
	/**
	 * <p>Return linked {@link ChanMarkup} instance.
	 *
	 * @param object Linked object: {@link ChanConfiguration}, {@link ChanPerformer},
	 * {@link ChanLocator} or {@link ChanMarkup}.
	 * @return {@link ChanMarkup} instance.
	 */
	public static <T extends ChanMarkup> T get(Object object) {
		throw new IllegalAccessError();
	}

	/**
	 * Bold tag constant value.
	 */
	public static final int TAG_BOLD;

	/**
	 * Italic tag constant value.
	 */
	public static final int TAG_ITALIC;

	/**
	 * Underline tag constant value.
	 */
	public static final int TAG_UNDERLINE;

	/**
	 * Overline tag constant value.
	 */
	public static final int TAG_OVERLINE;

	/**
	 * Strikethrough tag constant value.
	 */
	public static final int TAG_STRIKE;

	/**
	 * Subscript tag constant value.
	 */
	public static final int TAG_SUBSCRIPT;

	/**
	 * Superscript tag constant value.
	 */
	public static final int TAG_SUPERSCRIPT;

	/**
	 * Spoiler tag constant value.
	 */
	public static final int TAG_SPOILER;

	/**
	 * Quote tag constant value.
	 */
	public static final int TAG_QUOTE;

	/**
	 * Code tag constant value.
	 */
	public static final int TAG_CODE;

	/**
	 * Ascii art tag constant value.
	 */
	public static final int TAG_ASCII_ART;

	/**
	 * Code tag constant value.
	 */
	public static final int TAG_HEADING;

	static {
		// noinspection ConstantIfStatement,ConstantConditions
		if (true) {
			throw new IllegalAccessError();
		}
	}

	/**
	 * <p>Calls when client want to show posting activity.</p>
	 *
	 * @param boardName Board name string.
	 * @return {@link CommentEditor} instance.
	 */
	public CommentEditor obtainCommentEditor(String boardName) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Calls when client want to determine tag's supportability. This method must return whether board support
	 * given tag.</p>
	 *
	 * @param boardName Board name to check.
	 * @param tag Tag to check.
	 * @return True if tag is supported, false otherwise.
	 */
	public boolean isTagSupported(String boardName, int tag) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Add tag to handle. Given {@code tagName} will be replaced with span defined by {@code tag}.</p>
	 *
	 * @param tagName Tag to handle.
	 * @param tag Tag type.
	 */
	public void addTag(String tagName, int tag) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Add tag to handle. Given {@code tagName} will be replaced with span defined by {@code tag}
	 * if tag contains {@code cssClass} in class attribute.</p>
	 *
	 * @param tagName Tag to handle.
	 * @param cssClass Tag CSS class.
	 * @param tag Tag type.
	 */
	public void addTag(String tagName, String cssClass, int tag) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Add tag to handle. Given {@code tagName} will be replaced with span defined by {@code spanType}
	 * if tag contains {@code attribute} that exactly equals {@code value}.</p>
	 *
	 * @param tagName Tag to handle.
	 * @param attribute Tag attribute.
	 * @param value Attribute value.
	 * @param tag Tag type.
	 */
	public void addTag(String tagName, String attribute, String value, int tag) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Marks given {@code tagName} as tag that may contain color attribute or CSS style. Parser will handle
	 * these cases automatically.</p>
	 *
	 * @param tagName Tag to handle.
	 */
	public void addColorable(String tagName) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Marks given {@code tagName} as tag that may contain color attribute or CSS style if tag contains
	 * {@code cssClass} in class attribute. Parser will handle these cases automatically.</p>
	 *
	 * @param tagName Tag to handle.
	 * @param cssClass Tag CSS class.
	 */
	public void addColorable(String tagName, String cssClass) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Marks given {@code tagName} as tag that may contain color attribute or CSS style if tag contains
	 * {@code attribute} that exactly equals {@code value}. Parser will handle these cases automatically.</p>
	 *
	 * @param tagName Tag to handle.
	 * @param attribute Tag attribute.
	 * @param value Attribute value.
	 */
	public void addColorable(String tagName, String attribute, String value) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Marks given {@code tagName} as block tag. For {@code spaced} blocks parser will add empty lines around.</p>
	 *
	 * @param tagName Tag to handle.
	 * @param block True to enable block tag.
	 * @param spaced True to enable spacing.
	 */
	public void addBlock(String tagName, boolean block, boolean spaced) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Marks given {@code tagName} as block tag if tag contains {@code cssClass} in class attribute.
	 * For {@code spaced} blocks parser will add empty lines around.</p>
	 *
	 * @param tagName Tag to handle.
	 * @param cssClass Tag CSS class.
	 * @param block True to enable block tag.
	 * @param spaced True to enable spacing.
	 */
	public void addBlock(String tagName, String cssClass, boolean block, boolean spaced) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Marks given {@code tagName} as block tag if tag contains {@code attribute} that exactly equals {@code value}.
	 * For {@code spaced} blocks parser will add empty lines around.</p>
	 *
	 * @param tagName Tag to handle.
	 * @param attribute Tag attribute.
	 * @param value Attribute value.
	 * @param block True to enable block tag.
	 * @param spaced True to enable spacing.
	 */
	public void addBlock(String tagName, String attribute, String value, boolean block, boolean spaced) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Marks given {@code tagName} as preformatted. In this mode all tabs, spaces and line breaks
	 * will be taken into account.</p>
	 *
	 * @param tagName Tag to handle.
	 * @param preformatted True to enable preformatted tag.
	 */
	public void addPreformatted(String tagName, boolean preformatted) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Marks given {@code tagName} as preformatted if tag contains {@code cssClass} in class attribute.
	 * In this mode all tabs, spaces and line breaks will be taken into account.</p>
	 *
	 * @param tagName Tag to handle.
	 * @param cssClass Tag CSS class.
	 * @param preformatted True to enable preformatted tag.
	 */
	public void addPreformatted(String tagName, String cssClass, boolean preformatted) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Marks given {@code tagName} as preformatted if tag contains {@code attribute} that exactly
	 * equals {@code value}. In this mode all tabs, spaces and line breaks will be taken into account.</p>
	 *
	 * @param tagName Tag to handle.
	 * @param attribute Tag attribute.
	 * @param value Attribute value.
	 * @param preformatted True to enable preformatted tag.
	 */
	public void addPreformatted(String tagName, String attribute, String value, boolean preformatted) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>This method calls every time HTML parser reaches links to other posts like {@code >>12345678}.</p>
	 *
	 * <p>You can leave this method not overridden, but overriding can make this method much faster and more correct
	 * in some cases.</p>
	 *
	 * <p>You must return a {@code Pair} of strings where the first string is thread number and the second string is
	 * post number. You can return both values as null: null thread number means this thread, null post number means
	 * original post.</p>
	 *
	 * @param uriString Parsed URI string.
	 * @return Pair of strings.
	 */
	public Pair<String, String> obtainPostLinkThreadPostNumbers(String uriString) {
		throw new IllegalAccessError();
	}
}
