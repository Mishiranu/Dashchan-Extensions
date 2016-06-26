package com.mishiranu.dashchan.chan.fourchan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;

import chan.content.ApiException;
import chan.content.ChanConfiguration;
import chan.content.ChanLocator;
import chan.content.ChanPerformer;
import chan.content.InvalidResponseException;
import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.content.model.ThreadSummary;
import chan.http.CookieBuilder;
import chan.http.HttpException;
import chan.http.HttpHolder;
import chan.http.HttpRequest;
import chan.http.HttpResponse;
import chan.http.MultipartEntity;
import chan.http.UrlEncodedEntity;
import chan.util.CommonUtils;
import chan.util.StringUtils;

public class FourchanChanPerformer extends ChanPerformer
{
	private static final String RECAPTCHA_API_KEY = "6Ldp2bsSAAAAAAJ5uyx_lx34lJeEpTLVkP5k04qc";
	
	private static final String[] PREFERRED_BOARDS_ORDER = {"Misc", "Interests", "Creative", "Other",
		"Japanese Culture", "Adult"};
	
	private static final String[][] PREFERRED_BOARDS_MAPPING = {{"b", "r", "r9k", "pol", "soc", "s4s"},
		{"v", "vg", "vr", "co", "g", "tv", "k", "o", "an", "tg", "sp", "asp", "sci", "int", "out", "toy", "biz"},
		{"i", "po", "p", "ck", "ic", "wg", "mu", "fa", "3", "gd", "diy", "wsg"},
		{"trv", "fit", "x", "lit", "adv", "lgbt", "mlp"}, {"a", "c", "w", "m", "cgl", "cm", "f", "n", "jp", "vp"},
		{"s", "hc", "hm", "h", "e", "u", "d", "y", "t", "hr", "gif"}};
	
	@Override
	public ReadThreadsResult onReadThreads(ReadThreadsData data) throws HttpException, InvalidResponseException
	{
		FourchanChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.createApiUri(data.boardName, (data.isCatalog() ? "catalog"
				: Integer.toString(data.pageNumber + 1)) + ".json");
		HttpResponse response = new HttpRequest(uri, data.holder, data).setValidator(data.validator).read();
		JSONObject jsonObject = response.getJsonObject();
		JSONArray jsonArray = response.getJsonArray();
		if (jsonObject != null && !data.isCatalog())
		{
			try
			{
				JSONArray threadsArray = jsonObject.getJSONArray("threads");
				Posts[] threads = new Posts[threadsArray.length()];
				for (int i = 0; i < threads.length; i++)
				{
					threads[i] = FourchanModelMapper.createThread(threadsArray.getJSONObject(i),
							locator, data.boardName, false);
				}
				return new ReadThreadsResult(threads);
			}
			catch (JSONException e)
			{
				throw new InvalidResponseException(e);
			}
		}
		else if (jsonArray != null && data.isCatalog())
		{
			try
			{
				ArrayList<Posts> threads = new ArrayList<>();
				for (int i = 0; i < jsonArray.length(); i++)
				{
					JSONArray threadsArray = jsonArray.getJSONObject(i).getJSONArray("threads");
					for (int j = 0; j < threadsArray.length(); j++)
					{
						threads.add(FourchanModelMapper.createThread(threadsArray.getJSONObject(j),
								locator, data.boardName, true));
					}
				}
				return new ReadThreadsResult(threads);
			}
			catch (JSONException e)
			{
				throw new InvalidResponseException(e);
			}
		}
		throw new InvalidResponseException();
	}
	
	@Override
	public ReadPostsResult onReadPosts(ReadPostsData data) throws HttpException, InvalidResponseException
	{
		FourchanChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.createApiUri(data.boardName, "thread", data.threadNumber + ".json");
		JSONObject jsonObject = new HttpRequest(uri, data.holder, data).setValidator(data.validator)
				.read().getJsonObject();
		if (jsonObject != null)
		{
			try
			{
				JSONArray jsonArray = jsonObject.getJSONArray("posts");
				if (jsonArray.length() > 0)
				{
					int uniquePosters = 0;
					Post[] posts = new Post[jsonArray.length()];
					for (int i = 0; i < posts.length; i++)
					{
						jsonObject = jsonArray.getJSONObject(i);
						posts[i] = FourchanModelMapper.createPost(jsonObject, locator, data.boardName);
						if (i == 0) uniquePosters = jsonObject.optInt("unique_ips");
					}
					return new ReadPostsResult(new Posts(posts).setUniquePosters(uniquePosters));
				}
				return null;
			}
			catch (JSONException e)
			{
				throw new InvalidResponseException(e);
			}
		}
		throw new InvalidResponseException();
	}
	
