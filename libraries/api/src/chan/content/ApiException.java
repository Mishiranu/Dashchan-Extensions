package chan.content;

import chan.library.api.BuildConfig;

/**
 * <p>Thrown by sending methods from {@link ChanPerformer}.</p>
 */
public final class ApiException extends Exception {
	/**
	 * <p>Board not exists error.</p>
	 */
	public static final int SEND_ERROR_NO_BOARD = BuildConfig.Private.expr();

	/**
	 * <p>Thread not exists error.</p>
	 */
	public static final int SEND_ERROR_NO_THREAD = BuildConfig.Private.expr();

	/**
	 * <p>No access to post on this board or thread.</p>
	 */
	public static final int SEND_ERROR_NO_ACCESS = BuildConfig.Private.expr();

	/**
	 * <p>Mistyped or empty captcha.</p>
	 */
	public static final int SEND_ERROR_CAPTCHA = BuildConfig.Private.expr();

	/**
	 * <p>User is banned.</p>
	 *
	 * <p>May be returned with {@link BanExtra} instance.</p>
	 */
	public static final int SEND_ERROR_BANNED = BuildConfig.Private.expr();

	/**
	 * <p>Thread closed.</p>
	 */
	public static final int SEND_ERROR_CLOSED = BuildConfig.Private.expr();

	/**
	 * <p>User sends posts too fast.</p>
	 */
	public static final int SEND_ERROR_TOO_FAST = BuildConfig.Private.expr();

	/**
	 * <p>Comment or another field exceeds limit.</p>
	 */
	public static final int SEND_ERROR_FIELD_TOO_LONG = BuildConfig.Private.expr();

	/**
	 * <p>File with the same hash sum exists on server.</p>
	 */
	public static final int SEND_ERROR_FILE_EXISTS = BuildConfig.Private.expr();

	/**
	 * <p>File type is not supported.</p>
	 */
	public static final int SEND_ERROR_FILE_NOT_SUPPORTED = BuildConfig.Private.expr();

	/**
	 * <p>File size exceeds limit.</p>
	 */
	public static final int SEND_ERROR_FILE_TOO_BIG = BuildConfig.Private.expr();

	/**
	 * <p>Too many files attached to post.</p>
	 */
	public static final int SEND_ERROR_FILES_TOO_MANY = BuildConfig.Private.expr();

	/**
	 * <p>Comment or another field contains a word from spam list.</p>
	 *
	 * <p>May be returned with {@link WordsExtra} instance.</p>
	 */
	public static final int SEND_ERROR_SPAM_LIST = BuildConfig.Private.expr();

	/**
	 * <p>User must attach file to send post.</p>
	 */
	public static final int SEND_ERROR_EMPTY_FILE = BuildConfig.Private.expr();

	/**
	 * <p>User must specify subject to send post.</p>
	 */
	public static final int SEND_ERROR_EMPTY_SUBJECT = BuildConfig.Private.expr();

	/**
	 * <p>User must specify comment to send post.</p>
	 */
	public static final int SEND_ERROR_EMPTY_COMMENT = BuildConfig.Private.expr();

	/**
	 * <p>Reached maximum files count in thread.</p>
	 */
	public static final int SEND_ERROR_FILES_LIMIT = BuildConfig.Private.expr();

	/**
	 * <p>No access to delete posts: unsupported or canceled operation.</p>
	 */
	public static final int DELETE_ERROR_NO_ACCESS = BuildConfig.Private.expr();

	/**
	 * <p>User entered invalid password.</p>
	 */
	public static final int DELETE_ERROR_PASSWORD = BuildConfig.Private.expr();

	/**
	 * <p>Deleted post was not found.</p>
	 */
	public static final int DELETE_ERROR_NOT_FOUND = BuildConfig.Private.expr();

	/**
	 * <p>User must wait before deleting new posts.</p>
	 */
	public static final int DELETE_ERROR_TOO_NEW = BuildConfig.Private.expr();

	/**
	 * <p>The post is too old to delete.</p>
	 */
	public static final int DELETE_ERROR_TOO_OLD = BuildConfig.Private.expr();

	/**
	 * <p>User sends delete post requests too often.</p>
	 */
	public static final int DELETE_ERROR_TOO_OFTEN = BuildConfig.Private.expr();

	/**
	 * <p>No access to report post: unsupported or canceled operation.</p>
	 */
	public static final int REPORT_ERROR_NO_ACCESS = BuildConfig.Private.expr();

