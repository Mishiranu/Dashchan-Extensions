package chan.content;

/**
 * <p>Thrown by sending methods from {@link ChanPerformer}.</p>
 */
public final class ApiException extends Exception {
	/**
	 * <p>Board not exists error.</p>
	 */
	public static final int SEND_ERROR_NO_BOARD;

	/**
	 * <p>Thread not exists error.</p>
	 */
	public static final int SEND_ERROR_NO_THREAD;

	/**
	 * <p>No access to post on this board or thread.</p>
	 */
	public static final int SEND_ERROR_NO_ACCESS;

	/**
	 * <p>Mistyped or empty captcha.</p>
	 */
	public static final int SEND_ERROR_CAPTCHA;

	/**
	 * <p>User is banned.</p>
	 *
	 * <p>May be returned with {@link BanExtra} instance.</p>
	 */
	public static final int SEND_ERROR_BANNED;

	/**
	 * <p>Thread closed.</p>
	 */
	public static final int SEND_ERROR_CLOSED;

	/**
	 * <p>User sends posts too fast.</p>
	 */
	public static final int SEND_ERROR_TOO_FAST;

	/**
	 * <p>Comment or another field exceeds limit.</p>
	 */
	public static final int SEND_ERROR_FIELD_TOO_LONG;

	/**
	 * <p>File with the same hash sum exists on server.</p>
	 */
	public static final int SEND_ERROR_FILE_EXISTS;

	/**
	 * <p>File type is not supported.</p>
	 */
	public static final int SEND_ERROR_FILE_NOT_SUPPORTED;

	/**
	 * <p>File size exceeds limit.</p>
	 */
	public static final int SEND_ERROR_FILE_TOO_BIG;

	/**
	 * <p>Too many files attached to post.</p>
	 */
	public static final int SEND_ERROR_FILES_TOO_MANY;

	/**
	 * <p>Comment or another field contains a word from spam list.</p>
	 *
	 * <p>May be returned with {@link WordsExtra} instance.</p>
	 */
	public static final int SEND_ERROR_SPAM_LIST;

	/**
	 * <p>User must attach file to send post.</p>
	 */
	public static final int SEND_ERROR_EMPTY_FILE;

	/**
	 * <p>User must specify subject to send post.</p>
	 */
	public static final int SEND_ERROR_EMPTY_SUBJECT;

	/**
	 * <p>User must specify comment to send post.</p>
	 */
	public static final int SEND_ERROR_EMPTY_COMMENT;

	/**
	 * <p>Reached maximum files count in thread.</p>
	 */
	public static final int SEND_ERROR_FILES_LIMIT;

	/**
	 * <p>No access to delete posts: unsupported or canceled operation.</p>
	 */
	public static final int DELETE_ERROR_NO_ACCESS;

	/**
	 * <p>User entered invalid password.</p>
	 */
	public static final int DELETE_ERROR_PASSWORD;

	/**
	 * <p>Deleted post was not found.</p>
	 */
	public static final int DELETE_ERROR_NOT_FOUND;

	/**
	 * <p>User must wait before deleting new posts.</p>
	 */
	public static final int DELETE_ERROR_TOO_NEW;

	/**
	 * <p>The post is too old to delete.</p>
	 */
	public static final int DELETE_ERROR_TOO_OLD;

	/**
	 * <p>User sends delete post requests too often.</p>
	 */
	public static final int DELETE_ERROR_TOO_OFTEN;

	/**
	 * <p>No access to report post: unsupported or canceled operation.</p>
	 */
	public static final int REPORT_ERROR_NO_ACCESS;

	/**
	 * <p>User sends report post requests too often.</p>
	 */
	public static final int REPORT_ERROR_TOO_OFTEN;

	/**
	 * <p>User must specify comment to send report.</p>
	 */
	public static final int REPORT_ERROR_EMPTY_COMMENT;

	/**
	 * <p>No access to archive thread: unsupported or canceled operation.</p>
	 */
	public static final int ARCHIVE_ERROR_NO_ACCESS;

	/**
	 * <p>User sends archive requests too often.</p>
	 */
	public static final int ARCHIVE_ERROR_TOO_OFTEN;

	/**
	 * <p>Flag: client will not reset captcha due to exception.</p>
	 */
	public static final int FLAG_KEEP_CAPTCHA;

	static {
		// noinspection ConstantIfStatement,ConstantConditions
		if (true) {
			throw new IllegalAccessError();
		}
	}

	/**
	 * <p>Constructor for an {@link ApiException}.</p>
	 *
	 * @param errorType Error type constant value.
	 */
	public ApiException(int errorType) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Constructor for an {@link ApiException}.</p>
	 *
	 * @param errorType Error type constant value.
	 * @param flags Additional option flags. The following flags are available: {@link #FLAG_KEEP_CAPTCHA}.
	 */
	public ApiException(int errorType, int flags) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Constructor for an {@link ApiException}.</p>
	 *
	 * @param errorType Error type constant value.
	 * @param extra Additional extra data. The following types are available {@link BanExtra}, {@link WordsExtra}.
	 */
	public ApiException(int errorType, Object extra) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Constructor for an {@link ApiException}.</p>
	 *
	 * @param errorType Error type constant value.
	 * @param flags Additional option flags. The following flags are available: {@link #FLAG_KEEP_CAPTCHA}.
	 * @param extra Additional extra data. The following types are available {@link BanExtra}, {@link WordsExtra}.
	 */
	public ApiException(int errorType, int flags, Object extra) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Constructor for an {@link ApiException}.</p>
	 *
	 * @param detailMessage Error message.
	 */
	public ApiException(String detailMessage) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Constructor for an {@link ApiException}.</p>
	 *
	 * @param detailMessage Error message.
	 * @param flags Additional option flags. The following flags are available: {@link #FLAG_KEEP_CAPTCHA}.
	 */
	public ApiException(String detailMessage, int flags) {
		throw new IllegalAccessError();
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
			throw new IllegalAccessError();
		}

		/**
		 * <p>Sets ban reason message.</p>
		 *
		 * @param message Ban reason message.
		 * @return This object.
		 */
		public BanExtra setMessage(String message) {
			throw new IllegalAccessError();
		}

		/**
		 * <p>Sets ban start date.</p>
		 *
		 * @param startDate Ban start date.
		 * @return This object.
		 */
		public BanExtra setStartDate(long startDate) {
			throw new IllegalAccessError();
		}

		/**
		 * <p>Sets ban expire date. May be {@link Long#MAX_VALUE} if ban is permanent.</p>
		 *
		 * @param expireDate Ban expire date.
		 * @return This object.
		 */
		public BanExtra setExpireDate(long expireDate) {
			throw new IllegalAccessError();
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
			throw new IllegalAccessError();
		}
	}
}
