package chan.http;

import android.net.Uri;
import chan.library.api.BuildConfig;
import java.io.InputStream;

/**
 * <p>Web socket request builder and executor.</p>
 */
public final class WebSocket {
	/**
	 * <p>Read data holder.</p>
	 */
	public static class Event {
		private Event() {
			BuildConfig.Private.expr();
		}

		/**
		 * <p>Converts and results event data as {@link HttpResponse} instance.</p>
		 */
		public HttpResponse getResponse() {
			return BuildConfig.Private.expr();
		}

		/**
		 * <p>Returns whether event data is binary.</p>
		 */
		public boolean isBinary() {
			return BuildConfig.Private.expr();
		}

		/**
		 * @see Connection#store(String, Object)
		 */
		public void store(String key, Object object) {
			BuildConfig.Private.expr(key, object);
		}

		/**
		 * @see Connection#get(String)
		 */
		public <T> T get(String key) {
			return BuildConfig.Private.expr(key);
		}

		/**
		 * <p>Stores result to {@link Connection} instance to unlock running {@link Connection#await(Object...)}
		 * method.</p>
		 *
		 * @see Connection#await(Object...)
		 */
		public void complete(Object result) {
			BuildConfig.Private.expr(result);
		}

		/**
		 * <p>Safe close of {@link Connection} instance. Use this method instead of {@link Connection#close()}
		 * directly.</p>
		 */
		public void close() {
			BuildConfig.Private.expr();
		}
	}

	/**
	 * <p>Callback for read socket data.</p>
	 */
	public interface EventHandler {
		/**
		 * <p>Callback method.</p>
		 *
		 * <p>This method is invoked from a separate thread, so you should use synchronization
		 * to access data from thread with running web socket. You can use {@link Event#store(String, Object)}
		 * and {@link Event#get(String)} methods for this purpose.</p>
		 *
		 * @param event Event data.
		 */
		void onEvent(Event event);
	}

	/**
	 * <p>Constructor for {@link WebSocket}.</p>
	 *
	 * @param uri URI for request.
	 * @param holder {@link HttpHolder} instance. May be null.
	 * @param preset Preset with configuration.
	 */
	public WebSocket(Uri uri, HttpHolder holder, HttpRequest.Preset preset) {
		BuildConfig.Private.expr(uri, holder, preset);
	}

	/**
	 * <p>Constructor for {@link WebSocket} without preset.</p>
	 *
	 * @param uri URI for request.
	 * @param holder {@link HttpHolder} instance. May be null.
	 */
	public WebSocket(Uri uri, HttpHolder holder) {
		BuildConfig.Private.expr(uri, holder);
	}

	/**
	 * <p>Constructor for {@link WebSocket}. In most cases {@link HttpRequest.Preset} can provide it's own
	 * {@link HttpHolder}, so you can use this constructor.</p>
	 *
	 * @param uri URI for request.
	 * @param preset Preset with configuration.
	 */
	public WebSocket(Uri uri, HttpRequest.Preset preset) {
		BuildConfig.Private.expr(uri, preset);
	}

	/**
	 * <p>Add a header with given {@code name} and {@code value}.</p>
	 *
	 * @param name Header name.
	 * @param value Header value.
	 * @return This builder.
	 */
	public WebSocket addHeader(String name, String value) {
		return BuildConfig.Private.expr(name, value);
	}

	/**
	 * <p>Add a cookie with given {@code name} and {@code value}.</p>
	 *
	 * @param name Cookie name.
	 * @param value Cookie value.
	 * @return This builder.
	 * @see CookieBuilder
	 */
	public WebSocket addCookie(String name, String value) {
		return BuildConfig.Private.expr(name, value);
	}

	/**
	 * <p>Add a cookie string.</p>
	 *
	 * @param cookie Cookie string.
	 * @return This builder.
	 * @see CookieBuilder
	 */
	public WebSocket addCookie(String cookie) {
		return BuildConfig.Private.expr(cookie);
	}

	/**
	 * <p>Add a {@link CookieBuilder} and concatenate it with existing one.</p>
	 *
	 * @param builder {@link CookieBuilder} instance.
	 * @return This builder.
	 * @see CookieBuilder
	 */
	public WebSocket addCookie(CookieBuilder builder) {
		return BuildConfig.Private.expr(builder);
	}

	/**
	 * <p>Sets the timeouts of connection.</p>
	 *
	 * @param connectTimeout TCP handshake timeout in milliseconds.
	 * @param readTimeout Max delay in milliseconds between reading data.
	 * @return This builder.
	 */
	public WebSocket setTimeouts(int connectTimeout, int readTimeout) {
		return BuildConfig.Private.expr(connectTimeout, readTimeout);
	}

	/**
	 * <p>Opens a WebSocket connection.</p>
	 *
	 * @param handler Event handler for incoming data.
	 * @return Connection instance.
	 * @throws HttpException if HTTP exception occurred.
	 */
	@SuppressWarnings("RedundantThrows")
	public Connection open(EventHandler handler) throws HttpException {
		handler.onEvent(BuildConfig.Private.expr());
		return BuildConfig.Private.expr(handler);
	}

	/**
	 * <p>A builder for complex binary data.</p>
	 */
	public static class ComplexBinaryBuilder {
		private ComplexBinaryBuilder() {
			BuildConfig.Private.expr();
		}

