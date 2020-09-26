package chan.http;

import chan.library.api.BuildConfig;
import java.io.IOException;
import java.io.OutputStream;

/**
 * <p>Simple implementation of {@link RequestEntity}.</p>
 *
 * <p>Method {@link SimpleEntity#add(String, String)} is not supported for this entity.</p>
 */
public class SimpleEntity implements RequestEntity {
	@Override
	public void add(String name, String value) {
		BuildConfig.Private.expr(name, value);
	}

	/**
	 * <p>Sets string {@code data} with UTF-8 encoding.</p>
	 *
	 * @param data String data.
	 */
	public void setData(String data) {
		BuildConfig.Private.expr(data);
	}

	/**
	 * <p>Sets string {@code data} with given {@code charsetName} encoding.</p>
	 *
	 * @param data String data.
	 * @param charsetName Charset name.
	 */
	public void setData(String data, String charsetName) {
		BuildConfig.Private.expr(data, charsetName);
	}

	/**
	 * <p>Sets byte array {@code data}.</p>
	 *
	 * @param data Byte array data.
	 */
	public void setData(byte[] data) {
		BuildConfig.Private.expr(data);
	}

	/**
	 * <p>Sets a content type of entity.</p>
	 *
	 * @param contentType Content type.
	 */
	public void setContentType(String contentType) {
		BuildConfig.Private.expr(contentType);
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
