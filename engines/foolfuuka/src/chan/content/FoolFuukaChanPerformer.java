package chan.content;

import android.net.Uri;
import chan.http.HttpException;
import chan.http.HttpRequest;
import chan.http.HttpResponse;
import chan.text.ParseException;
import java.net.HttpURLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FoolFuukaChanPerformer extends ChanPerformer {
	@Override
	public ReadThreadsResult onReadThreads(ReadThreadsData data) throws HttpException, InvalidResponseException {
		FoolFuukaChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildPath(data.boardName, "page", Integer.toString(data.pageNumber + 1), "");
		String responseText = new HttpRequest(uri, data).setValidator(data.validator).perform().readString();
		try {
			return new ReadThreadsResult(new FoolFuukaPostsParser(responseText, this).convertThreads());
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		}
	}

	private static final Pattern PATTERN_REDIRECT = Pattern.compile("You are being redirected to .*?/thread/(\\d+)/#");

	@Override
	public ReadPostsResult onReadPosts(ReadPostsData data) throws HttpException, InvalidResponseException,
			RedirectException {
		FoolFuukaChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.createThreadUri(data.boardName, data.threadNumber);
		HttpResponse response = new HttpRequest(uri, data).setValidator(data.validator).setSuccessOnly(false).perform();
		String responseText;
		if (response.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
			uri = locator.buildPath(data.boardName, "post", data.threadNumber, "");
			responseText = new HttpRequest(uri, data).perform().readString();
			Matcher matcher = PATTERN_REDIRECT.matcher(responseText);
			if (matcher.find()) {
				throw RedirectException.toThread(data.boardName, matcher.group(1), data.threadNumber);
			}
			throw HttpException.createNotFoundException();
		} else {
			response.checkResponseCode();
			responseText = response.readString();
		}
		try {
			// TODO Move to child classes
			Uri threadUri = locator.buildPathWithHost("boards.4chan.org", data.boardName, "thread", data.threadNumber);
			return new ReadPostsResult(new FoolFuukaPostsParser(responseText, this).convertPosts(threadUri));
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		}
	}

	@Override
	public ReadSearchPostsResult onReadSearchPosts(ReadSearchPostsData data) throws HttpException,
			InvalidResponseException {
		FoolFuukaChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildPath(data.boardName, "search", "text").buildUpon().appendPath(data.searchQuery)
				.appendEncodedPath("page/" + (data.pageNumber + 1) + "/").build();
		String responseText = new HttpRequest(uri, data).perform().readString();
		try {
			return new ReadSearchPostsResult(new FoolFuukaPostsParser(responseText, this).convertSearch());
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		}
	}

	@Override
	public ReadBoardsResult onReadBoards(ReadBoardsData data) throws HttpException, InvalidResponseException {
		FoolFuukaChanLocator locator = ChanLocator.get(this);
		String responseText = new HttpRequest(locator.buildPath(), data).perform().readString();
		try {
			return new ReadBoardsResult(new FoolFuukaBoardsParser(responseText).convert());
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		}
	}
}
