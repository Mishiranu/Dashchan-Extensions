package chan.http;

import android.net.Uri;
import java.io.OutputStream;

/**
 * <p>HTTP request builder and executor.</p>
 */
public final class HttpRequest {
	/**
	 * <p>Client's preset with timeout settings, listeners, etc. You should never implement this interface.</p>
	 */
	public interface Preset {}

	/**
	 * <p>Redirection handler interface.</p>
	 *
	 * @see HttpRequest#setRedirectHandler(RedirectHandler)
	 */
	public interface RedirectHandler {
		/**
		 * <p>Redirection handler result.</p>
		 */
		public enum Action {
			/**
			 * <p>Cancel redirect handling.</p>
			 */
			CANCEL,

			/**
			 * <p>Follow redirect with GET method.</p>
			 */
			GET,

			/**
			 * <p>Follow redirect with previous method including data retransmission for POST.</p>
			 */
			RETRANSMIT;

			/**
			 * <p>Overrides redirected URI.</p>
			 */
			public Action setRedirectedUri(Uri redirectedUri) {
				throw new IllegalAccessError();
			}
		}

		/**
		 * <p>HTTP client will call this method every time it reaches redirect response code.
		 * You must return the most suitable {@link Action} type for this response.</p>
		 *
		 * @param responseCode Response code.
		 * @param requestedUri Requested URI.
		 * @param redirectedUri URI decoded from {@code Location} header.
		 * @param holder HTTP holder instance.
		 * @return {@link Action} type.
		 * @throws HttpException if HTTP exception occurred.
		 */
		public Action onRedirectReached(int responseCode, Uri requestedUri, Uri redirectedUri, HttpHolder holder)
				throws HttpException;

		/**
		 * <p>{@link RedirectHandler} implementation. This handler will not follow any redirects.</p>
		 */
		public static final RedirectHandler NONE = stub();

		/**
		 * <p>{@link RedirectHandler} implementation. This handler will follow all redirects with GET method.</p>
		 */
		public static final RedirectHandler BROWSER = stub();

		/**
		 * <p>{@link RedirectHandler} implementation. This handler will follow {@code 301} and {@code 302} redirects
		 * with previous method. The rest will be followed with GET method.</p>
		 */
		public static final RedirectHandler STRICT = stub();
	}