	private static String getPreferredBoardCategory(String boardName)
	{
		for (int i = 0; i < PREFERRED_BOARDS_ORDER.length; i++)
		{
			String category = PREFERRED_BOARDS_ORDER[i];
			for (int j = 0; j < PREFERRED_BOARDS_MAPPING[i].length; j++)
			{
				if (PREFERRED_BOARDS_MAPPING[i][j].equals(boardName)) return category;
			}
		}
		return PREFERRED_BOARDS_ORDER[0];
	}
	
	@Override
	public ReadBoardsResult onReadBoards(ReadBoardsData data) throws HttpException, InvalidResponseException
	{
		FourchanChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.createApiUri("boards.json");
		JSONObject jsonObject = new HttpRequest(uri, data.holder, data).read().getJsonObject();
		if (jsonObject != null)
		{
			FourchanChanConfiguration configuration = ChanConfiguration.get(this);
			LinkedHashMap<String, ArrayList<Board>> boardsMap = new LinkedHashMap<>();
			for (String title : PREFERRED_BOARDS_ORDER) boardsMap.put(title, new ArrayList<Board>());
			try
			{
				JSONArray jsonArray = jsonObject.getJSONArray("boards");
				for (int i = 0; i < jsonArray.length(); i++)
				{
					JSONObject boardObject = jsonArray.getJSONObject(i);
					String boardName = CommonUtils.getJsonString(boardObject, "board");
					String title = CommonUtils.getJsonString(boardObject, "title");
					Board board = new Board(boardName, title);
					String category = getPreferredBoardCategory(boardName);
					ArrayList<Board> boards = boardsMap.get(category);
					if (boards != null) boards.add(board);
				}
				ArrayList<BoardCategory> boardCategories = new ArrayList<>();
				for (LinkedHashMap.Entry<String, ArrayList<Board>> entry : boardsMap.entrySet())
				{
					ArrayList<Board> boards = entry.getValue();
					if (!boards.isEmpty())
					{
						Collections.sort(boards);
						boardCategories.add(new BoardCategory(entry.getKey(), boards));
					}
				}
				configuration.updateFromBoardsJson(jsonObject);
				return new ReadBoardsResult(boardCategories);
			}
			catch (JSONException e)
			{
				throw new InvalidResponseException(e);
			}
		}
		throw new InvalidResponseException();
	}

	private static final Pattern PATTERN_ARCHIVED_THREAD = Pattern.compile("<tr><td>(\\d+)</td>.*?" +
			"<td class=\"teaser-col\">(.*?)</td>");
	
	@Override
	public ReadThreadSummariesResult onReadThreadSummaries(ReadThreadSummariesData data) throws HttpException,
			InvalidResponseException
	{
		if (data.type == ReadThreadSummariesData.TYPE_ARCHIVED_THREADS)
		{
			FourchanChanLocator locator = ChanLocator.get(this);
			Uri uri = locator.createBoardUri(data.boardName, 0).buildUpon().appendPath("archive").build();
			String responseText = new HttpRequest(uri, data.holder, data).read().getString();
			ArrayList<ThreadSummary> threadSummaries = new ArrayList<>();
			Matcher matcher = PATTERN_ARCHIVED_THREAD.matcher(responseText);
			while (matcher.find())
			{
				threadSummaries.add(new ThreadSummary(data.boardName, matcher.group(1),
						StringUtils.clearHtml(matcher.group(2))));
			}
			return new ReadThreadSummariesResult(threadSummaries);
		}
		else return super.onReadThreadSummaries(data);
	}
	
	@Override
	public ReadPostsCountResult onReadPostsCount(ReadPostsCountData data) throws HttpException, InvalidResponseException
	{
		FourchanChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.createApiUri(data.boardName, "thread", data.threadNumber + ".json");
		JSONObject jsonObject = new HttpRequest(uri, data.holder, data).setValidator(data.validator)
				.read().getJsonObject();
		if (jsonObject != null)
		{
			try
			{
				return new ReadPostsCountResult(jsonObject.getJSONArray("posts").length());
			}
			catch (JSONException e)
			{
				throw new InvalidResponseException(e);
			}
		}
		throw new InvalidResponseException();
	}
	
