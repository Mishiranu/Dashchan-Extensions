package chan.http;

import android.net.Uri;
import chan.library.api.BuildConfig;
import java.util.List;
import java.util.Map;

/**
 * <p>HTTP connection holder.</p>
 */
public final class HttpHolder {
	/**
	 * <p>Disconnect from server.</p>
	 */
	public void disconnect() {
		BuildConfig.Private.expr();
	}

	/**
	 * <p>Reads {@link HttpResponse} from server.</p>
	 */
	@SuppressWarnings("RedundantThrows")
	public HttpResponse read() throws HttpException {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>This method will throw {@link HttpException} if response code is not success ({@code 2xx}) or redirect
	 * ({@code 301}, {@code 302}, {@code 303} or {@code 307}).</p>
	 */
	@SuppressWarnings("RedundantThrows")
	public void checkResponseCode() throws HttpException {
		BuildConfig.Private.expr();
	}

	/**
	 * <p>Returns response code from last connection.</p>
	 *
	 * @return Response code.
	 */
	public int getResponseCode() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Returns response message from last connection.</p>
	 *
	 * @return Response message.
	 */
	public String getResponseMessage() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Returns redirected URI from {@code Location} header.</p>
	 *
	 * @return Redirected URI.
	 */
	public Uri getRedirectedUri() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Returns map of all HTTP header fields.</p>
	 *
	 * @return HTTP header fields.
	 */
	public Map<String, List<String>> getHeaderFields() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Decodes and returns cookie by given {@code name}.</p>
	 *
	 * @param name Name of cookie.
	 * @return Value of cookie or {@code null} if given cookie doesn't exist.
	 */
	public String getCookieValue(String name) {
		return BuildConfig.Private.expr(name);
	}

	/**
	 * <p>Returns {@link HttpValidator} instance, decoded from header.</p>
	 *
	 * @return {@link HttpValidator} instance.
	 * @see HttpValidator
	 */
	public HttpValidator getValidator() {
		return BuildConfig.Private.expr();
	}
}
