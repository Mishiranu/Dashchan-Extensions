package chan.content;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.util.Pair;
import chan.library.api.BuildConfig;
import chan.util.DataFile;
import java.util.List;
import java.util.Set;

/**
 * <p>Provides extension configuration.</p>
 *
 * <p>During construction you can enable the following options using {@link #request(String)} method:</p>
 *
 * <ul>
 * <li>{@link #OPTION_SINGLE_BOARD_MODE}</li>
 * <li>{@link #OPTION_READ_THREAD_PARTIALLY}</li>
 * <li>{@link #OPTION_READ_SINGLE_POST}</li>
 * <li>{@link #OPTION_READ_POSTS_COUNT}</li>
 * <li>{@link #OPTION_READ_USER_BOARDS}</li>
 * <li>{@link #OPTION_ALLOW_CAPTCHA_PASS}</li>
 * <li>{@link #OPTION_ALLOW_USER_AUTHORIZATION}</li>
 * <li>{@link #OPTION_LOCAL_MODE}</li>
 * </ul>
 *
 * <h3>Static configuration</h3>
 *
 * <p>This configuration remains in RAM only while client is launched. You must configure these settings
 * during construction.</p>
 *
 * <p>You can configure poster's default name using {@link #setDefaultName(String)} and
 * {@link #setDefaultName(String, String)}.</p>
 *
 * <p>You can configure board title and description using {@link #setBoardTitle(String, String)} and
 * {@link #setBoardDescription(String, String)}.</p>
 *
 * <p>You can configure bump limit using {@link #setBumpLimit(int)} and {@link #setBumpLimit(String, int)}.
 * You can change bump limit mode using {@link #setBumpLimitMode(BumpLimitMode)}.</p>
 *
 * <p>You can configure pages count using {@link #setPagesCount(String, int)}.</p>
 *
 * <p>You can add supported captchas using {@link #addCaptchaType(String)}.</p>
 *
 * <h3>Dynamic configuration</h3>
 *
 * <p>This configuration can be written to client's preferences and read every time client launches.</p>
 *
 * <p>You can store poster's default name using {@link #storeDefaultName(String, String)}.</p>
 *
 * <p>You can store board title and description using {@link #storeBoardTitle(String, String)} and
 * {@link #storeBoardDescription(String, String)}.</p>
 *
 * <p>You can store bump limit using {@link #storeBumpLimit(String, int)}.</p>
 *
 * <p>You can store pages count using {@link #storePagesCount(String, int)}.</p>
 *
 * <p>You can store any another properties using {@link #set(String, String, boolean)},
 * {@link #set(String, String, int)} and {@link #set(String, String, String)} methods.</p>
 *
 * <p>You can get stored properties using {@link #get(String, String, boolean)},
 * {@link #get(String, String, int)} and {@link #get(String, String, String)} methods.</p>
 *
 * <h3>Additional features</h3>
 *
 * <p>You can store cookies using {@link #storeCookie(String, String, String)}. Later you can get it using
 * {@link #getCookie(String)}. User can clear cookies in application preferences.</p>
 *
 * <p>You can get chan's resources using {@link #getResources()}.</p>
 *
 * <p>You can add additional preferences to chan preferences screen using
 * {@link #addCustomPreference(String, boolean)} and {@link #obtainCustomPreferenceConfiguration(String)}.</p>
 *
 * <h3>Configuring actions</h3>
 *
 * <p>You can configure board features using {@link #obtainBoardConfiguration(String)}.</p>
 *
 * <p>You can configure custom captcha using {@link #obtainCustomCaptchaConfiguration(String)}.</p>
 *
 * <p>You can configure posting using {@link #obtainPostingConfiguration(String, boolean)}.</p>
 *
 * <p>You can configure deleting using {@link #obtainDeletingConfiguration(String)}.</p>
 *
 * <p>You can configure reporting using {@link #obtainReportingConfiguration(String)}.</p>
 *
 * <p>You can configure captcha pass using {@link #obtainCaptchaPassConfiguration()}.</p>
 *
 * <p>You can configure authorization using {@link #obtainUserAuthorizationConfiguration()}.</p>
 *
 * <p>You can configure archivation using {@link #obtainArchivationConfiguration()}.</p>
 *
 * <p>You can configure displayed statistics data using {@link #obtainStatisticsConfiguration()}.</p>
 *
 * <p>You can configure custom preferences using {@link #obtainStatisticsConfiguration()}.</p>
 */
