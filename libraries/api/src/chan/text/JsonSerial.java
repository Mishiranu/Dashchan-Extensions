package chan.text;

import chan.library.api.BuildConfig;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * <p>JSON serial parser/generator.</p>
 */
public class JsonSerial {
	/**
	 * <p>Creates JSON parser using provided {@code input}.</p>
	 *
	 * @param input Byte array JSON data.
	 * @return {@link Reader} instance.
	 */
	public static Reader reader(byte[] input) throws IOException, ParseException {
		BuildConfig.Private.<IOException>error();
		BuildConfig.Private.<ParseException>error();
		return BuildConfig.Private.expr(input);
	}

	/**
	 * <p>Creates JSON parser using provided {@code input}.</p>
	 *
	 * @param input Input stream JSON data.
	 * @return {@link Reader} instance.
	 */
	public static Reader reader(InputStream input) throws IOException, ParseException {
		BuildConfig.Private.<IOException>error();
		BuildConfig.Private.<ParseException>error();
		return BuildConfig.Private.expr(input);
	}

	/**
	 * <p>Creates JSON generator using in-memory byte array.</p>
	 *
	 * @return {@link Writer} instance.
	 */
	public static Writer writer() throws IOException {
		BuildConfig.Private.<IOException>error();
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Creates JSON generator using provided {@code output}.</p>
	 *
	 * @param output Output stream.
	 * @return {@link Writer} instance.
	 */
	public static Writer writer(OutputStream output) throws IOException {
		BuildConfig.Private.<IOException>error();
		return BuildConfig.Private.expr(output);
	}

	/**
	 * <p>Value type, used with {@link Reader#valueType()}.</p>
	 */
	public enum ValueType {
		/**
		 * <p>Scalar value (string, integer, boolean).</p>
		 */
		SCALAR,
		/**
		 * <p>Start of object.</p>
		 */
		OBJECT,
		/**
		 * <p>Start of array.</p>
		 */
		ARRAY
	}

	/**
	 * <p>JSON parser.</p>
	 */
	public interface Reader extends Closeable {
		/**
		 * <p>Reads starting marker of an object.</p>
		 */
		void startObject() throws IOException, ParseException;

		/**
		 * <p>Reads starting marker of an array.</p>
		 */
		void startArray() throws IOException, ParseException;

		/**
		 * <p>Reads ending marker of an object or array.</p>
		 *
		 * @return True if object or array is ended, false otherwise.
		 */
		boolean endStruct() throws IOException, ParseException;

		/**
		 * <p>Reads object field name.</p>
		 */
		String nextName() throws IOException, ParseException;

		/**
		 * <p>Returns the type of the current value.</p>
		 */
		ValueType valueType() throws IOException, ParseException;

		/**
		 * <p>Reads integer value.</p>
		 */
		int nextInt() throws IOException, ParseException;

		/**
		 * <p>Reads long value.</p>
		 */
		long nextLong() throws IOException, ParseException;

		/**
		 * <p>Reads double value.</p>
		 */
		double nextDouble() throws IOException, ParseException;

		/**
		 * <p>Reads boolean value.</p>
		 */
		boolean nextBoolean() throws IOException, ParseException;

		/**
		 * <p>Reads string value.</p>
		 */
		String nextString() throws IOException, ParseException;

		/**
		 * <p>Skips current object or array. Can be used at the start of the object or array only. Does nothing
		 * for scalar value. Throws {@link ParseException} if field name or object/array ending is expected.</p>
		 */
		void skip() throws IOException, ParseException;
	}

	/**
	 * <p>JSON generator.</p>
	 */
	public interface Writer extends Closeable {
		/**
		 * <p>Writes starting marker of an object.</p>
		 */
		void startObject() throws IOException;

		/**
		 * <p>Writes ending marker of an object.</p>
		 */
		void endObject() throws IOException;

		/**
		 * <p>Writes starting marker of an array.</p>
		 */
		void startArray() throws IOException;

		/**
		 * <p>Writes ending marker of an array.</p>
		 */
		void endArray() throws IOException;

		/**
		 * <p>Writes object field name.</p>
		 */
		void name(String name) throws IOException;

		/**
		 * <p>Writes integer value.</p>
		 */
		void value(int value) throws IOException;

		/**
		 * <p>Writes long value.</p>
		 */
		void value(long value) throws IOException;

		/**
		 * <p>Writes double value.</p>
		 */
		void value(double value) throws IOException;

		/**
		 * <p>Writes boolean value.</p>
		 */
		void value(boolean value) throws IOException;

		/**
		 * <p>Writes string value.</p>
		 */
		void value(String value) throws IOException;

		/**
		 * <p>Flushes buffered content to the underlying output.</p>
		 */
		void flush() throws IOException;

		/**
		 * <p>Creates a byte array with JSON data.</p>
		 */
		byte[] build() throws IOException;
	}
}
