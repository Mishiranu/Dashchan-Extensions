package chan.content.model;

import android.net.Uri;
import chan.content.ChanLocator;
import chan.library.api.BuildConfig;

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
		return BuildConfig.Private.expr(locator);
	}

	/**
	 * <p>Encodes and stores attachment file URI in this model.</p>
	 *
	 * @param locator {@link ChanLocator} instance to encode URI in model.
	 * @param fileUri Attachment file URI.
	 * @return This model.
	 */
	public FileAttachment setFileUri(ChanLocator locator, Uri fileUri) {
		return BuildConfig.Private.expr(locator, fileUri);
	}

	/**
	 * <p>Returns attachment thumbnail URI.</p>
	 *
	 * @param locator {@link ChanLocator} instance to decode URI in model.
	 * @return Attachment thumbnail URI.
	 */
	public Uri getThumbnailUri(ChanLocator locator) {
		return BuildConfig.Private.expr(locator);
	}

	/**
	 * <p>Encodes and stores attachment thumbnail URI in this model.</p>
	 *
	 * @param locator {@link ChanLocator} instance to encode URI in model.
	 * @param thumbnailUri Attachment thumbnail URI.
	 * @return This model.
	 */
	public FileAttachment setThumbnailUri(ChanLocator locator, Uri thumbnailUri) {
		return BuildConfig.Private.expr(locator, thumbnailUri);
	}

	/**
	 * <p>Returns original file name (file name before uploading).</p>
	 *
	 * @return Original file name.
	 */
	public String getOriginalName() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Stores original file name (file name before uploading) in this model.</p>
	 *
	 * @param originalName Original file name.
	 * @return This model.
	 */
	public FileAttachment setOriginalName(String originalName) {
		return BuildConfig.Private.expr(originalName);
	}

	/**
	 * <p>Returns file size in bytes.</p>
	 *
	 * @return File size.
	 */
	public int getSize() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Stores file size in bytes in this model.</p>
	 *
	 * @param size File size in bytes.
	 * @return This model.
	 */
	public FileAttachment setSize(int size) {
		return BuildConfig.Private.expr(size);
	}

	/**
	 * <p>Returns file width in pixels.</p>
	 *
	 * @return File width.
	 */
	public int getWidth() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Stores file width in this model.</p>
	 *
	 * @param width File width in pixels.
	 * @return This model.
	 */
	public FileAttachment setWidth(int width) {
		return BuildConfig.Private.expr(width);
	}

	/**
	 * <p>Returns file height in pixels.</p>
	 *
	 * @return File height.
	 */
	public int getHeight() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Stores file height in this model.</p>
	 *
	 * @param height File height in pixels.
	 * @return This model.
	 */
	public FileAttachment setHeight(int height) {
		return BuildConfig.Private.expr(height);
	}

	/**
	 * <p>Returns whether file is spoiler.</p>
	 *
	 * @return Whether file is spoiler.
	 */
	public boolean isSpoiler() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Stores whether file is spoiler in this model.</p>
	 *
	 * @param spoiler True if file is spoiler, false otherwise.
	 * @return This model.
	 */
	public FileAttachment setSpoiler(boolean spoiler) {
		return BuildConfig.Private.expr(spoiler);
	}
}
