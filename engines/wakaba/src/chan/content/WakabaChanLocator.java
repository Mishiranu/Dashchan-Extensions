package chan.content;

import android.net.Uri;
import java.util.List;
import java.util.regex.Pattern;

public class WakabaChanLocator extends ChanLocator {
	protected Pattern boardPath = Pattern.compile("/\\w+(?:/(?:(?:index|\\d+)\\.html)?)?");
	protected Pattern threadPath = Pattern.compile("/\\w+/res/(\\d+)\\.html");
	protected Pattern attachmentPath = Pattern.compile("/\\w+/src/\\d+\\.\\w+");

	@Override
	public boolean isBoardUri(Uri uri) {
		return isChanHostOrRelative(uri) && isPathMatches(uri, boardPath);
	}

	@Override
	public boolean isThreadUri(Uri uri) {
		return isChanHostOrRelative(uri) && isPathMatches(uri, threadPath);
	}

	@Override
	public boolean isAttachmentUri(Uri uri) {
		return isChanHostOrRelative(uri) && isPathMatches(uri, attachmentPath);
	}

	@Override
	public String getBoardName(Uri uri) {
		if (uri != null) {
			List<String> segments = uri.getPathSegments();
			if (segments.size() > 0) {
				return segments.get(0);
			}
		}
		return null;
	}

	@Override
	public String getThreadNumber(Uri uri) {
		return uri != null ? getGroupValue(uri.getPath(), threadPath, 1) : null;
	}

	@Override
	public String getPostNumber(Uri uri) {
		String fragment = uri.getFragment();
		if (fragment != null && fragment.startsWith("i")) {
			return fragment.substring(1);
		}
		return fragment;
	}

	@Override
	public Uri createBoardUri(String boardName, int pageNumber) {
		return pageNumber > 0 ? buildPath(boardName, pageNumber + ".html") : buildPath(boardName, "");
	}

	@Override
	public Uri createThreadUri(String boardName, String threadNumber) {
		return buildPath(boardName, "res", threadNumber + ".html");
	}

	@Override
	public Uri createPostUri(String boardName, String threadNumber, String postNumber) {
		return createThreadUri(boardName, threadNumber).buildUpon().fragment(postNumber).build();
	}

	public Uri createScriptUri(String boardName, String script) {
		return buildPath(boardName, script);
	}
}