public class ChanConfiguration {
	/**
	 * <p>Allows client to use only one board. That exempts you from obligation to override
	 * {@link ChanPerformer#onReadBoards(chan.content.ChanPerformer.ReadBoardsData)} method.</p>
	 *
	 * <p>The {@code boardName} argument in all methods will be equal to {@code null}. You can change this argument
	 * using {@link #setSingleBoardName(String)} method.</p>
	 */
	public static final String OPTION_SINGLE_BOARD_MODE = BuildConfig.Private.expr();

	/**
	 * <p>Allows client to download thread partially.</p>
	 *
	 * @see ChanPerformer#onReadPosts(chan.content.ChanPerformer.ReadPostsData)
	 */
	public static final String OPTION_READ_THREAD_PARTIALLY = BuildConfig.Private.expr();

	/**
	 * <p>Allows client to download single post knowing it's board name and post number.</p>
	 *
	 * <p>If you enable this option, you <strong>must</strong> implement
	 * {@link ChanPerformer#onReadSinglePost(chan.content.ChanPerformer.ReadSinglePostData)}.</p>
	 */
	public static final String OPTION_READ_SINGLE_POST = BuildConfig.Private.expr();

	/**
	 * <p>Allows client to download posts count in thread. This option is necessary for threads watcher.</p>
	 *
	 * <p>If you enable this option, you <strong>must</strong> implement
	 * {@link ChanPerformer#onReadPostsCount(chan.content.ChanPerformer.ReadPostsCountData)}.</p>
	 */
	public static final String OPTION_READ_POSTS_COUNT = BuildConfig.Private.expr();

	/**
	 * <p>Allows client to download user boards.</p>
	 *
	 * <p>If you enable this option, you <strong>must</strong> implement
	 * {@link ChanPerformer#onReadUserBoards(chan.content.ChanPerformer.ReadUserBoardsData)}.</p>
	 */
	public static final String OPTION_READ_USER_BOARDS = BuildConfig.Private.expr();

	/**
	 * <p>Allows user to enter authorization data and skip the captcha.
	 * With entered data you will receive {@link ChanPerformer.ReadCaptchaData#captchaPass} argument.
	 * You can perform authorization and save cookies or another data that represents authorization state.
	 * Then you can use them to skip captcha.</p>
	 *
	 * <p>If you enable this option, you <strong>must</strong> implement
	 * {@link ChanPerformer#onCheckAuthorization(chan.content.ChanPerformer.CheckAuthorizationData)}.</p>
	 *
	 * <p>You should also implement {@link #obtainCaptchaPassConfiguration()}.</p>
	 *
	 * @see ChanPerformer.CaptchaState#PASS
	 */
	public static final String OPTION_ALLOW_CAPTCHA_PASS = BuildConfig.Private.expr();

	/**
	 * <p>Allows user to enter authorization data to access some features.
	 * You can obtain authorization data using {@link #getUserAuthorizationData()}.
	 * With this data you can perform authorization and save cookies or another data that represents authorization
	 * state. Then you can grant user rights to read some threads or something like this.</p>
	 *
	 * <p>If you enable this option, you <strong>must</strong> implement
	 * {@link ChanPerformer#onCheckAuthorization(chan.content.ChanPerformer.CheckAuthorizationData)}.</p>
	 *
	 * <p>You should also implement {@link #obtainUserAuthorizationConfiguration()}.</p>
	 */
	public static final String OPTION_ALLOW_USER_AUTHORIZATION = BuildConfig.Private.expr();

	/**
	 * <p>Turns extension into local mode, which disables caching, hides domain and proxy preferences, and disallows
	 * archivation. This is useful for extensions which access local file systems or local networks.</p>
	 */
	public static final String OPTION_LOCAL_MODE = BuildConfig.Private.expr();

