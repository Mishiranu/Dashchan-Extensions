package chan.http;

import android.net.Uri;
import chan.library.api.BuildConfig;

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
		enum Action {
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
			RETRANSMIT
		}

		/**
		 * <p>HTTP client will call this method every time it reaches redirect response code.
		 * You must return the most suitable {@link Action} type for this response.</p>
		 *
		 * <p>You can also override the redirected URI using {@link HttpResponse#setRedirectedUri(Uri)}.</p>
		 *
		 * @param response Response.
		 * @return {@link Action} type.
		 * @throws HttpException if HTTP exception occurred.
		 */
		Action onRedirect(HttpResponse response) throws HttpException;

		/**
		 * <p>{@link RedirectHandler} implementation. This handler will not follow any redirects.</p>
		 */
		RedirectHandler NONE = BuildConfig.Private.expr();

		/**
		 * <p>{@link RedirectHandler} implementation. This handler will follow all redirects with GET method.</p>
		 */
		RedirectHandler BROWSER = BuildConfig.Private.expr();

		/**
		 * <p>{@link RedirectHandler} implementation. This handler will follow {@code 301} and {@code 302} redirects
		 * with previous method. The rest will be followed with GET method.</p>
		 */
		RedirectHandler STRICT = BuildConfig.Private.expr();
	}

	/**
	 * <p>Constructor for {@link HttpRequest}.</p>
	 *
	 * @param uri URI for request.
	 * @param preset Preset with configuration.
	 */
	public HttpRequest(Uri uri, Preset preset) {
		BuildConfig.Private.expr(uri, preset);
	}

	/**
	 * <p>Sets HTTP request method to GET.</p>
	 *
	 * @return This builder.
	 */
	public HttpRequest setGetMethod() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Sets HTTP request method to HEAD.</p>
	 *
	 * @return This builder.
	 */
	public HttpRequest setHeadMethod() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Sets HTTP request method to POST with request entity.</p>
	 *
	 * @param entity {@link RequestEntity} instance.
	 * @return This builder.
	 */
	public HttpRequest setPostMethod(RequestEntity entity) {
		return BuildConfig.Private.expr(entity);
	}

	/**
	 * <p>Sets HTTP request method to PUT with request entity.</p>
	 *
	 * @param entity {@link RequestEntity} instance.
	 * @return This builder.
	 */
	public HttpRequest setPutMethod(RequestEntity entity) {
		return BuildConfig.Private.expr(entity);
	}

	/**
	 * <p>Sets HTTP request method to DELETE with request entity.</p>
	 *
	 * @param entity {@link RequestEntity} instance.
	 * @return This builder.
	 */
	public HttpRequest setDeleteMethod(RequestEntity entity) {
		return BuildConfig.Private.expr(entity);
	}

	/**
	 * <p>Configures response code handling. The {@link HttpResponse#checkResponseCode()} will be called automatically
	 * if this handling enabled. Enabled by default.</p>
	 *
	 * @param successOnly True to enable handling, false to disable one.
	 * @return This builder.
	 */
	public HttpRequest setSuccessOnly(boolean successOnly) {
		return BuildConfig.Private.expr(successOnly);
	}

	/**
	 * <p>Configures redirect handling with {@link RedirectHandler}.</p>
	 *
	 * @param redirectHandler Redirect handler interface.
	 * @return This builder.
	 */
	public HttpRequest setRedirectHandler(RedirectHandler redirectHandler) {
		return BuildConfig.Private.expr(redirectHandler);
	}

	/**
	 * <p>Sets the {@link HttpValidator} to handle data changes with {@code 304 Not Modified}.</p>
	 *
	 * @param validator {@link HttpValidator} instance. May be null.
	 * @return This builder.
	 */
	public HttpRequest setValidator(HttpValidator validator) {
		return BuildConfig.Private.expr(validator);
	}

	/**
	 * <p>Enabled or disables connection pooling. Enabled by default.</p>
	 *
	 * @param keepAlive True to enable pooling, false to disable one.
	 * @return This builder.
	 */
	public HttpRequest setKeepAlive(boolean keepAlive) {
		return BuildConfig.Private.expr(keepAlive);
	}

	/**
	 * <p>Sets the timeouts of connection.</p>
	 *
	 * @param connectTimeout TCP handshake timeout in milliseconds.
	 * @param readTimeout Max delay in milliseconds between reading data.
	 * @return This builder.
	 */
	public HttpRequest setTimeouts(int connectTimeout, int readTimeout) {
		return BuildConfig.Private.expr(connectTimeout, readTimeout);
	}

	/**
	 * <p>Sets the delay before opening previous and this connection.
	 * May be helpful in adjusting connection frequency.</p>
	 *
	 * @param delay Delay in milliseconds.
	 * @return This builder.
	 */
	public HttpRequest setDelay(int delay) {
		return BuildConfig.Private.expr(delay);
	}

	/**
	 * <p>Add a header with given {@code name} and {@code value}.</p>
	 *
	 * @param name Header name.
	 * @param value Header value.
	 * @return This builder.
	 */
	public HttpRequest addHeader(String name, String value) {
		return BuildConfig.Private.expr(name, value);
	}

	/**
	 * <p>Removes all headers added before.</p>
	 *
	 * @return This builder.
	 */
	public HttpRequest clearHeaders() {
		return BuildConfig.Private.expr();
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
		return BuildConfig.Private.expr(name, value);
	}

	/**
	 * <p>Add a cookie string.</p>
	 *
	 * @param cookie Cookie string.
	 * @return This builder.
	 * @see CookieBuilder
	 */
	public HttpRequest addCookie(String cookie) {
		return BuildConfig.Private.expr(cookie);
	}

	/**
	 * <p>Add a {@link CookieBuilder} and concatenate it with existing one.</p>
	 *
	 * @param builder {@link CookieBuilder} instance.
	 * @return This builder.
	 * @see CookieBuilder
	 */
	public HttpRequest addCookie(CookieBuilder builder) {
		return BuildConfig.Private.expr(builder);
	}

	/**
	 * <p>Removes all cookies added before.</p>
	 *
	 * @return This builder.
	 */
	public HttpRequest clearCookies() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Returns a deep copy of this builder. Request entities will be copied shallowly!</p>
	 *
	 * @return Copy of builder.
	 */
	public HttpRequest copy() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Executes HTTP request and reads response.</p>
	 *
	 * @return HTTP response.
	 * @throws HttpException if HTTP exception occurred.
	 * @see HttpResponse
	 */
	public HttpResponse perform() throws HttpException {
		BuildConfig.Private.<HttpException>error();
		return BuildConfig.Private.expr();
	}
}
