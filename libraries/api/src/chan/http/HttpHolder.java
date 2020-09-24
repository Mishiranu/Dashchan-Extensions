package chan.http;

import android.net.Uri;
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
		throw new IllegalAccessError();
	}

	/**
	 * <p>Reads {@link HttpResponse} from server.</p>
	 */
	public HttpResponse read() throws HttpException {
		throw new IllegalAccessError();
	}

	/**
	 * <p>This method will throw {@link HttpException} if response code is not success ({@code 2xx}) or redirect
	 * ({@code 301}, {@code 302}, {@code 303} or {@code 307}).</p>
	 */
	public void checkResponseCode() throws HttpException {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Returns response code from last connection.</p>
	 *
	 * @return Response code.
	 */
	public int getResponseCode() {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Returns response message from last connection.</p>
	 *
	 * @return Response message.
	 */
	public String getResponseMessage() {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Returns redirected URI from {@code Location} header.</p>
	 *
	 * @return Redirected URI.
	 */
	public Uri getRedirectedUri() {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Returns map of all HTTP header fields.</p>
	 *
	 * @return HTTP header fields.
	 */
	public Map<String, List<String>> getHeaderFields() {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Decodes and returns cookie by given {@code name}.</p>
	 *
	 * @param name Name of cookie.
	 * @return Value of cookie or {@code null} if given cookie doesn't exist.
	 */
	public String getCookieValue(String name) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Returns {@link HttpValidator} instance, decoded from header.</p>
	 *
	 * @return {@link HttpValidator} instance.
	 * @see HttpValidator
	 */
	public HttpValidator getValidator() {
		throw new IllegalAccessError();
	}
}
