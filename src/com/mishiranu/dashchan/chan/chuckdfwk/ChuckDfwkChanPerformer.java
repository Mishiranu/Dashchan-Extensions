package com.mishiranu.dashchan.chan.chuckdfwk;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.util.Pair;

import chan.content.ApiException;
import chan.content.ChanPerformer;
import chan.content.ChanLocator;
import chan.content.InvalidResponseException;
import chan.content.ThreadRedirectException;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.http.HttpException;
import chan.http.HttpRequest;
import chan.http.MultipartEntity;
import chan.http.UrlEncodedEntity;
import chan.text.ParseException;
import chan.util.CommonUtils;
import chan.util.StringUtils;

public class ChuckDfwkChanPerformer extends ChanPerformer
{
	private static final Pattern PATTERN_CATALOG = Pattern.compile("(?s)alt=\"(\\d+)\" border=\"0\" /></a>.*?" +
			"<small>(\\d+)</small>");
	
	@Override
	public ReadThreadsResult onReadThreads(ReadThreadsData data) throws HttpException, InvalidResponseException
	{
		ChuckDfwkChanLocator locator = ChanLocator.get(this);
		if (data.isCatalog())
		{
			Uri uri = locator.buildPath(data.boardName, "catalog.html");
			String responseText = new HttpRequest(uri, data.holder, data).setValidator(data.validator)
					.read().getString();
			ArrayList<Pair<String, Integer>> threadInfos = new ArrayList<>();
			Matcher matcher = PATTERN_CATALOG.matcher(responseText);
			while (matcher.find())
			{
				String threadNumber = matcher.group(1);
				int replies = Integer.parseInt(matcher.group(2));
				threadInfos.add(new Pair<String, Integer>(threadNumber, replies));
			}
			if (threadInfos.isEmpty()) return null;
			uri = locator.buildQuery("expand.php", "board", data.boardName, "threadid", "0");
			responseText = new HttpRequest(uri, data.holder, data).read().getString();
			ArrayList<Post> posts;
			try
			{
				posts = new ChuckDfwkPostsParser(responseText, this, data.boardName).convertExpand();
			}
			catch (ParseException e)
			{
				throw new InvalidResponseException(e);
			}
			HashMap<String, Post> postsMap = new HashMap<>();
			for (Post post : posts)
			{
				if (post.getParentPostNumber() == null) postsMap.put(post.getPostNumber(), post);
			}
			ArrayList<Posts> threads = new ArrayList<>();
			for (Pair<String, Integer> threadInfo : threadInfos)
			{
				Post post = postsMap.get(threadInfo.first);
				if (post != null)
				{
					Posts thread = new Posts(post);
					thread.addPostsCount(1 + threadInfo.second);
					threads.add(thread);
				}
			}
			return new ReadThreadsResult(threads);
		}
		else
		{
			Uri uri = locator.createBoardUri(data.boardName, data.pageNumber);
			String responseText = new HttpRequest(uri, data.holder, data).setValidator(data.validator)
					.read().getString();
			try
			{
				return new ReadThreadsResult(new ChuckDfwkPostsParser(responseText, this, data.boardName)
						.convertThreads());
			}
			catch (ParseException e)
			{
				throw new InvalidResponseException(e);
			}
		}
	}
	
	@Override
	public ReadPostsResult onReadPosts(ReadPostsData data) throws HttpException, ThreadRedirectException,
			InvalidResponseException
	{
		ChuckDfwkChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.createThreadUri(data.boardName, data.threadNumber);
		String responseText = new HttpRequest(uri, data.holder, data).setValidator(data.validator)
				.read().getString();
		try
		{
			return new ReadPostsResult(new ChuckDfwkPostsParser(responseText, this, data.boardName).convertPosts());
		}
		catch (ParseException e)
		{
			throw new InvalidResponseException(e);
		}
	}
	
	@Override
	public ReadBoardsResult onReadBoards(ReadBoardsData data) throws HttpException, InvalidResponseException
	{
		ChuckDfwkChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildPath("");
		String responseText = new HttpRequest(uri, data.holder, data).read().getString();
		try
		{
			return new ReadBoardsResult(new ChuckDfwkBoardsParser(responseText).convert());
		}
		catch (ParseException e)
		{
			throw new InvalidResponseException(e);
		}
	}
	
	@Override
	public ReadPostsCountResult onReadPostsCount(ReadPostsCountData data) throws HttpException, InvalidResponseException
	{
		ChuckDfwkChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildPath(data.boardName, "res", data.threadNumber + "+50.html");
		String responseText = new HttpRequest(uri, data.holder, data).setValidator(data.validator)
				.setSuccessOnly(false).read().getString();
		boolean notFound = data.holder.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND;
		int count = 0;
		if (notFound)
		{
			uri = locator.createThreadUri(data.boardName, data.threadNumber);
			responseText = new HttpRequest(uri, data.holder, data).setValidator(data.validator).read().getString();
		}
		else data.holder.checkResponseCode();
		int index = 0;
		while (index != -1)
		{
			count++;
			index = responseText.indexOf("<td class=\"reply\"", index + 1);
		}
		index = responseText.indexOf("<span class=\"omittedposts\">");
		if (index >= 0)
		{
			Matcher matcher = ChuckDfwkPostsParser.NUMBER.matcher(responseText);
			matcher.find(index + 27);
			count += Integer.parseInt(matcher.group(1));
		}
		return new ReadPostsCountResult(count);
	}
	
