package chan.content;

import android.util.Pair;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FoolFuukaChanMarkup extends ChanMarkup {
	public FoolFuukaChanMarkup() {
		addTag("pre", TAG_CODE);
		addTag("span", "spoiler", TAG_SPOILER);
		addTag("span", "greentext", TAG_QUOTE);
	}

	private static final Pattern THREAD_LINK = Pattern.compile("thread/(\\d+)/(?:#(\\d+))?$");

	@Override
	public Pair<String, String> obtainPostLinkThreadPostNumbers(String uriString) {
		Matcher matcher = THREAD_LINK.matcher(uriString);
		if (matcher.find()) {
			return new Pair<>(matcher.group(1), matcher.group(2));
		}
		return null;
	}
}
