package chan.content.model;

import android.net.Uri;
import chan.content.ChanLocator;

/**
 * <p>Model containing attached file data.</p>
 *
 * <p>Use {@link FileAttachment#setFileUri(ChanLocator, Uri)} to store attachment file URI.</p>
 *
 * <p>Use {@link FileAttachment#setThumbnailUri(ChanLocator, Uri)} to store attachment thumbnail URI.</p>
 *
 * <p>If this file contains width, height or size data, you can use {@link FileAttachment#setWidth(int)},
 * {@link FileAttachment#setHeight(int)} and {@link FileAttachment#setSize(int)} respectively to store them.</p>
 *
 * <p>If this file is embedded frame like YouTube or Vocaroo, use {@link EmbeddedAttachment} class.</p>
 */
public final class FileAttachment implements Attachment {
	/**
	 * <p>Returns attachment file URI.</p>
	 *
	 * @param locator {@link ChanLocator} instance to decode URI in model.
	 * @return Attachment file URI.
	 */
	public Uri getFileUri(ChanLocator locator) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Encodes and stores attachment file URI in this model.</p>
	 *
	 * @param locator {@link ChanLocator} instance to encode URI in model.
	 * @param fileUri Attachment file URI.
	 * @return This model.
	 */
	public FileAttachment setFileUri(ChanLocator locator, Uri fileUri) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Returns attachment thumbnail URI.</p>
	 *
	 * @param locator {@link ChanLocator} instance to decode URI in model.
	 * @return Attachment thumbnail URI.
	 */
	public Uri getThumbnailUri(ChanLocator locator) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Encodes and stores attachment thumbnail URI in this model.</p>
	 *
	 * @param locator {@link ChanLocator} instance to encode URI in model.
	 * @param thumbnailUri Attachment thumbnail URI.
	 * @return This model.
	 */
	public FileAttachment setThumbnailUri(ChanLocator locator, Uri thumbnailUri) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Returns original file name (file name before uploading).</p>
	 *
	 * @return Original file name.
	 */
	public String getOriginalName() {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Stores original file name (file name before uploading) in this model.</p>
	 *
	 * @param originalName Original file name.
	 * @return This model.
	 */
	public FileAttachment setOriginalName(String originalName) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Returns file size in bytes.</p>
	 *
	 * @return File size.
	 */
	public int getSize() {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Stores file size in bytes in this model.</p>
	 *
	 * @param size File size in bytes.
	 * @return This model.
	 */
	public FileAttachment setSize(int size) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Returns file width in pixels.</p>
	 *
	 * @return File width.
	 */
	public int getWidth() {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Stores file width in this model.</p>
	 *
	 * @param width File width in pixels.
	 * @return This model.
	 */
	public FileAttachment setWidth(int width) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Returns file height in pixels.</p>
	 *
	 * @return File height.
	 */
	public int getHeight() {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Stores file height in this model.</p>
	 *
	 * @param height File height in pixels.
	 * @return This model.
	 */
	public FileAttachment setHeight(int height) {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Returns whether file is spoiler.</p>
	 *
	 * @return Whether file is spoiler.
	 */
	public boolean isSpoiler() {
		throw new IllegalAccessError();
	}

	/**
	 * <p>Stores whether file is spoiler in this model.</p>
	 *
	 * @param spoiler True if file is spoiler, false otherwise.
	 * @return This model.
	 */
	public FileAttachment setSpoiler(boolean spoiler) {
		throw new IllegalAccessError();
	}
}
