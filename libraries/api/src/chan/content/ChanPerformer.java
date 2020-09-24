package chan.content;

import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Pair;
import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.content.model.ThreadSummary;
import chan.http.HttpException;
import chan.http.HttpHolder;
import chan.http.HttpRequest;
import chan.http.HttpResponse;
import chan.http.HttpValidator;
import chan.http.MultipartEntity;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

/**
 * <p>Provides performing connectivity with chan.</p>
 *
 * <p>By default you must implement the following methods:</p>
 *
 * <ul>
 * <li>{@link #onReadThreads(ReadThreadsData)}</li>
 * <li>{@link #onReadPosts(ReadPostsData)}</li>
 * <li>{@link #onReadBoards(ReadBoardsData)}</li>
 * </ul>
 *
 * <p>Depending one the {@link ChanConfiguration.Board} configuration you must implement the following methods:</p>
 *
 * <table summary="">
 * <tr><th>Option</th><th>Method</th></tr>
 * <tr><td>{@link ChanConfiguration.Board#allowSearch}</td>
 * <td>{@link #onReadSearchPosts(ReadSearchPostsData)}</td></tr>
 * <tr><td>{@link ChanConfiguration.Board#allowCatalog}</td>
 * <td>{@link #onReadThreads(ReadThreadsData)} (see description)</td></tr>
 * <tr><td>{@link ChanConfiguration.Board#allowArchive}</td>
 * <td>{@link #onReadThreadSummaries(ReadThreadSummariesData)}</td></tr>
 * <tr><td>{@link ChanConfiguration.Board#allowPosting}</td>
 * <td>{@link #onReadCaptcha(ReadCaptchaData)}</td></tr>
 * <tr><td>{@link ChanConfiguration.Board#allowPosting}</td>
 * <td>{@link #onSendPost(SendPostData)}</td></tr>
 * <tr><td>{@link ChanConfiguration.Board#allowDeleting}</td>
 * <td>{@link #onSendDeletePosts(SendDeletePostsData)}</td></tr>
 * <tr><td>{@link ChanConfiguration.Board#allowReporting}</td>
 * <td>{@link #onSendReportPosts(SendReportPostsData)}</td></tr>
 * </table>
 *
 * <p>If you configure some options, you also must implement the following methods:</p>
 *
 * <table summary="">
 * <tr><th>Option</th><th>Method</th></tr>
 * <tr><td>{@link ChanConfiguration#OPTION_READ_SINGLE_POST}</td>
 * <td>{@link #onReadSinglePost(ReadSinglePostData)}</td></tr>
 * <tr><td>{@link ChanConfiguration#OPTION_READ_POSTS_COUNT}</td>
 * <td>{@link #onReadPostsCount(ReadPostsCountData)}</td></tr>
 * <tr><td>{@link ChanConfiguration#OPTION_READ_USER_BOARDS}</td>
 * <td>{@link #onReadUserBoards(ReadUserBoardsData)}</td></tr>
 * <tr><td>{@link ChanConfiguration#OPTION_ALLOW_CAPTCHA_PASS}</td>
 * <td>{@link #onCheckAuthorization(CheckAuthorizationData)}</td></tr>
 * <tr><td>{@link ChanConfiguration#OPTION_ALLOW_USER_AUTHORIZATION}</td>
 * <td>{@link #onCheckAuthorization(CheckAuthorizationData)}</td></tr>
 * </table>
 */
