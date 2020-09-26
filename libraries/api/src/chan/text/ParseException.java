package chan.text;

import chan.library.api.BuildConfig;

/**
 * <p>Thrown when parsing exception occurred.
 * Usually thrown by {@link GroupParser#parse(String, chan.text.GroupParser.Callback)} method.</p>
 */
public class ParseException extends Exception {
	/**
	 * <p>Default constructor for a {@link ParseException}.</p>
	 */
	public ParseException() {
		BuildConfig.Private.expr();
	}

	/**
	 * <p>Constructor for a {@link ParseException} with specified cause.</p>
	 *
	 * @param throwable The cause of this exception.
	 */
	public ParseException(Throwable throwable) {
		BuildConfig.Private.expr(throwable);
	}
}
