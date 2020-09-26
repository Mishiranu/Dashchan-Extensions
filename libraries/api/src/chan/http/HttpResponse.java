package chan.http;

import android.graphics.Bitmap;
import chan.library.api.BuildConfig;
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
		BuildConfig.Private.expr(bytes);
	}

	/**
	 * <p>Sets encoding for this instance. UTF-8 is used by default.</p>
	 */
	public void setEncoding(String charsetName) {
		BuildConfig.Private.expr(charsetName);
	}

	/**
	 * <p>Returns raw response as string.</p>
	 *
	 * @return Byte array response.
	 */
	public byte[] getBytes() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Decodes and returns response as string.</p>
	 *
	 * @return String response.
	 */
	public String getString() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Decodes and returns response as {@code Bitmap}.</p>
	 *
	 * @return Bitmap response or {@code null} if response is not bitmap.
	 */
	public Bitmap getBitmap() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Decodes and returns response as {@code JSONObject}.</p>
	 *
	 * @return JSON object response or {@code null} if response is not JSON object.
	 */
	public JSONObject getJsonObject() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Decodes and returns response as {@code JSONArray}.</p>
	 *
	 * @return JSON array response or {@code null} if response is not JSON array.
	 */
	public JSONArray getJsonArray() {
		return BuildConfig.Private.expr();
	}
}