		/**
		 * <p>Appends bytes to builder.</p>
		 *
		 * @param bytes Array of bytes to send.
		 * @return This builder.
		 */
		public ComplexBinaryBuilder bytes(byte... bytes) {
			return BuildConfig.Private.expr(bytes);
		}

		/**
		 * <p>Appends bytes to builder.</p>
		 *
		 * @param bytes Array of bytes to send.
		 * @return This builder.
		 */
		public ComplexBinaryBuilder bytes(int... bytes) {
			return BuildConfig.Private.expr(bytes);
		}

		/**
		 * <p>Appends string to builder.</p>
		 *
		 * @param string String to send.
		 * @return This builder.
		 */
		public ComplexBinaryBuilder string(String string) {
			return BuildConfig.Private.expr(string);
		}

		/**
		 * <p>Appends input stream to builder with specified bytes {@code count} read.</p>
		 *
		 * @param inputStream Stream to read from.
		 * @param count Bytes count to read.
		 * @return This builder.
		 */
		public ComplexBinaryBuilder stream(InputStream inputStream, int count) {
			return BuildConfig.Private.expr(inputStream, count);
		}

		/**
		 * <p>Appends data provided by wrapper callback.</p>
		 *
		 * @param wrapper Callback which performs modifying the builder.
		 * @return This builder.
		 */
		public ComplexBinaryBuilder wrap(Wrapper wrapper) {
			BuildConfig.Private.expr(wrapper.apply(BuildConfig.Private.expr()));
			return BuildConfig.Private.expr(wrapper);
		}

		/**
		 * <p>Sends a binary data from this builder to socket.</p>
		 *
		 * @return Connection instance.
		 * @throws HttpException if HTTP exception occurred.
		 */
		@SuppressWarnings("RedundantThrows")
		public Connection send() throws HttpException {
			return BuildConfig.Private.expr();
		}

		/**
		 * <p>Callback to modify the builder.</p>
		 */
		public interface Wrapper {
			/**
			 * <p>Modifies the {@code builder}.</p>
			 *
			 * @param builder Initial builder.
			 * @return Modified builder.
			 */
			ComplexBinaryBuilder apply(ComplexBinaryBuilder builder);
		}
	}

	/**
	 * <p>WebSocker connection instance.</p>
	 */
	public class Connection {
		private Connection() {
			BuildConfig.Private.expr();
		}

		/**
		 * <p>Sends a text data to socket.</p>
		 *
		 * @param text Text data.
		 * @return this connection.
		 * @throws HttpException if HTTP exception occurred.
		 */
		@SuppressWarnings("RedundantThrows")
		public Connection sendText(String text) throws HttpException {
			return BuildConfig.Private.expr(text);
		}

		/**
		 * <p>Sends a binary data to socket.</p>
		 *
		 * @param data Binary data.
		 * @return this connection.
		 * @throws HttpException if HTTP exception occurred.
		 */
		@SuppressWarnings("RedundantThrows")
		public Connection sendBinary(byte[] data) throws HttpException {
			return BuildConfig.Private.expr(data);
		}

		/**
		 * <p>Sends a binary data to socket provided by {@link ComplexBinaryBuilder}.</p>
		 *
		 * @return {@link ComplexBinaryBuilder} instance.
		 * @throws HttpException if HTTP exception occurred.
		 */
		@SuppressWarnings("RedundantThrows")
		public ComplexBinaryBuilder sendComplexBinary() throws HttpException {
			return BuildConfig.Private.expr();
		}

		/**
		 * <p>Wait until {@link Event#complete(Object)} call. You can provide multiple {@code results}
		 * and wait until at least one of them will be gotten.</p>
		 *
		 * @param results Array of results to await.
		 * @return this connection.
		 * @throws HttpException if HTTP exception occurred.
		 * @see Event#complete(Object)
		 */
		@SuppressWarnings("RedundantThrows")
		public Connection await(Object... results) throws HttpException {
			return BuildConfig.Private.expr(results);
		}

		/**
		 * <p>Adds data to thread-safe container.</p>
		 *
		 * @param key Data key.
		 * @param data Data value.
		 * @return this connection.
		 */
		public Connection store(String key, Object data) {
			return BuildConfig.Private.expr(key, data);
		}

		/**
		 * <p>Retrieves data from thread-safe container.</p>
		 *
		 * @param key Data key.
		 * @return Data value.
		 */
		public <T> T get(String key) {
			return BuildConfig.Private.expr(key);
		}

		/**
		 * <p>Closes websocket connection.</p>
		 *
		 * @return this connection.
		 * @throws HttpException if HTTP exception occurred.
		 */
		@SuppressWarnings("RedundantThrows")
		public Result close() throws HttpException {
			return BuildConfig.Private.expr();
		}
	}

	/**
	 * <p>Connection result holder.</p>
	 */
	@SuppressWarnings("InnerClassMayBeStatic")
	public class Result {
		/**
		 * <p>Retrieves data from thread-safe container.</p>
		 *
		 * @param key Data key.
		 * @return Data value.
		 */
		public <T> T get(String key) {
			return BuildConfig.Private.expr(key);
		}
	}
}
