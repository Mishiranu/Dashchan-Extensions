package com.mishiranu.dashchan.chan.fourchan;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Base64;
import chan.content.ApiException;
import chan.content.ChanPerformer;
import chan.content.InvalidResponseException;
import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.content.model.ThreadSummary;
import chan.http.CookieBuilder;
import chan.http.HttpException;
import chan.http.HttpRequest;
import chan.http.HttpResponse;
import chan.http.HttpValidator;
import chan.http.MultipartEntity;
import chan.http.SimpleEntity;
import chan.http.UrlEncodedEntity;
import chan.text.JsonSerial;
import chan.text.ParseException;
import chan.util.CommonUtils;
import chan.util.StringUtils;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONException;
import org.json.JSONObject;

public class FourchanChanPerformer extends ChanPerformer {
	private static final String RECAPTCHA_API_KEY = "6Ldp2bsSAAAAAAJ5uyx_lx34lJeEpTLVkP5k04qc";

	private static final String CAPTCHA_DATA_KEY_TYPE = "captchaType";
	private static final String CAPTCHA_DATA_KEY_PASS_COOKIE = "captchaPassCookie";

	private static final String COOKIE_FOURCHAN_PASS = "4chan_pass";

	private final HashMap<String, Long> lastRulesUpdate = new HashMap<>();

	private void updateBoardRules(HttpRequest.Preset preset,
			String boardName, List<Posts> threads) throws HttpException {
		Long update;
		synchronized (lastRulesUpdate) {
			update = lastRulesUpdate.get(boardName);
		}
		if (update != null && update + 24 * 60 * 60 * 1000 > SystemClock.elapsedRealtime()) {
			return;
		}
		String postNumber = null;
		for (Posts posts : threads) {
			Post post = posts.getPosts()[0];
			if (!post.isClosed() && !post.isArchived() && !post.isSticky()) {
				postNumber = post.getPostNumber();
			}
		}
		HttpResponse response = null;
		if (postNumber != null) {
			FourchanChanLocator locator = FourchanChanLocator.get(this);
			Uri uri = locator.createSysUri(boardName, "imgboard.php").buildUpon()
					.appendQueryParameter("mode", "report").appendQueryParameter("no", postNumber).build();
			response = new HttpRequest(uri, preset).setSuccessOnly(false).perform();
		}
		List<ReportReason> reportReasons = Collections.emptyList();
		if (response != null) {
			try (InputStream input = response.open()) {
				reportReasons = new FourchanRulesParser().parse(input);
			} catch (ParseException e) {
				// Ignore
			} catch (IOException e) {
				throw response.fail(e);
			}
		}
		if (!reportReasons.isEmpty()) {
			synchronized (lastRulesUpdate) {
				lastRulesUpdate.put(boardName, SystemClock.elapsedRealtime());
			}
			FourchanChanConfiguration configuration = FourchanChanConfiguration.get(this);
			configuration.updateReportingConfiguration(boardName, reportReasons);
		}
	}

	@SuppressWarnings("SwitchStatementWithTooFewBranches")
	@Override
	public ReadThreadsResult onReadThreads(ReadThreadsData data) throws HttpException, InvalidResponseException {
		FourchanChanLocator locator = FourchanChanLocator.get(this);
		FourchanChanConfiguration configuration = FourchanChanConfiguration.get(this);
		Uri uri = locator.createApiUri(data.boardName, (data.isCatalog() ? "catalog"
				: Integer.toString(data.pageNumber + 1)) + ".json");
		HttpResponse response = new HttpRequest(uri, data).setValidator(data.validator).perform();
		HttpValidator validator = response.getValidator();
		ArrayList<Posts> threads = new ArrayList<>();
		boolean handleMathTags = configuration.isMathTagsHandlingEnabled();
		try (InputStream input = response.open();
				JsonSerial.Reader reader = JsonSerial.reader(input)) {
			if (data.isCatalog()) {
				reader.startArray();
				while (!reader.endStruct()) {
					reader.startObject();
					while (!reader.endStruct()) {
						switch (reader.nextName()) {
							case "threads": {
								reader.startArray();
								while (!reader.endStruct()) {
									threads.add(FourchanModelMapper.createThread(reader,
											locator, data.boardName, handleMathTags, true));
								}
								break;
							}
							default: {
								reader.skip();
								break;
							}
						}
					}
				}
			} else {
				reader.startObject();
				while (!reader.endStruct()) {
					switch (reader.nextName()) {
						case "threads": {
							reader.startArray();
							while (!reader.endStruct()) {
								threads.add(FourchanModelMapper.createThread(reader,
										locator, data.boardName, handleMathTags, false));
							}
							break;
						}
						default: {
							reader.skip();
							break;
						}
					}
				}
			}
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		} catch (IOException e) {
			throw response.fail(e);
		}
		if (data.pageNumber == 0) {
			updateBoardRules(data, data.boardName, threads);
		}
		return new ReadThreadsResult(threads).setValidator(validator);
	}

