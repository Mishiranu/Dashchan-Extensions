package chan.http;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

/**
 * <p>Multipart Form Data implementation of {@link RequestEntity}.</p>
 */
public class MultipartEntity implements RequestEntity {
	/**
	 * <p>Default constructor for a {@link MultipartEntity}.</p>
	 */
	public MultipartEntity() {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Constructor for a {@link MultipartEntity}.</p>
	 *
	 * @param alternation Alternation of string field's names and values (name, value, name, value...).
	 */
	public MultipartEntity(String... alternation) {
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

	/**
	 * <p>Add file field to entity with given {@code name} and {@code file}.</p>
	 *
	 * @param name Field name.
	 * @param file File to write.
	 */
	public void add(String name, File file) {
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
