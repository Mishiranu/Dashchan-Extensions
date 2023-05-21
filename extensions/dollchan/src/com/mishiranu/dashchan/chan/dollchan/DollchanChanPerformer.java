package com.mishiranu.dashchan.chan.dollchan;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.SystemClock;
import android.util.Base64;
import chan.content.ApiException;
import chan.content.ChanLocator;
import chan.content.ChanPerformer;
import chan.content.InvalidResponseException;
import chan.content.RedirectException;
import chan.content.WakabaChanPerformer;
import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.http.CookieBuilder;
import chan.http.HttpException;
import chan.http.HttpRequest;
import chan.http.HttpResponse;
import chan.http.MultipartEntity;
import chan.http.SimpleEntity;
import chan.util.CommonUtils;
import chan.util.StringUtils;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DollchanChanPerformer extends WakabaChanPerformer {
	@Override
	protected List<Posts> parseThreads(String boardName, InputStream input) throws IOException, chan.text.ParseException {
		return new DollchanPostsParser(this, boardName).convertThreads(input);
	}

	@Override
	protected List<Post> parsePosts(String boardName, InputStream input) throws IOException, chan.text.ParseException {
		return new DollchanPostsParser(this, boardName).convertPosts(input);
	}

	@Override
	public ReadBoardsResult onReadBoards(ReadBoardsData data) {
		return new ReadBoardsResult(new BoardCategory(null, new Board[] {
			new Board("ukr", "Україна"),
			new Board("de", "Scripts"),
			new Board("btb", "Bytebeat")}));
	}

	private static final Pattern PATTERN_POST_ERROR = Pattern.compile("<div class=\"reply\".*?>(.*?)</div>");
	private static final Pattern PATTERN_POST_ERROR_UNCOMMON = Pattern.compile("<h2>(.*?)</h2>");
	@Override
	public ReadCaptchaResult onReadCaptcha(ReadCaptchaData data) throws HttpException, InvalidResponseException {
		DollchanChanLocator locator = DollchanChanLocator.get(this);
		Uri uri = locator.buildPath(data.boardName, "inc", "captcha.php");
		HttpResponse response = new HttpRequest(uri, data).perform();
		Bitmap image = response.readBitmap();
		String captchaId = response.getCookieValue("PHPSESSID");
		if (image == null || captchaId == null) {
			throw new InvalidResponseException();
		}

		int[] pixels = new int[image.getWidth() * image.getHeight()];
		image.getPixels(pixels, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());
		Bitmap newImage = Bitmap.createBitmap(pixels, image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
		image.recycle();
		image = CommonUtils.trimBitmap(newImage, 0x00000000);
		if (image == null) {
			image = newImage;
		} else if (image != newImage) {
			newImage.recycle();
		}

		CaptchaData captchaData = new CaptchaData();
		captchaData.put(CaptchaData.CHALLENGE, captchaId);
		return new ReadCaptchaResult(CaptchaState.CAPTCHA, captchaData).setImage(image);
	}

	private String trimPassword(String password) {
		// Max password length: 8
		return password != null && password.length() > 8 ? password.substring(0, 8) : password;
	}

	private CookieBuilder buildCookies(CaptchaData data) {
		CookieBuilder builder = new CookieBuilder();
		if (data != null)
		{
			builder.append("PHPSESSID", data.get(CaptchaData.CHALLENGE));
		}
		return builder;
	}


	@Override
	public SendPostResult onSendPost(SendPostData data) throws HttpException, ApiException, InvalidResponseException {
		MultipartEntity entity = new MultipartEntity();

		entity.add("parent", data.threadNumber != null ? data.threadNumber : "0");
		entity.add("name", data.name != null ? data.name : "");
		entity.add("subject", data.subject != null ? data.subject : "");
		entity.add("message", data.comment);
		entity.add("password", data.password);
		if (data.optionSage) {
			entity.add("email", "sage");
		} else {
			entity.add("email", data.email != null ? data.email : "");
		}
		if (data.attachments != null) {
			for (int i = 0; i < data.attachments.length; i++) {
				SendPostData.Attachment attachment = data.attachments[i];
				attachment.addToEntity(entity, "file[]");
			}
		}

		DollchanChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildPath(data.boardName, "imgboard.php");

		if (data.captchaData != null) {
			entity.add("captcha", data.captchaData.get(CaptchaData.INPUT));
		}

		HttpResponse response = new HttpRequest(uri, data).setPostMethod(entity).
			addCookie(buildCookies(data.captchaData)).
			setRedirectHandler(HttpRequest.RedirectHandler.NONE).perform();

		String responseText;
		if (response.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP) {
			uri = response.getRedirectedUri();
			String path = uri.getPath();
			if (path == null) {
				throw new InvalidResponseException();
			}
			if (!path.startsWith("/error")) {
				String threadNumber = locator.getThreadNumber(uri);
				if (threadNumber == null) {
					throw new InvalidResponseException();
				}
				return new SendPostResult(threadNumber, null);
			}
			responseText = new HttpRequest(uri, data).addCookie(buildCookies(data.captchaData)).perform().readString();
		} else {
			responseText = response.readString();
		}

		String errorMessage = null;
		Matcher matcher = PATTERN_POST_ERROR.matcher(responseText);
		if (matcher.find()) {
			errorMessage = matcher.group(1);
		} else {
			matcher = PATTERN_POST_ERROR_UNCOMMON.matcher(responseText);
			if (matcher.find()) {
				errorMessage = matcher.group(1);
			}
		}
		if (errorMessage != null) {
			int errorType = 0;
			if (errorMessage.contains("Incorrect CAPTCHA text entered")) {
				errorType = ApiException.SEND_ERROR_CAPTCHA;
			}
			if (errorType != 0) {
				throw new ApiException(errorType);
			} else {
				CommonUtils.writeLog("Dollchan send message", errorMessage);
				throw new ApiException(errorMessage);
			}
		}
		return null;
	}

	private static void fillDeleteReportPostings(JSONObject parametersObject, String boardName, String threadNumber,
			Collection<String> postNumbers) throws JSONException {
		JSONArray jsonArray = new JSONArray();
		for (String postNumber : postNumbers) {
			JSONObject postObject = new JSONObject();
			postObject.put("board", boardName);
			postObject.put("thread", threadNumber);
			if (!postNumber.equals(threadNumber)) {
				postObject.put("post", postNumber);
			}
			jsonArray.put(postObject);
		}
		parametersObject.put("postings", jsonArray);
	}

	@Override
	public SendDeletePostsResult onSendDeletePosts(SendDeletePostsData data) throws HttpException, ApiException,
			InvalidResponseException {
		JSONObject jsonObject = new JSONObject();
		JSONObject parametersObject = new JSONObject();
		try {
			jsonObject.put("parameters", parametersObject);
			parametersObject.put("password", trimPassword(data.password));
			parametersObject.put("deleteMedia", true);
			if (data.optionFilesOnly) {
				parametersObject.put("deleteUploads", true);
			}
			fillDeleteReportPostings(parametersObject, data.boardName, data.threadNumber, data.postNumbers);
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		SimpleEntity entity = new SimpleEntity();
		entity.setContentType("application/json; charset=utf-8");
		entity.setData(jsonObject.toString());
		DollchanChanLocator locator = DollchanChanLocator.get(this);
		Uri uri = locator.buildPath(".api", "deleteContent");
		try {
			jsonObject = new JSONObject(new HttpRequest(uri, data).setPostMethod(entity)
					.setRedirectHandler(HttpRequest.RedirectHandler.STRICT).perform().readString());
		} catch (JSONException e) {
			throw new InvalidResponseException(e);
		}
		if ("error".equals(CommonUtils.optJsonString(jsonObject, "status"))) {
			String errorMessage = CommonUtils.optJsonString(jsonObject, "data");
			if (errorMessage != null) {
				if (errorMessage.contains("Invalid account")) {
					throw new ApiException(ApiException.DELETE_ERROR_PASSWORD);
				}
				CommonUtils.writeLog("Dollchan delete message", errorMessage);
				throw new ApiException(errorMessage);
			}
		}
		try {
			jsonObject = jsonObject.getJSONObject("data");
		} catch (JSONException e) {
			throw new InvalidResponseException(e);
		}
		if (jsonObject.optInt("removedThreads") + jsonObject.optInt("removedPosts") > 0) {
			return new SendDeletePostsResult();
		} else {
			throw new ApiException(ApiException.DELETE_ERROR_PASSWORD);
		}
	}
}