	@SuppressWarnings("SwitchStatementWithTooFewBranches")
	@Override
	public ReadPostsResult onReadPosts(ReadPostsData data) throws HttpException, InvalidResponseException {
		FourchanChanLocator locator = FourchanChanLocator.get(this);
		FourchanChanConfiguration configuration = FourchanChanConfiguration.get(this);
		boolean handleMathTags = configuration.isMathTagsHandlingEnabled();
		boolean tail = data.partialThreadLoading && data.lastPostNumber != null;
		ArrayList<Post> posts = new ArrayList<>();
		int uniquePosters = 0;
		if (tail) {
			Uri uri = locator.createApiUri(data.boardName, "thread", data.threadNumber + "-tail.json");
			HttpResponse response = new HttpRequest(uri, data).setValidator(data.validator)
					.setSuccessOnly(false).perform();
			if (response.getResponseCode() == HttpURLConnection.HTTP_OK) {
				TRY: try (InputStream input = response.open();
						JsonSerial.Reader reader = JsonSerial.reader(input)) {
					reader.startObject();
					while (!reader.endStruct()) {
						switch (reader.nextName()) {
							case "posts": {
								reader.startArray();
								String sincePostNumber = null;
								reader.startObject();
								while (!reader.endStruct()) {
									switch (reader.nextName()) {
										case "unique_ips": {
											uniquePosters = reader.nextInt();
											break;
										}
										case "tail_id": {
											sincePostNumber = reader.nextString();
											break;
										}
										default: {
											reader.skip();
											break;
										}
									}
								}
								if (sincePostNumber != null && Integer.parseInt(data.lastPostNumber)
										>= Integer.parseInt(sincePostNumber)) {
									while (!reader.endStruct()) {
										posts.add(FourchanModelMapper.createPost(reader,
												locator, data.boardName, handleMathTags, null));
									}
									return new ReadPostsResult(new Posts(posts).setUniquePosters(uniquePosters));
								} else {
									// Load full thread
									break TRY;
								}
							}
							default: {
								reader.skip();
								break;
							}
						}
					}
				} catch (ParseException e) {
					throw new InvalidResponseException(e);
				} catch (IOException e) {
					throw response.fail(e);
				}
			}
		}
		Uri uri = locator.createApiUri(data.boardName, "thread", data.threadNumber + ".json");
		HttpResponse response = new HttpRequest(uri, data).setValidator(data.validator).perform();
		try (InputStream input = response.open();
				JsonSerial.Reader reader = JsonSerial.reader(input)) {
			reader.startObject();
			while (!reader.endStruct()) {
				switch (reader.nextName()) {
					case "posts": {
						FourchanModelMapper.Extra extra = new FourchanModelMapper.Extra();
						reader.startArray();
						while (!reader.endStruct()) {
							posts.add(FourchanModelMapper.createPost(reader,
									locator, data.boardName, handleMathTags, extra));
							if (extra != null) {
								uniquePosters = extra.uniquePosters;
								extra = null;
							}
						}
						break;
					}
					default: {
						reader.skip();
						break;
					}
				}
			}
			return new ReadPostsResult(new Posts(posts).setUniquePosters(uniquePosters)).setFullThread(true);
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		} catch (IOException e) {
			throw response.fail(e);
		}
	}

	@SuppressWarnings("SwitchStatementWithTooFewBranches")
	@Override
	public ReadSearchPostsResult onReadSearchPosts(ReadSearchPostsData data)
			throws HttpException, InvalidResponseException {
		FourchanChanLocator locator = FourchanChanLocator.get(this);
		FourchanChanConfiguration configuration = FourchanChanConfiguration.get(this);
		boolean handleMathTags = configuration.isMathTagsHandlingEnabled();
		Uri uri = locator.createSearchApiUri("b", data.boardName, "q", data.searchQuery,
				"o", Integer.toString(10 * data.pageNumber));
		HttpResponse response = new HttpRequest(uri, data).perform();
		Locale locale = Locale.US;
		String lowerSearchQuery = data.searchQuery.toLowerCase(locale);
		try (InputStream input = response.open();
				JsonSerial.Reader reader = JsonSerial.reader(input)) {
			ArrayList<Post> posts = new ArrayList<>();
			reader.startObject();
			while (!reader.endStruct()) {
				switch (reader.nextName()) {
					case "threads": {
						reader.startArray();
						while (!reader.endStruct()) {
							reader.startObject();
							while (!reader.endStruct()) {
								switch (reader.nextName()) {
									case "posts": {
										reader.startArray();
										while (!reader.endStruct()) {
											Post post = FourchanModelMapper.createPost(reader, locator,
													data.boardName, handleMathTags, null);
											boolean matches = post.getParentPostNumber() != null;
											if (!matches) {
												matches = StringUtils.clearHtml(post.getSubject())
														.toLowerCase(locale).contains(lowerSearchQuery);
											}
											if (!matches) {
												matches = StringUtils.clearHtml(post.getComment())
														.toLowerCase(locale).contains(lowerSearchQuery);
											}
											if (matches) {
												posts.add(post);
											}
										}
										break;
									}
									default: {
										reader.skip();
										break;
									}
								}
							}
						}
						break;
					}
					default: {
						reader.skip();
						break;
					}
				}
			}
			return new ReadSearchPostsResult(posts);
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		} catch (IOException e) {
			throw response.fail(e);
		}
	}