	@Override
	public ReadCaptchaResult onReadCaptcha(ReadCaptchaData data) throws HttpException, InvalidResponseException
	{
		ChuckDfwkChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildPath("captcha.php");
		Bitmap image = new HttpRequest(uri, data.holder, data).read().getBitmap();
		if (image != null)
		{
			Bitmap newImage = Bitmap.createBitmap(image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
			int[] line = new int[image.getWidth()];
			for (int j = 0; j < image.getHeight(); j++)
			{
				image.getPixels(line, 0, line.length, 0, j, line.length, 1);
				for (int i = 0; i < line.length; i++)
				{
					int color = line[i];
					color = (int) (Color.red(color) * 0.3f + Color.green(color) * 0.59f + Color.blue(color) * 0.11f);
					line[i] = color > 95 ? 0x00000000 : 0xff000000;
				}
				newImage.setPixels(line, 0, line.length, 0, j, line.length, 1);
			}
			image.recycle();
			Bitmap trimmedImage = CommonUtils.trimBitmap(newImage, 0x00000000);
			if (trimmedImage != null && trimmedImage != newImage)
			{
				newImage.recycle();
				newImage = trimmedImage;
			}
			String sessionCookie = data.holder.getCookieValue("PHPSESSID");
			CaptchaData captchaData = new CaptchaData();
			captchaData.put(CaptchaData.CHALLENGE, sessionCookie);
			return new ReadCaptchaResult(CaptchaState.CAPTCHA, captchaData, newImage);
		}
		throw new InvalidResponseException();
	}
	
	private static final Pattern PATTERN_POST_ERROR = Pattern.compile("(?s)<h2.*?>(.*?)</h2>");
	
	@Override
	public SendPostResult onSendPost(SendPostData data) throws HttpException, ApiException, InvalidResponseException
	{
		MultipartEntity entity = new MultipartEntity();
		entity.add("board", data.boardName);
		entity.add("replythread", data.threadNumber == null ? "0" : data.threadNumber);
		entity.add("name", data.name);
		entity.add("em", data.optionSage ? "sage" : data.email);
		entity.add("subject", data.subject);
		entity.add("message", StringUtils.emptyIfNull(data.comment));
		entity.add("postpassword", data.password);
		entity.add("embed", ""); // Otherwise there will be a "Please enter an embed ID" error
		if (data.attachments != null) data.attachments[0].addToEntity(entity, "imagefile");
		String sessionCookie = null;
		if (data.captchaData != null)
		{
			entity.add("captcha", data.captchaData.get(CaptchaData.INPUT));
			sessionCookie = data.captchaData.get(CaptchaData.CHALLENGE);
		}
		
		ChuckDfwkChanLocator locator = ChanLocator.get(this);
		Uri uri = locator.buildPath("board45.php");
		String responseText;
		try
		{
			new HttpRequest(uri, data.holder, data).setPostMethod(entity).addCookie("PHPSESSID", sessionCookie)
					.setRedirectHandler(HttpRequest.RedirectHandler.NONE).execute();
			if (data.holder.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP)
			{
				uri = data.holder.getRedirectedUri();
				String threadNumber = locator.getThreadNumber(uri);
				return threadNumber != null ? new SendPostResult(threadNumber, null) : null;
			}
			responseText = data.holder.read().getString();
		}
		finally
		{
			data.holder.disconnect();
		}
		
		Matcher matcher = PATTERN_POST_ERROR.matcher(responseText);
		if (matcher.find())
		{
			String errorMessage = matcher.group(1).trim();
			int errorType = 0;
			Object extra = null;
			if (errorMessage.contains("Введен неправильный код подтверждения"))
			{
				errorType = ApiException.SEND_ERROR_CAPTCHA;
			}
			else if (errorMessage.contains("Чтобы ответить, загрузите изображение или введите текст"))
			{
				errorType = ApiException.SEND_ERROR_EMPTY_COMMENT;
			}
			else if (errorMessage.contains("A file is required for a new thread") ||
					errorMessage.contains("Please enter an embed ID"))
			{
				errorType = ApiException.SEND_ERROR_EMPTY_FILE;
			}
			else if (errorMessage.contains("Invalid thread ID"))
			{
				errorType = ApiException.SEND_ERROR_NO_THREAD;
			}
			else if (responseText.contains("Sorry, your message is too long"))
			{
				errorType = ApiException.SEND_ERROR_FIELD_TOO_LONG;
			}
			if (errorType != 0) throw new ApiException(errorType, extra);
			CommonUtils.writeLog("Chuckdfwk send message", errorMessage);
			throw new ApiException(errorMessage);
		}
		throw new InvalidResponseException();
	}
	
	@Override
	public SendDeletePostsResult onSendDeletePosts(SendDeletePostsData data) throws HttpException, ApiException,
			InvalidResponseException
	{
		ChuckDfwkChanLocator locator = ChanLocator.get(this);
		UrlEncodedEntity entity = new UrlEncodedEntity("board", data.boardName, "deletepost", "1",
				"postpassword", data.password);
		for (String postNumber : data.postNumbers) entity.add("del_" + postNumber, postNumber);
		if (data.optionFilesOnly) entity.add("fileonly", "on");
		Uri uri = locator.buildPath("board45.php");
		String responseText = new HttpRequest(uri, data.holder, data).setPostMethod(entity).read().getString();
		if (responseText != null)
		{
			if (responseText.contains("Сообщение удалено") || responseText.contains("Изображение из сообщения удалено")
					|| responseText.contains("В Вашем сообщении нет изображений") ||
					StringUtils.clearHtml(responseText).trim().isEmpty())
			{
				// Response has message for any post
				// Ignore them, if at least 1 of them was deleted
				return null;
			}
			else if (responseText.contains("Неправильный пароль"))
			{
				throw new ApiException(ApiException.DELETE_ERROR_PASSWORD);
			}
			CommonUtils.writeLog("Chuckdfwk delete message", responseText);
		}
		throw new InvalidResponseException();
	}
}