	public static final String CAPTCHA_TYPE_RECAPTCHA_2 = BuildConfig.Private.expr();
	public static final String CAPTCHA_TYPE_RECAPTCHA_2_INVISIBLE = BuildConfig.Private.expr();
	public static final String CAPTCHA_TYPE_HCAPTCHA = BuildConfig.Private.expr();

	/**
	 * <p>Return linked {@link ChanConfiguration} instance.
	 *
	 * @param object Linked object: {@link ChanConfiguration}, {@link ChanPerformer},
	 * {@link ChanLocator} or {@link ChanMarkup}.
	 * @return {@link ChanConfiguration} instance.
	 */
	public static <T extends ChanConfiguration> T get(Object object) {
		return BuildConfig.Private.expr(object);
	}

	/**
	 * <p>Mode of bump limit handling.</p>
	 *
	 * @see ChanConfiguration#setBumpLimitMode(BumpLimitMode)
	 */
	public enum BumpLimitMode {
		/**
		 * <p>Default mode. If bump limit if {@code 500}, posts from {@code 501} won't bump a thread.</p>
		 */
		AFTER_POST,

		/**
		 * <p>If bump limit if {@code 500}, posts from {@code 502} won't bump a thread.
		 * I.e., from {@code 501} reply.</p>
		 */
		AFTER_REPLY,

		/**
		 * <p>If bump limit if {@code 500}, posts from {@code 500} won't bump a thread.</p>
		 */
		BEFORE_POST
	}

	/**
	 * <p>Board configuration holder.</p>
	 */
	public static final class Board {
		/**
		 * <p>Set {@code true} to allow user to search for threads and posts. You must implement
		 * {@link ChanPerformer#onReadSearchPosts(chan.content.ChanPerformer.ReadSearchPostsData)} then.</p>
		 */
		public boolean allowSearch;

		/**
		 * <p>Set {@code true} to allow user to read catalog.</p>
		 *
		 * @see ChanPerformer#onReadThreads(chan.content.ChanPerformer.ReadThreadsData)
		 */
		public boolean allowCatalog;

		/**
		 * <p>Set {@code true} to allow client to use catalog as search source. In this case {@link #allowCatalog}
		 * also must be {@code true}. It can be useful if your chan's catalog is very detailed and can show last
		 * replies to all threads.</p>
		 */
		public boolean allowCatalogSearch;

		/**
		 * <p>Set {@code true} to allow client to read an archive of threads.</p>
		 */
		public boolean allowArchive;

		/**
		 * <p>Set {@code true} to allow user to send posts. You must implement
		 * {@link ChanPerformer#onSendPost(chan.content.ChanPerformer.SendPostData)} and configure
		 * posting with {@link ChanConfiguration#obtainPostingConfiguration(String, boolean)} then.</p>
		 */
		public boolean allowPosting;

		/**
		 * <p>Set {@code true} to allow user to delete posts. You must implement
		 * {@link ChanPerformer#onSendDeletePosts(chan.content.ChanPerformer.SendDeletePostsData)} and configure
		 * deleting with {@link ChanConfiguration#obtainDeletingConfiguration(String)} then.</p>
		 */
		public boolean allowDeleting;

		/**
		 * <p>Set {@code true} to allow user to send reports. You must implement
		 * {@link ChanPerformer#onSendReportPosts(chan.content.ChanPerformer.SendReportPostsData)} and configure
		 * reporting with {@link ChanConfiguration#obtainReportingConfiguration(String)} then.</p>
		 */
		public boolean allowReporting;
	}

	/**
	 * <p>Captcha configuration holder.</p>
	 */
	public static final class Captcha {
		/**
		 * <p>Captcha input mode.</p>
		 */
		public enum Input {
			/**
			 * <p>Captcha may contain any letters and numbers.</p>
			 */
			ALL,

			/**
			 * <p>Captcha may contain only latin letters and numbers.</p>
			 */
			LATIN,

			/**
			 * <p>Captcha may contain only numbers.</p>
			 */
			NUMERIC
		}