	@SuppressWarnings("SwitchStatementWithTooFewBranches")
	@Override
	public ReadBoardsResult onReadBoards(ReadBoardsData data) throws HttpException, InvalidResponseException {
		FourchanChanLocator locator = FourchanChanLocator.get(this);
		Uri uri = locator.buildPath();
		HttpResponse response = new HttpRequest(uri, data).perform();
		Map<String, List<String>> categoryMap;
		try (InputStream input = response.open()) {
			categoryMap = new FourchanBoardsParser(this).parse(input);
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		} catch (IOException e) {
			throw response.fail(e);
		}
		String uncategorized = "Uncategorized";
		FourchanChanConfiguration configuration = FourchanChanConfiguration.get(this);
		LinkedHashMap<String, ArrayList<Board>> boardsMap = new LinkedHashMap<>();
		for (String title : categoryMap.keySet()) {
			boardsMap.put(title, new ArrayList<>());
		}
		boardsMap.put(uncategorized, new ArrayList<>());
		HashMap<String, String> boardToCategory = new HashMap<>();
		for (Map.Entry<String, List<String>> entry : categoryMap.entrySet()) {
			for (String boardName : entry.getValue()) {
				boardToCategory.put(boardName, entry.getKey());
			}
		}
		uri = locator.createApiUri("boards.json");
		response = new HttpRequest(uri, data).perform();
		try (InputStream input = response.open();
				JsonSerial.Reader reader = JsonSerial.reader(input)) {
			reader.startObject();
			while (!reader.endStruct()) {
				switch (reader.nextName()) {
					case "boards": {
						reader.startArray();
						while (!reader.endStruct()) {
							Board board = configuration.updateBoard(reader);
							if (board != null) {
								String category = boardToCategory.get(board.getBoardName());
								ArrayList<Board> boards = boardsMap.get(category);
								if (boards == null) {
									boards = boardsMap.get(uncategorized);
								}
								Objects.requireNonNull(boards).add(board);
							}
						}
						break;
					}
					default: {
						reader.skip();
						break;
					}
				}
			}
			ArrayList<BoardCategory> boardCategories = new ArrayList<>();
			for (LinkedHashMap.Entry<String, ArrayList<Board>> entry : boardsMap.entrySet()) {
				ArrayList<Board> boards = entry.getValue();
				if (!boards.isEmpty()) {
					Collections.sort(boards);
					boardCategories.add(new BoardCategory(entry.getKey(), boards));
				}
			}
			return new ReadBoardsResult(boardCategories);
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		} catch (IOException e) {
			throw response.fail(e);
		}
	}

	private static final Pattern PATTERN_ARCHIVED_THREAD = Pattern.compile("<tr><td>(\\d+)</td>.*?" +
			"<td class=\"teaser-col\">(.*?)</td>");

	@Override
	public ReadThreadSummariesResult onReadThreadSummaries(ReadThreadSummariesData data) throws HttpException,
			InvalidResponseException {
		if (data.type == ReadThreadSummariesData.TYPE_ARCHIVED_THREADS) {
			FourchanChanLocator locator = FourchanChanLocator.get(this);
			Uri uri = locator.createBoardUri(data.boardName, 0).buildUpon().appendPath("archive").build();
			String responseText = new HttpRequest(uri, data).perform().readString();
			ArrayList<ThreadSummary> threadSummaries = new ArrayList<>();
			Matcher matcher = PATTERN_ARCHIVED_THREAD.matcher(responseText);
			while (matcher.find()) {
				threadSummaries.add(new ThreadSummary(data.boardName, matcher.group(1),
						StringUtils.clearHtml(matcher.group(2))));
			}
			return new ReadThreadSummariesResult(threadSummaries);
		} else {
			return super.onReadThreadSummaries(data);
		}
	}

