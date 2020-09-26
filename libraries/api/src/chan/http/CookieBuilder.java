package chan.http;

import chan.library.api.BuildConfig;

/**
 * <p>Provides easy cookie building.</p>
 */
public final class CookieBuilder {
	/**
	 * <p>Append cookie with given {@code name} and {@code value}.</p>
	 *
	 * @param name Cookie name.
	 * @param value Cookie value.
	 * @return This builder.
	 */
	public CookieBuilder append(String name, String value) {
		return BuildConfig.Private.expr(name, value);
	}

	/**
	 * <p>Constructs cookie string.</p>
	 *
	 * @return Cookie string.
	 */
	public String build() {
		return BuildConfig.Private.expr();
	}
}
