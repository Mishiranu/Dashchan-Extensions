package chan.content.model;

import chan.content.ChanMarkup;
import chan.library.api.BuildConfig;
import java.io.Serializable;
import java.util.Collection;

/**
 * <p>Model containing post data.</p>
 *
 * <p>You can describe thread number and post number with the following methods:</p>
 *
 * <ul>
 * <li>{@link Post#setThreadNumber(String)}</li>
 * <li>{@link Post#setParentPostNumber(String)}</li>
 * <li>{@link Post#setPostNumber(String)}</li>
 * </ul>
 */
public final class Post implements Serializable, Comparable<Post> {
	/**
	 * <p>Returns real thread number with this post.</p>
	 *
	 * @return Thread number.
	 */
	public String getThreadNumber() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Stores thread number in this model. Usually thread number equals original post number, so in most cases
	 * you shouldn't use this method.</p>
	 *
	 * @param threadNumber Thread number.
	 * @return This model.
	 */
	public Post setThreadNumber(String threadNumber) {
		return BuildConfig.Private.expr(threadNumber);
	}

	/**
	 * <p>Returns parent post number.</p>
	 *
	 * @return Parent post number.
	 */
	public String getParentPostNumber() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Stores parent post number for this post. In chan context parent post number must be equal original post
	 * number which means all posts are replies to original one. The parent post number stored for original post
	 * must be {@code null}.</p>
	 *
	 * @param parentPostNumber Parent post number.
	 * @return This model.
	 */
	public Post setParentPostNumber(String parentPostNumber) {
		return BuildConfig.Private.expr(parentPostNumber);
	}

	/**
	 * <p>Returns post number.</p>
	 *
	 * @return Post number.
	 */
	public String getPostNumber() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Stores post number for this post.</p>
	 *
	 * @param postNumber Post number.
	 * @return This model.
	 */
	public Post setPostNumber(String postNumber) {
		return BuildConfig.Private.expr(postNumber);
	}

	/**
	 * <p>Returns date of post created.</p>
	 *
	 * @return Post creation timestamp.
	 */
	public long getTimestamp() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Stores date of post created in this model.</p>
	 *
	 * @param timestamp UNIX timestamp.
	 * @return This model.
	 */
	public Post setTimestamp(long timestamp) {
		return BuildConfig.Private.expr(timestamp);
	}

	/**
	 * <p>Returns post subject.</p>
	 *
	 * @return Post subject.
	 */
	public String getSubject() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Stores post subject in this model.</p>
	 *
	 * @param subject Post subject.
	 * @return This model.
	 */
	public Post setSubject(String subject) {
		return BuildConfig.Private.expr(subject);
	}

	/**
	 * <p>Returns post comment.</p>
	 *
	 * @return Post comment.
	 */
	public String getComment() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Stores post comment in this model.</p>
	 *
	 * @param comment Post comment.
	 * @return This model.
	 */
	public Post setComment(String comment) {
		return BuildConfig.Private.expr(comment);
	}

	/**
	 * <p>Returns post original comment markup.</p>
	 *
	 * <p>This method calls when application want to get original comment markup. By default {@link ChanMarkup} provides
	 * unmark operation, but you can override this method and provide more correct operation if it possible.</p>
	 *
	 * @return Original comment markup.
	 */
	public String getCommentMarkup() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Stores post original comment markup in this model.</p>
	 *
	 * <p>By default {@link ChanMarkup} provides unmark operation. Some chans stores original markup along with
	 * parsed comment, so you can simplify and precisify unmark operation with this method.</p>
	 *
	 * @param commentMarkup Post comment markup.
	 * @return This model.
	 */
	public Post setCommentMarkup(String commentMarkup) {
		return BuildConfig.Private.expr(commentMarkup);
	}

	/**
	 * <p>Returns poster name.</p>
	 *
	 * @return Poster name.
	 */
	public String getName() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Stores poster name in this model.</p>
	 *
	 * @param name Poster name.
	 * @return This model.
	 */
	public Post setName(String name) {
		return BuildConfig.Private.expr(name);
	}