	@Override
	public ReadContentResult onReadContent(ReadContentData data) throws HttpException, InvalidResponseException {
		FourchanChanLocator locator = FourchanChanLocator.get(this);
		String mathData = locator.extractMathData(data.uri);
		if (mathData != null) {
			Uri uri = locator.buildPathWithHost("quicklatex.com", "latex3.f");
			SimpleEntity entity = new SimpleEntity();
			entity.setData("formula=" + mathData.replace("%", "%25").replace("&", "%26") + "&fsize=60px&" +
					"fcolor=000000&mode=0&out=1&remhost=quicklatex.com&preamble=\\usepackage{amsmath}\n" +
					"\\usepackage{amsfonts}\n\\usepackage{amssymb}");
			entity.setContentType("application/x-www-form-urlencoded");
			String responseText = new HttpRequest(uri, data).setPostMethod(entity).perform().readString();
			String[] splitted = responseText.split("\r?\n| ");
			if (splitted.length >= 2 && "0".equals(splitted[0])) {
				uri = Uri.parse(splitted[1]);
				return new ReadContentResult(new HttpRequest(uri, data).perform());
			}
			throw HttpException.createNotFoundException();
		}
		return super.onReadContent(data);
	}

	private CookieBuilder buildCookies(String captchaPassCookie) {
		if (captchaPassCookie != null) {
			CookieBuilder builder = new CookieBuilder();
			builder.append("pass_enabled", "1");
			builder.append("pass_id", captchaPassCookie);
			return builder;
		}
		return null;
	}

	private static String removeErrorFromMessage(String message) {
		if (message != null && message.startsWith("Error: ")) {
			message = message.substring(7);
		}
		return message;
	}

	private static final Pattern PATTERN_AUTH_MESSAGE = Pattern.compile("<h2.*?>(.*?)<(?:br|/h2)>");

	@Override
	public CheckAuthorizationResult onCheckAuthorization(CheckAuthorizationData data) throws HttpException,
			InvalidResponseException {
		return new CheckAuthorizationResult(readCaptchaPass(data,
				data.authorizationData[0], data.authorizationData[1]) != null);
	}

	private String lastCaptchaPassData;
	private String lastCaptchaPassCookie;

	private String getCaptchaPassData(String token, String pin) {
		return token + '|' + pin;
	}

	private String readCaptchaPass(HttpRequest.Preset preset, String token, String pin)
			throws HttpException, InvalidResponseException {
		lastCaptchaPassData = null;
		lastCaptchaPassCookie = null;
		FourchanChanLocator locator = FourchanChanLocator.get(this);
		Uri uri = locator.createSysUri("auth");
		UrlEncodedEntity entity = new UrlEncodedEntity("act", "do_login", "id", token, "pin", pin, "long_login", "yes");
		HttpResponse response = new HttpRequest(uri, preset).setPostMethod(entity)
				.setRedirectHandler(HttpRequest.RedirectHandler.STRICT).perform();
		String responseText = response.readString();
		Matcher matcher = PATTERN_AUTH_MESSAGE.matcher(responseText);
		if (matcher.find()) {
			String message = StringUtils.clearHtml(matcher.group(1));
			if (message.startsWith("Error: ")) {
				message = message.substring(7);
			}
			if (message.contains("Your device is now authorized")) {
				String captchaPassCookie = null;
				List<String> cookies = response.getHeaderFields().get("Set-Cookie");
				if (cookies != null) {
					for (String cookie : cookies) {
						if (cookie.startsWith("pass_id=") && !cookie.startsWith("pass_id=0;")) {
							int index = cookie.indexOf(';');
							captchaPassCookie = cookie.substring(8, index >= 0 ? index : cookie.length());
							break;
						}
					}
				}
				if (captchaPassCookie == null) {
					throw new InvalidResponseException();
				}
				lastCaptchaPassData = getCaptchaPassData(token, pin);
				lastCaptchaPassCookie = captchaPassCookie;
				return captchaPassCookie;
			}
			if (message.contains("Incorrect Token or PIN") || message.contains("Your Token must be exactly") ||
					message.contains("You have left one or more fields blank")) {
				return null;
			}
			message = removeErrorFromMessage(message);
			throw new HttpException(0, message);
		} else {
			throw new InvalidResponseException();
		}
	}

	private static final String REQUIREMENT_BANNED = "banned";
	private static final String REQUIREMENT_REPORT = "report";

