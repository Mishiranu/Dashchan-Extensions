package chan.http;

import android.graphics.Bitmap;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * <p>HTTP response holder.</p>
 */
public class HttpResponse {
	/**
	 * <p>Constructor for {@link HttpResponse}.</p>
	 *
	 * @param bytes Byte array of data.
	 */
	public HttpResponse(byte[] bytes) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Sets encoding for this instance. UTF-8 is used by default.</p>
	 */
	public void setEncoding(String charsetName) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Returns raw response as string.</p>
	 *
	 * @return Byte array response.
	 */
	public byte[] getBytes() {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Decodes and returns response as string.</p>
	 *
	 * @return String response.
	 */
	public String getString() {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Decodes and returns response as {@code Bitmap}.</p>
	 *
	 * @return Bitmap response or {@code null} if response is not bitmap.
	 */
	public Bitmap getBitmap() {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Decodes and returns response as {@code JSONObject}.</p>
	 *
	 * @return JSON object response or {@code null} if response is not JSON object.
	 */
	public JSONObject getJsonObject() {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Decodes and returns response as {@code JSONArray}.</p>
	 *
	 * @return JSON array response or {@code null} if response is not JSON array.
	 */
	public JSONArray getJsonArray() {
		throw new IllegalAccessError();
	}
}