	/**
	 * <p>Returns poster identifier.</p>
	 *
	 * @return Poster identifier.
	 */
	public String getIdentifier() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Stores poster identifier (an unique poster number or name within the thread) in this model.</p>
	 *
	 * @param identifier Poster identifier.
	 * @return This model.
	 */
	public Post setIdentifier(String identifier) {
		return BuildConfig.Private.expr(identifier);
	}

	/**
	 * <p>Returns poster tripcode.</p>
	 *
	 * @return Poster tripcode.
	 */
	public String getTripcode() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Stores poster tripcode in this model. Tripcode <strong>must include</strong> {@code !} characters.</p>
	 *
	 * @param tripcode Poster tripcode.
	 * @return This model.
	 */
	public Post setTripcode(String tripcode) {
		return BuildConfig.Private.expr(tripcode);
	}

	/**
	 * <p>Returns poster capcode.</p>
	 *
	 * @return Poster capcode.
	 */
	public String getCapcode() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Stores poster capcode in this model. Capcode <strong>must not contain</strong> {@code #} characters.</p>
	 *
	 * @param capcode Poster capcode.
	 * @return This model.
	 */
	public Post setCapcode(String capcode) {
		return BuildConfig.Private.expr(capcode);
	}

	/**
	 * <p>Returns poster email.</p>
	 *
	 * @return Poster email.
	 */
	public String getEmail() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Stores poster email in this model. You must handle "sage" mails by yourself using
	 * {@link #setSage(boolean)} method.</p>
	 *
	 * @param email Poster email.
	 * @return This model.
	 */
	public Post setEmail(String email) {
		return BuildConfig.Private.expr(email);
	}

	/**
	 * <p>Returns attachments count.</p>
	 *
	 * @return Attachments count.
	 */
	public int getAttachmentsCount() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Returns attachment at given {@code index}.</p>
	 *
	 * @return {@link Attachment} instance.
	 */
	public Attachment getAttachmentAt(int index) {
		return BuildConfig.Private.expr(index);
	}

	/**
	 * <p>Stores attachments in this model.</p>
	 *
	 * @param attachments Array of {@link Attachment}.
	 * @return This model.
	 */
	public Post setAttachments(Attachment... attachments) {
		return BuildConfig.Private.expr(attachments);
	}

	/**
	 * <p>Stores attachments in this model.</p>
	 *
	 * @param attachments Collection of {@link Attachment}.
	 * @return This model.
	 */
	@SuppressWarnings("unchecked")
	public Post setAttachments(Collection<? extends Attachment> attachments) {
		return BuildConfig.Private.expr(attachments);
	}

	/**
	 * <p>Returns icons count.</p>
	 *
	 * @return Icons count.
	 */
	public int getIconsCount() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Returns icon at given {@code index}.</p>
	 *
	 * @return {@link Icon} instance.
	 */
	public Icon getIconAt(int index) {
		return BuildConfig.Private.expr(index);
	}

	/**
	 * <p>Stores icons in this model.</p>
	 *
	 * @param icons Array of {@link Icon}.
	 * @return This model.
	 */
	public Post setIcons(Icon... icons) {
		return BuildConfig.Private.expr(icons);
	}

	/**
	 * <p>Stores icons in this model.</p>
	 *
	 * @param icons Collection of {@link Icon}.
	 * @return This model.
	 */
	@SuppressWarnings("unchecked")
	public Post setIcons(Collection<? extends Icon> icons) {
		return BuildConfig.Private.expr(icons);
	}

	/**
	 * <p>Returns whether post contains sage mark.</p>
	 *
	 * @return True if posts contains sage mark.
	 */
	public boolean isSage() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Stores whether post contains sage mark (in email or another field) and doesn't bump a thread.</p>
	 *
	 * @param sage True if post contains sage mark, false otherwise.
	 * @return This model.
	 */
	public Post setSage(boolean sage) {
		return BuildConfig.Private.expr(sage);
	}

	/**
	 * <p>Returns whether thread is sticky.</p>
	 *
	 * @return True if thread is sticky.
	 */
	public boolean isSticky() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Stores whether thread is sticky. Will be ignored by application if post is not original.</p>
	 *
	 * @param sticky True if thread is sticky, false otherwise.
	 * @return This model.
	 */
	public Post setSticky(boolean sticky) {
		return BuildConfig.Private.expr(sticky);
	}

	/**
	 * <p>Returns whether thread is closed.</p>
	 *
	 * @return True if thread is closed.
	 */
	public boolean isClosed() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Stores whether thread is closed. Will be ignored by application if post is not original.</p>
	 *
	 * @param closed True if thread is closed, false otherwise.
	 * @return This model.
	 */
	public Post setClosed(boolean closed) {
		return BuildConfig.Private.expr(closed);
	}

	/**
	 * <p>Returns whether thread is archived.</p>
	 *
	 * @return True if thread is archived.
	 */
	public boolean isArchived() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Stores whether thread is archived. Will be ignored by application if post is not original.</p>
	 *
	 * @param archived True if thread is archived, false otherwise.
	 * @return This model.
	 */
	public Post setArchived(boolean archived) {
		return BuildConfig.Private.expr(archived);
	}

	/**
	 * <p>Returns whether thread is cyclical.</p>
	 *
	 * @return Trust if thread is cyclical.
	 */
	public boolean isCyclical() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Stores whether thread is cyclical. Will be ignored by application if post is not original.</p>
	 *
	 * @param cyclical True if thread is cyclical, false otherwise.
	 * @return This model.
	 */
	public Post setCyclical(boolean cyclical) {
		return BuildConfig.Private.expr(cyclical);
	}

	/**
	 * <p>Returns whether poster was warned by moderator.</p>
	 *
	 * @return True if poster is warned.
	 */
	public boolean isPosterWarned() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Stores whether poster was warned by moderator.</p>
	 *
	 * @param posterWarned True if poster was warned, false otherwise.
	 * @return This model.
	 */
	public Post setPosterWarned(boolean posterWarned) {
		return BuildConfig.Private.expr(posterWarned);
	}

	/**
	 * <p>Returns whether poster was banned by moderator.</p>
	 *
	 * @return True if poster is banned.
	 */
	public boolean isPosterBanned() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Stores whether poster was banned by moderator.</p>
	 *
	 * @param posterBanned True if poster was banned, false otherwise.
	 * @return This model.
	 */
	public Post setPosterBanned(boolean posterBanned) {
		return BuildConfig.Private.expr(posterBanned);
	}

	/**
	 * <p>Returns whether post was written by original poster.</p>
	 *
	 * @return True if poster is original poster.
	 */
	public boolean isOriginalPoster() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Stores whether post was written by original poster.</p>
	 *
	 * @param originalPoster True if post was written by original poster, false otherwise.
	 * @return This model.
	 */
	public Post setOriginalPoster(boolean originalPoster) {
		return BuildConfig.Private.expr(originalPoster);
	}

	/**
	 * <p>Returns whether poster name is default.</p>
	 *
	 * @return True if name is default.
	 */
	public boolean isDefaultName() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Stores whether poster name is default. In this case it may be hidden in posts list.</p>
	 *
	 * @param defaultName True if poster name is default, false otherwise.
	 * @return This model.
	 */
	public Post setDefaultName(boolean defaultName) {
		return BuildConfig.Private.expr(defaultName);
	}

	/**
	 * <p>Returns whether bump limit is reached.</p>
	 *
	 * @return True if bump limit is reached.
	 */
	public boolean isBumpLimitReached() {
		return BuildConfig.Private.expr();
	}

	/**
	 * <p>Stores whether thread reached a bump limit. In this case user will see an icon.</p>
	 *
	 * @param bumpLimitReached True if bump limit reached, false otherwise.
	 * @return This model.
	 */
	public Post setBumpLimitReached(boolean bumpLimitReached) {
		return BuildConfig.Private.expr(bumpLimitReached);
	}

	/**
	 * Compares this post with specified post.
	 *
	 * @param another Post to compare with.
	 * @return Integer value which represents {@code Comparable} result.
	 */
	@SuppressWarnings("NullableProblems")
	@Override
	public int compareTo(Post another) {
		return BuildConfig.Private.expr(another);
	}
}