	@SuppressWarnings("BusyWait")
	@Override
	public ReadCaptchaResult onReadCaptcha(ReadCaptchaData data) throws HttpException, InvalidResponseException {
		boolean banned = REQUIREMENT_BANNED.equals(data.requirement);
		if (!banned) {
			String token = data.captchaPass != null ? data.captchaPass[0] : null;
			String pin = data.captchaPass != null ? data.captchaPass[1] : null;
			String captchaPassCookie = null;
			if (token != null || pin != null) {
				if (getCaptchaPassData(token, pin).equals(lastCaptchaPassData)) {
					captchaPassCookie = lastCaptchaPassCookie;
				} else {
					captchaPassCookie = readCaptchaPass(data, token, pin);
				}
			}
			if (captchaPassCookie != null) {
				CaptchaData captchaData = new CaptchaData();
				captchaData.put(CAPTCHA_DATA_KEY_PASS_COOKIE, captchaPassCookie);
				return new ReadCaptchaResult(CaptchaState.PASS, captchaData)
						.setValidity(FourchanChanConfiguration.Captcha.Validity.LONG_LIFETIME);
			}
		}
		String captchaType = banned ? FourchanChanConfiguration.CAPTCHA_TYPE_RECAPTCHA_2 : data.captchaType;
		FourchanChanConfiguration configuration = FourchanChanConfiguration.get(this);
		FourchanChanLocator locator = FourchanChanLocator.get(this);
		ReadCaptchaResult result;
		if (FourchanChanConfiguration.CAPTCHA_TYPE_4CHAN_CAPTCHA.equals(captchaType)) {
			if (data.mayShowLoadButton) {
				return new ReadCaptchaResult(CaptchaState.NEED_LOAD, null);
			}
			String threadNumber = data.requirement == null ? data.threadNumber : "1";
			Uri.Builder builder = locator.createSysUri("captcha").buildUpon()
					.appendQueryParameter("board", data.boardName);
			if (threadNumber != null) {
				builder.appendQueryParameter("thread_id", threadNumber);
			}
			Uri uri = builder.build();
			String challenge;
			Bitmap image;
			Bitmap background;
			int wait = 2000;
			while (true) {
				try {
					JSONObject jsonObject = new JSONObject(new HttpRequest(uri, data)
							.addCookie(COOKIE_FOURCHAN_PASS, configuration.getCookie(COOKIE_FOURCHAN_PASS))
							.perform().readString());
					String error = jsonObject.optString("error");
					if ("You have to wait a while before doing this again".equals(error)) {
						try {
							Thread.sleep(wait);
						} catch (InterruptedException e) {
							throw new HttpException(0, null);
						}
						wait += 1000;
					} else {
						challenge = jsonObject.getString("challenge");
						byte[] imageBytes = Base64.decode(jsonObject.getString("img"), 0);
						byte[] backgroundBytes = Base64.decode(jsonObject.optString("bg"), 0);
						image = imageBytes.length == 0 ? null
								: BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
						background = backgroundBytes.length == 0 ? null
								: BitmapFactory.decodeByteArray(backgroundBytes, 0, backgroundBytes.length);
						break;
					}
				} catch (JSONException e) {
					throw new InvalidResponseException(e);
				}
			}
			if (image == null) {
				throw new InvalidResponseException(new Exception("Image is null"));
			}
			int centerOffset = 0;
			if (background != null) {
				Integer offset = FourchanCaptchaUtils.findCenterOffset(image);
				if (offset != null) {
					centerOffset = offset;
				} else {
					background.recycle();
					background = null;
				}
			}
			if (background != null) {
				if (image.getHeight() != background.getHeight()) {
					throw new InvalidResponseException(new Exception("Image heights are not equal"));
				} else if (background.getWidth() < image.getWidth()) {
					throw new InvalidResponseException(new Exception("Invalid image sizes"));
				}
			}
			int resultOffset = 0;
			if (background != null) {
				// Use user-driven binary search to find the offset for the most readable image
				String description = configuration.getResources().getString(R.string.select_the_most_readable_captcha);
				Integer offset = FourchanCaptchaUtils.binarySearchOffset(image, background, 9, centerOffset,
						images -> requireUserImageSingleChoice(-1, images, description, null));
				if (offset != null) {
					resultOffset = offset;
				} else {
					return new ReadCaptchaResult(CaptchaState.NEED_LOAD, null);
				}
			}
			Bitmap resultImage = FourchanCaptchaUtils.create(image, background, resultOffset);
			CaptchaData captchaData = new CaptchaData();
			captchaData.put(CAPTCHA_DATA_KEY_TYPE, captchaType);
			captchaData.put(CaptchaData.CHALLENGE, challenge);
			result = new ReadCaptchaResult(CaptchaState.CAPTCHA, captchaData).setImage(resultImage);
		} else if (FourchanChanConfiguration.CAPTCHA_TYPE_RECAPTCHA_2.equals(captchaType)) {
			CaptchaData captchaData = new CaptchaData();
			captchaData.put(CAPTCHA_DATA_KEY_TYPE, captchaType);
			captchaData.put(CaptchaData.API_KEY, RECAPTCHA_API_KEY);
			if (banned) {
				captchaData.put(CaptchaData.REFERER, locator.buildPath("banned").toString());
			} else {
				captchaData.put(CaptchaData.REFERER, locator.createBoardsRootUri(data.boardName).toString());
			}
			result = new ReadCaptchaResult(CaptchaState.CAPTCHA, captchaData)
					.setValidity(FourchanChanConfiguration.Captcha.Validity.IN_BOARD_SEPARATELY);
		} else {
			throw new IllegalStateException();
		}
		if (!CommonUtils.equals(data.captchaType, captchaType)) {
			result.setCaptchaType(captchaType);
		}
		return result;
	}

