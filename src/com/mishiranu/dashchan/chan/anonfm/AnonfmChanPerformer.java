package com.mishiranu.dashchan.chan.anonfm;

import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.net.Uri;

import chan.content.ApiException;
import chan.content.ChanConfiguration;
import chan.content.ChanLocator;
import chan.content.ChanPerformer;
import chan.content.InvalidResponseException;
import chan.content.ThreadRedirectException;
import chan.content.model.Board;
import chan.content.model.BoardCategory;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.http.HttpException;
import chan.http.HttpHolder;
import chan.http.HttpRequest;
import chan.http.MultipartEntity;
import chan.http.UrlEncodedEntity;
import chan.text.ParseException;
import chan.util.CommonUtils;
import chan.util.StringUtils;

public class AnonfmChanPerformer extends ChanPerformer
{
	private static final SimpleDateFormat DATE_FORMAT_ANSWER = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);
	private static final SimpleDateFormat DATE_FORMAT_SERVER = new SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss",
			Locale.US);
	
	static
	{
		DATE_FORMAT_ANSWER.setTimeZone(TimeZone.getTimeZone("GMT"));
		DATE_FORMAT_SERVER.setTimeZone(TimeZone.getTimeZone("GMT"));
	}
	
	private static String escapeHtml(String string)
	{
		return string != null ? string.replace("&", "&amp;").replace("\"", "&quot;")
				.replace("<", "&lt;").replace(">", "&gt;") : "";
	}
	
	private Post createFmOriginalPost(HttpHolder holder, HttpRequest.Preset preset) throws HttpException,
			InvalidResponseException
	{
		StringBuilder builder = new StringBuilder();
		AnonfmChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildPath("state.txt");
		String[] splitted = new HttpRequest(uri, holder, preset).read().getString().split("\r?\n");
		HashMap<String, String> state = new HashMap<>();
		for (int i = 0; i < splitted.length / 2; i++)
		{
			state.put(splitted[2 * i], splitted[2 * i + 1]);
		}
		String stateArtist = state.get("Artist");
		String stateTitle = state.get("Title");
		if (!StringUtils.isEmpty(stateArtist) && !StringUtils.isEmpty(stateTitle))
		{
			builder.append("<p>Сейчас играет: ").append(escapeHtml(stateArtist));
			builder.append(" — ").append(escapeHtml(stateTitle));
		}
		uri = locator.buildPath("info.js");
		JSONObject jsonObject = new HttpRequest(uri, holder, preset).read().getJsonObject();
		String infoVideo = jsonObject != null ? CommonUtils.optJsonString(jsonObject, "video") : null;
		String infoCall = jsonObject != null ? CommonUtils.optJsonString(jsonObject, "call") : null;
		if (!StringUtils.isEmpty(infoVideo))
		{
			builder.append("<p>Видео: <a href=\"").append(infoVideo).append("\">").append(infoVideo).append("</a></p>");
		}
		if (!StringUtils.isEmpty(infoCall))
		{
			builder.append("<p>Звонок: <a href=\"").append(infoCall).append("\">").append(infoCall).append("</a></p>");
		}
		uri = locator.buildPath("shed.js");
		JSONArray jsonArray = new HttpRequest(uri, holder, preset).read().getJsonArray();
		if (jsonArray != null && jsonArray.length() > 0)
		{
			SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
			builder.append("<h3>Расписание</h3>");
			for (int i = 0; i < jsonArray.length(); i++)
			{
				try
				{
					JSONArray itemArray = jsonArray.getJSONArray(i);
					long start = itemArray.getLong(0) * 1000L;
					long end = itemArray.getLong(1) * 1000L;
					String name = itemArray.getString(2);
					String title = itemArray.getString(3);
					builder.append("<p>").append(escapeHtml(title)).append(" (")
							.append(escapeHtml(name)).append(")<br />");
					builder.append("<em>").append(format.format(start)).append(" — ")
							.append(format.format(end)).append("</em></p>");
				}
				catch (JSONException e)
				{
					
				}
			}
		}
		Post post = new Post();
		post.setPostNumber("1");
		post.setSubject("Кукареканье со стороны диджейки");
		post.setTimestamp(AnonfmPostsParser.START_TIMESTAMP);
		post.setComment(builder.toString());
		return post;
	}
	
	@Override
	public ReadThreadsResult onReadThreads(ReadThreadsData data) throws HttpException, InvalidResponseException
	{
		if (AnonfmChanLocator.BOARD_NAME_FM.equals(data.boardName))
		{
			if (data.pageNumber != 0) throw HttpException.createNotFoundException();
			return new ReadThreadsResult(new Posts(createFmOriginalPost(data.holder, data)));
		}
		else if (AnonfmChanLocator.BOARD_NAME_TEXTUAL.equals(data.boardName))
		{
			if (data.pageNumber != 0) throw HttpException.createNotFoundException();
			AnonfmChanLocator locator = ChanLocator.get(this);
			Uri uri = locator.createBoardUri(data.boardName, 0);
			String responseText = new HttpRequest(uri, data.holder, data).setValidator(data.validator)
					.read().getString();
			try
			{
				return new ReadThreadsResult(new AnonfmPostsParser(responseText).convertThreads());
			}
			catch (ParseException e)
			{
				throw new InvalidResponseException(e);
			}
		}
		else throw HttpException.createNotFoundException();
	}
	
	@Override
	public ReadPostsResult onReadPosts(ReadPostsData data) throws HttpException, ThreadRedirectException,
			InvalidResponseException
	{
		if (AnonfmChanLocator.BOARD_NAME_FM.equals(data.boardName))
		{
			if (!"1".equals(data.threadNumber)) throw HttpException.createNotFoundException();
			AnonfmChanLocator locator = ChanLocator.get(this);
			Uri uri = locator.buildPath("answers.js");
			JSONArray jsonArray = new HttpRequest(uri, data.holder, data).read().getJsonArray();
			if (jsonArray == null) throw new InvalidResponseException();
			ArrayList<Post> posts = new ArrayList<>();
			posts.add(createFmOriginalPost(data.holder, data));
			Calendar serverDayStart;
			try
			{
				Calendar calendar = Calendar.getInstance();
				calendar.setTimeZone(DATE_FORMAT_SERVER.getTimeZone());
				calendar.setTime(DATE_FORMAT_SERVER.parse(data.holder.getHeaderFields().get("Last-Modified").get(0)));
				calendar.add(Calendar.HOUR_OF_DAY, 3);
				calendar.set(Calendar.HOUR_OF_DAY, 0);
				calendar.set(Calendar.MINUTE, 0);
				calendar.set(Calendar.SECOND, 0);
				calendar.set(Calendar.MILLISECOND, 0);
				calendar.add(Calendar.HOUR_OF_DAY, -3);
				serverDayStart = calendar;
			}
			catch (java.text.ParseException e)
			{
				throw new InvalidResponseException(e);
			}
			try
			{
				long lastTime = Long.MAX_VALUE;
				for (int i = 0; i < jsonArray.length(); i++)
				{
					JSONArray itemArray = jsonArray.getJSONArray(i);
					String identifier = itemArray.getString(1);
					String message = itemArray.getString(2);
					message = StringUtils.linkify(message);
					String timeString = StringUtils.clearHtml(itemArray.getString(3)).trim();
					String reply = itemArray.getString(5);
					reply = StringUtils.linkify(reply);
					long time;
					try
					{
						time = DATE_FORMAT_ANSWER.parse(timeString).getTime();
					}
					catch (java.text.ParseException e)
					{
						throw new InvalidResponseException(e);
					}
					if (time > lastTime) serverDayStart.add(Calendar.DATE, -1);
					lastTime = time;
					long timestamp = serverDayStart.getTimeInMillis() + time;
					long number = AnonfmPostsParser.timestampToPostNumber(timestamp);
					if ("!".equals(identifier) || StringUtils.isEmpty(message) && !identifier.matches("[a-z0-9]{12}"))
					{
						Post post = new Post();
						post.setParentPostNumber("1");
						post.setPostNumber(Long.toString(number));
						post.setSubject("!".equals(identifier) ? "Объявление" : identifier);
						post.setComment(reply);
						post.setTimestamp(timestamp);
						posts.add(post);
					}
					else
					{
						String messageNumber = Long.toString(number - 1);
						Post post1 = new Post();
						post1.setParentPostNumber("1");
						post1.setPostNumber(messageNumber);
						post1.setIdentifier(identifier);
						post1.setComment(AnonfmPostsParser.quotifyComment(message.replaceAll(" {2,}", "<br>")));
						post1.setTimestamp(timestamp);
						posts.add(post1);
						Post post2 = new Post();
						post2.setParentPostNumber("1");
						post2.setPostNumber(Long.toString(number));
						post2.setComment("<a href=\"#" + messageNumber + "\">&gt;&gt;" + messageNumber
								+ "</a><br>" + reply);
						post2.setTimestamp(timestamp);
						posts.add(post2);
					}
				}
				return new ReadPostsResult(posts);
			}
			catch (JSONException e)
			{
				throw new InvalidResponseException(e);
			}
		}
		else if (AnonfmChanLocator.BOARD_NAME_TEXTUAL.equals(data.boardName))
		{
			AnonfmChanLocator locator = ChanLocator.get(this);
			Uri uri = locator.createThreadUri(data.boardName, data.threadNumber);
			String responseText = new HttpRequest(uri, data.holder, data).setValidator(data.validator)
					.read().getString();
			try
			{
				return new ReadPostsResult(new AnonfmPostsParser(responseText).convertPosts());
			}
			catch (ParseException e)
			{
				throw new InvalidResponseException(e);
			}
		}
		else throw HttpException.createNotFoundException();
	}
	
	@Override
	public ReadBoardsResult onReadBoards(ReadBoardsData data) throws HttpException, InvalidResponseException
	{
		Board[] boards =
		{
			new Board(AnonfmChanLocator.BOARD_NAME_FM, "Радио Анонимус"),
			new Board(AnonfmChanLocator.BOARD_NAME_TEXTUAL, "Текстаба")
		};
		return new ReadBoardsResult(new BoardCategory(null, boards));
	}
	
	private static final Pattern PATTERN_CAPTCHA_IMAGE = Pattern.compile("<img src=\"(/feedback/(\\d+).gif)\">");
	
	@Override
	public ReadCaptchaResult onReadCaptcha(ReadCaptchaData data) throws HttpException, InvalidResponseException
	{
		if (AnonfmChanLocator.BOARD_NAME_FM.equals(data.boardName))
		{
			AnonfmChanLocator locator = ChanLocator.get(this);
			Uri uri = locator.buildPath("feedback");
			String responseText = new HttpRequest(uri, data.holder, data).read().getString();
			Matcher matcher = PATTERN_CAPTCHA_IMAGE.matcher(responseText);
			if (matcher.find())
			{
				uri = locator.buildPath(matcher.group(1));
				Bitmap image = new HttpRequest(uri, data.holder, data).read().getBitmap();
				if (image != null)
				{
					CaptchaData captchaData = new CaptchaData();
					captchaData.put(CaptchaData.CHALLENGE, matcher.group(2));
					return new ReadCaptchaResult(CaptchaState.CAPTCHA, captchaData).setImage(image);
				}
			}
			throw new InvalidResponseException();
		}
		else if (AnonfmChanLocator.BOARD_NAME_TEXTUAL.equals(data.boardName))
		{
			AnonfmChanLocator locator = ChanLocator.get(this);
			Uri uri = locator.buildPath("captcha.php");
			Bitmap image = new HttpRequest(uri, data.holder, data).read().getBitmap();
			if (image == null) throw new InvalidResponseException();
			return new ReadCaptchaResult(CaptchaState.CAPTCHA, new CaptchaData()).setImage(image)
					.setInput(ChanConfiguration.Captcha.Input.ALL);
		}
		else return super.onReadCaptcha(data);
	}
	
	private static final Pattern PATTERN_ERROR = Pattern.compile("<strong>(.*?)</strong>");
	
	public SendPostResult onSendPost(SendPostData data) throws HttpException, ApiException, InvalidResponseException
	{
		if (AnonfmChanLocator.BOARD_NAME_FM.equals(data.boardName))
		{
			String comment = data.comment;
			int maxLength = AnonfmChanConfiguration.MAX_COMMENT_LENGTH_FM;
			if (StringUtils.isEmpty(comment))
			{
				throw new ApiException(ApiException.SEND_ERROR_EMPTY_COMMENT, ApiException.FLAG_KEEP_CAPTCHA);
			}
			if (comment.length() > maxLength)
			{
				throw new ApiException(ApiException.SEND_ERROR_FIELD_TOO_LONG, ApiException.FLAG_KEEP_CAPTCHA);
			}
			UrlEncodedEntity entity = new UrlEncodedEntity();
			entity.add("cid", data.captchaData != null ? data.captchaData.get(CaptchaData.CHALLENGE) : "");
			entity.add("left", Integer.toString(maxLength - comment.length()));
			entity.add("msg", comment);
			entity.add("check", data.captchaData != null ? data.captchaData.get(CaptchaData.INPUT) : "");
			AnonfmChanLocator locator = ChanLocator.get(this);
			Uri uri = locator.buildPath("feedback");
			String responseText = new HttpRequest(uri, data.holder, data).setPostMethod(entity)
					.setRedirectHandler(HttpRequest.RedirectHandler.STRICT).read().getString();
			if (responseText.contains("<h3>Отправлено.</h3>")) return null;
			Matcher matcher = PATTERN_ERROR.matcher(responseText);
			if (matcher.find())
			{
				int errorType = 0;
				String errorMessage = matcher.group(1);
				if (errorMessage.contains("Неверный код подтверждения"))
				{
					errorType = ApiException.SEND_ERROR_CAPTCHA;
				}
				if (errorType != 0) throw new ApiException(errorType);
				CommonUtils.writeLog("Anonfm send message", errorMessage);
				throw new ApiException(errorMessage);
			}
			throw new InvalidResponseException();
		}
		else if (AnonfmChanLocator.BOARD_NAME_TEXTUAL.equals(data.boardName))
		{
			String comment = data.comment;
			if (StringUtils.isEmpty(comment))
			{
				throw new ApiException(ApiException.SEND_ERROR_EMPTY_COMMENT, ApiException.FLAG_KEEP_CAPTCHA);
			}
			MultipartEntity entity = new MultipartEntity();
			entity.add("w", data.threadNumber);
			entity.add("text", comment);
			entity.add("captcha", data.captchaData != null ? data.captchaData.get(CaptchaData.INPUT) : null);
			AnonfmChanLocator locator = ChanLocator.get(this);
			Uri uri = locator.buildPath("board", "dopost.php");
			String responseText = new HttpRequest(uri, data.holder, data).setPostMethod(entity)
					.setRedirectHandler(HttpRequest.RedirectHandler.NONE).read().getString();
			if (data.holder.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP)
			{
				uri = data.holder.getRedirectedUri();
				return new SendPostResult(locator.getThreadNumber(uri), null);
			}
			int errorType = 0;
			if (responseText.contains("А кто будет вводить капчу?") || responseText.contains("введена неправильно")
					|| responseText.contains("Капча в базе не найдена"))
			{
				errorType = ApiException.SEND_ERROR_CAPTCHA;
			}
			if (errorType != 0) throw new ApiException(errorType);
			CommonUtils.writeLog("Anonfm send message", responseText);
			throw new ApiException(responseText);
		}
		else throw new ApiException(ApiException.SEND_ERROR_NO_BOARD);
	}
}