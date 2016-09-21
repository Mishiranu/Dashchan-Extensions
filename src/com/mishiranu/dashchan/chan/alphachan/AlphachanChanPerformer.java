package com.mishiranu.dashchan.chan.alphachan;

import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;

import chan.content.ApiException;
import chan.content.ChanPerformer;
import chan.content.InvalidResponseException;
import chan.content.ThreadRedirectException;
import chan.http.HttpException;
import chan.http.HttpRequest;
import chan.http.MultipartEntity;
import chan.text.ParseException;
import chan.util.CommonUtils;

public class AlphachanChanPerformer extends ChanPerformer
{
	@Override
	public ReadThreadsResult onReadThreads(ReadThreadsData data) throws HttpException, InvalidResponseException
	{
		AlphachanChanLocator locator = AlphachanChanLocator.get(this);
		Uri uri = locator.createBoardUri(data.boardName, data.pageNumber);
		String responseText = new HttpRequest(uri, data.holder, data).setValidator(data.validator).read().getString();
		try
		{
			return new ReadThreadsResult(new AlphachanPostsParser(responseText, this, data.boardName).convertThreads());
		}
		catch (ParseException e)
		{
			throw new InvalidResponseException(e);
		}
	}

	@Override
	public ReadPostsResult onReadPosts(ReadPostsData data) throws HttpException, ThreadRedirectException,
			InvalidResponseException
	{
		AlphachanChanLocator locator = AlphachanChanLocator.get(this);
		Uri uri = locator.createThreadUri(data.boardName, data.threadNumber);
		String responseText = new HttpRequest(uri, data.holder, data).read().getString();
		try
		{
			return new ReadPostsResult(new AlphachanPostsParser(responseText, this, data.boardName).convertPosts());
		}
		catch (ParseException e)
		{
			throw new InvalidResponseException(e);
		}
	}

	@Override
	public ReadBoardsResult onReadBoards(ReadBoardsData data) throws HttpException, InvalidResponseException
	{
		AlphachanChanLocator locator = AlphachanChanLocator.get(this);
		Uri uri = locator.buildPath("");
		String responseText = new HttpRequest(uri, data.holder, data).read().getString();
		try
		{
			return new ReadBoardsResult(new AlphachanBoardsParser(responseText).convert());
		}
		catch (ParseException e)
		{
			throw new InvalidResponseException(e);
		}
	}

	@Override
	public ReadPostsCountResult onReadPostsCount(ReadPostsCountData data) throws HttpException, InvalidResponseException
	{
		AlphachanChanLocator locator = AlphachanChanLocator.get(this);
		Uri uri = locator.createThreadUri(data.boardName, data.threadNumber);
		String responseText = new HttpRequest(uri, data).setValidator(data.validator).read().getString();
		if (!responseText.contains("<form name=\"postform\"")) throw new InvalidResponseException();
		int count = 0;
		int index = 0;
		while (index != -1)
		{
			count++;
			index = responseText.indexOf("<div class=\"postnode reply\"", index + 1);
		}
		return new ReadPostsCountResult(count);
	}

	private static final ColorMatrixColorFilter CAPTCHA_FILTER = new ColorMatrixColorFilter(new float[]
			{1f, 0f, 0f, 0f, 15f, 1f, 0f, 0f, 0f, 15f, 1f, 0f, 0f, 0f, 15f, 0f, 0f, 0f, 1f, 0f});

	@Override
	public ReadCaptchaResult onReadCaptcha(ReadCaptchaData data) throws HttpException, InvalidResponseException
	{
		AlphachanChanLocator locator = AlphachanChanLocator.get(this);
		Uri uri = locator.buildPath("captcha.php");
		Bitmap image = new HttpRequest(uri, data).read().getBitmap();
		if (image != null)
		{
			Bitmap newImage = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
			Paint paint = new Paint();
			paint.setColorFilter(CAPTCHA_FILTER);
			new Canvas(newImage).drawBitmap(image, 0f, 0f, paint);
			image.recycle();
			CaptchaData captchaData = new CaptchaData();
			captchaData.put(CaptchaData.CHALLENGE, data.holder.getCookieValue("PHPSESSID"));
			return new ReadCaptchaResult(CaptchaState.CAPTCHA, captchaData).setImage(newImage);
		}
		throw new InvalidResponseException();
	}

	private static final Pattern PATTERN_BOARD_ID = Pattern.compile("<input name=\"board\" value=\"(.*?)\".*?>");

	private final HashMap<String, String> mBoardIds = new HashMap<>();

	@Override
	public SendPostResult onSendPost(SendPostData data) throws HttpException, ApiException, InvalidResponseException
	{
		AlphachanChanLocator locator = AlphachanChanLocator.get(this);
		String boardId;
		synchronized (mBoardIds)
		{
			boardId = mBoardIds.get(data.boardName);
		}
		if (boardId == null)
		{
			Uri uri = locator.createBoardUri(data.boardName, 0);
			String responseText = new HttpRequest(uri, data.holder).setSuccessOnly(false).read().getString();
			if (data.holder.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND)
			{
				throw new ApiException(ApiException.SEND_ERROR_NO_BOARD);
			}
			Matcher matcher = PATTERN_BOARD_ID.matcher(responseText);
			if (!matcher.find()) throw new InvalidResponseException();
			boardId = matcher.group(1);
			synchronized (mBoardIds)
			{
				mBoardIds.put(data.boardName, boardId);
			}
		}
		MultipartEntity entity = new MultipartEntity();
		entity.add("board", boardId);
		entity.add("replythread", data.threadNumber);
		entity.add("subject", data.subject);
		entity.add("message", data.comment);
		entity.add("redirecttothread", "1");
		if (data.attachments != null) data.attachments[0].addToEntity(entity, "imagefile");
		String sessionCookie = null;
		if (data.captchaData != null)
		{
			sessionCookie = data.captchaData.get(CaptchaData.CHALLENGE);
			entity.add("captcha", data.captchaData.get(CaptchaData.INPUT));
		}

		Uri uri = locator.buildPath("add.php");
		String responseText = new HttpRequest(uri, data).setPostMethod(entity).addCookie("PHPSESSID", sessionCookie)
				.addHeader("Referer", (data.threadNumber == null ? locator.createBoardUri(data.boardName, 0)
				: locator.createThreadUri(data.boardName, data.threadNumber)).toString())
				.setRedirectHandler(HttpRequest.RedirectHandler.NONE).read().getString();
		if (data.holder.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP)
		{
			uri = data.holder.getRedirectedUri();
			String threadNumber = locator.getThreadNumber(uri);
			return new SendPostResult(threadNumber, null);
		}
		if (responseText == null) throw new InvalidResponseException();

		int errorType = 0;
		if (responseText.contains("Неправильно введены проверочные цифры"))
		{
			errorType = ApiException.SEND_ERROR_CAPTCHA;
		}
		else if (responseText.contains("Вы ничего не отправили"))
		{
			errorType = ApiException.SEND_ERROR_EMPTY_COMMENT;
		}
		else if (responseText.contains("Приложите файл для создания нити"))
		{
			errorType = ApiException.SEND_ERROR_EMPTY_FILE;
		}
		else if (responseText.contains("Нить не существуе"))
		{
			errorType = ApiException.SEND_ERROR_NO_THREAD;
		}
		else if (responseText.contains("Не указан номер доски") || responseText.contains("Неправильный номер доски"))
		{
			errorType = ApiException.SEND_ERROR_NO_BOARD;
		}
		if (errorType != 0) throw new ApiException(errorType);
		CommonUtils.writeLog("Alphachan send message", responseText);
		throw new ApiException(responseText);
	}
}