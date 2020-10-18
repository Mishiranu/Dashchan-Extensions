package chan.content.model;

import android.net.Uri;
import chan.content.ChanLocator;
import chan.library.api.BuildConfig;

/**
 * <p>Model containing post icon.</p>
 */
public final class Icon {
	/**
	 * <p>Constructor for {@link Icon}.</p>
	 *
	 * @param locator {@link ChanLocator} instance to encode URI in model.
	 * @param uri Icon URI.
	 * @param title Icon title.
	 */
	public Icon(ChanLocator locator, Uri uri, String title) {
		BuildConfig.Private.expr(locator, uri, title);
	}

	/**
	 * <p>Returns icon URI.</p>
	 *
	 * @param locator {@link ChanLocator} instance to decode URI in model.
	 */
	public Uri getUri(ChanLocator locator) {
		return BuildConfig.Private.expr(locator);
	}

	/**
	 * <p>Returns icon title.</p>
	 *
	 * @return Icon title.
	 */
	public String getTitle() {
		return BuildConfig.Private.expr();
	}
}
