package chan.content;

import android.net.Uri;
import chan.library.api.BuildConfig;
import java.util.regex.Pattern;

/**
 * <p>Provides URI handling and building.</p>
 *
 * <p>In the first you must declare chan hosts. You can do this using {@link ChanLocator#addChanHost(String)} method.
 * If you add more than one host, user can choice one of them in preferences.</p>
 *
 * <p>If you want to add special host that user might not choice, use {@link ChanLocator#addSpecialChanHost(String)}.
 * This method is used for special hosts like JSON API or host for static data, for example.</p>
 *
 * <p>There is the list of methods you <strong>must</strong> override:</p>
 *
 * <ul>
 * <li>{@link ChanLocator#isBoardUri(Uri)}</li>
 * <li>{@link ChanLocator#isThreadUri(Uri)}</li>
 * <li>{@link ChanLocator#isAttachmentUri(Uri)}</li>
 * <li>{@link ChanLocator#getBoardName(Uri)}</li>
 * <li>{@link ChanLocator#getThreadNumber(Uri)}</li>
 * <li>{@link ChanLocator#getPostNumber(Uri)}</li>
 * <li>{@link ChanLocator#createBoardUri(String, int)}</li>
 * <li>{@link ChanLocator#createThreadUri(String, String)}</li>
 * <li>{@link ChanLocator#createPostUri(String, String, String)}</li>
 * </ul>
 *
 * <p>URI building with preferred configuration provided by the following methods:</p>
 *
 * <ul>
 * <li>{@link ChanLocator#buildPath(String...)}</li>
 * <li>{@link ChanLocator#buildPathWithHost(String, String...)}</li>
 * <li>{@link ChanLocator#buildPathWithSchemeHost(boolean, String, String...)}</li>
 * <li>{@link ChanLocator#buildQuery(String, String...)}</li>
 * <li>{@link ChanLocator#buildQueryWithHost(String, String, String...)}</li>
 * <li>{@link ChanLocator#buildQueryWithSchemeHost(boolean, String, String, String...)}</li>
 * </ul>
 */
public class ChanLocator {
	/**
	 * <p>HTTPS mode, used in {@link #setHttpsMode(HttpsMode)} method.</p>
	 */
	public enum HttpsMode {
		/**
		 * <p>HTTPS is not used. All URI's will be built with HTTP scheme by default.</p>
		 */
		NO_HTTPS,

		/**
		 * <p>HTTPS is enabled. All URI's will be built with HTTPS scheme by default.</p>
		 */
		HTTPS_ONLY,

		/**
		 * <p>User can change HTTPS mode in preferences.</p>
		 */
		CONFIGURABLE
	}

	/**
	 * <p>Navigation data holder. Used in {@link #handleUriClickSpecial(Uri)} method.</p>
	 */
	public static final class NavigationData {
		/**
		 * <p>Target to list of threads.</p>
		 */
		public static final int TARGET_THREADS = BuildConfig.Private.expr();

		/**
		 * <p>Target to list of posts.</p>
		 */
		public static final int TARGET_POSTS = BuildConfig.Private.expr();

		/**
		 * <p>Target to list of search results. You <strong>must</strong> enable
		 * {@link ChanConfiguration.Board#allowSearch} option for specified board to use this target.</p>
		 */
		public static final int TARGET_SEARCH = BuildConfig.Private.expr();

		/**
		 * @param target Can take the values {@link #TARGET_THREADS}, {@link #TARGET_POSTS} or {@link #TARGET_SEARCH}.
		 * @param boardName Board name.
		 * @param threadNumber Thread number (must be not null for target == {@link #TARGET_POSTS}).
		 * @param postNumber Post number.
		 * @param searchQuery Search query (must be not null for target == {@link #TARGET_SEARCH}).
		 */
		public NavigationData(int target, String boardName, String threadNumber, String postNumber,
				String searchQuery) {
			BuildConfig.Private.expr(target, boardName, threadNumber, postNumber, searchQuery);
		}
	}

	/**
	 * <p>Return linked {@link ChanLocator} instance.
	 *
	 * @param object Linked object: {@link ChanConfiguration}, {@link ChanPerformer},
	 * {@link ChanLocator} or {@link ChanMarkup}.
	 * @return {@link ChanLocator} instance.
	 */
	public static <T extends ChanLocator> T get(Object object) {
		return BuildConfig.Private.expr(object);
	}

	/**
	 * <p>Declares host as chan host. This host might be default host in {@link #buildPath(String...)} and
	 * {@link #buildQuery(String, String...)} methods. If you declare multiple hosts, user can choice one of them
	 * in preferences. The first declared host will be chosen by default.</p>
	 *
	 * <p>For example, a chan has 3 addresses: {@code addr1.com}, {@code addr2.com}, {@code addr3.com}. If user choose
	 * {@code addr2.com} in preferences, URIs with the rest addresses will be converted to {@code addr2.com}.</p>
	 */
	public final void addChanHost(String host) {
		BuildConfig.Private.expr(host);
	}

