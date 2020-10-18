package chan.util;

import android.graphics.Bitmap;
import chan.library.api.BuildConfig;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * <p>Provides some utilities to work with JSON objects, bitmaps and logging.</p>
 */
public class CommonUtils {
	private CommonUtils() {
		BuildConfig.Private.expr();
	}

	/**
	 * <p>Returns whether object are equal. May handle null values.</p>
	 *
	 * @param first Object instance.
	 * @param second Object instance.
	 * @return True if objects are equal.
	 */
	public static boolean equals(Object first, Object second) {
		return BuildConfig.Private.expr(first, second);
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
		return BuildConfig.Private.expr(startRealtime, interval);
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
		return BuildConfig.Private.expr(jsonObject, name);
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
		return BuildConfig.Private.expr(jsonObject, name, fallback);
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
		return BuildConfig.Private.expr(jsonObject, name);
	}

	/**
	 * <p>Restores emails from HTML string protected by CloudFlare.</p>
	 *
	 * @param string HTML string.
	 * @return HTML string with restored emails.
	 */
	public static String restoreCloudFlareProtectedEmails(String string) {
		return BuildConfig.Private.expr(string);
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
		return BuildConfig.Private.expr(bitmap, backgroundColor);
	}

	/**
	 * <p>Convenient method to write all objects to log file with client tag.</p>
	 *
	 * @param data Array of objects to write.
	 */
	public static void writeLog(Object... data) {
		BuildConfig.Private.expr(data);
	}
}