		/**
		 * <p>Captcha validity mode.</p>
		 */
		public enum Validity {
			/**
			 * <p>Short lifetime captcha. Client will request new captcha every time user opens posting activity.</p>
			 */
			SHORT_LIFETIME,

			/**
			 * <p>Captcha live only within a thread. Client will request new captcha when user opens posting activity
			 * in another thread.</p>
			 */
			IN_THREAD,

			/**
			 * <p>Captcha live only within a board or any thread separately. Client will request new captcha when
			 * user opens posting activity in another board or in any thread. Captcha will be alive when user opens
			 * another thread in the same board.</p>
			 */
			IN_BOARD_SEPARATELY,

			/**
			 * <p>Captcha live only within a board. Client will request new captcha when user opens posting activity
			 * in another board.</p>
			 */
			IN_BOARD,

			/**
			 * <p>Long lifetime captcha. Client will use old captcha when user opens posting activity.</p>
			 */
			LONG_LIFETIME
		}

		/**
		 * <p>Captcha title. This title may be shown in application preferences.</p>
		 */
		public String title;

		/**
		 * <p>Captcha input type.</p>
		 */
		public Input input;

		/**
		 * <p>Captcha validity.</p>
		 */
		public Validity validity;
	}

	/**
	 * <p>Posting configuration holder.</p>
	 *
	 * @see ChanPerformer.SendPostData
	 */
	public static final class Posting {
		/**
		 * <p>Set {@code true} to enable names. You will receive user's input from
		 * {@link ChanPerformer.SendPostData#name}</p>
		 */
		public boolean allowName;

		/**
		 * <p>Set {@code true} to enable tripcodes. You will receive user's input from
		 * {@link ChanPerformer.SendPostData#name}</p>
		 */
		public boolean allowTripcode;

		/**
		 * <p>Set {@code true} to enable emails. You will receive user's input from
		 * {@link ChanPerformer.SendPostData#email}</p>
		 */
		public boolean allowEmail;

		/**
		 * <p>Set {@code true} to enable subjects. You will receive user's input from
		 * {@link ChanPerformer.SendPostData#subject}</p>
		 */
		public boolean allowSubject;

		/**
		 * <p>Set {@code true} to enable sage mark. You will receive user's choice from
		 * {@link ChanPerformer.SendPostData#optionSage}</p>
		 */
		public boolean optionSage;

		/**
		 * <p>Set {@code true} to enable spoiler mark. You will receive user's choice from
		 * {@link ChanPerformer.SendPostData#optionSpoiler}</p>
		 */
		public boolean optionSpoiler;

		/**
		 * <p>Set {@code true} to enable original poster mark. You will receive user's choice from
		 * {@link ChanPerformer.SendPostData#optionSage}</p>
		 */
		public boolean optionOriginalPoster;

		/**
		 * <p>Maximum number of characters in comment.</p>
		 */
		public int maxCommentLength;

		/**
		 * <p>Characters encoding in comment. Use this to make client count number of bytes in your encoding
		 * instead of number of chars.</p>
		 */
		public String maxCommentLengthEncoding;

		/**
		 * <p>Maximum number of attachments.</p>
		 */
		public int attachmentCount;

		/**
		 * <p>Mime types of attachments.</p>
		 */
		public final Set<String> attachmentMimeTypes = BuildConfig.Private.expr();

		/**
		 * <p>Attachment ratings. The {@code first} field in pair is value, the {@code second} one is display name.
		 * You will receive user's selected value from {@link ChanPerformer.SendPostData.Attachment#rating}. The first
		 * item in list will be selected by default.</p>
		 */
		public final List<Pair<String, String>> attachmentRatings = BuildConfig.Private.expr();

		/**
		 * <p>Set {@code true} to enable spoiler option. You will receive user's choice from
		 * {@link ChanPerformer.SendPostData.Attachment#optionSpoiler}.</p>
		 */
		public boolean attachmentSpoiler;

		/**
		 * <p>User icons. The {@code first} field in pair is value, the {@code second} one is display name.
		 * You will receive user's selected value from {@link ChanPerformer.SendPostData#userIcon}.</p>
		 */
		public final List<Pair<String, String>> userIcons = BuildConfig.Private.expr();