	private CookieBuilder buildCookies(String captchaPassCookie)
	{
		if (captchaPassCookie != null)
		{
			CookieBuilder builder = new CookieBuilder();
			builder.append("pass_enabled", "1");
			builder.append("pass_id", captchaPassCookie);
			return builder;
		}
		return null;
	}
	
	private static String removeErrorFromMessage(String message)
	{
		if (message.startsWith("Error: ")) message = message.substring(7);
		return message;
	}
	
	private static final Pattern PATTERN_AUTH_MESSAGE = Pattern.compile("<span.*?>(.*?)<(?:br|/span)>");
	
	@Override
	public CheckAuthorizationResult onCheckAuthorization(CheckAuthorizationData data) throws HttpException,
			InvalidResponseException
	{
		return new CheckAuthorizationResult(readCaptchaPass(data.holder, data,
				data.authorizationData[0], data.authorizationData[1]) != null);
	}
	
	private String mLastCaptchaPassData;
	private String mLastCaptchaPassCookie;
	
	private String getCaptchaPassData(String token, String pin)
	{
		return token + '|' + pin;
	}
	
	private String readCaptchaPass(HttpHolder holder, HttpRequest.Preset preset, String token, String pin)
			throws HttpException, InvalidResponseException
	{
		mLastCaptchaPassData = null;
		mLastCaptchaPassCookie = null;
		FourchanChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.createSysUri("auth");
		UrlEncodedEntity entity = new UrlEncodedEntity("act", "do_login", "id", token, "pin", pin, "long_login", "yes");
		String responseText = new HttpRequest(uri, holder, preset).setPostMethod(entity)
				.setRedirectHandler(HttpRequest.RedirectHandler.STRICT).read().getString();
		Matcher matcher = PATTERN_AUTH_MESSAGE.matcher(responseText);
		if (matcher.find())
		{
			String message = StringUtils.clearHtml(matcher.group(1));
			if (message.startsWith("Error: ")) message = message.substring(7);
			if (message.contains("Your device is now authorized"))
			{
				String captchaPassCookie = null;
				List<String> cookies = holder.getHeaderFields().get("Set-Cookie");
				if (cookies != null)
				{
					for (String cookie : cookies)
					{
						if (cookie.startsWith("pass_id=") && !cookie.startsWith("pass_id=0;"))
						{
							int index = cookie.indexOf(';');
							captchaPassCookie = cookie.substring(8, index >= 0 ? index : cookie.length());
							break;
						}
					}
				}
				if (captchaPassCookie == null) throw new InvalidResponseException();
				mLastCaptchaPassData = getCaptchaPassData(token, pin);
				mLastCaptchaPassCookie = captchaPassCookie;
				return captchaPassCookie;
			}
			if (message.contains("Incorrect Token or PIN") || message.contains("Your Token must be exactly") ||
					message.contains("You have left one or more fields blank"))
			{
				return null;
			}
			message = removeErrorFromMessage(message);
			throw new HttpException(0, message);
		}
		else throw new InvalidResponseException();
	}
	
	private static final String CAPTCHA_TYPE = "captchaType";
	private static final String CAPTCHA_PASS_COOKIE = "captchaPassCookie";
	
	@Override
	public ReadCaptchaResult onReadCaptcha(ReadCaptchaData data) throws HttpException, InvalidResponseException
	{
		String token = data.captchaPass != null ? data.captchaPass[0] : null;
		String pin = data.captchaPass != null ? data.captchaPass[1] : null;
		String captchaPassCookie = null;
		if (token != null || pin != null)
		{
			if (getCaptchaPassData(token, pin).equals(mLastCaptchaPassData)) captchaPassCookie = mLastCaptchaPassCookie;
			else captchaPassCookie = readCaptchaPass(data.holder, data, token, pin);
		}
		if (captchaPassCookie != null)
		{
			CaptchaData captchaData = new CaptchaData();
			captchaData.put(CAPTCHA_PASS_COOKIE, captchaPassCookie);
			return new ReadCaptchaResult(CaptchaState.PASS, captchaData)
					.setValidity(ChanConfiguration.Captcha.Validity.LONG_LIFETIME);
		}
		CaptchaData captchaData = new CaptchaData();
		captchaData.put(CaptchaData.API_KEY, RECAPTCHA_API_KEY);
		if ("report".equals(data.requirement)) captchaData.put(CAPTCHA_TYPE, data.captchaType);
		return new ReadCaptchaResult(CaptchaState.CAPTCHA, captchaData);
	}
	
