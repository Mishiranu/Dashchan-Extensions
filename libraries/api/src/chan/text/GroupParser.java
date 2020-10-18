package chan.text;

import chan.library.api.BuildConfig;
import java.io.IOException;
import java.io.Reader;

/**
 * <p>HTML text parser. Can work in two modes: linear and group.</p>
 *
 * <p>In linear mode, parser will call {@link Callback#onStartElement(GroupParser, String, Attributes)}  every
 * time parser reaches new tag. This method has boolean result, and when this method returns true - parser switches
 * to group mode.</p>
 *
 * <p>In group mode parser will handle all text inside started tag. Then it call
 * {@link Callback#onGroupComplete(GroupParser, String)} with all text inside tag.</p>
 */
public final class GroupParser {
	/**
	 * <p>Attributes holder and parser.</p>
	 */
	public static final class Attributes {
		private Attributes() {
			BuildConfig.Private.expr();
		}

		/**
		 * <p>Parses the attribute and returns its value if attribute exists.</p>
		 *
		 * @param attribute Attribute name.
		 * @return Attribute value.
		 */
		public String get(String attribute) {
			return BuildConfig.Private.expr(attribute);
		}

		/**
		 * <p>Checks the attributes line contains the string.</p>
		 *
		 * @param string String to search for.
		 * @return True if string contains the {@code string}.
		 */
		public boolean contains(CharSequence string) {
			return BuildConfig.Private.expr(string);
		}
	}

	/**
	 * <p>Callback for {@link GroupParser}.</p>
	 */
	public interface Callback {
		/**
		 * <p>This method will be called in linear mode every time parser reaches new tag.</p>
		 *
		 * @param parser {@link GroupParser} instance.
		 * @param tagName Name of tag.
		 * @param attributes Tag attributes.
		 * @return True to switch parser to group mode.
		 * @throws ParseException to interrupt parsing process.
		 */
		boolean onStartElement(GroupParser parser, String tagName, Attributes attributes) throws ParseException;

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
		 * @param text Source string.
		 * @throws ParseException to interrupt parsing process.
		 */
		void onText(GroupParser parser, CharSequence text) throws ParseException;

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
	public static void parse(String source, Callback callback) throws ParseException {
		BuildConfig.Private.<ParseException>error();
		BuildConfig.Private.expr(source, callback);
	}

	/**
	 * <p>Starts a new parsing process.</p>
	 *
	 * @param reader Input to parse.
	 * @param callback Callback to handle parsed data.
	 * @throws IOException when reading process was interrupted due to I/O problem.
	 * @throws ParseException when parsing process was interrupted.
	 */
	public static void parse(Reader reader, Callback callback) throws IOException, ParseException {
		BuildConfig.Private.<IOException>error();
		BuildConfig.Private.<ParseException>error();
		BuildConfig.Private.expr(reader, callback);
	}
}