	/**
	 * <p>Declares host as chan host. Unlike {@link #addChanHost(String)} user can't choice this host in preferences,
	 * but it still can be converted. For example, it can be useful for old domains that don't work now.</p>
	 */
	public final void addConvertableChanHost(String host) {
		BuildConfig.Private.expr(host);
	}

	/**
	 * <p>Declares host as chan host. Unlike {@link #addChanHost(String)} user can't choice this host in preferences
	 * and and can't be converted. For example, it can be useful for special hosts like JSON API or static data.</p>
	 */
	public final void addSpecialChanHost(String host) {
		BuildConfig.Private.expr(host);
	}

	/**
	 * <p>Changes default HTTPS mode. By default it equals {@link HttpsMode#NO_HTTPS}.</p>
	 *
	 * @see HttpsMode
	 */
	public final void setHttpsMode(HttpsMode httpsMode) {
		BuildConfig.Private.expr(httpsMode);
	}

	/**
	 * <p>Returns whether HTTPS enabled in preferences.</p>
	 *
	 * @return True if HTTPS enabled.
	 */
	public final boolean isUseHttps() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Returns whether URI's host is chan host or URI is relative (URI without scheme and host). This method will
	 * return true for all URI's with hosts declared with {@link #addChanHost(String)} or
	 * {@link #addSpecialChanHost(String)} methods and all relative URIs.</p>
	 *
	 * @return True if host is chan host or relative.
	 */
	public final boolean isChanHostOrRelative(Uri uri) {
		return BuildConfig.Private.expr(uri);
	}

	/**
	 * <p>Overriding this method allows developer to handle multiple subdomains.</p>
	 *
	 * <p>For example, {@code myimageboard.org} is a primary domain and {@code images.myimageboard.org} is used
	 * to store posted images. User set the domain in settings to {@code alternativedomain.org} which also uses
	 * {@code images.alternativedomain.org} for images. Extension's developer can handle this situation overriging
	 * this method.</p>
	 *
	 * <p>In this example {@code chanHost} will be {@code alternativedomain.org}, but {@code requiredHost} may be
	 * {@code images.myimageboard.org} or {@code images.alternativedomain.org}. Developer should take into account
	 * all these cases.</p>
	 *
	 * @param chanHost Host set by user.
	 * @param requiredHost A host to transit from.
	 * @return Resulting host or {@code null}.
	 */
	public String getHostTransition(String chanHost, String requiredHost) {
		return BuildConfig.Private.expr(chanHost, requiredHost);
	}

	/**
	 * <p>Returns whether URI is board URI. You <strong>must</strong> override this method.</p>
	 *
	 * @param uri URI to inspect.
	 * @return True if URI is board URI.
	 */
	public boolean isBoardUri(Uri uri) {
		return BuildConfig.Private.expr(uri);
	}

	/**
	 * <p>Returns whether URI is thread URI. You <strong>must</strong> override this method.</p>
	 *
	 * @param uri URI to inspect.
	 * @return True if URI is thread URI.
	 */
	public boolean isThreadUri(Uri uri) {
		return BuildConfig.Private.expr(uri);
	}

	/**
	 * <p>Returns whether URI is attachment URI. You <strong>must</strong> override this method.</p>
	 *
	 * @param uri URI to inspect.
	 * @return True if URI is attachment URI.
	 */
	public boolean isAttachmentUri(Uri uri) {
		return BuildConfig.Private.expr(uri);
	}

	/**
	 * <p>Returns board name from given URI. You <strong>must</strong> override this method.</p>
	 *
	 * @param uri URI to inspect.
	 * @return Board name.
	 */
	public String getBoardName(Uri uri) {
		return BuildConfig.Private.expr(uri);
	}

	/**
	 * <p>Returns thread number from given URI. You <strong>must</strong> override this method.</p>
	 *
	 * @param uri URI to inspect.
	 * @return Thread number.
	 */
	public String getThreadNumber(Uri uri) {
		return BuildConfig.Private.expr(uri);
	}

	/**
	 * <p>Returns post number from given URI. You <strong>must</strong> override this method.</p>
	 *
	 * @param uri URI to inspect.
	 * @return Posts number.
	 */
	public String getPostNumber(Uri uri) {
		return BuildConfig.Private.expr(uri);
	}

	/**
	 * <p>Calls when client intend to create board URI. You <strong>must</strong> override this method.</p>
	 *
	 * @param boardName Board name.
	 * @param pageNumber Number of page, might be {@link ChanPerformer.ReadThreadsData#PAGE_NUMBER_CATALOG}.
	 * @return Board URI.
	 */
	public Uri createBoardUri(String boardName, int pageNumber) {
		return BuildConfig.Private.expr(boardName, pageNumber);
	}

	/**
	 * <p>Calls when client intend to create thread URI. You <strong>must</strong> override this method.</p>
	 *
	 * @param boardName Board name.
	 * @param threadNumber Thread number.
	 * @return Thread URI.
	 */
	public Uri createThreadUri(String boardName, String threadNumber) {
		return BuildConfig.Private.expr(boardName, threadNumber);
	}

	/**
	 * <p>Calls when client intend to create thread URI with anchor to post.
	 * You <strong>must</strong> override this method.</p>
	 *
	 * @param boardName Board name.
	 * @param threadNumber Thread number.
	 * @param postNumber Post number.
	 * @return Post URI.
	 */
	public Uri createPostUri(String boardName, String threadNumber, String postNumber) {
		return BuildConfig.Private.expr(boardName, threadNumber);
	}

	/**
	 * <p>Calls when client intend to obtain a file name from URI. By default client obtains a name from last
	 * path segment of URI. You can override this behavior using this method.</p>
	 *
	 * @param fileUri file URI
	 * @return File name.
	 */
	public String createAttachmentForcedName(Uri fileUri) {
		return BuildConfig.Private.expr(fileUri);
	}

	/**
	 * <p>Calls when client intend to handle link click. You can return {@link NavigationData} instance
	 * with necessary navigation information.</p>
	 *
	 * @param uri URI to inspect.
	 * @return {@link NavigationData} instance or null.
	 */
	public NavigationData handleUriClickSpecial(Uri uri) {
		return BuildConfig.Private.expr(uri);
	}

	/**
	 * <p>Returns whether path has image extension.</p>
	 *
	 * @param path Path to inspect.
	 * @return True if extension is image's.
	 */
	public final boolean isImageExtension(String path) {
		return BuildConfig.Private.expr(path);
	}

	/**
	 * <p>Returns whether path has audio extension.</p>
	 *
	 * @param path Path to inspect.
	 * @return True if extension is audio's.
	 */
	public final boolean isAudioExtension(String path) {
		return BuildConfig.Private.expr(path);
	}

	/**
	 * <p>Returns whether path has video extension.</p>
	 *
	 * @param path Path to inspect.
	 * @return True if extension is video's.
	 */
	public final boolean isVideoExtension(String path) {
		return BuildConfig.Private.expr(path);
	}

	/**
	 * <p>Returns extension of file with given path.</p>
	 *
	 * @param path Path to inspect.
	 * @return File extension in lower case.
	 */
	public final String getFileExtension(String path) {
		return BuildConfig.Private.expr(path);
	}

	/**
	 * <p>Builds URI with given path segments and preferred host and scheme.</p>
	 *
	 * @param segments Path segments.
	 * @return URI.
	 */
	public final Uri buildPath(String... segments) {
		return BuildConfig.Private.expr(segments);
	}

	/**
	 * <p>Builds URI with given host and path segments and preferred scheme.</p>
	 *
	 * @param host URI host.
	 * @param segments Path segments.
	 * @return URI.
	 */
	public final Uri buildPathWithHost(String host, String... segments) {
		return BuildConfig.Private.expr(host, segments);
	}

	/**
	 * <p>Builds URI with given scheme, host and path segments.</p>
	 *
	 * @param useHttps Defines whether use HTTPS or not.
	 * @param host URI host.
	 * @param segments Path segments.
	 * @return URI.
	 */
	public final Uri buildPathWithSchemeHost(boolean useHttps, String host, String... segments) {
		return BuildConfig.Private.expr(useHttps, host, segments);
	}

	/**
	 * <p>Builds URI with given path and parameters and preferred host and scheme.</p>
	 *
	 * @param path URI path.
	 * @param alternation Alternation of param's names and values (name, value, name, value...).
	 * @return URI.
	 */
	public final Uri buildQuery(String path, String... alternation) {
		return BuildConfig.Private.expr(path, alternation);
	}

	/**
	 * <p>Builds URI with given host, path and parameters and preferred scheme.</p>
	 *
	 * @param host URI host.
	 * @param path URI path.
	 * @param alternation Alternation of param's names and values (name, value, name, value...).
	 * @return URI.
	 */
	public final Uri buildQueryWithHost(String host, String path, String... alternation) {
		return BuildConfig.Private.expr(host, path, alternation);
	}

	/**
	 * <p>Builds URI with given scheme, host, path and parameters.</p>
	 *
	 * @param useHttps Defines whether use HTTPS or not.
	 * @param host URI host.
	 * @param path URI path.
	 * @param alternation Alternation of param's names and values (name, value, name, value...).
	 * @return URI.
	 */
	public final Uri buildQueryWithSchemeHost(boolean useHttps, String host, String path, String... alternation) {
		return BuildConfig.Private.expr(useHttps, host, path, alternation);
	}

	/**
	 * <p>Returns whether URI's path matches to given pattern.</p>
	 *
	 * @param uri URI to inspect.
	 * @param pattern Pattern to match.
	 * @return True if URI's path matches to pattern.
	 */
	public final boolean isPathMatches(Uri uri, Pattern pattern) {
		return BuildConfig.Private.expr(uri, pattern);
	}

	/**
	 * <p>Finds given pattern in string and returns group by index.</p>
	 *
	 * @param from String to inspect.
	 * @param pattern Pattern to find.
	 * @param groupIndex Index of group.
	 * @return First found value in string by group index.
	 */
	public final String getGroupValue(String from, Pattern pattern, int groupIndex) {
		return BuildConfig.Private.expr(from, pattern, groupIndex);
	}
}
