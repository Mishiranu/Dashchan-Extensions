package chan.http;

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
		throw new IllegalAccessError();
	}

	/**
	 * <p>Constructor for an {@link UrlEncodedEntity}.</p>
	 *
	 * @param alternation Alternation of string field's names and values (name, value, name, value...).
	 */
	public UrlEncodedEntity(String... alternation) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Changes encoding type for this entity. By default UTF-8 is used.</p>
	 *
	 * @param charsetName Charset name.
	 */
	public void setEncoding(String charsetName) {
		throw new IllegalAccessError();
	}

	@Override
	public void add(String name, String value) {
		throw new IllegalAccessError();
	}

	@Override
	public String getContentType() {
		throw new IllegalAccessError();
	}

	@Override
	public long getContentLength() {
		throw new IllegalAccessError();
	}

	@Override
	public void write(OutputStream output) throws IOException {
		throw new IllegalAccessError();
	}

	@Override
	public RequestEntity copy() {
		throw new IllegalAccessError();
	}
}
