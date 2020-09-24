package chan.http;

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
		throw new IllegalAccessError();
	}

	/**
	 * <p>Sets string {@code data} with UTF-8 encoding.</p>
	 *
	 * @param data String data.
	 */
	public void setData(String data) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Sets string {@code data} with given {@code charsetName} encoding.</p>
	 *
	 * @param data String data.
	 * @param charsetName Charset name.
	 */
	public void setData(String data, String charsetName) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Sets byte array {@code data}.</p>
	 *
	 * @param data Byte array data.
	 */
	public void setData(byte[] data) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Sets a content type of entity.</p>
	 *
	 * @param contentType Content type.
	 */
	public void setContentType(String contentType) {
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
