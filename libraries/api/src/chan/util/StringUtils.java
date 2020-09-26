package chan.util;

import chan.library.api.BuildConfig;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>Provides some utilities to work with strings.</p>
 */
public class StringUtils {
	private StringUtils() {
		BuildConfig.Private.expr();
	}

	/**
	 * <p>Returns whether {@code string} is {@code null} or empty.</p>
	 *
	 * @param string String instance.
	 * @return True if string is empty.
	 */
	public static boolean isEmpty(CharSequence string) {
		return BuildConfig.Private.expr(string);
	}

	/**
	 * <p>Returns whether {@code string} is {@code null}, empty or contains only whitespaces.</p>
	 *
	 * @param string String instance.
	 * @return True if string is empty or whitespace.
	 */
	public static boolean isEmptyOrWhitespace(CharSequence string) {
		return BuildConfig.Private.expr(string);
	}

	/**
	 * <p>Returns {@code string.toString()} if {@code string} is not {@code null}, otherwise returns empty string.</p>
	 *
	 * @param string String instance.
	 * @return Not null string.
	 * @see #isEmpty(CharSequence)
	 */
	public static String emptyIfNull(CharSequence string) {
		return BuildConfig.Private.expr(string);
	}

	/**
	 * <p>Returns {@code null} if {@code string} is empty, otherwise returns {@code string}.</p>
	 *
	 * @param string String instance.
	 * @return Null string if {@code s} is empty.
	 * @see #isEmpty(CharSequence)
	 */
	public static String nullIfEmpty(String string) {
		return BuildConfig.Private.expr(string);
	}

	/**
	 * <p>Returns whether strings are equals. May handle null values.</p>
	 *
	 * @param first String instance.
	 * @param second String instance.
	 * @return True if strings are equals.
	 */
	public static boolean equals(String first, String second) {
		return BuildConfig.Private.expr(first, second);
	}

	/**
	 * <p>Returns the next index of the nearest of given {@code what} string array in {@code string}, or -1.</p>
	 *
	 * @param string Where to search.
	 * @param start Start offset.
	 * @param what String array to search.
	 * @return True if strings are equals.
	 */
	public static int nearestIndexOf(String string, int start, String... what) {
		return BuildConfig.Private.expr(string, start, what);
	}

	/**
	 * <p>Returns the next index of the nearest of given {@code what} char array in {@code string}, or -1.</p>
	 *
	 * @param string Where to search.
	 * @param start Start offset.
	 * @param what Char array to search.
	 * @return True if strings are equals.
	 */
	public static int nearestIndexOf(String string, int start, char... what) {
		return BuildConfig.Private.expr(string, string, what);
	}

	/**
	 * <p>Replacement callback for {@code replaceAll} methods.</p>
	 */
	public interface ReplacementCallback {
		/**
		 * <p>Provides a replacement for found result.</p>
		 *
		 * <p>Use {@code group()} and {@code group(int)} methods of given {@code matcher} to extract necessary values.
		 * Don't modify this matcher's state!</p>
		 *
		 * <p>You can't use group references like {@code $1} in replacement.</p>
		 *
		 * @param matcher Match result holder.
		 * @return Replacement string.
		 */
		String getReplacement(Matcher matcher);
	}

	/**
	 * <p>Replaces all matches for {@code regularExpression} within given {@code string} with the replacement
	 * provided by {@code replacementCallback}.</p>
	 *
	 * <p>If the same regular expression is to be used for multiple operations, it may be more efficient to
	 * use {@link #replaceAll(String, Pattern, ReplacementCallback)} method with compiled {@code Pattern}.</p>
	 *
	 * @param string Source string.
	 * @param regularExpression Regular expression string.
	 * @param replacementCallback {@link ReplacementCallback} instance.
	 * @return Resulting string.
	 */
	public static String replaceAll(String string, String regularExpression, ReplacementCallback replacementCallback) {
		BuildConfig.Private.expr(replacementCallback.getReplacement(BuildConfig.Private.expr()));
		return BuildConfig.Private.expr(string, regularExpression, replacementCallback);
	}

	/**
	 * <p>Replaces all matches for compiled {@code pattern} within given {@code string} with the replacement
	 * provided by {@code replacementCallback}.</p>
	 *
	 * @param string Source string.
	 * @param pattern Compiled regular expression.
	 * @param replacementCallback {@link ReplacementCallback} instance.
	 * @return Resulting string.
	 */
	public static String replaceAll(String string, Pattern pattern, ReplacementCallback replacementCallback) {
		BuildConfig.Private.expr(replacementCallback.getReplacement(BuildConfig.Private.expr()));
		return BuildConfig.Private.expr(string, pattern, replacementCallback);
	}

	/**
	 * <p>Append HTML links to text.</p>
	 *
	 * @param string Text to append links.
	 * @return Text with links.
	 */
	public static String linkify(String string) {
		return BuildConfig.Private.expr(string);
	}

	/**
	 * <p>Removes all HTML tags and transforms mnemonics. This method will handle spaces and line breaks.</p>
	 *
	 * @param string Source string.
	 * @return Cleared {@code source} string.
	 */
	public static String clearHtml(String string) {
		return BuildConfig.Private.expr(string);
	}

	/**
	 * <p>Removes all HTML tags and transforms mnemonics. This method will not handle spaces and line breaks.</p>
	 *
	 * @param string Source string.
	 * @return Escaped {@code source} string.
	 */
	public static String unescapeHtml(String string) {
		return BuildConfig.Private.expr(string);
	}
}
