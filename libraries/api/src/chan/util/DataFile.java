package chan.util;

import chan.content.ChanConfiguration;
import chan.library.api.BuildConfig;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * <p>File abstraction.</p>
 *
 * <ul>
 * <li>{@link ChanConfiguration#getDownloadDirectory()}</li>
 * </ul>
 */
public class DataFile {
	private DataFile() {
		BuildConfig.Private.expr();
	}

	/**
	 * <p>Returns the name of the file.</p>
	 *
	 * @return File name.
	 */
	public String getName() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Returns whether file is directory.</p>
	 *
	 * @return True if file is directory.
	 */
	public boolean isDirectory() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Returns the time the file was modified.</p>
	 *
	 * @return Last modified timestamp.
	 */
	public long getLastModified() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Returns a child file in the directory.</p>
	 *
	 * @param path Path to the child.
	 * @return Child file.
	 */
	public DataFile getChild(String path) {
		return BuildConfig.Private.expr(path);
	}

	/**
	 * <p>Returns a list of file names in the directory.</p>
	 *
	 * @return List of children {@link DataFile}.
	 */
	public List<DataFile> getChildren() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Deletes the file.</p>
	 *
	 * @return True if file was successfully deleted.
	 */
	public boolean delete() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Opens the file for reading.</p>
	 *
	 * @return Input stream.
	 * @throws IOException If an I/O error occurs.
	 */
	public InputStream openInputStream() throws IOException {
		BuildConfig.Private.<IOException>error();
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Opens the file for writing.</p>
	 *
	 * @return Output stream.
	 * @throws IOException If an I/O error occurs.
	 */
	public OutputStream openOutputStream() throws IOException {
		BuildConfig.Private.<IOException>error();
		return BuildConfig.Private.expr();
	}
}
