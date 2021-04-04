package chan.http;

import android.net.Uri;
import chan.content.ChanConfiguration;
import chan.library.api.BuildConfig;
import java.util.Map;

/**
 * <p>Firewall block resolver.</p>
 *
 * <p>Allows to intercept each HTTP response and perform necessary checks and computations to resolve firewall block,
 * which may involve additional HTTP requests and JavaScript execution.</p>
 */
public abstract class FirewallResolver {
	/**
	 * <p>Request identifier, which allows to group multiple requests depending on request parameters.</p>
	 */
	public static final class Identifier {
		/**
		 * <p>Filter flag to generate string identifier.</p>
		 */
		public enum Flag {
			/**
			 * <p>Include User-Agent header into string identifier.</p>
			 */
			USER_AGENT
		}

		/**
		 * <p>User-Agent header used to execute the request.</p>
		 */
		public final String userAgent = BuildConfig.Private.expr();

		/**
		 * <p>Whether User-Agent is default user agent or one replaced with
		 * {@link HttpRequest#addHeader(String, String)}.</p>
		 */
		public final boolean defaultUserAgent = BuildConfig.Private.expr();

		private Identifier() {
			BuildConfig.Private.expr();
		}
	}

	/**
	 * <p>WebView client which allows to load a page end execute JavaScript to resolve firewall block.</p>
	 *
	 * @param <Result> Result to get from client.
	 */
	public static abstract class WebViewClient<Result> {
		/**
		 * <p>Constructor for {@link WebViewClient}.</p>
		 *
		 * @param name Name to be displayed when captcha input is requested.
		 */
		public WebViewClient(String name) {
			BuildConfig.Private.expr(name);
		}

		/**
		 * <p>Set the result.</p>
		 *
		 * @param result Execution result.
		 */
		public final void setResult(Result result) {
			BuildConfig.Private.expr(result);
		}

		/**
		 * <p>Callback to determine whether resolution is finished.</p>
		 *
		 * <p>This callback is the right place to extract and save necessary cookies.</p>
		 *
		 * @param uri URI by which the page is loaded.
		 * @param cookies Map of cookies present on the page.
		 * @param title Page title.
		 * @return True if resolution is finished (not necessarily successful), false otherwise.
		 */
		public boolean onPageFinished(Uri uri, Map<String, String> cookies, String title) {
			return BuildConfig.Private.expr(uri, cookies, title);
		}

		/**
		 * <p>Callback to filter page content to reduce the amount of loaded data.</p>
		 *
		 * @param initialUri Initial URI which started the request.
		 * @param uri Content URI to determine whether is should be loaded.
		 * @return True is content should be loaded, false otherwise.
		 */
		public boolean onLoad(Uri initialUri, Uri uri) {
			return BuildConfig.Private.expr(initialUri, uri);
		}
	}

	/**
	 * <p>Represents firewall resolution session.</p>
	 */
	public interface Session extends HttpRequest.Preset {
		/**
		 * <p>URI on which the session was started.</p>
		 */
		Uri getUri();

		/**
		 * <p>{@link ChanConfiguration} bound to this chan.</p>
		 */
		<T extends ChanConfiguration> T getChanConfiguration();

		/**
		 * <p>Request identifier.</p>
		 */
		Identifier getIdentifier();

		/**
		 * <p>Formats key to run blocking resolution process exclusively.</p>
		 *
		 * @param flags Flags to generate the key.
		 */
		Exclusive.Key getKey(Identifier.Flag... flags);

		/**
		 * <p>Whether this request will perform solution, or just check whether content is blocked.</p>
		 *
		 * <p>This allows to optimize the process (e.g. not to download the page response) if not needed.</p>
		 */
		boolean isResolveRequest();

		/**
		 * <p>Runs the request in WebView.</p>
		 *
		 * @param webViewClient WebView client to handle callbacks.
		 * @param <Result> Result to get from client.
		 */
		<Result> Result resolveWebView(WebViewClient<Result> webViewClient)
				throws CancelException, InterruptedException;
	}

	/**
	 * <p>Exception is thrown when resolution is cancelled by user.</p>
	 *
	 * <p>This usually happens when loaded page requests captcha, and user denies to solve it.</p>
	 */
	public static final class CancelException extends Exception {
		private CancelException() {
			BuildConfig.Private.expr();
		}
	}

	/**
	 * <p>Exclusive resolution callback, which will block other resolution requests.</p>
	 */
	public interface Exclusive {
		/**
		 * <p>Unique key to run exclusive request.</p>
		 *
		 * <p>This allows to block parallel requests with the same key executed at the same time.</p>
		 *
		 * <p>The key is also used to generate a string to make unique cookie key to store and extract depending
		 * on the request data.</p>
		 */
		interface Key {
			/**
			 * <p>Formats a proper key from {@code value}.</p>
			 *
			 * @param value Value to format.
			 */
			String formatKey(String value);

			/**
			 * <p>Formats a proper user-visible title from {@code value}.</p>
			 *
			 * @param value Value to format.
			 */
			String formatTitle(String value);
		}

		/**
		 * <p>Callback to be executed to resolve firewall block.</p>
		 *
		 * @param session Resolution session.
		 * @param key The block key.
		 * @return True if resolution was successful, false otherwise.
		 */
		boolean resolve(Session session, Key key) throws CancelException, HttpException, InterruptedException;
	}

	/**
	 * <p>Result holder for {@link #checkResponse(Session, HttpResponse)}.</p>
	 */
	public static class CheckResponseResult {
		/**
		 * <p>Constructor for {@link CheckResponseResult}.</p>
		 *
		 * @param key Key to run resolution exclusively.
		 * @param exclusive Callback to run resolution.
		 */
		public CheckResponseResult(Exclusive.Key key, Exclusive exclusive) {
			BuildConfig.Private.expr(key, exclusive);
		}

		/**
		 * <p>Specify whether request entity should be retransmitted upon successful resolution.</p>
		 *
		 * @param retransmitOnSuccess Whether request entity should be retransmitted.
		 * @return This object.
		 * @see HttpRequest.RedirectHandler.Action#RETRANSMIT
		 */
		public CheckResponseResult setRetransmitOnSuccess(boolean retransmitOnSuccess) {
			return BuildConfig.Private.expr(retransmitOnSuccess);
		}
	}

	/**
	 * <p>Callback to check or resolve firewall block.</p>
	 *
	 * <p>When it's determined that response contains firewall block data, a {@link CheckResponseResult}
	 * instance may be returned to resolve firewall block.</p>
	 *
	 * @param session Resolution session.
	 * @param response HTTP response to check.
	 * @return {@link CheckResponseResult} instance, or null.
	 */
	public abstract CheckResponseResult checkResponse(Session session, HttpResponse response) throws HttpException;

	/**
	 * <p>Callback to collect required cookies to run any request.</p>
	 *
	 * @param session Resolution session.
	 * @param cookieBuilder Builder to collect cookies to.
	 */
	public void collectCookies(Session session, CookieBuilder cookieBuilder) {
		BuildConfig.Private.expr(session, cookieBuilder);
	}
}