	private static final SimpleDateFormat DATE_FORMAT_BAN = new SimpleDateFormat("MMMM d yyyy", Locale.US);

	private static long parseBanDate(String value) {
		if (value == null) {
			return -1;
		}
		value = value.replaceAll("(st|nd|rd|th),", "");
		try {
			Date date = DATE_FORMAT_BAN.parse(value);
			return date != null ? date.getTime() : 0;
		} catch (java.text.ParseException e) {
			return 0;
		}
	}

	private ApiException.BanExtra readBanExtra(HttpRequest.Preset preset, String boardName) throws HttpException {
		FourchanChanLocator locator = FourchanChanLocator.get(this);
		Uri uri = locator.buildPath("banned");
		String responseText = new HttpRequest(uri, preset).perform().readString();
		while (responseText.contains(RECAPTCHA_API_KEY)) {
			CaptchaData captchaData = requireUserCaptcha(REQUIREMENT_BANNED, boardName, null, false);
			if (captchaData == null) {
				return null;
			}
			MultipartEntity entity = new MultipartEntity();
			entity.add("g-recaptcha-response", captchaData.get(CaptchaData.INPUT));
			responseText = new HttpRequest(uri, preset).setPostMethod(entity).perform().readString();
		}
		HashMap<String, String> fields = new HashMap<>();
		for (String name : Arrays.asList("reason", "startDate", "endDate")) {
			for (String tag : Arrays.asList("b", "span")) {
				String open = "<" + tag + " class=\"" + name + "\">";
				int start = responseText.indexOf(open);
				if (start < 0) {
					continue;
				}
				start += open.length();
				int end = responseText.indexOf("</" + tag + ">", start);
				if (end < start) {
					continue;
				}
				if (responseText.charAt(end - 1) == '.') {
					end--;
				}
				fields.put(name, responseText.substring(start, end));
				break;
			}
		}
		return new ApiException.BanExtra()
				.setMessage(fields.get("reason"))
				.setStartDate(parseBanDate(fields.get("startDate")))
				.setExpireDate(parseBanDate(fields.get("endDate")));
	}

	private void handleFourchanPass(HttpResponse response) {
		String fourchanPass = response.getCookieValue(COOKIE_FOURCHAN_PASS);
		if (fourchanPass != null) {
			FourchanChanConfiguration configuration = FourchanChanConfiguration.get(this);
			configuration.storeCookie(COOKIE_FOURCHAN_PASS, fourchanPass, "4chan Pass");
		}
	}

	private static final Pattern PATTERN_POST_ERROR = Pattern.compile("<span id=\"errmsg\".*?>(.*?)</span>");
	private static final Pattern PATTERN_POST_SUCCESS = Pattern.compile("<!-- thread:(\\d+),no:(\\d+) -->");

	private static final HashSet<String> FORBIDDEN_OPTIONS = new HashSet<>(Arrays.asList("nonoko", "nonokosage"));