		/**
		 * <p>Set {@code true} to enable notification that board has flags.</p>
		 */
		public boolean hasCountryFlags;

		/**
		 * <p>Default constructor for {@link Posting}.</p>
		 */
		public Posting() {
			BuildConfig.Private.expr();
		}
	}


	/**
	 * <p>Deleting configuration holder.</p>
	 *
	 * @see ChanPerformer.SendDeletePostsData
	 */
	public static final class Deleting {
		/**
		 * <p>Set {@code true} to allow user to enter password.</p>
		 */
		public boolean password;

		/**
		 * <p>Set {@code true} to allow user to choose multiple posts to delete.</p>
		 */
		public boolean multiplePosts;

		/**
		 * <p>Set {@code true} to allow user choose "delete files only" option.</p>
		 */
		public boolean optionFilesOnly;
	}

	/**
	 * <p>Reporting configuration holder.</p>
	 *
	 * @see ChanPerformer.SendReportPostsData
	 */
	public static final class Reporting {
		/**
		 * <p>Set {@code true} to allow user to enter comment.</p>
		 */
		public boolean comment;

		/**
		 * <p>Set {@code true} to allow user to choose multiple posts for report.</p>
		 */
		public boolean multiplePosts;

		/**
		 * <p>Reporting types. The {@code first} field in pair is value, the {@code second} one is display name.
		 * You will receive user's selected value from {@link ChanPerformer.SendReportPostsData#type}.</p>
		 */
		public final List<Pair<String, String>> types = BuildConfig.Private.expr();

		/**
		 * <p>Reporting options. The {@code first} field in pair is value, the {@code second} one is display name.
		 * You will receive user's selected value from {@link ChanPerformer.SendReportPostsData#options}.</p>
		 */
		public final List<Pair<String, String>> options = BuildConfig.Private.expr();

		/**
		 * <p>Default constructor for {@link Reporting}.</p>
		 */
		public Reporting() {
			BuildConfig.Private.expr();
		}
	}

	/**
	 * <p>Authorization configuration holder.</p>
	 *
	 * @see ChanPerformer.CheckAuthorizationData
	 */
	public static final class Authorization {
		/**
		 * <p>Number of text field to perform authentication.</p>
		 */
		public int fieldsCount;

		/**
		 * <p>Hint strings for every text field.</p>
		 */
		public String[] hints;
	}

	/**
	 * <p>Archivation configuration holder.</p>
	 *
	 * @see ChanPerformer.SendAddToArchiveData
	 */
	public static final class Archivation {
		/**
		 * <p>List of allowed chan hosts for archivation.</p>
		 */
		public final List<String> hosts = BuildConfig.Private.expr();

		/**
		 * <p>Archivation options. The {@code first} field in pair is value, the {@code second} one is display name.
		 * You will receive user's selected values from {@link ChanPerformer.SendAddToArchiveData#options}.</p>
		 */
		public final List<Pair<String, String>> options = BuildConfig.Private.expr();

		/**
		 * <p>Default constructor for {@link Archivation}.</p>
		 */
		public Archivation() {
			BuildConfig.Private.expr();
		}
	}

	/**
	 * <p>Statistics configuration holder.</p>
	 */
	public static final class Statistics {
		/**
		 * <p>Count threads viewed. True by default.</p>
		 */
		public boolean threadsViewed;

		/**
		 * <p>Count posts sent. True by default.</p>
		 */
		public boolean postsSent;

		/**
		 * <p>Count threads created. True by default.</p>
		 */
		public boolean threadsCreated;
	}

	/**
	 * <p>Custom preference configuration holder.</p>
	 */
	public static final class CustomPreference {
		/**
		 * <p>Custom preference title.</p>
		 */
		public String title;

		/**
		 * <p>Custom preference summary.</p>
		 */
		public String summary;
	}

	/**
	 * <p>Returns stored boolean value.</p>
	 *
	 * @param boardName Board name part of real key.
	 * @param key Key name.
	 * @param defaultValue Value to return if this preference does not exist.
	 * @return Stored value.
	 */
	public final boolean get(String boardName, String key, boolean defaultValue) {
		return BuildConfig.Private.expr(boardName, key, defaultValue);
	}