	private static RedirectHandler stub() {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Constructor for {@link HttpRequest}.</p>
	 *
	 * @param uri URI for request.
	 * @param holder {@link HttpHolder} instance. May be null.
	 * @param preset Preset with configuration.
	 */
	public HttpRequest(Uri uri, HttpHolder holder, Preset preset) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Constructor for {@link HttpRequest} without preset.</p>
	 *
	 * @param uri URI for request.
	 * @param holder {@link HttpHolder} instance. May be null.
	 */
	public HttpRequest(Uri uri, HttpHolder holder) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Constructor for {@link HttpRequest}. In most cases {@link Preset} can provide it's own
	 * {@link HttpHolder}, so you can use this constructor.</p>
	 *
	 * @param uri URI for request.
	 * @param preset Preset with configuration.
	 */
	public HttpRequest(Uri uri, Preset preset) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Sets HTTP request method to GET.</p>
	 *
	 * @return This builder.
	 */
	public HttpRequest setGetMethod() {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Sets HTTP request method to HEAD.</p>
	 *
	 * @return This builder.
	 */
	public HttpRequest setHeadMethod() {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Sets HTTP request method to POST with request entity.</p>
	 *
	 * @param entity {@link RequestEntity} instance.
	 * @return This builder.
	 */
	public HttpRequest setPostMethod(RequestEntity entity) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Sets HTTP request method to PUT with request entity.</p>
	 *
	 * @param entity {@link RequestEntity} instance.
	 * @return This builder.
	 */
	public HttpRequest setPutMethod(RequestEntity entity) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Sets HTTP request method to DELETE with request entity.</p>
	 *
	 * @param entity {@link RequestEntity} instance.
	 * @return This builder.
	 */
	public HttpRequest setDeleteMethod(RequestEntity entity) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Configures response code handling. The {@link HttpHolder#checkResponseCode()} will be called automatically
	 * if this handling enabled. Enabled by default.</p>
	 *
	 * @param successOnly True to enable handling, false to disable one.
	 * @return This builder.
	 */
	public HttpRequest setSuccessOnly(boolean successOnly) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Configures redirect handling with {@link RedirectHandler}.</p>
	 *
	 * @param redirectHandler Redirect handler interface.
	 * @return This builder.
	 */
	public HttpRequest setRedirectHandler(RedirectHandler redirectHandler) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Sets the {@link HttpValidator} to handle data changes with {@code 304 Not Modified}.</p>
	 *
	 * @param validator {@link HttpValidator} instance. May be null.
	 * @return This builder.
	 */
	public HttpRequest setValidator(HttpValidator validator) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Enabled or disables connection pooling. Enabled by default.</p>
	 *
	 * @param keepAlive True to enable pooling, false to disable one.
	 * @return This builder.
	 */
	public HttpRequest setKeepAlive(boolean keepAlive) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Sets the timeouts of connection.</p>
	 *
	 * @param connectTimeout TCP handshake timeout in milliseconds.
	 * @param readTimeout Max delay in milliseconds between reading data.
	 * @return This builder.
	 */
	public HttpRequest setTimeouts(int connectTimeout, int readTimeout) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Sets the delay before opening previous and this connection.
	 * May be helpful in adjusting connection frequency.</p>
	 *
	 * @param delay Delay in milliseconds.
	 * @return This builder.
	 */
	public HttpRequest setDelay(int delay) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Sets the output stream as the destination of response. In this case {@link #read()} method
	 * will return {@code null} value.</p>
	 *
	 * @param outputStream Output stream.
	 * @return This builder.
	 */
	public HttpRequest setOutputStream(OutputStream outputStream) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Add a header with given {@code name} and {@code value}.</p>
	 *
	 * @param name Header name.
	 * @param value Header value.
	 * @return This builder.
	 */
	public HttpRequest addHeader(String name, String value) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Removes all headers added before.</p>
	 *
	 * @return This builder.
	 */
	public HttpRequest clearHeaders() {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Add a cookie with given {@code name} and {@code value}.</p>
	 *
	 * @param name Cookie name.
	 * @param value Cookie value.
	 * @return This builder.
	 * @see CookieBuilder
	 */
	public HttpRequest addCookie(String name, String value) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Add a cookie string.</p>
	 *
	 * @param cookie Cookie string.
	 * @return This builder.
	 * @see CookieBuilder
	 */
	public HttpRequest addCookie(String cookie) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Add a {@link CookieBuilder} and concatenate it with existing one.</p>
	 *
	 * @param builder {@link CookieBuilder} instance.
	 * @return This builder.
	 * @see CookieBuilder
	 */
	public HttpRequest addCookie(CookieBuilder builder) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Removes all cookies added before.</p>
	 *
	 * @return This builder.
	 */
	public HttpRequest clearCookies() {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Returns a deep copy of this builder. Request entities will be copied shallowly!</p>
	 *
	 * @return Copy of builder.
	 */
	public HttpRequest copy() {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Executes HTTP request. This make available response headers with returned {@link HttpHolder}.
	 * Later you can read response body with {@link HttpHolder#read()} or disconnect.</p>
	 *
	 * @return HTTP connection holder.
	 * @throws HttpException if HTTP exception occurred.
	 */
	public HttpHolder execute() throws HttpException {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Executes HTTP request and reads response.</p>
	 *
	 * @return HTTP response.
	 * @throws HttpException if HTTP exception occurred.
	 * @see HttpResponse
	 */
	public HttpResponse read() throws HttpException {
		throw new IllegalAccessError();
	}
}