	@Override
	public SendPostResult onSendPost(SendPostData data) throws HttpException, ApiException, InvalidResponseException {
		MultipartEntity entity = new MultipartEntity();
		entity.add("mode", "regist");
		entity.add("resto", data.threadNumber);
		entity.add("sub", data.subject);
		entity.add("com", data.comment);
		entity.add("name", data.name);
		if (data.optionSage) {
			entity.add("email", "sage");
		} else if (data.email != null && !FORBIDDEN_OPTIONS.contains(data.email.toLowerCase(Locale.US))) {
			entity.add("email", data.email);
		}
		entity.add("pwd", data.password);
		entity.add("flag", data.userIcon);
		if (data.attachments != null) {
			SendPostData.Attachment attachment = data.attachments[0];
			attachment.addToEntity(entity, "upfile");
			if (attachment.optionSpoiler) {
				entity.add("spoiler", "on");
			}
		}
		String captchaPassCookie = null;
		if (data.captchaData != null) {
			if (FourchanChanConfiguration.CAPTCHA_TYPE_4CHAN_CAPTCHA.equals(data.captchaType)) {
				entity.add("t-challenge", data.captchaData.get(CaptchaData.CHALLENGE));
				entity.add("t-response", data.captchaData.get(CaptchaData.INPUT));
			} else if (FourchanChanConfiguration.CAPTCHA_TYPE_RECAPTCHA_2.equals(data.captchaType)) {
				entity.add("g-recaptcha-response", data.captchaData.get(CaptchaData.INPUT));
			}
			captchaPassCookie = data.captchaData.get(CAPTCHA_DATA_KEY_PASS_COOKIE);
		}

		FourchanChanLocator locator = FourchanChanLocator.get(this);
		FourchanChanConfiguration configuration = FourchanChanConfiguration.get(this);
		boolean nsfwFix = !configuration.isSafeForWork(data.boardName) && configuration.isFixNsfwBoardsEnabled();
		Uri uri = locator.createSysUri(data.boardName, "post");
		HttpRequest request = new HttpRequest(uri, data).addCookie(buildCookies(captchaPassCookie))
				.addCookie(COOKIE_FOURCHAN_PASS, configuration.getCookie(COOKIE_FOURCHAN_PASS));
		if (nsfwFix) {
			// More reliable fix: add non-browser User-Agent (breaks CloudFlare bypass)
			request.addHeader("User-Agent", "curl/7.64.0");
		} else {
			// Less reliable fix: works in /b/, doesn't work in /bant/
			request.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:91.0) Gecko/20100101 Firefox/91.0");
			request.addHeader("Accept", "text/html").addHeader("Accept-Language", "en")
					.addHeader("Referer", uri.toString());
		}
		HttpResponse response = request.setPostMethod(entity)
				.setRedirectHandler(HttpRequest.RedirectHandler.STRICT).perform();
		handleFourchanPass(response);
		String responseText = response.readString();