	/**
	 * <p>Returns stored int value.</p>
	 *
	 * @param boardName Board name part of real key.
	 * @param key Key name.
	 * @param defaultValue Value to return if this preference does not exist.
	 * @return Stored value.
	 */
	public final int get(String boardName, String key, int defaultValue) {
		return BuildConfig.Private.expr(boardName, key, defaultValue);
	}

	/**
	 * <p>Returns stored string value.</p>
	 *
	 * @param boardName Board name part of real key.
	 * @param key Key name.
	 * @param defaultValue Value to return if this preference does not exist.
	 * @return Stored value.
	 */
	public final String get(String boardName, String key, String defaultValue) {
		return BuildConfig.Private.expr(boardName, key, defaultValue);
	}

	/**
	 * <p>Stores boolean value.</p>
	 *
	 * @param boardName Board name part of key.
	 * @param key Key name.
	 * @param value Value to store.
	 */
	public final void set(String boardName, String key, boolean value) {
		BuildConfig.Private.expr(boardName, key, value);
	}

	/**
	 * <p>Stores int value.</p>
	 *
	 * @param boardName Board name part of key.
	 * @param key Key name.
	 * @param value Value to store.
	 */
	public final void set(String boardName, String key, int value) {
		BuildConfig.Private.expr(boardName, key, value);
	}

	/**
	 * <p>Stores string value.</p>
	 *
	 * @param boardName Board name part of key.
	 * @param key Key name.
	 * @param value Value to store.
	 */
	public final void set(String boardName, String key, String value) {
		BuildConfig.Private.expr(boardName, key, value);
	}

	/**
	 * <p>Returns a chan title. This title is display name used in client.</p>
	 */
	public final String getTitle() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Requests {@code option}. You can do it only during construction.</p>
	 *
	 * @param option Option to request.
	 */
	public final void request(String option) {
		BuildConfig.Private.expr(option);
	}

	/**
	 * <p>Set {@code boardName} argument for all requests when {@link #OPTION_SINGLE_BOARD_MODE} enabled.
	 * That allow you disable this option in the future when chan add multiple boards for example.</p>
	 *
	 * @param boardName Default board name string.
	 */
	public final void setSingleBoardName(String boardName) {
		BuildConfig.Private.expr(boardName);
	}

	/**
	 * <p>Set {@code title} for board with given {@code boardName}.
	 *
	 * @param boardName Board name.
	 * @param title Board title.
	 */
	public final void setBoardTitle(String boardName, String title) {
		BuildConfig.Private.expr(boardName, title);
	}

	/**
	 * <p>Store {@code title} for board with given {@code boardName}.
	 *
	 * @param boardName Board name.
	 * @param title Board title.
	 */
	public final void storeBoardTitle(String boardName, String title) {
		BuildConfig.Private.expr(boardName, title);
	}

	/**
	 * <p>Set {@code description} for board with given {@code boardName}.
	 *
	 * @param boardName Board name.
	 * @param description Board description.
	 */
	public final void setBoardDescription(String boardName, String description) {
		BuildConfig.Private.expr(boardName, description);
	}

	/**
	 * <p>Store {@code description} for board with given {@code boardName}.
	 *
	 * @param boardName Board name.
	 * @param description Board description.
	 */
	public final void storeBoardDescription(String boardName, String description) {
		BuildConfig.Private.expr(boardName, description);
	}

	/**
	 * <p>Set poster's {@code defaultName} for all boards.
	 *
	 * @param defaultName Default name.
	 */
	public final void setDefaultName(String defaultName) {
		BuildConfig.Private.expr(defaultName);
	}

	/**
	 * <p>Set poster's {@code defaultName} for specified {@code boardName}.
	 *
	 * @param boardName Board name.
	 * @param defaultName Default name.
	 */
	public final void setDefaultName(String boardName, String defaultName) {
		BuildConfig.Private.expr(boardName, defaultName);
	}

