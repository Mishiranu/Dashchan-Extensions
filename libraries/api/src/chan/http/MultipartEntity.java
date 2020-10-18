package chan.http;

import chan.library.api.BuildConfig;
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
		BuildConfig.Private.expr();
	}

	/**
	 * <p>Constructor for a {@link MultipartEntity}.</p>
	 *
	 * @param alternation Alternation of string field's names and values (name, value, name, value...).
	 */
	public MultipartEntity(String... alternation) {
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

	/**
	 * <p>Add file field to entity with given {@code name} and {@code file}.</p>
	 *
	 * @param name Field name.
	 * @param file File to write.
	 */
	public void add(String name, File file) {
		BuildConfig.Private.expr(name, file);
	}

	@Override
	public String getContentType() {
		return BuildConfig.Private.expr();
	}

	@Override
	public long getContentLength() {
		return BuildConfig.Private.expr();
	}

	@Override
	public void write(OutputStream output) throws IOException {
		BuildConfig.Private.<IOException>error();
		BuildConfig.Private.expr(output);
	}

	@Override
	public RequestEntity copy() {
		return BuildConfig.Private.expr();
	}
}
