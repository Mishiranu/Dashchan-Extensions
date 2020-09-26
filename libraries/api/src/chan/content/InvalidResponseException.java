package chan.content;

import chan.library.api.BuildConfig;

/**
 * <p>Thrown then unknown or incorrect data read. This exceptions is thrown by {@link ChanPerformer} methods.</p>
 */
public final class InvalidResponseException extends Exception {
	/**
	 * <p>Default constructor for an {@link InvalidResponseException}.</p>
	 */
	public InvalidResponseException() {
		BuildConfig.Private.expr();
	}

	/**
	 * <p>Constructor for an {@link InvalidResponseException} with specified cause.</p>
	 *
	 * @param throwable The cause of this exception.
	 */
	public InvalidResponseException(Throwable throwable) {
		BuildConfig.Private.expr(throwable);
	}
}