	/**
	 * <p>User sends report post requests too often.</p>
	 */
	public static final int REPORT_ERROR_TOO_OFTEN = BuildConfig.Private.expr();

	/**
	 * <p>User must specify comment to send report.</p>
	 */
	public static final int REPORT_ERROR_EMPTY_COMMENT = BuildConfig.Private.expr();

	/**
	 * <p>No access to archive thread: unsupported or canceled operation.</p>
	 */
	public static final int ARCHIVE_ERROR_NO_ACCESS = BuildConfig.Private.expr();

	/**
	 * <p>User sends archive requests too often.</p>
	 */
	public static final int ARCHIVE_ERROR_TOO_OFTEN = BuildConfig.Private.expr();

	/**
	 * <p>Flag: client will not reset captcha due to exception.</p>
	 */
	public static final int FLAG_KEEP_CAPTCHA = BuildConfig.Private.expr();

	/**
	 * <p>Constructor for an {@link ApiException}.</p>
	 *
	 * @param errorType Error type constant value.
	 */
	public ApiException(int errorType) {
		BuildConfig.Private.expr(errorType);
	}

	/**
	 * <p>Constructor for an {@link ApiException}.</p>
	 *
	 * @param errorType Error type constant value.
	 * @param flags Additional option flags. The following flags are available: {@link #FLAG_KEEP_CAPTCHA}.
	 */
	public ApiException(int errorType, int flags) {
		BuildConfig.Private.expr(errorType, flags);
	}

	/**
	 * <p>Constructor for an {@link ApiException}.</p>
	 *
	 * @param errorType Error type constant value.
	 * @param extra Additional extra data. The following types are available {@link BanExtra}, {@link WordsExtra}.
	 */
	public ApiException(int errorType, Object extra) {
		BuildConfig.Private.expr(errorType, extra);
	}

	/**
	 * <p>Constructor for an {@link ApiException}.</p>
	 *
	 * @param errorType Error type constant value.
	 * @param flags Additional option flags. The following flags are available: {@link #FLAG_KEEP_CAPTCHA}.
	 * @param extra Additional extra data. The following types are available {@link BanExtra}, {@link WordsExtra}.
	 */
	public ApiException(int errorType, int flags, Object extra) {
		BuildConfig.Private.expr(errorType, flags, extra);
	}

	/**
	 * <p>Constructor for an {@link ApiException}.</p>
	 *
	 * @param detailMessage Error message.
	 */
	public ApiException(String detailMessage) {
		BuildConfig.Private.expr(detailMessage);
	}

	/**
	 * <p>Constructor for an {@link ApiException}.</p>
	 *
	 * @param detailMessage Error message.
	 * @param flags Additional option flags. The following flags are available: {@link #FLAG_KEEP_CAPTCHA}.
	 */
	public ApiException(String detailMessage, int flags) {
		BuildConfig.Private.expr(detailMessage, flags);
	}

	/**
	 * <p>{@link #SEND_ERROR_BANNED} extra holder.</p>
	 */
	public static final class BanExtra {
		/**
		 * <p>Sets ban ID.</p>
		 *
		 * @param id Ban ID.
		 * @return This object.
		 */
		public BanExtra setId(String id) {
			return BuildConfig.Private.expr(id);
		}

		/**
		 * <p>Sets ban reason message.</p>
		 *
		 * @param message Ban reason message.
		 * @return This object.
		 */
		public BanExtra setMessage(String message) {
			return BuildConfig.Private.expr(message);
		}

		/**
		 * <p>Sets ban start date.</p>
		 *
		 * @param startDate Ban start date.
		 * @return This object.
		 */
		public BanExtra setStartDate(long startDate) {
			return BuildConfig.Private.expr(startDate);
		}

		/**
		 * <p>Sets ban expire date. May be {@link Long#MAX_VALUE} if ban is permanent.</p>
		 *
		 * @param expireDate Ban expire date.
		 * @return This object.
		 */
		public BanExtra setExpireDate(long expireDate) {
			return BuildConfig.Private.expr(expireDate);
		}
	}

	/**
	 * <p>{@link #SEND_ERROR_SPAM_LIST} extra holder.</p>
	 */
	public static final class WordsExtra {
		/**
		 * <p>Adds a rejected word from message.</p>
		 *
		 * @param word Rejected word.
		 * @return This object.
		 */
		public WordsExtra addWord(String word) {
			return BuildConfig.Private.expr(word);
		}
	}
}