		Matcher matcher = PATTERN_POST_SUCCESS.matcher(responseText);
		if (matcher.find()) {
			String threadNumber = matcher.group(1);
			String postNumber = matcher.group(2);
			if ("0".equals(threadNumber)) {
				// New thread
				threadNumber = postNumber;
				postNumber = null;
			}
			return new SendPostResult(threadNumber, postNumber);
		}
		matcher = PATTERN_POST_ERROR.matcher(responseText);
		if (matcher.find()) {
			String errorMessage = matcher.group(1);
			if (errorMessage != null) {
				int errorType = 0;
				Object extra = null;
				if (errorMessage.contains("CAPTCHA")) {
					errorType = ApiException.SEND_ERROR_CAPTCHA;
				} else if (errorMessage.contains("No text entered")) {
					errorType = ApiException.SEND_ERROR_EMPTY_COMMENT;
				} else if (errorMessage.contains("No file selected")) {
					errorType = ApiException.SEND_ERROR_EMPTY_FILE;
				} else if (errorMessage.contains("File too large")) {
					errorType = ApiException.SEND_ERROR_FILE_TOO_BIG;
				} else if (errorMessage.contains("Field too long")) {
					errorType = ApiException.SEND_ERROR_FIELD_TOO_LONG;
				} else if (errorMessage.contains("You cannot reply to this thread anymore")) {
					errorType = ApiException.SEND_ERROR_CLOSED;
				} else if (errorMessage.contains("This board doesn't exist")) {
					errorType = ApiException.SEND_ERROR_NO_BOARD;
				} else if (errorMessage.contains("Specified thread does not exist")) {
					errorType = ApiException.SEND_ERROR_NO_THREAD;
				} else if (errorMessage.contains("You must wait")) {
					errorType = ApiException.SEND_ERROR_TOO_FAST;
				} else if (errorMessage.contains("Corrupted file or unsupported file type")) {
					errorType = ApiException.SEND_ERROR_FILE_NOT_SUPPORTED;
				} else if (errorMessage.contains("Duplicate file exists")) {
					errorType = ApiException.SEND_ERROR_FILE_EXISTS;
				} else if (errorMessage.contains("has been blocked due to abuse")) {
					errorType = ApiException.SEND_ERROR_RANGE_BANNED;
				} else if (errorMessage.contains("banned")) {
					errorType = ApiException.SEND_ERROR_BANNED;
					extra = readBanExtra(data, data.boardName);
				} else if (errorMessage.contains("image replies has been reached")) {
					errorType = ApiException.SEND_ERROR_FILES_LIMIT;
				}
				if (errorType != 0) {
					throw new ApiException(errorType, extra);
				}
			}
			CommonUtils.writeLog("4chan send message", errorMessage);
			throw new ApiException(errorMessage);
		}
		throw new InvalidResponseException();
	}

	@Override
	public SendDeletePostsResult onSendDeletePosts(SendDeletePostsData data) throws HttpException, ApiException {
		FourchanChanLocator locator = FourchanChanLocator.get(this);
		Uri uri = locator.createSysUri(data.boardName, "imgboard.php");
		UrlEncodedEntity entity = new UrlEncodedEntity("mode", "usrdel", "pwd", data.password);
		for (String postNumber : data.postNumbers) {
			entity.add(postNumber, "delete");
		}
		if (data.optionFilesOnly) {
			entity.add("onlyimgdel", "on");
		}
		String responseText = new HttpRequest(uri, data).setPostMethod(entity)
				.setRedirectHandler(HttpRequest.RedirectHandler.STRICT).perform().readString();
		Matcher matcher = PATTERN_POST_ERROR.matcher(responseText);
		if (matcher.find()) {
			String errorMessage = matcher.group(1);
			if (errorMessage != null) {
				int errorType = 0;
				if (errorMessage.contains("Password incorrect")) {
					errorType = ApiException.DELETE_ERROR_PASSWORD;
				} else if (errorMessage.contains("You must wait longer before deleting this post")) {
					errorType = ApiException.DELETE_ERROR_TOO_NEW;
				} else if (errorMessage.contains("You cannot delete a post this old")) {
					errorType = ApiException.DELETE_ERROR_TOO_OLD;
				} else if (errorMessage.contains("Can't find the post")) {
					errorType = ApiException.DELETE_ERROR_NOT_FOUND;
				} else if (errorMessage.contains("You cannot delete posts this often")) {
					errorType = ApiException.DELETE_ERROR_TOO_OFTEN;
				}
				if (errorType == ApiException.SEND_ERROR_CAPTCHA) {
					lastCaptchaPassData = null;
					lastCaptchaPassCookie = null;
				}
				if (errorType != 0) {
					throw new ApiException(errorType);
				}
			}
			errorMessage = removeErrorFromMessage(errorMessage);
			CommonUtils.writeLog("4chan delete message", errorMessage);
			throw new ApiException(errorMessage);
		}
		return null;
	}

	private static final Pattern PATTERN_REPORT_MESSAGE = Pattern.compile("<font.*?>(.*?)<(?:br|/font)>");

	@Override
	public SendReportPostsResult onSendReportPosts(SendReportPostsData data) throws HttpException, ApiException,
			InvalidResponseException {
		ReportReason reportReason = ReportReason.fromKey(data.type);
		FourchanChanConfiguration configuration = FourchanChanConfiguration.get(this);
		FourchanChanLocator locator = FourchanChanLocator.get(this);
		Uri uri = locator.createSysUri(data.boardName, "imgboard.php").buildUpon()
				.appendQueryParameter("mode", "report").appendQueryParameter("no", data.postNumbers.get(0)).build();
		boolean retry = false;
		String message;
		while (true) {
			CaptchaData captchaData = requireUserCaptcha(REQUIREMENT_REPORT, data.boardName, data.threadNumber, retry);
			retry = true;
			if (captchaData == null) {
				throw new ApiException(ApiException.REPORT_ERROR_NO_ACCESS);
			}
			UrlEncodedEntity entity = new UrlEncodedEntity("cat", reportReason.category, "cat_id", reportReason.value,
					"board", data.boardName);
			String captchaType = captchaData.get(CAPTCHA_DATA_KEY_TYPE);
			if (FourchanChanConfiguration.CAPTCHA_TYPE_4CHAN_CAPTCHA.equals(captchaType)) {
				entity.add("t-challenge", captchaData.get(CaptchaData.CHALLENGE));
				entity.add("t-response", captchaData.get(CaptchaData.INPUT));
			} else if (FourchanChanConfiguration.CAPTCHA_TYPE_RECAPTCHA_2.equals(captchaType)) {
				entity.add("g-recaptcha-response", captchaData.get(CaptchaData.INPUT));
			}
			HttpResponse response = new HttpRequest(uri, data).setPostMethod(entity)
					.addCookie(COOKIE_FOURCHAN_PASS, configuration.getCookie(COOKIE_FOURCHAN_PASS))
					.setRedirectHandler(HttpRequest.RedirectHandler.STRICT).perform();
			handleFourchanPass(response);
			String responseText = response.readString();
			Matcher matcher = PATTERN_REPORT_MESSAGE.matcher(responseText);
			if (matcher.find()) {
				message = StringUtils.emptyIfNull(matcher.group(1));
				if (!message.contains("CAPTCHA")) {
					break;
				}
			} else {
				throw new InvalidResponseException();
			}
		}
		message = StringUtils.clearHtml(message).trim();
		int errorType = 0;
		if (message.contains("Report submitted") || message.contains("You have already reported this post")) {
			return null;
		} else if (message.contains("You cannot report a sticky")) {
			errorType = ApiException.REPORT_ERROR_NO_ACCESS;
		}
		if (errorType != 0) {
			throw new ApiException(errorType);
		}
		message = removeErrorFromMessage(message);
		CommonUtils.writeLog("4chan report message", message);
		throw new ApiException(message);
	}
}