	/**
	 * <p>Store poster's {@code defaultName} for specified {@code boardName}.
	 *
	 * @param boardName Board name.
	 * @param defaultName Default name.
	 */
	public final void storeDefaultName(String boardName, String defaultName) {
		BuildConfig.Private.expr(boardName, defaultName);
	}

	/**
	 * <p>Set {@code bumpLimit} for all boards.
	 *
	 * @param bumpLimit Bump limit value.
	 */
	public final void setBumpLimit(int bumpLimit) {
		BuildConfig.Private.expr(bumpLimit);
	}

	/**
	 * <p>Set {@code bumpLimit} for specified {@code boardName}.
	 *
	 * @param boardName Board name.
	 * @param bumpLimit Bump limit value.
	 */
	public final void setBumpLimit(String boardName, int bumpLimit) {
		BuildConfig.Private.expr(boardName, bumpLimit);
	}

	/**
	 * <p>Store {@code bumpLimit} for specified {@code boardName}.
	 *
	 * @param boardName Board name.
	 * @param bumpLimit Bump limit value.
	 */
	public final void storeBumpLimit(String boardName, int bumpLimit) {
		BuildConfig.Private.expr(boardName, bumpLimit);
	}

	/**
	 * <p>Set specified {@link BumpLimitMode}.</p>
	 *
	 * @param mode Bump limit mode.
	 */
	public final void setBumpLimitMode(BumpLimitMode mode) {
		BuildConfig.Private.expr(mode);
	}

	/**
	 * <p>Set {@code pagesCount} for {@code boardName}.
	 *
	 * @param boardName Board name.
	 * @param pagesCount Pages count value.
	 */
	public final void setPagesCount(String boardName, int pagesCount) {
		BuildConfig.Private.expr(boardName, pagesCount);
	}

	/**
	 * <p>Store {@code pagesCount} for {@code boardName}.
	 *
	 * @param boardName Board name.
	 * @param pagesCount Pages count value.
	 */
	public final void storePagesCount(String boardName, int pagesCount) {
		BuildConfig.Private.expr(boardName, pagesCount);
	}

	/**
	 * <p>Add captcha type to list of supported capthas. User may choose captcha in application preferences.
	 * Client will obtain configuration of custom captchas with {@link #obtainCustomCaptchaConfiguration(String)}.</p>
	 *
	 * <p>Here is the list of default captcha types: {@link #CAPTCHA_TYPE_RECAPTCHA_2},
	 * {@link #CAPTCHA_TYPE_RECAPTCHA_2_INVISIBLE}, and {@link #CAPTCHA_TYPE_HCAPTCHA}.
	 * Client is able to handle these captchas by itself.</p>
	 *
	 * @param captchaType Captcha type string.
	 */
	public final void addCaptchaType(String captchaType) {
		BuildConfig.Private.expr(captchaType);
	}

	/**
	 * <p>Add custom preference to chan preferences screen. You can obtain a value using
	 * {@link #get(String, String, boolean)} method with {@code null} {@code boardName} argument. You also must override
	 * {@link #obtainCustomPreferenceConfiguration(String)} to configure such values as title and summary.</p>
	 *
	 * @param key Custom preference key.
	 * @param defaultValue Default value.
	 */
	public final void addCustomPreference(String key, boolean defaultValue) {
		BuildConfig.Private.expr(key, defaultValue);
	}

	/**
	 * <p>Calls every time client requests board configuration. You must return new instance of {@link Board}
	 * with configuration for given {@code boardName}.</p>
	 *
	 * <p><strong>The {@code boardName} argument may be {@code null}!</strong> In this case you must return the widest
	 * configuration for your chan.</p>
	 *
	 * @param boardName Board name string.
	 */
	public Board obtainBoardConfiguration(String boardName) {
		return BuildConfig.Private.expr(boardName);
	}

	/**
	 * <p>Calls every time client requests custom captcha configuration. You must return new instance of {@link Captcha}
	 * with configuration for given {@code captchaType}.</p>
	 *
	 * @param captchaType Captcha type string.
	 */
	public Captcha obtainCustomCaptchaConfiguration(String captchaType) {
		return BuildConfig.Private.expr(captchaType);
	}

