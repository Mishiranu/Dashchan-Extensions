package chan.text;

/**
 * <p>Thrown when parsing exception occurred.
 * Usually thrown by {@link GroupParser#parse(String, chan.text.GroupParser.Callback)} method.</p>
 */
public class ParseException extends Exception {
	/**
	 * <p>Default constructor for a {@link ParseException}.</p>
	 */
	public ParseException() {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Constructor for a {@link ParseException} with specified cause.</p>
	 *
	 * @param throwable The cause of this exception.
	 */
	public ParseException(Throwable throwable) {
		throw new IllegalAccessError();
	}
}
