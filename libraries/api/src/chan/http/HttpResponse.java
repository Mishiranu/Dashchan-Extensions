package chan.http;

import android.graphics.Bitmap;
import android.net.Uri;
import chan.library.api.BuildConfig;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * <p>HTTP response holder.</p>
 */
public final class HttpResponse {
	/**
	 * <p>Constructor for {@link HttpResponse}.</p>
	 *
	 * @param input Data input stream.
	 */
	public HttpResponse(InputStream input) {
		BuildConfig.Private.expr(input);
	}

	/**
	 * <p>Constructor for {@link HttpResponse}.</p>
	 *
	 * @param bytes Byte array of data.
	 */
	public HttpResponse(byte[] bytes) {
		BuildConfig.Private.expr(bytes);
	}

	/**
	 * <p>Sets encoding for this response. ISO-8859-1 is used by default.</p>
	 *
	 * @param charsetName Encoding charset name.
	 */
	public void setEncoding(String charsetName) {
		BuildConfig.Private.expr(charsetName);
	}

	/**
	 * <p>Gets the encoding for this response.</p>
	 *
	 * @return Encoding used by this response.
	 */
	public String getEncoding() throws HttpException {
		BuildConfig.Private.<HttpException>error();
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>This method will throw {@link HttpException} if response code is not success ({@code 2xx}) or redirect
	 * ({@code 301}, {@code 302}, {@code 303} or {@code 307}).</p>
	 */
	public void checkResponseCode() throws HttpException {
		BuildConfig.Private.<HttpException>error();
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
	 * <p>Returns the URI to read response from.</p>
	 *
	 * @return Requested URI.
	 */
	public Uri getRequestedUri() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Returns the historical list of URIs.</p>
	 *
	 * @return List of requested URIs.
	 */
	public List<Uri> getRequestedUris() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Returns the redirected URI from {@code Location} header.</p>
	 *
	 * @return Redirected URI.
	 */
	public Uri getRedirectedUri() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Sets the redirected URI used by redirect handler.</p>
	 */
	public void setRedirectedUri(Uri redirectedUri) {
		BuildConfig.Private.expr(redirectedUri);
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

	/**
	 * <p>Opens input stream for this response. You should call {@link #fail(IOException)} when any
	 * {@link IOException} occurs and throw this exception!</p>
	 *
	 * @return Input stream.
	 * @throws HttpException if HTTP exception occurred.
	 */
	public InputStream open() throws HttpException {
		BuildConfig.Private.<HttpException>error();
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Opens input stream for this response. You should call </p>
	 *
	 * @return HTTP exception.
	 */
	public HttpException fail(IOException exception) {
		return BuildConfig.Private.expr(exception);
	}

	/**
	 * <p>Reads and returns response as byte array.</p>
	 *
	 * @return Byte array response.
	 * @throws HttpException if HTTP exception occurred.
	 */
	public byte[] readBytes() throws HttpException {
		BuildConfig.Private.<HttpException>error();
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Reads and returns response as string using provided by {@link #setEncoding(String)} encoding.</p>
	 *
	 * @return String response.
	 * @throws HttpException if HTTP exception occurred.
	 */
	public String readString() throws HttpException {
		BuildConfig.Private.<HttpException>error();
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Reads, decodes, and returns response as {@code Bitmap}.</p>
	 *
	 * @return Bitmap response or {@code null} if response is not bitmap.
	 * @throws HttpException if HTTP exception occurred.
	 */
	public Bitmap readBitmap() throws HttpException {
		BuildConfig.Private.<HttpException>error();
		return BuildConfig.Private.expr();
	}
}