	private static final Pattern PATTERN_POST_ERROR = Pattern.compile("<span id=\"errmsg\".*?>(.*?)</span>");
	private static final Pattern PATTERN_POST_SUCCESS = Pattern.compile("<!-- thread:(\\d+),no:(\\d+) -->");
	
	@Override
	public SendPostResult onSendPost(SendPostData data) throws HttpException, ApiException, InvalidResponseException
	{
		MultipartEntity entity = new MultipartEntity();
		entity.add("mode", "regist");
		entity.add("resto", data.threadNumber);
		entity.add("sub", data.subject);
		entity.add("com", data.comment);
		entity.add("name", data.name);
		if (data.optionSage) entity.add("email", "sage");
		entity.add("pwd", data.password);
		if (data.attachments != null)
		{
			SendPostData.Attachment attachment = data.attachments[0];
			attachment.addToEntity(entity, "upfile");
			if (attachment.optionSpoiler) entity.add("spoiler", "on");
		}
		String captchaPassCookie = null;
		if (data.captchaData != null)
		{
			if (ChanConfiguration.CAPTCHA_TYPE_RECAPTCHA_1.equals(data.captchaType))
			{
				entity.add("recaptcha_challenge_field", data.captchaData.get(CaptchaData.CHALLENGE));
				entity.add("recaptcha_response_field", data.captchaData.get(CaptchaData.INPUT));
			}
			else if (ChanConfiguration.CAPTCHA_TYPE_RECAPTCHA_2.equals(data.captchaType))
			{
				entity.add("g-recaptcha-response", data.captchaData.get(CaptchaData.INPUT));
			}
			captchaPassCookie = data.captchaData.get(CAPTCHA_PASS_COOKIE);
		}

		FourchanChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.createSysUri(data.boardName, "post");
		String responseText = new HttpRequest(uri, data.holder, data).addCookie(buildCookies(captchaPassCookie))
				.setPostMethod(entity).setRedirectHandler(HttpRequest.RedirectHandler.STRICT).read().getString();
		
		Matcher matcher = PATTERN_POST_SUCCESS.matcher(responseText);
		if (matcher.find())
		{
			String threadNumber = matcher.group(1);
			String postNumber = matcher.group(2);
			if ("0".equals(threadNumber))
			{
				// New thread
				threadNumber = postNumber;
				postNumber = null;
			}
			return new SendPostResult(threadNumber, postNumber);
		}
		matcher = PATTERN_POST_ERROR.matcher(responseText);
		if (matcher.find())
		{
			String errorMessage = matcher.group(1);
			if (errorMessage != null)
			{
				int errorType = 0;
				if (errorMessage.contains("CAPTCHA"))
				{
					errorType = ApiException.SEND_ERROR_CAPTCHA;
				}
				else if (errorMessage.contains("No text entered"))
				{
					errorType = ApiException.SEND_ERROR_EMPTY_COMMENT;
				}
				else if (errorMessage.contains("No file selected"))
				{
					errorType = ApiException.SEND_ERROR_EMPTY_FILE;
				}
				else if (errorMessage.contains("File too large"))
				{
					errorType = ApiException.SEND_ERROR_FILE_TOO_BIG;
				}
				else if (errorMessage.contains("Field too long"))
				{
					errorType = ApiException.SEND_ERROR_FIELD_TOO_LONG;
				}
				else if (errorMessage.contains("You cannot reply to this thread anymore"))
				{
					errorType = ApiException.SEND_ERROR_CLOSED;
				}
				else if (errorMessage.contains("This board doesn't exist"))
				{
					errorType = ApiException.SEND_ERROR_NO_BOARD;
				}
				else if (errorMessage.contains("Specified thread does not exist"))
				{
					errorType = ApiException.SEND_ERROR_NO_THREAD;
				}
				else if (errorMessage.contains("You must wait"))
				{
					errorType = ApiException.SEND_ERROR_TOO_FAST;
				}
				else if (errorMessage.contains("Corrupted file or unsupported file type"))
				{
					errorType = ApiException.SEND_ERROR_FILE_NOT_SUPPORTED;
				}
				else if (errorMessage.contains("Duplicate file exists"))
				{
					errorType = ApiException.SEND_ERROR_FILE_EXISTS;
				}
				else if (errorMessage.contains("has been blocked due to abuse"))
				{
					errorType = ApiException.SEND_ERROR_BANNED;
				}
				else if (errorMessage.contains("image replies has been reached"))
				{
					errorType = ApiException.SEND_ERROR_FILES_LIMIT;
				}
				if (errorType != 0) throw new ApiException(errorType);
			}
			CommonUtils.writeLog("4chan send message", errorMessage);
			throw new ApiException(errorMessage);
		}
		throw new InvalidResponseException();
	}
	