	/**
	 * <p>Calls every time client requests posting configuration. You must return new instance of {@link Posting}
	 * with configuration for given {@code boardName}.</p>
	 *
	 * <p><strong>The {@code boardName} argument may be {@code null}!</strong> In this case you must return the widest
	 * configuration for your chan.</p>
	 *
	 * @param boardName Board name string.
	 * @param newThread True if user starts new thread.
	 */
	public Posting obtainPostingConfiguration(String boardName, boolean newThread) {
		return BuildConfig.Private.expr(boardName, newThread);
	}

	/**
	 * <p>Calls every time client requests deleting configuration. You must return new instance of {@link Deleting}
	 * with configuration for given {@code boardName}.</p>
	 *
	 * <p><strong>The {@code boardName} argument may be {@code null}!</strong> In this case you must return the widest
	 * configuration for your chan.</p>
	 *
	 * @param boardName Board name string.
	 */
	public Deleting obtainDeletingConfiguration(String boardName) {
		return BuildConfig.Private.expr(boardName);
	}

	/**
	 * <p>Calls every time client requests reporting configuration. You must return new instance of {@link Reporting}
	 * with configuration for given {@code boardName}.</p>
	 *
	 * <p><strong>The {@code boardName} argument may be {@code null}!</strong> In this case you must return the widest
	 * configuration for your chan.</p>
	 *
	 * @param boardName Board name string.
	 */
	public Reporting obtainReportingConfiguration(String boardName) {
		return BuildConfig.Private.expr(boardName);
	}

	/**
	 * <p>Calls every time client requests captcha pass configuration. You must return new instance of
	 * {@link Authorization} with captcha pass configuration.</p>
	 */
	public Authorization obtainCaptchaPassConfiguration() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Calls every time client requests user authorization configuration. You must return new instance of
	 * {@link Authorization} with user authorization configuration.</p>
	 */
	public Authorization obtainUserAuthorizationConfiguration() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Calls every time client requests archivation configuration. You must return new instance of
	 * {@link Archivation} with archivation configuration.</p>
	 */
	public Archivation obtainArchivationConfiguration() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Calls every time client requests statistics configuration. You must return new instance of
	 * {@link Statistics} with statistics configuration.</p>
	 */
	public Statistics obtainStatisticsConfiguration() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Calls every time client requests custom captcha configuration. You must return new instance of
	 * {@link CustomPreference} with configuration for given {@code key}.</p>
	 *
	 * @param key Custom preference key.
	 */
	public CustomPreference obtainCustomPreferenceConfiguration(String key) {
		return BuildConfig.Private.expr(key);
	}

	/**
	 * <p>Returns application context.</p>
	 */
	public final Context getContext() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Returns resources from chan APK file.</p>
	 */
	public final Resources getResources() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Returns URI for resource in chan APK file.</p>
	 *
	 * @param resId Resource ID.
	 */
	public final Uri getResourceUri(int resId) {
		return BuildConfig.Private.expr(resId);
	}

	/**
	 * <p>Returns stored cookie.</p>
	 *
	 * @param cookie Cookie name.
	 * @return Cookie value or null if cookie not found.
	 */
	public final String getCookie(String cookie) {
		return BuildConfig.Private.expr(cookie);
	}


	/**
	 * <p>Stores cookie. You can specify human-friendly displayName of cookie.
	 * User can remove this cookie using cookie manager.</p>
	 *
	 * @param cookie Cookie name.
	 * @param value Cookie value. Set this argument to {@code null} to remove the cookie.
	 * @param displayName Human-friendly name of cookie. May be {@code null} if value is {@code null} too.
	 */
	public final void storeCookie(String cookie, String value, String displayName) {
		BuildConfig.Private.expr(cookie, value, displayName);
	}

	/**
	 * <p>Returns user authorization data. User can specify authorization data when
	 * {@link #OPTION_ALLOW_USER_AUTHORIZATION} enabled.</p>
	 *
	 * @return User authorization fields values.
	 */
	public final String[] getUserAuthorizationData() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Returns download directory.</p>
	 */
	public final DataFile getDownloadDirectory() {
		return BuildConfig.Private.expr();
	}
}