public class ChanPerformer {
	/**
	 * <p>Return linked {@link ChanPerformer} instance.
	 *
	 * @param object Linked object: {@link ChanConfiguration}, {@link ChanPerformer},
	 * {@link ChanLocator} or {@link ChanMarkup}.
	 * @return {@link ChanPerformer} instance.
	 */
	public static <T extends ChanPerformer> T get(Object object) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Calls when application requests chan to download threads list.</p>
	 *
	 * <p>This method <strong>must</strong> be overridden.</p>
	 *
	 * <p>Option {@link ChanConfiguration.Board#allowCatalog} allows client to download catalog of threads.
	 * Use {@link ReadThreadsData#isCatalog()} to determine whether it necessary to do.</p>
	 *
	 * @param data {@link ReadThreadsData} instance with arguments.
	 * @return {@link ReadThreadsResult} instance.
	 * @throws HttpException if HTTP or another error with message occurred.
	 * @throws InvalidResponseException if server returned an invalid data.
	 * @throws RedirectException if server returned data that can be considered as redirect.
	 */
	public ReadThreadsResult onReadThreads(ReadThreadsData data) throws HttpException, InvalidResponseException,
			RedirectException {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Calls when application requests chan to download posts list.</p>
	 *
	 * <p>This method <strong>must</strong> be overridden.</p>
	 *
	 * <p>Option {@link ChanConfiguration#OPTION_READ_THREAD_PARTIALLY} allows you to download threads partially.
	 * You can check {@link ReadPostsData#partialThreadLoading} flag and download only posts after
	 * {@link ReadPostsData#lastPostNumber}.</p>
	 *
	 * @param data {@link ReadPostsData} instance with arguments.
	 * @return {@link ReadPostsResult} instance.
	 * @throws HttpException if HTTP or another error with message occurred.
	 * @throws InvalidResponseException if server returned an invalid data.
	 * @throws RedirectException if server returned data that can be considered as redirect.
	 */
	public ReadPostsResult onReadPosts(ReadPostsData data) throws HttpException, InvalidResponseException,
			RedirectException {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Calls when application requests chan to download single post.</p>
	 *
	 * <p>This method <strong>must</strong> be overridden if chan supports
	 * {@link ChanConfiguration#OPTION_READ_SINGLE_POST}.</p>
	 *
	 * @param data {@link ReadSinglePostData} instance with arguments.
	 * @return {@link ReadSinglePostResult} instance.
	 * @throws HttpException if HTTP or another error with message occurred.
	 * @throws InvalidResponseException if server returned an invalid data.
	 */
	public ReadSinglePostResult onReadSinglePost(ReadSinglePostData data) throws HttpException,
			InvalidResponseException {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Calls when application requests chan to download search posts list.</p>
	 *
	 * <p>This method <strong>must</strong> be overridden if option {@link ChanConfiguration.Board#allowSearch} is
	 * enabled for this board.</p>
	 *
	 * @param data {@link ReadSearchPostsData} instance with arguments.
	 * @return {@link ReadSearchPostsResult} instance.
	 * @throws HttpException if HTTP or another error with message occurred.
	 * @throws InvalidResponseException if server returned an invalid data.
	 */
	public ReadSearchPostsResult onReadSearchPosts(ReadSearchPostsData data) throws HttpException,
			InvalidResponseException {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Calls when application requests chan to download boards list.</p>
	 *
	 * <p>This method <strong>must</strong> be overridden if option
	 * {@link ChanConfiguration#OPTION_SINGLE_BOARD_MODE} is <strong>not</strong> enabled.</p>
	 *
	 * @param data {@link ReadBoardsData} instance with arguments.
	 * @return {@link ReadBoardsResult} instance.
	 * @throws HttpException if HTTP or another error with message occurred.
	 * @throws InvalidResponseException if server returned an invalid data.
	 */
	public ReadBoardsResult onReadBoards(ReadBoardsData data) throws HttpException, InvalidResponseException {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Calls when application requests chan to download user boards list.</p>
	 *
	 * <p>This method <strong>must</strong> be overridden if option
	 * {@link ChanConfiguration#OPTION_READ_USER_BOARDS} is enabled.</p>
	 *
	 * @param data {@link ReadUserBoardsData} instance with arguments.
	 * @return {@link ReadUserBoardsResult} instance.
	 * @throws HttpException if HTTP or another error with message occurred.
	 * @throws InvalidResponseException if server returned an invalid data.
	 */
	public ReadUserBoardsResult onReadUserBoards(ReadUserBoardsData data) throws HttpException,
			InvalidResponseException {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Calls when application requests chan to download thread summaries.
	 * You can check {@link ReadThreadSummariesData#type} field and return necessary data.</p>
	 *
	 * <p>This method <strong>must</strong> be overridden in the following cases:</p>
	 *
	 * <ul>
	 * <li>{@link ChanConfiguration.Board#allowArchive} enabled</li>
	 * </ul>
	 *
	 * @param data {@link ReadThreadSummariesData} instance with arguments.
	 * @return {@link ReadThreadSummariesResult} instance.
	 * @throws HttpException if HTTP or another error with message occurred.
	 * @throws InvalidResponseException if server returned an invalid data.
	 */
	public ReadThreadSummariesResult onReadThreadSummaries(ReadThreadSummariesData data) throws HttpException,
			InvalidResponseException {
		throw new UnsupportedOperationException();
	}

	/**
	 * <p>Calls when application requests chan to download posts count.</p>
	 *
	 * <p>This method is not the same as {@link #onReadPosts(ReadPostsData)}. This method must download
	 * and parse data quickly as possible.</p>
	 *
	 * <p>This method <strong>must</strong> be overridden if option
	 * {@link ChanConfiguration#OPTION_READ_POSTS_COUNT} is enabled.</p>
	 *
	 * @param data {@link ReadPostsCountData} instance with arguments.
	 * @return {@link ReadPostsCountResult} instance.
	 * @throws HttpException if HTTP or another error with message occurred.
	 * @throws InvalidResponseException if server returned an invalid data.
	 */
	public ReadPostsCountResult onReadPostsCount(ReadPostsCountData data) throws HttpException,
			InvalidResponseException {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Calls when application requests chan to download thumbnail, image, etc.</p>
	 *
	 * @param data {@link ReadContentData} instance with arguments.
	 * @return {@link ReadContentResult} instance.
	 * @throws HttpException if HTTP or another error with message occurred.
	 * @throws InvalidResponseException if server returned an invalid data.
	 */
	public ReadContentResult onReadContent(ReadContentData data) throws HttpException, InvalidResponseException {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Calls when application requests chan to check authorization data.</p>
	 *
	 * <p>This method <strong>must</strong> be overridden in the following cases:</p>
	 *
	 * <ul>
	 * <li>{@link ChanConfiguration#OPTION_ALLOW_CAPTCHA_PASS} enabled</li>
	 * <li>{@link ChanConfiguration#OPTION_ALLOW_USER_AUTHORIZATION} enabled</li>
	 * </ul>
	 *
	 * @param data {@link CheckAuthorizationData} instance with arguments.
	 * @return {@link CheckAuthorizationResult} instance.
	 * @throws HttpException if HTTP or another error with message occurred.
	 * @throws InvalidResponseException if server returned an invalid data.
	 */
	public CheckAuthorizationResult onCheckAuthorization(CheckAuthorizationData data) throws HttpException,
			InvalidResponseException {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Calls when application requests chan to read captcha.</p>
	 *
	 * <p>This method <strong>must</strong> be overridden if option {@link ChanConfiguration.Board#allowPosting} is
	 * enabled for this board.</p>
	 *
	 * <p>This method must return {@link ReadCaptchaResult} with captcha data.</p>
	 *
	 * <p>If your chan uses custom captcha, you must specify a resulting image using
	 * {@link ReadCaptchaResult#setImage(Bitmap)}.</p>
	 *
	 * <p>In the case of Yandex Captcha, resulting {@code captchaData} must contain challenge string by
	 * {@link CaptchaData#CHALLENGE}. key</p>
	 *
	 * <p>In the case of Google reCAPTCHA and Mail.Ru Nocaptcha, resulting {@code captchaData} must contain API key by
	 * {@link CaptchaData#API_KEY}. key</p>
	 *
	 * <p>If your captcha has short lifetime, you can check {@link ReadCaptchaData#mayShowLoadButton}.
	 * If this argument equals {@code true}, the result may hold {@link CaptchaState#NEED_LOAD}. This will
	 * show "Click to load captcha" button for user.</p>
	 *
	 * @param data {@link ReadCaptchaData} instance with arguments.
	 * @return {@link ReadCaptchaResult} instance.
	 * @throws HttpException if HTTP or another error with message occurred.
	 * @throws InvalidResponseException if server returned an invalid data.
	 */
	public ReadCaptchaResult onReadCaptcha(ReadCaptchaData data) throws HttpException, InvalidResponseException {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Calls when application requests chan to send post.</p>
	 *
	 * <p>You can throw {@link ApiException} with {@code SEND_*} error types.</p>
	 *
	 * @param data {@link SendPostData} instance with arguments.
	 * @return {@link SendPostResult} instance.
	 * @throws HttpException if HTTP or another error with message occurred.
	 * @throws ApiException if sending wasn't complete due to user errors.
	 * @throws InvalidResponseException if server returned an invalid data.
	 */
	public SendPostResult onSendPost(SendPostData data) throws HttpException, ApiException, InvalidResponseException {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Calls when application requests chan to delete posts.</p>
	 *
	 * <p>This method <strong>must</strong> be overridden if option {@link ChanConfiguration.Board#allowDeleting} is
	 * enabled for this board.</p>
	 *
	 * <p>{@link SendDeletePostsData#postNumbers} contains several post numbers. You must perform deleting all of these
	 * posts. <strong>You must throw {@link ApiException} only in case if nothing was deleted</strong>. In other cases
	 * you must return and throw nothing.</p>
	 *
	 * <p>You can throw {@link ApiException} with {@code DELETE_*} error types.</p>
	 *
	 * @param data {@link SendDeletePostsData} instance with arguments.
	 * @return {@link SendDeletePostsResult} instance.
	 * @throws HttpException if HTTP or another error with message occurred.
	 * @throws ApiException if deleting wasn't complete due to user errors.
	 * @throws InvalidResponseException if server returned an invalid data.
	 */
	public SendDeletePostsResult onSendDeletePosts(SendDeletePostsData data) throws HttpException, ApiException,
			InvalidResponseException {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Calls when application requests chan to send report.</p>
	 *
	 * <p>This method <strong>must</strong> be overridden if option {@link ChanConfiguration.Board#allowReporting} is
	 * enabled for this board.</p>
	 *
	 * <p>You can throw {@link ApiException} with {@code REPORT_*} error types.</p>
	 *
	 * @param data {@link SendDeletePostsData} instance with arguments.
	 * @return {@link SendReportPostsResult} instance.
	 * @throws HttpException if HTTP or another error with message occurred.
	 * @throws ApiException if reporting wasn't complete due to user errors.
	 * @throws InvalidResponseException if server returns an invalid data.
	 */
	public SendReportPostsResult onSendReportPosts(SendReportPostsData data) throws HttpException, ApiException,
			InvalidResponseException {
		throw new UnsupportedOperationException();
	}

	/**
	 * <p>Calls when application requests chan to add thread to archive.</p>
	 *
	 * <p>This method <strong>must</strong> be overridden if chan implements archivation.</p>
	 *
	 * <p>You can throw {@link ApiException} with {@code ARCHIVE_*} error types.</p>
	 *
	 * @param data {@link SendAddToArchiveData} instance with arguments.
	 * @return {@link SendAddToArchiveResult} instance.
	 * @throws ApiException if archivation wasn't complete due to user errors. It can hold error code,
	 * see {@link ApiException}.
	 * @throws InvalidResponseException if server returns an invalid data.
	 */
	public SendAddToArchiveResult onSendAddToArchive(SendAddToArchiveData data) throws HttpException, ApiException,
			InvalidResponseException {
		throw new UnsupportedOperationException();
	}

	/**
	 * <p>Arguments holder for {@link #onReadThreads(ReadThreadsData)}. Notify that this class
	 * might be used as {@link HttpRequest.Preset}.</p>
	 */
	public static class ReadThreadsData implements HttpRequest.Preset {
		/**
		 * <p>Possible value of {@link #pageNumber}. In this case application requests catalog page.</p>
		 */
		public static final int PAGE_NUMBER_CATALOG = -1;

		/**
		 * <p>Board name argument.</p>
		 */
		public final String boardName;

		/**
		 * <p>Page number argument.</p>
		 */
		public final int pageNumber;

		/**
		 * <p>HTTP holder. You must use it when building new {@link HttpRequest}.</p>
		 */
		public final HttpHolder holder;

		/**
		 * <p>HTTP validator. With this argument you can check if page was changed.</p>
		 */
		public final HttpValidator validator;

		ReadThreadsData() {
			throw new IllegalAccessError();
		}

		/**
		 * <p>Returns whether need to load catalog page. This method just compares {@link #pageNumber} with
		 * {@link #PAGE_NUMBER_CATALOG}.</p>
		 *
		 * @return True if application requests a catalog page.
		 */
		public boolean isCatalog() {
			throw new IllegalAccessError();
		}
	}

	/**
	 * <p>Result holder for {@link #onReadThreads(ReadThreadsData)}.</p>
	 */
	public static final class ReadThreadsResult {
		/**
		 * <p>Constructor for {@link ReadThreadsResult}.</p>
		 *
		 * @param threads Array of {@link Posts}.
		 */
		public ReadThreadsResult(Posts... threads) {
			throw new IllegalAccessError();
		}

		/**
		 * <p>Constructor for {@link ReadThreadsResult}.</p>
		 *
		 * @param threads Collection of {@link Posts}.
		 */
		public ReadThreadsResult(Collection<Posts> threads) {
			throw new IllegalAccessError();
		}

		/**
		 * <p>Stores board speed value (number of posts per hour) in this result.</p>
		 *
		 * @param boardSpeed Number of posts per hour.
		 * @return This object.
		 */
		public ReadThreadsResult setBoardSpeed(int boardSpeed) {
			throw new IllegalAccessError();
		}

		/**
		 * <p>Stores {@code validator} in this result. By default client will call {@link HttpHolder#getValidator()}
		 * after the last request. This can be useful if you want to store validator from intermediate request.</p>
		 *
		 * @param validator {@link HttpValidator} instance.
		 * @return This object.
		 */
		public ReadThreadsResult setValidator(HttpValidator validator) {
			throw new IllegalAccessError();
		}
	}

	/**
	 * <p>Arguments holder for {@link #onReadPosts(ReadPostsData)}. Notify that this class might be used
	 * as {@link HttpRequest.Preset}.</p>
	 */
	public static class ReadPostsData implements HttpRequest.Preset {
		/**
		 * <p>Board name argument.</p>
		 */
		public final String boardName;

		/**
		 * <p>Thread number argument.</p>
		 */
		public final String threadNumber;

		/**
		 * <p>Last post number argument. Used when partial thread loading enabled.</p>
		 */
		public final String lastPostNumber;

		/**
		 * <p>Defines whether use partial thread loading or not.</p>
		 */
		public final boolean partialThreadLoading;

		/**
		 * <p>Current cached posts model. <strong>Do not modity this model!</strong> This model is used
		 * only for reading.</p>
		 */
		public final Posts cachedPosts;

		/**
		 * <p>HTTP holder. You must use it when building new {@link HttpRequest}.</p>
		 */
		public final HttpHolder holder;

		/**
		 * <p>HTTP validator. With this argument you can check if page was changed.</p>
		 */
		public final HttpValidator validator;

		ReadPostsData() {
			throw new IllegalAccessError();
		}
	}

	/**
	 * <p>Result holder for {@link #onReadPosts(ReadPostsData)}.</p>
	 */
	public static final class ReadPostsResult {
		/**
		 * <p>Constructor for {@link ReadPostsResult}.</p>
		 *
		 * @param posts {@link Posts} model instance.
		 */
		public ReadPostsResult(Posts posts) {
			throw new IllegalAccessError();
		}

		/**
		 * <p>Constructor for {@link ReadPostsResult}. Will create {@link Posts} instance from your
		 * array of {@link Post}.</p>
		 *
		 * @param posts Array of {@link Post}.
		 */
		public ReadPostsResult(Post... posts) {
			throw new IllegalAccessError();
		}

		/**
		 * <p>Constructor for {@link ReadPostsResult}. Will create {@link Posts} instance from your
		 * collection of {@link Post}.</p>
		 *
		 * @param posts Collection of {@link Post}.
		 */
		public ReadPostsResult(Collection<Post> posts) {
			throw new IllegalAccessError();
		}

		/**
		 * <p>Stores {@code validator} in this cache. By default client will call {@link HttpHolder#getValidator()}
		 * after the last request. This can be useful if you want to store validator from intermediate request.</p>
		 *
		 * @param validator {@link HttpValidator} instance.
		 * @return This object.
		 */
		public ReadPostsResult setValidator(HttpValidator validator) {
			throw new IllegalAccessError();
		}

		/**
		 * <p>Allow client to handle result as a full thread even when partial result requested.</p>
		 *
		 * @param fullThread True if client must handle result as full thread, false otherwise.
		 * @return This object.
		 */
		public ReadPostsResult setFullThread(boolean fullThread) {
			throw new IllegalAccessError();
		}
	}

	/**
	 * <p>Arguments holder for {@link #onReadSinglePost(ReadSinglePostData)}. Notify that this class might
	 * be used as {@link HttpRequest.Preset}.</p>
	 */
	public static class ReadSinglePostData implements HttpRequest.Preset {
		/**
		 * <p>Board name argument.</p>
		 */
		public final String boardName;

		/**
		 * <p>Post number argument.</p>
		 */
		public final String postNumber;

		/**
		 * <p>HTTP holder. You must use it when building new {@link HttpRequest}.</p>
		 */
		public final HttpHolder holder;

		ReadSinglePostData() {
			throw new IllegalAccessError();
		}
	}

	/**
	 * <p>Result holder for {@link #onReadSinglePost(ReadSinglePostData)}.</p>
	 */
	public static final class ReadSinglePostResult {
		/**
		 * <p>Constructor for {@link ReadSinglePostResult}.</p>
		 *
		 * @param post {@link Post} model instance.
		 */
		public ReadSinglePostResult(Post post) {
			throw new IllegalAccessError();
		}
	}

	/**
	 * <p>Arguments holder for {@link #onReadSearchPosts(ReadSearchPostsData)}. Notify that this class
	 * might be used as {@link HttpRequest.Preset}.</p>
	 */
	public static class ReadSearchPostsData implements HttpRequest.Preset {
		/**
		 * <p>Board name argument.</p>
		 */
		public final String boardName;

		/**
		 * <p>Search query argument.</p>
		 */
		public final String searchQuery;

		/**
		 * <p>Page number argument.</p>
		 */
		public final int pageNumber;

		/**
		 * <p>HTTP holder. You must use it when building new {@link HttpRequest}.</p>
		 */
		public final HttpHolder holder;

		ReadSearchPostsData() {
			throw new IllegalAccessError();
		}
	}

	/**
	 * <p>Result holder for {@link #onReadSearchPosts(ReadSearchPostsData)}.</p>
	 */
	public static final class ReadSearchPostsResult {
		/**
		 * <p>Constructor for {@link ReadSearchPostsResult}.</p>
		 *
		 * @param posts Array of {@link Post}.
		 */
		public ReadSearchPostsResult(Post... posts) {
			throw new IllegalAccessError();
		}

		/**
		 * <p>Constructor for {@link ReadSearchPostsResult}.</p>
		 *
		 * @param posts Collection of {@link Post}.
		 */
		public ReadSearchPostsResult(Collection<Post> posts) {
			throw new IllegalAccessError();
		}
	}

	/**
	 * <p>Arguments holder for {@link #onReadBoards(ReadBoardsData)}. Notify that this class might
	 * be used as {@link HttpRequest.Preset}.</p>
	 */
	public static class ReadBoardsData implements HttpRequest.Preset {
		/**
		 * <p>HTTP holder. You must use it when building new {@link HttpRequest}.</p>
		 */
		public final HttpHolder holder;

		ReadBoardsData() {
			throw new IllegalAccessError();
		}
	}

	/**
	 * <p>Result holder for {@link #onReadBoards(ReadBoardsData)}.</p>
	 */
	public static final class ReadBoardsResult {
		/**
		 * <p>Constructor for {@link ReadBoardsResult}.</p>
		 *
		 * @param boardCategories Array of {@link BoardCategory}.
		 */
		public ReadBoardsResult(BoardCategory... boardCategories) {
			throw new IllegalAccessError();
		}

		/**
		 * <p>Constructor for {@link ReadBoardsResult}.</p>
		 *
		 * @param boardCategories Collection of {@link BoardCategory}.
		 */
		public ReadBoardsResult(Collection<BoardCategory> boardCategories) {
			throw new IllegalAccessError();
		}
	}

	/**
	 * <p>Arguments holder for {@link #onReadUserBoards(ReadUserBoardsData)}. Notify that this class might
	 * be used as {@link HttpRequest.Preset}.</p>
	 */
	public static class ReadUserBoardsData implements HttpRequest.Preset {
		/**
		 * <p>HTTP holder. You must use it when building new {@link HttpRequest}.</p>
		 */
		public final HttpHolder holder;

		ReadUserBoardsData() {
			throw new IllegalAccessError();
		}
	}

	/**
	 * <p>Result holder for {@link #onReadUserBoards(ReadUserBoardsData)}.</p>
	 */
	public static final class ReadUserBoardsResult {
		/**
		 * <p>Constructor for {@link ReadUserBoardsResult}.</p>
		 *
		 * @param boards Array of {@link Board}.
		 */
		public ReadUserBoardsResult(Board... boards) {
			throw new IllegalAccessError();
		}

		/**
		 * <p>Constructor for {@link ReadUserBoardsResult}.</p>
		 *
		 * @param boards Collection of {@link Board}.
		 */
		public ReadUserBoardsResult(Collection<Board> boards) {
			throw new IllegalAccessError();
		}
	}

	/**
	 * <p>Arguments holder for {@link #onReadThreadSummaries(ReadThreadSummariesData)}.
	 * Notify that this class might be used as {@link HttpRequest.Preset}.</p>
	 */
	public static class ReadThreadSummariesData implements HttpRequest.Preset {
		/**
		 * <p>Archived threads page type.</p>
		 */
		public static final int TYPE_ARCHIVED_THREADS;

		/**
		 * <p>Board name argument. May be null.</p>
		 */
		public final String boardName;

		/**
		 * <p>Page number argument.</p>
		 */
		public final int pageNumber;

		/**
		 * <p>Page type argument.</p>
		 */
		public final int type;

		/**
		 * <p>HTTP holder. You must use it when building new {@link HttpRequest}.</p>
		 */
		public final HttpHolder holder;

		ReadThreadSummariesData() {
			throw new IllegalAccessError();
		}

		static {
			// noinspection ConstantIfStatement, ConstantConditions
			if (true) {
				throw new IllegalAccessError();
			}
		}
	}

	/**
	 * <p>Result holder for {@link #onReadThreadSummaries(ReadThreadSummariesData)}.</p>
	 */
	public static final class ReadThreadSummariesResult {
		/**
		 * <p>Constructor for {@link ReadThreadSummariesResult}.</p>
		 *
		 * @param threadSummaries Array of {@link ThreadSummary}.
		 */
		public ReadThreadSummariesResult(ThreadSummary... threadSummaries) {
			throw new IllegalAccessError();
		}

		/**
		 * <p>Constructor for {@link ReadThreadSummariesResult}.</p>
		 *
		 * @param threadSummaries Collection of {@link ThreadSummary}.
		 */
		public ReadThreadSummariesResult(Collection<ThreadSummary> threadSummaries) {
			throw new IllegalAccessError();
		}
	}

	/**
	 * <p>Arguments holder for {@link #onReadPostsCount(ReadPostsCountData)}. Notify that this class might
	 * be used as {@link HttpRequest.Preset}.</p>
	 */
	public static class ReadPostsCountData implements HttpRequest.Preset {
		/**
		 * <p>Board name argument.</p>
		 */
		public final String boardName;

		/**
		 * <p>Thread number argument.</p>
		 */
		public final String threadNumber;

		/**
		 * <p>HTTP holder. You must use it when building new {@link HttpRequest}.</p>
		 */
		public final HttpHolder holder;

		/**
		 * <p>HTTP validator. With this argument you can check if page was changed.</p>
		 */
		public final HttpValidator validator;

		ReadPostsCountData() {
			throw new IllegalAccessError();
		}
	}

	/**
	 * <p>Result holder for {@link #onReadPostsCount(ReadPostsCountData)}.</p>
	 */
	public static final class ReadPostsCountResult {
		/**
		 * <p>Constructor for {@link ReadPostsCountResult}.</p>
		 *
		 * @param postsCount Total number of posts in thread.
		 */
		public ReadPostsCountResult(int postsCount) {
			throw new IllegalAccessError();
		}

		/**
		 * <p>Stores {@code validator} in this cache. By default client will call {@link HttpHolder#getValidator()}
		 * after the last request. This can be useful if you want to store validator from intermediate request.</p>
		 *
		 * @param validator {@link HttpValidator} instance.
		 * @return This object.
		 */
		public ReadPostsCountResult setValidator(HttpValidator validator) {
			throw new IllegalAccessError();
		}
	}

	/**
	 * <p>Arguments holder for {@link #onReadContent(ReadContentData)}. Notify that this class might be
	 * used as {@link HttpRequest.Preset}.</p>
	 */
	public static class ReadContentData implements HttpRequest.Preset {
		/**
		 * <p>URI to content argument.</p>
		 */
		public final Uri uri;

		/**
		 * <p>HTTP holder. You must use it when building new {@link HttpRequest}.</p>
		 */
		public final HttpHolder holder;

		ReadContentData() {
			throw new IllegalAccessError();
		}
	}

	/**
	 * <p>Result holder for {@link #onReadContent(ReadContentData)}.</p>
	 */
	public static final class ReadContentResult {
		/**
		 * <p>Constructor for {@link ReadContentResult}.</p>
		 *
		 * @param response HTTP response.
		 */
		public ReadContentResult(HttpResponse response) {
			throw new IllegalAccessError();
		}
	}

	/**
	 * <p>Arguments holder for {@link #onCheckAuthorization(CheckAuthorizationData)}.
	 * Notify that this class might be used as {@link HttpRequest.Preset}.</p>
	 */
	public static class CheckAuthorizationData implements HttpRequest.Preset {
		/**
		 * <p>Captcha pass authorization type.</p>
		 */
		public static final int TYPE_CAPTCHA_PASS;

		/**
		 * <p>User authorization type.</p>
		 */
		public static final int TYPE_USER_AUTHORIZATION;

		/**
		 * <p>Authorization type argument.</p>
		 */
		public final int type;

		/**
		 * <p>Authorization data fields.</p>
		 */
		public final String[] authorizationData;

		/**
		 * <p>HTTP holder. You must use it when building new {@link HttpRequest}.</p>
		 */
		public final HttpHolder holder;

		CheckAuthorizationData() {
			throw new IllegalAccessError();
		}

		static {
			// noinspection ConstantIfStatement, ConstantConditions
			if (true) {
				throw new IllegalAccessError();
			}
		}
	}

	/**
	 * <p>Result holder for {@link #onCheckAuthorization(CheckAuthorizationData)}.</p>
	 */
	public static final class CheckAuthorizationResult {
		/**
		 * <p>Constructor for {@link CheckAuthorizationResult}.</p>
		 *
		 * @param success True if authorization data is correct, false otherwise.
		 */
		public CheckAuthorizationResult(boolean success) {
			throw new IllegalAccessError();
		}
	}

	/**
	 * <p>Arguments holder for {@link #onReadCaptcha(ReadCaptchaData)}. Notify that this class might be
	 * used as {@link HttpRequest.Preset}.</p>
	 */
	public static class ReadCaptchaData implements HttpRequest.Preset {
		/**
		 * <p>Captcha type argument.</p>
		 */
		public final String captchaType;

		/**
		 * <p>Captcha pass authorization data argument.</p>
		 */
		public final String[] captchaPass;

		/**
		 * <p>If {@code true} you can return {@link CaptchaState#NEED_LOAD} if captcha has short lifetime.</p>
		 */
		public final boolean mayShowLoadButton;

		/**
		 * <p>Requirement string argument.
		 * Used with {@link #requireUserCaptcha(String, String, String, boolean)}.</p>
		 */
		public final String requirement;

		/**
		 * <p>Board name argument</p>
		 */
		public final String boardName;

		/**
		 * <p>Thread number argument</p>
		 */
		public final String threadNumber;

		/**
		 * <p>HTTP holder. You must use it when building new {@link HttpRequest}.</p>
		 */
		public final HttpHolder holder;

		ReadCaptchaData() {
			throw new IllegalAccessError();
		}

		static {
			// noinspection ConstantIfStatement, ConstantConditions
			if (true) {
				throw new IllegalAccessError();
			}
		}
	}

	/**
	 * <p>Captcha states for {@link #onReadCaptcha(ReadCaptchaData)}.</p>
	 */
	public enum CaptchaState {
		/**
		 * <p>User must enter captcha.</p>
		 */
		CAPTCHA,

		/**
		 * <p>Without captcha.</p>
		 */
		SKIP,

		/**
		 * <p>Captcha pass enabled.</p>
		 */
		PASS,

		/**
		 * <p>User must click to load captcha.</p>
		 */
		NEED_LOAD
	}

	/**
	 * <p>Captcha result for {@link #onReadCaptcha(ReadCaptchaData)}.</p>
	 */
	public static final class ReadCaptchaResult {
		/**
		 * <p>Constructor for {@link ReadCaptchaResult}.</p>
		 *
		 * @param captchaState Resulting captcha state.
		 * @param captchaData Resulting captcha data map.
		 */
		public ReadCaptchaResult(CaptchaState captchaState, CaptchaData captchaData) {
			throw new IllegalAccessError();
		}

		/**
		 * <p>Overrides captcha type. It might be useful when chan requires a captcha with specific type.</p>
		 *
		 * @param captchaType Captcha type.
		 * @return This object.
		 */
		public ReadCaptchaResult setCaptchaType(String captchaType) {
			throw new IllegalAccessError();
		}

		/**
		 * <p>Overrides captcha input mode from configuration. It might be useful when captcha becomes harder than usual
		 * and user will have to enter a letters instead of numbers, for example.</p>
		 *
		 * @param input Captcha input mode.
		 * @return This object.
		 */
		public ReadCaptchaResult setInput(ChanConfiguration.Captcha.Input input) {
			throw new IllegalAccessError();
		}

		/**
		 * <p>Overrides captcha validity from configuration. It might be useful when captcha valid in thread,
		 * but with captcha pass captcha valid in all chan for example.</p>
		 *
		 * @param validity Captcha validity.
		 * @return This object.
		 */
		public ReadCaptchaResult setValidity(ChanConfiguration.Captcha.Validity validity) {
			throw new IllegalAccessError();
		}

		/**
		 * <p>Stores resulting captcha image. You must set this field with {@link CaptchaState#CAPTCHA} result.</p>
		 *
		 * @param image Captcha image bitmap.
		 * @return This object.
		 */
		public ReadCaptchaResult setImage(Bitmap image) {
			throw new IllegalAccessError();
		}

		/**
		 * <p>Use this method to make image field larger for user.</p>
		 *
		 * @param large True if captcha image is large.
		 * @return This object.
		 */
		public ReadCaptchaResult setLarge(boolean large) {
			throw new IllegalAccessError();
		}
	}

	/**
	 * <p>Captcha data map. You can fill this map in {@link #onReadCaptcha(ReadCaptchaData)} and then
	 * read from this map while sending post for example.</p>
	 *
	 * <p>User's input is available by {@link CaptchaData#INPUT} key.</p>
	 */
	public static class CaptchaData {
		/**
		 * <p>Captcha challenge string: public key, image id, cookie or another string that
		 * represents captcha session.</p>
		 */
		public static final String CHALLENGE;

		/**
		 * <p>User's input.</p>
		 */
		public static final String INPUT;

		/**
		 * <p>Captcha API key. May be used for default captcha handler.</p>
		 */
		public static final String API_KEY;

		/**
		 * <p>Referer header for captcha requests. May be useful with reCAPTCHA 2.</p>
		 */
		public static final String REFERER;

		/**
		 * <p>Put captcha data to map.</p>
		 *
		 * @param key Data key.
		 * @param value Data value.
		 */
		public void put(String key, String value) {
			throw new IllegalAccessError();
		}

		/**
		 * <p>Get captcha data from map.</p>
		 *
		 * @param key Data key.
		 * @return Data value.
		 */
		public String get(String key) {
			throw new IllegalAccessError();
		}

		static {
			// noinspection ConstantIfStatement, ConstantConditions
			if (true) {
				throw new IllegalAccessError();
			}
		}
	}

	/**
	 * <p>Arguments holder for {@link #onSendPost(SendPostData)}. Notify that this class might be
	 * used as {@link HttpRequest.Preset}.</p>
	 */
	public static class SendPostData implements HttpRequest.Preset {
		/**
		 * <p>Board name argument.</p>
		 */
		public final String boardName;

		/**
		 * <p>Thread number argument. Equals {@code null} if this new thread request.</p>
		 */
		public final String threadNumber;

		/**
		 * <p>Subject argument. Equals {@code null} if empty.</p>
		 */
		public final String subject;

		/**
		 * <p>Comment argument. Equals {@code null} if empty.</p>
		 */
		public final String comment;

		/**
		 * <p>Name argument. Equals {@code null} if empty.</p>
		 */
		public final String name;

		/**
		 * <p>Email argument. Equals {@code null} if empty.</p>
		 */
		public final String email;

		/**
		 * <p>Password for deleting argument. Equals {@code null} if empty.</p>
		 */
		public final String password;

		/**
		 * <p>Array of attachments argument. Equals {@code null} if empty.</p>
		 */
		public final Attachment[] attachments;

		/**
		 * <p>Sage option.</p>
		 */
		public final boolean optionSage;

		/**
		 * <p>Spoiler option.</p>
		 */
		public final boolean optionSpoiler;

		/**
		 * <p>Original poster option.</p>
		 */
		public final boolean optionOriginalPoster;

		/**
		 * <p>User icon value. Equals {@code null} if nothing chosen.</p>
		 */
		public final String userIcon;

		/**
		 * <p>Captcha type argument.</p>
		 */
		public final String captchaType;

		/**
		 * <p>Captcha data argument. Obtained from {@link #onReadCaptcha(ReadCaptchaData)}.
		 * May be {@code null} if captcha was not loaded.</p>
		 */
		public final CaptchaData captchaData;

		/**
		 * <p>HTTP holder. You must use it when building new {@link HttpRequest}.</p>
		 */
		public final HttpHolder holder;

		/**
		 * <p>Holds attachment data.</p>
		 */
		public static class Attachment {
			/**
			 * <p>Attachment rating argument.</p>
			 */
			public final String rating;

			/**
			 * <p>Spoiler option.</p>
			 */
			public final boolean optionSpoiler;

			Attachment() {
				throw new IllegalAccessError();
			}

			/**
			 * <p>Configures and adds attachment to {@link MultipartEntity} instance.</p>
			 *
			 * @param entity {@link MultipartEntity} instance.
			 * @param name Field name.
			 */
			public void addToEntity(MultipartEntity entity, String name) {
				throw new IllegalAccessError();
			}

			/**
			 * <p>Returns attachment file name.</p>
			 */
			public String getFileName() {
				throw new IllegalAccessError();
			}

			/**
			 * <p>Returns attachment mime type.</p>
			 */
			public String getMimeType() {
				throw new IllegalAccessError();
			}

			/**
			 * <p>Opens {@code InputStream} for this attachment.</p>
			 *
			 * @throws IOException If an error occurs while initializing a stream.
			 */
			public InputStream openInputSteam() throws IOException {
				throw new IllegalAccessError();
			}

			/**
			 * <p>Opens {@code InputStream} for this attachment. This stream will update the progress
			 * dialog. It can be useful with custom entities or with web sockets.</p>
			 *
			 * @throws IOException If an error occurs while initializing a stream.
			 */
			public InputStream openInputSteamForSending() throws IOException {
				throw new IllegalAccessError();
			}

			/**
			 * <p>Returns attachment file size.</p>
			 */
			public long getSize() {
				throw new IllegalAccessError();
			}

			/**
			 * <p>Returns a pair of image demensions for image files or {@code null} for other files.</p>
			 */
			public Pair<Integer, Integer> getImageSize() {
				throw new IllegalAccessError();
			}
		}

		SendPostData() {
			throw new IllegalAccessError();
		}
	}

	/**
	 * <p>Result for {@link #onSendPost(SendPostData)}.</p>
	 */
	public static final class SendPostResult {
		/**
		 * <p>Constructor for {@link SendPostResult}.</p>
		 *
		 * @param threadNumber Resulting thread number.
		 * @param postNumber Resulting post number.
		 */
		public SendPostResult(String threadNumber, String postNumber) {
			throw new IllegalAccessError();
		}
	}

	/**
	 * <p>Arguments holder for {@link #onSendDeletePosts(SendDeletePostsData)}. Notify that this class
	 * might be used as {@link HttpRequest.Preset}.</p>
	 */
	public static class SendDeletePostsData implements HttpRequest.Preset {
		/**
		 * <p>Board name argument.</p>
		 */
		public final String boardName;

		/**
		 * <p>Thread number argument.</p>
		 */
		public final String threadNumber;

		/**
		 * <p>List of deleting post numbers.</p>
		 */
		public final List<String> postNumbers;

		/**
		 * <p>Password for deleting argument.</p>
		 */
		public final String password;

		/**
		 * <p>Delete files only option.</p>
		 */
		public final boolean optionFilesOnly;

		/**
		 * <p>HTTP holder. You must use it when building new {@link HttpRequest}.</p>
		 */
		public final HttpHolder holder;

		SendDeletePostsData() {
			throw new IllegalAccessError();
		}
	}

	/**
	 * <p>Result for {@link #onSendDeletePosts(SendDeletePostsData)}.</p>
	 */
	public static final class SendDeletePostsResult {}

	/**
	 * <p>Arguments holder for {@link #onSendReportPosts(SendReportPostsData)}. Notify that this class
	 * might be used as {@link HttpRequest.Preset}.</p>
	 */
	public static class SendReportPostsData implements HttpRequest.Preset {
		/**
		 * <p>Board name argument.</p>
		 */
		public final String boardName;

		/**
		 * <p>Thread number argument.</p>
		 */
		public final String threadNumber;

		/**
		 * <p>List of reporting post numbers.</p>
		 */
		public final List<String> postNumbers;

		/**
		 * <p>Reporting type argument.</p>
		 */
		public final String type;

		/**
		 * <p>List of reporting options argument.</p>
		 */
		public final List<String> options;

		/**
		 * <p>Comment argument.</p>
		 */
		public final String comment;

		/**
		 * <p>HTTP holder. You must use it when building new {@link HttpRequest}.</p>
		 */
		public final HttpHolder holder;

		SendReportPostsData() {
			throw new IllegalAccessError();
		}
	}

	/**
	 * <p>Result for {@link #onSendReportPosts(SendReportPostsData)}.</p>
	 */
	public static final class SendReportPostsResult {}

	/**
	 * <p>Arguments holder for {@link #onSendAddToArchive(SendAddToArchiveData)}. Notify that this class
	 * might be used as {@link HttpRequest.Preset}.</p>
	 */
	public static class SendAddToArchiveData implements HttpRequest.Preset {
		/**
		 * <p>Archiving thread URI argument.</p>
		 */
		public final Uri uri;

		/**
		 * <p>Archiving board name argument.</p>
		 */
		public final String boardName;

		/**
		 * <p>Archiving thread number argument.</p>
		 */
		public final String threadNumber;

		/**
		 * <p>List of archiving options argument.</p>
		 */
		public final List<String> options;

		/**
		 * <p>HTTP holder. You must use it when building new {@link HttpRequest}.</p>
		 */
		public final HttpHolder holder;

		SendAddToArchiveData() {
			throw new IllegalAccessError();
		}
	}

	/**
	 * <p>Result for {@link #onSendAddToArchive(SendAddToArchiveData)}.</p>
	 */
	public static final class SendAddToArchiveResult {
		/**
		 * <p>Constructor for {@link SendAddToArchiveResult}.</p>
		 *
		 * @param boardName Resulting board name in archive.
		 * @param threadNumber Resulting thread number in archive.
		 */
		public SendAddToArchiveResult(String boardName, String threadNumber) {
			throw new IllegalAccessError();
		}
	}

	/**
	 * <p>Suspends this thread and shows captcha dialog for user. Captcha will be loaded with
	 * {@link #onReadCaptcha(ReadCaptchaData)}. You can specify {@code requirement} for differen behaviors.</p>
	 *
	 * @param requirement Requirement string.
	 * @param boardName Board name.
	 * @param threadNumber Thread number.
	 * @param retry True if this is not the first request. If true, this will show "invalid captcha" toast for user.
	 * @return {@link CaptchaData} with {@link CaptchaData#INPUT} or null if user has canceled an operation.
	 */
	protected final CaptchaData requireUserCaptcha(String requirement, String boardName, String threadNumber,
			boolean retry) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Suspends this thread and shows items dialog for user. User can choose only one item.</p>
	 *
	 * @param selected Default selected index, may be -1.
	 * @param items Array of items.
	 * @param descriptionText Description text (e.g. "Select all burgers").
	 * @param descriptionImage Description image (e.g. example image).
	 * @return Index of chosen item, -1 if item wasn't chosen or {@code null} if user has canceled an operation.
	 */
	protected final Integer requireUserItemSingleChoice(int selected, CharSequence[] items, String descriptionText,
			Bitmap descriptionImage) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Suspends this thread and shows items dialog for user. User can choose multiple items.</p>
	 *
	 * @param selected Array of default selected indexes, {@code true} for selected, may be null.
	 * @param items Array of items.
	 * @param descriptionText Description text (e.g. "Select all burgers").
	 * @param descriptionImage Description image (e.g. example image).
	 * @return Array of selected indexes or {@code null} if user has canceled an operation.
	 */
	protected final boolean[] requireUserItemMultipleChoice(boolean[] selected, CharSequence[] items,
			String descriptionText, Bitmap descriptionImage) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Suspends this thread and shows images dialog for user. User can choose only one image.</p>
	 *
	 * @param selected Default selected index, may be -1.
	 * @param images Array of images.
	 * @param descriptionText Description text (e.g. "Select all burgers").
	 * @param descriptionImage Description image (e.g. example image).
	 * @return Index of chosen image, -1 if image wasn't chosen or {@code null} if user has canceled an operation.
	 */
	protected final Integer requireUserImageSingleChoice(int selected, Bitmap[] images, String descriptionText,
			Bitmap descriptionImage) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Suspends this thread and shows images dialog for user. User can choose multiple images.</p>
	 *
	 * @param selected Array of default selected indexes, {@code true} for selected, may be null.
	 * @param images Array of images.
	 * @param descriptionText Description text (e.g. "Select all burgers").
	 * @param descriptionImage Description image (e.g. example image).
	 * @return Array of selected indexes or {@code null} if user has canceled an operation.
	 */
	protected final boolean[] requireUserImageMultipleChoice(boolean[] selected, Bitmap[] images,
			String descriptionText, Bitmap descriptionImage) {
		throw new IllegalAccessError();
	}
}
