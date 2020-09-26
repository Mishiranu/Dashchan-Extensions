package chan.text;

import chan.library.api.BuildConfig;

/**
 * <p>HTML text parser. Can work in two modes: linear and group.</p>
 *
 * <p>In linear mode, parser will call {@link Callback#onStartElement(GroupParser, String, String)} every
 * time parser reaches new tag. This method has boolean result, and when this method returns true - parser switches
 * to group mode.</p>
 *
 * <p>In group mode parser will handle all text inside started tag. Then it call
 * {@link Callback#onGroupComplete(GroupParser, String)} with all text inside tag.</p>
 */
public final class GroupParser {
	/**
	 * <p>Callback for {@link GroupParser}.</p>
	 */
	public interface Callback {
		/**
		 * <p>This method will be called in linear mode every time parser reaches new tag.</p>
		 *
		 * @param parser {@link GroupParser} instance.
		 * @param tagName Name of tag.
		 * @param attrs Encoded attributes inside tag.
		 * @return True to switch parser to group mode.
		 * @throws ParseException to interrupt parsing process.
		 */
		boolean onStartElement(GroupParser parser, String tagName, String attrs) throws ParseException;

		/**
		 * <p>This method will be called in linear mode every time parser reaches end of tag.</p>
		 *
		 * @param parser {@link GroupParser} instance.
		 * @param tagName Name of tag.
		 * @throws ParseException to interrupt parsing process.
		 */
		void onEndElement(GroupParser parser, String tagName) throws ParseException;

		/**
		 * <p>This method will be called in linear mode every time parser skips text.</p>
		 *
		 * @param parser {@link GroupParser} instance.
		 * @param source Source string.
		 * @param start Start index of text.
		 * @param end End index of text.
		 * @throws ParseException to interrupt parsing process.
		 */
		void onText(GroupParser parser, String source, int start, int end) throws ParseException;

		/**
		 * <p>This method will be called in group mode every time parser reaches end of group.</p>
		 *
		 * @param parser {@link GroupParser} instance.
		 * @param text Text inside tag.
		 * @throws ParseException to interrupt parsing process.
		 */
		void onGroupComplete(GroupParser parser, String text) throws ParseException;
	}

	private GroupParser() {
		BuildConfig.Private.expr();
	}

	/**
	 * <p>Starts a new parsing process.</p>
	 *
	 * @param source String to parse.
	 * @param callback Callback to handle parsed data.
	 * @throws ParseException when parsing process was interrupted.
	 */
	@SuppressWarnings("RedundantThrows")
	public static void parse(String source, Callback callback) throws ParseException {
		BuildConfig.Private.expr(source, callback);
	}

	/**
	 * <p>Stores parser's position. Later you can come back with {@link #reset()} method.</p>
	 */
	public void mark() {
		BuildConfig.Private.expr();
	}

	/**
	 * <p>Resets parser's position to last marked one.</p>
	 */
	public void reset() {
		BuildConfig.Private.expr();
	}

	/**
	 * <p>Decodes attribute from encoded string. You can use it to handle {@code attrs} argument in
	 * {@link Callback#onStartElement(GroupParser, String, String)} method.</p>
	 *
	 * @param attrs Encoded string.
	 * @param attr Attribute name.
	 * @return Attribute value if it exists or {@code null}.
	 */
	public String getAttr(String attrs, String attr) {
		return BuildConfig.Private.expr(attrs, attr);
	}
}
