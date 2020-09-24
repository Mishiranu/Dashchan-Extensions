package chan.util;

import android.graphics.Bitmap;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * <p>Provides some utilities to work with JSON objects, bitmaps and logging.</p>
 */
public class CommonUtils {
	CommonUtils() {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Wait time = {@code interval} - (current time - {@code startRealtime}).
	 * Returns whether thread was interrupted during sleep.</p>
	 *
	 * @param startRealtime Time when operation was started.
	 * @param interval Minimum time for operation.
	 * @return True if thread was interrupted.
	 */
	public static boolean sleepMaxRealtime(long startRealtime, long interval) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Returns the value mapped by {@code name} if exists, coercing it if necessary, or {@code null} value
	 * if no such mapping exists. This method may handle null values.</p>
	 *
	 * @param jsonObject JSONObject from which the value will be taken.
	 * @param name Field name.
	 * @return Value mapped by name.
	 */
	public static String optJsonString(JSONObject jsonObject, String name) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Returns the value mapped by {@code name} if exists, coercing it if necessary, or {@code fallback} value
	 * if no such mapping exists. This method may handle null values.</p>
	 *
	 * @param jsonObject JSONObject from which the value will be taken.
	 * @param name Field name.
	 * @return Value mapped by name.
	 */
	public static String optJsonString(JSONObject jsonObject, String name, String fallback) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Returns the value mapped by {@code name} if exists, coercing it if necessary, or throws
	 * if no such mapping exists. This method may handle null values.</p>
	 *
	 * @param jsonObject JSONObject from which the value will be taken.
	 * @param name Field name.
	 * @return Value mapped by name.
	 * @throws JSONException If no such mapping exists.
	 */
	public static String getJsonString(JSONObject jsonObject, String name) throws JSONException {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Restores emails from HTML string protected by CloudFlare.</p>
	 *
	 * @param string HTML string.
	 * @return HTML string with restored emails.
	 */
	public static String restoreCloudFlareProtectedEmails(String string) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Trims bitmap removing empty lines on the edges of image. May return {@code null} bitmap.
	 * If bitmap wasn't trimmed, this method will return original bitmap.</p>
	 *
	 * @param bitmap Bitmap to trim.
	 * @param backgroundColor Background color.
	 * @return Trimmed bitmap.
	 */
	public static Bitmap trimBitmap(Bitmap bitmap, int backgroundColor) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Convenient method to write all objects to log file with client tag.</p>
	 *
	 * @param data Array of objects to write.
	 */
	public static void writeLog(Object... data) {
		throw new IllegalAccessError();
	}
}
