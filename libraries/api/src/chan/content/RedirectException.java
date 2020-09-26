package chan.content;

import android.net.Uri;
import chan.library.api.BuildConfig;

/**
 * <p>Thrown to inform client about redirection. These exceptions is thrown by
 * {@link ChanPerformer#onReadThreads(chan.content.ChanPerformer.ReadThreadsData)} or
 * {@link ChanPerformer#onReadPosts(chan.content.ChanPerformer.ReadPostsData)} when
 * data returned from server might be considered as redirect.</p>
 */
public final class RedirectException extends Exception {
	private RedirectException() {
		BuildConfig.Private.expr();
	}

	/**
	 * <p>Creates a new instance of {@link RedirectException} that causes client to follow the URI.</p>
	 *
	 * @param uri Redirected URI.
	 */
	public static RedirectException toUri(Uri uri) {
		return BuildConfig.Private.expr(uri);
	}

	/**
	 * <p>Creates a new instance of {@link RedirectException} that causes client to follow the board.</p>
	 *
	 * @param boardName Redirected board name.
	 */
	public static RedirectException toBoard(String boardName) {
		return BuildConfig.Private.expr(boardName);
	}

	/**
	 * <p>Creates a new instance of {@link RedirectException} that causes client to follow the thread.</p>
	 *
	 * @param boardName Redirected board name.
	 * @param threadNumber Redirected thread number.
	 * @param postNumber Redirected post number.
	 */
	public static RedirectException toThread(String boardName, String threadNumber, String postNumber) {
		return BuildConfig.Private.expr(boardName, threadNumber, postNumber);
	}
}
