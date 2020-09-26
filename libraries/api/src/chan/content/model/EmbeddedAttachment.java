package chan.content.model;

import android.net.Uri;
import chan.library.api.BuildConfig;

/**
 * <p>This class can handle some embedded links. See {@link EmbeddedAttachment#obtain(String)}.</p>
 */
public final class EmbeddedAttachment implements Attachment {
	/**
	 * <p>Embedded file content type.</p>
	 */
	public enum ContentType {
		/**
		 * <p>Audio file.</p>
		 */
		AUDIO,

		/**
		 * <p>Video file.</p>
		 */
		VIDEO
	}

	/**
	 * <p>Constructor for {@link EmbeddedAttachment}.</p>
	 *
	 * <p>By default file name will be obtain from the last path segment of file URI. You can override this name
	 * with {@code forcedName} argument.</p>
	 *
	 * @param fileUri File or page URI.
	 * @param thumbnailUri Thumbnail URI.
	 * @param embeddedType Embedded file type string. For example, "YouTube".
	 * @param contentType Content type.
	 * @param canDownload If fileUri is direct file URI, you can pass {@code true}. This allows user to download file.
	 * @param forcedName Overridden file name.
	 */
	public EmbeddedAttachment(Uri fileUri, Uri thumbnailUri, String embeddedType, ContentType contentType,
			boolean canDownload, String forcedName) {
		BuildConfig.Private.expr(fileUri, thumbnailUri, embeddedType, contentType, canDownload, forcedName);
	}

	/**
	 * <p>Returns attachment file URI.</p>
	 *
	 * @return Attachment file URI.
	 */
	public Uri getFileUri() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Returns attachment thumbnail URI.</p>
	 *
	 * @return Attachment thumbnail URI.
	 */
	public Uri getThumbnailUri() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Returns attachment embedded type.</p>
	 *
	 * @return Attachment type.
	 */
	public String getEmbeddedType() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Returns attachment content type.</p>
	 *
	 * @return Attachment content type.
	 */
	public ContentType getContentType() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Returns whether attachment can be downloaded by its file URI.</p>
	 *
	 * @return True is attachment can be downloaded.
	 */
	public boolean isCanDownload() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Returns forced file name.</p>
	 *
	 * @return Forced file name.
	 */
	public String getForcedName() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Returns {@link EmbeddedAttachment} if client support this type by itself.</p>
	 *
	 * <p>List of supported embedded types:</p>
	 *
	 * <ul>
	 * <li>YouTube</li>
	 * <li>Vimeo</li>
	 * <li>Vocaroo</li>
	 * </ul>
	 *
	 * <p>You can pass any string as {@code data} argument that contains links from the list above including strings
	 * with {@code embed} or {@code iframe} HTML tags. Application will try to find links by itself.</p>
	 *
	 * @param data String with URI.
	 * @return {@link EmbeddedAttachment} instance or {@code null} if embedded link is not supported.
	 */
	public static EmbeddedAttachment obtain(String data) {
		return BuildConfig.Private.expr(data);
	}
}
