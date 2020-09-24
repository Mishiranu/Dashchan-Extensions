package chan.util;

import chan.content.ChanConfiguration;
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
		throw new IllegalAccessError();
	}

	/**
	 * <p>Returns the name of the file.</p>
	 *
	 * @return File name.
	 */
	public String getName() {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Returns whether file is directory.</p>
	 *
	 * @return True if file is directory.
	 */
	public boolean isDirectory() {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Returns the time the file was modified.</p>
	 *
	 * @return Last modified timestamp.
	 */
	public long getLastModified() {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Returns a child file in the directory.</p>
	 *
	 * @param path Path to the child.
	 * @return Child file.
	 */
	public DataFile getChild(String path) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Returns a list of file names in the directory.</p>
	 *
	 * @return List of children {@link DataFile}.
	 */
	public List<DataFile> getChildren() {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Deletes the file.</p>
	 *
	 * @return True if file was successfully deleted.
	 */
	public boolean delete() {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Opens the file for reading.</p>
	 *
	 * @return Input stream.
	 * @throws IOException If an I/O error occurs.
	 */
	public InputStream openInputStream() throws IOException {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Opens the file for writing.</p>
	 *
	 * @return Output stream.
	 * @throws IOException If an I/O error occurs.
	 */
	public OutputStream openOutputStream() throws IOException {
		throw new IllegalAccessError();
	}
}
