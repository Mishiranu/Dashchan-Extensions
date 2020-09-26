package chan.http;

import chan.content.ChanPerformer;
import chan.library.api.BuildConfig;

/**
 * <p>Thrown by HTTP client and {@link ChanPerformer} methods.</p>
 */
public final class HttpException extends Exception {
	/**
	 * <p>Constructor for a {@link HttpException} with response code and message.</p>
	 *
	 * @param responseCode Response code.
	 * @param responseText Response message.
	 */
	public HttpException(int responseCode, String responseText) {
		BuildConfig.Private.expr(responseCode, responseText);
	}

	/**
	 * <p>Returns response code.</p>
	 *
	 * @return Response code.
	 */
	public int getResponseCode() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Returns whether exception is HTTP protocol exception.</p>
	 */
	public boolean isHttpException() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Returns whether exception is socket level exception.</p>
	 */
	public boolean isSocketException() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Creates a new instance of {@link HttpException} with {@code 404 (Not Found)} response code and
	 * an appropriate message.</p>
	 *
	 * @return Exception object.
	 */
	public static HttpException createNotFoundException() {
		return BuildConfig.Private.expr();
	}
}
