package chan.http;

import chan.library.api.BuildConfig;
import java.io.IOException;
import java.io.OutputStream;

/**
 * <p>URL Encoded implementation of {@link RequestEntity}.</p>
 */
public class UrlEncodedEntity implements RequestEntity {
	/**
	 * <p>Default constructor for an {@link UrlEncodedEntity}.</p>
	 */
	public UrlEncodedEntity() {
		BuildConfig.Private.expr();
	}

	/**
	 * <p>Constructor for an {@link UrlEncodedEntity}.</p>
	 *
	 * @param alternation Alternation of string field's names and values (name, value, name, value...).
	 */
	public UrlEncodedEntity(String... alternation) {
		BuildConfig.Private.expr(alternation);
	}

	/**
	 * <p>Changes encoding type for this entity. By default UTF-8 is used.</p>
	 *
	 * @param charsetName Charset name.
	 */
	public void setEncoding(String charsetName) {
		BuildConfig.Private.expr(charsetName);
	}

	@Override
	public void add(String name, String value) {
		BuildConfig.Private.expr(name, value);
	}

	@Override
	public String getContentType() {
		return BuildConfig.Private.expr();
	}

	@Override
	public long getContentLength() {
		return BuildConfig.Private.expr();
	}

	@SuppressWarnings("RedundantThrows")
	@Override
	public void write(OutputStream output) throws IOException {
		BuildConfig.Private.expr(output);
	}

	@Override
	public RequestEntity copy() {
		return BuildConfig.Private.expr();
	}
}