	@Override
	public SendDeletePostsResult onSendDeletePosts(SendDeletePostsData data) throws HttpException, ApiException,
			InvalidResponseException
	{
		FourchanChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.createSysUri(data.boardName, "imgboard.php");
		UrlEncodedEntity entity = new UrlEncodedEntity("mode", "usrdel", "pwd", data.password);
		for (String postNumber : data.postNumbers) entity.add(postNumber, "delete");
		if (data.optionFilesOnly) entity.add("onlyimgdel", "on");
		String responseText = new HttpRequest(uri, data.holder, data).setPostMethod(entity)
				.setRedirectHandler(HttpRequest.RedirectHandler.STRICT).read().getString();
		Matcher matcher = PATTERN_POST_ERROR.matcher(responseText);
		if (matcher.find())
		{
			String errorMessage = matcher.group(1);
			if (errorMessage != null)
			{
				int errorType = 0;
				if (errorMessage.contains("Password incorrect"))
				{
					errorType = ApiException.DELETE_ERROR_PASSWORD;
				}
				else if (errorMessage.contains("You must wait longer before deleting this post"))
				{
					errorType = ApiException.DELETE_ERROR_TOO_NEW;
				}
				else if (errorMessage.contains("You cannot delete a post this old"))
				{
					errorType = ApiException.DELETE_ERROR_TOO_OLD;
				}
				else if (errorMessage.contains("Can't find the post"))
				{
					errorType = ApiException.DELETE_ERROR_NOT_FOUND;
				}
				else if (errorMessage.contains("You cannot delete posts this often"))
				{
					errorType = ApiException.DELETE_ERROR_TOO_OFTEN;
				}
				if (errorType == ApiException.SEND_ERROR_CAPTCHA)
				{
					mLastCaptchaPassData = null;
					mLastCaptchaPassCookie = null;
				}
				if (errorType != 0) throw new ApiException(errorType);
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
			InvalidResponseException
	{
		FourchanChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.createSysUri(data.boardName, "imgboard.php");
		boolean retry = false;
		String message = null;
		while (true)
		{
			CaptchaData captchaData = requireUserCaptcha("report", data.boardName, data.threadNumber, retry);
			if (captchaData == null) throw new ApiException(ApiException.REPORT_ERROR_NO_ACCESS);
			UrlEncodedEntity entity = new UrlEncodedEntity("mode", "report", "cat",
					data.type, "board", data.boardName, "no", data.postNumbers.get(0));
			String captchaType = captchaData.get(CAPTCHA_TYPE);
			if (ChanConfiguration.CAPTCHA_TYPE_RECAPTCHA_1.equals(captchaType))
			{
				entity.add("recaptcha_challenge_field", captchaData.get(CaptchaData.CHALLENGE));
				entity.add("recaptcha_response_field", captchaData.get(CaptchaData.INPUT));
			}
			else if (ChanConfiguration.CAPTCHA_TYPE_RECAPTCHA_2.equals(captchaType))
			{
				entity.add("g-recaptcha-response", captchaData.get(CaptchaData.INPUT));
			}
			String captchaPassCookie = captchaData.get(CAPTCHA_PASS_COOKIE);
			String responseText = new HttpRequest(uri, data.holder, data).addCookie(buildCookies(captchaPassCookie))
					.setPostMethod(entity).setRedirectHandler(HttpRequest.RedirectHandler.STRICT).read().getString();
			Matcher matcher = PATTERN_REPORT_MESSAGE.matcher(responseText);
			if (matcher.find())
			{
				message = matcher.group(1);
				if (message.contains("CAPTCHA"))
				{
					retry = true;
					continue;
				}
				else break;
			}
			else throw new InvalidResponseException();
		}
		message = StringUtils.clearHtml(message).trim();
		int errorType = 0;
		if (message.contains("Report submitted") || message.contains("You have already reported this post"))
		{
			return null;
		}
		else if (message.contains("You cannot report a sticky"))
		{
			errorType = ApiException.REPORT_ERROR_NO_ACCESS;
		}
		if (errorType != 0) throw new ApiException(errorType);
		message = removeErrorFromMessage(message);
		CommonUtils.writeLog("4chan report message", message);
		throw new ApiException(message);
	}
}