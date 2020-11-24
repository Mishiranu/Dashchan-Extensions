package com.mishiranu.dashchan.chan.erlach;

import android.net.Uri;
import android.util.Pair;
import chan.content.ApiException;
import chan.content.ChanPerformer;
import chan.content.InvalidResponseException;
import chan.content.model.Post;
import chan.content.model.Posts;
import chan.http.HttpException;
import chan.http.HttpRequest;
import chan.http.WebSocket;
import chan.text.ParseException;
import chan.util.CommonUtils;
import chan.util.StringUtils;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONException;
import org.json.JSONObject;

public class ErlachChanPerformer extends ChanPerformer {
	private String splitHtml(String response) throws InvalidResponseException {
		StringBuilder builder = new StringBuilder();
		int index = 0;
		String[] what = {"qi('posts').insertAdjacentHTML('beforeend', '", "outerHTML='"};
		while (true) {
			index = StringUtils.nearestIndexOf(response, index, what);
			if (index == -1) {
				break;
			}
			for (String s : what) {
				if (response.indexOf(s, index) == index) {
					index += s.length();
					break;
				}
			}
			for (int i = index; i < response.length(); i++) {
				if (response.charAt(i) == '\'' && response.charAt(i - 1) != '\\') {
					try {
						// Hack to extract escaped string
						JSONObject jsonObject = new JSONObject("{'html':'" + response.substring(index, i) + "'}");
						builder.append(jsonObject.getString("html"));
					} catch (JSONException e) {
						throw new InvalidResponseException(e);
					}
					break;
				}
			}
		}
		return builder.toString();
	}

	private static final Pattern PATTERN_BIN_DATA_LAMBDA = Pattern.compile("bin\\('lambda'\\),bin\\('(.*?)'\\)");

	private String readPage(Uri uri, HttpRequest.Preset preset) throws HttpException, InvalidResponseException {
		WebSocket.Connection connection = new WebSocket(uri, preset).open(event -> {
			ArrayList<String> strings = N2OUtils.parse(event.getData()).first;
			if (strings.size() >= 2 && "io".equals(strings.get(0))) {
				String value = strings.get(1);
				StringBuilder builder = event.get("response");
				if (builder == null) {
					builder = new StringBuilder();
					event.store("response", builder);
				}
				builder.append(value);
				String binData = null;
				Matcher matcher = PATTERN_BIN_DATA_LAMBDA.matcher(value);
				while (matcher.find()) {
					binData = matcher.group(1);
				}
				event.store("bin-data", binData);
				event.complete("next");
			}
		}).sendText("N2O,");
		WebSocket.Result result;
		try {
			while (true) {
				connection.await("next");
				String binData = connection.get("bin-data");
				if (binData != null) {
					connection.sendComplexBinary()
							.bytes(0x83)
							.wrap(N2OUtils.fromInt(0x68, 0x04))
							.wrap(N2OUtils.fromString(0x64, "pickle"))
							.wrap(N2OUtils.fromString(0x6d, "lambda"))
							.wrap(N2OUtils.fromString(0x6d, binData))
							.bytes(0x6a)
							.send();
				} else {
					break;
				}
			}
		} finally {
			result = connection.close();
		}
		StringBuilder response = result.get("response");
		if (response == null) {
			throw new InvalidResponseException();
		}
		return splitHtml(response.toString());
	}

	@Override
	public ReadThreadsResult onReadThreads(ReadThreadsData data) throws HttpException, InvalidResponseException {
		if (data.pageNumber == 0) {
			ErlachChanLocator locator = ErlachChanLocator.get(this);
			Uri uri = locator.buildPath("ws", data.boardName);
			String responseText = readPage(uri, data);
			try {
				ArrayList<Posts> result = new ErlachPostsParser(this).convertThreads(responseText);
				if (result == null || result.isEmpty()) {
					throw HttpException.createNotFoundException();
				}
				return new ReadThreadsResult(result);
			} catch (ParseException e) {
				throw new InvalidResponseException(e);
			}
		} else {
			throw HttpException.createNotFoundException();
		}
	}

	@Override
	public ReadPostsResult onReadPosts(ReadPostsData data) throws HttpException, InvalidResponseException {
		ErlachChanLocator locator = ErlachChanLocator.get(this);
		Uri uri = locator.buildPath("ws", data.boardName, locator.convertToPresentNumber(data.threadNumber));
		String responseText = readPage(uri, data);
		try {
			ArrayList<Post> result = new ErlachPostsParser(this).convertPosts(responseText);
			if (result == null || result.isEmpty()) {
				throw HttpException.createNotFoundException();
			}
			return new ReadPostsResult(result);
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		}
	}

	@Override
	public ReadBoardsResult onReadBoards(ReadBoardsData data) throws HttpException, InvalidResponseException {
		ErlachChanLocator locator = ErlachChanLocator.get(this);
		Uri uri = locator.buildPath("ws", "");
		String responseText = readPage(uri, data);
		try {
			return new ReadBoardsResult(new ErlachBoardsParser().convert(responseText));
		} catch (ParseException e) {
			throw new InvalidResponseException(e);
		}
	}

	private static final Pattern PATTERN_PAGE_ID = Pattern.compile("init\\('(-?\\d+)'\\);");
	private static final Pattern PATTERN_CREATE_THREAD = Pattern.compile("<button id=\"([^\"]*?)\" type=\"button\" " +
			"class=\"black\">.*?\\bCreate thread\\b.*?</button>");
	private static final Pattern PATTERN_CREATE_POST = Pattern.compile("<button id=\"([^\"]*?)\" type=\"button\" " +
			"class=\"orange store-button\".*?>.*?\\bSend\\b.*?</button>");
	private static final Pattern PATTERN_WEBSOCKET_SEND = Pattern.compile("ws\\.send\\(enc\\(tuple\\(atom" +
			"\\('pickle'\\),bin\\('([^']*?)'\\),.*?;");
	private static final Pattern PATTERN_WEBSOCKET_SEND_TEXT = Pattern.compile("'(.*?)'");
	private static final Pattern PATTERN_POST_IMAGE = Pattern.compile("<div id=\"([^\"]*?)\" " +
			"class=\"post-image empty\">");
	private static final Pattern PATTERN_POST_REFERENCE = Pattern.compile(">>(\\d+)");
	private static final Pattern PATTERN_POST_ERROR = Pattern.compile("div.innerHTML = '<button .*?>(.*?)</button>';");
	private static final Pattern PATTERN_POST_SUCCESS = Pattern.compile("qi\\('.*?'\\).dataset.id='(\\w+)'|" +
			"<a class=\"post-topic\" id=\"auto-\\d+\" href=\"/[^/]+/(\\w+)\">");

	private static ArrayList<String> findSendData(String response, String id) {
		Matcher matcher = PATTERN_WEBSOCKET_SEND.matcher(response);
		while (matcher.find()) {
			String foundId = matcher.group(1);
			if (CommonUtils.equals(id, foundId)) {
				ArrayList<String> data = new ArrayList<>();
				matcher = PATTERN_WEBSOCKET_SEND_TEXT.matcher(matcher.group());
				while (matcher.find()) {
					data.add(matcher.group(1));
				}
				return data;
			}
		}
		return null;
	}

	@Override
	public SendPostResult onSendPost(SendPostData data) throws HttpException, ApiException, InvalidResponseException {
		ErlachChanLocator locator = ErlachChanLocator.get(this);
		String comment = StringUtils.replaceAll(data.comment, PATTERN_POST_REFERENCE,
				matcher -> ">>" + locator.convertToPresentNumber(matcher.group(1)));
		Uri uri = data.threadNumber == null ? locator.buildPath("ws", data.boardName)
				: locator.buildPath("ws", data.boardName, locator.convertToPresentNumber(data.threadNumber));

		WebSocket.Connection connection = new WebSocket(uri, data).open(event -> {
			Pair<ArrayList<String>, ArrayList<Integer>> pair = N2OUtils.parse(event.getData());
			ArrayList<String> strings = pair.first;
			ArrayList<Integer> ints = pair.second;
			if (strings.size() >= 2 && "io".equals(strings.get(0))) {
				String value = strings.get(1);
				if (event.get("values") == null) {
					Matcher matcher = PATTERN_PAGE_ID.matcher(value);
					if (matcher.find()) {
						event.store("page-id", matcher.group(1));
					}
					boolean threadBinData = false;
					matcher = PATTERN_CREATE_THREAD.matcher(value);
					if (matcher.find()) {
						threadBinData = true;
						ArrayList<String> sendData = findSendData(value, matcher.group(1));
						if (sendData != null && sendData.size() == 5) {
							event.store("auto-id", sendData.get(1));
							event.store("bin-data", sendData.get(2));
						}
					}
					matcher = PATTERN_CREATE_POST.matcher(value);
					if (matcher.find()) {
						ArrayList<String> sendData = findSendData(value, matcher.group(1));
						if (sendData != null && sendData.size() == 11) {
							String[] values = {sendData.get(1), sendData.get(5), sendData.get(7), sendData.get(9)};
							event.store("values", values);
							event.store("bin-data", sendData.get(2));
						}
					}
					matcher = PATTERN_POST_IMAGE.matcher(value);
					if (matcher.find()) {
						event.store("post-image", matcher.group(1));
					}
					event.complete(threadBinData ? "thread" : "send");
				} else {
					Matcher matcher = PATTERN_POST_ERROR.matcher(value);
					if (matcher.find()) {
						event.store("error", StringUtils.emptyIfNull(matcher.group(1)));
						event.complete("result");
					} else {
						if ("image".equals(event.get("task"))) {
							if (strings.get(1).startsWith("imgLoad(")) {
								event.complete("ftp-complete");
							}
						} else {
							if (strings.size() >= 3 && "ok".equals(strings.get(2))) {
								event.store("ok", "ok");
							} else if ("ok".equals(event.get("ok"))) {
								matcher = PATTERN_POST_SUCCESS.matcher(value);
								if (matcher.find()) {
									String id = matcher.group(1);
									if (id == null) {
										id = matcher.group(2);
									}
									event.store("post-id", id);
									event.complete("result");
								}
							}
						}
					}
				}
			} else if (strings.size() >= 8 && "ftp".equals(strings.get(0))) {
				if ("init".equals(strings.get(7))) {
					event.store("ftp-id", strings.get(3));
					event.store("ftp-item-block", ints.get(6));
					event.complete("ftp-init");
				} else if ("send".equals(strings.get(7)) || "end".equals(strings.get(7))) {
					event.complete("ftp-send");
				}
			}
		}).sendText("N2O,");

		WebSocket.Result result;
		try {
			if (data.threadNumber == null) {
				connection.await("thread");
				String autoId = connection.get("auto-id");
				String binData = connection.get("bin-data");
				connection.store("bin-data", null);
				if (binData == null) {
					throw new InvalidResponseException();
				}
				connection.sendComplexBinary()
						.bytes(0x83)
						.wrap(N2OUtils.fromInt(0x68, 0x04))
						.wrap(N2OUtils.fromString(0x64, "pickle"))
						.wrap(N2OUtils.fromString(0x6d, autoId))
						.wrap(N2OUtils.fromString(0x6d, binData))
						.wrap(N2OUtils.fromInt(0x6c, 0x01))
						.wrap(N2OUtils.fromInt(0x68, 0x02))
						.wrap(N2OUtils.fromInt(0x68, 0x02))
						.wrap(N2OUtils.fromString(0x6b, autoId))
						.wrap(N2OUtils.fromString(0x6d, "detail"))
						.bytes(0x6a, 0x6a)
						.send();
			}
			connection.await("send");

			String pageId = connection.get("page-id");
			String[] values = connection.get("values");
			String binData = connection.get("bin-data");
			String postImage = connection.get("post-image");
			if (pageId == null || binData == null) {
				throw new InvalidResponseException();
			}

			if (postImage != null && data.attachments != null) {
				connection.store("task", "image");
				SendPostData.Attachment attachment = data.attachments[0];
				Pair<Integer, Integer> imageSize = attachment.getImageSize();
				int fileSize = (int) attachment.getSize();
				if (imageSize == null) {
					throw new ApiException(ApiException.SEND_ERROR_FILE_NOT_SUPPORTED);
				}

				connection.sendComplexBinary()
						.bytes(0x83)
						.wrap(N2OUtils.fromInt(0x68, 0x0a))
						.wrap(N2OUtils.fromString(0x64, "ftp"))
						.wrap(N2OUtils.fromString(0x6d, "00000.000"))
						.wrap(N2OUtils.fromString(0x6d, pageId))
						.wrap(N2OUtils.fromString(0x6d, "00000.000000000000"))
						.wrap(N2OUtils.fromInt(0x68, 0x04))
						.wrap(N2OUtils.fromString(0x64, "meta"))
						.wrap(N2OUtils.fromString(0x6d, postImage))
						.wrap(N2OUtils.fromInt(0x62, imageSize.first))
						.wrap(N2OUtils.fromInt(0x62, imageSize.second))
						.wrap(N2OUtils.fromInt(0x62, fileSize))
						.wrap(N2OUtils.fromInt(0x62, 0))
						.wrap(N2OUtils.fromInt(0x62, 1))
						.wrap(N2OUtils.fromInt(0x6d, 0))
						.wrap(N2OUtils.fromString(0x6d, "init"))
						.send();
				connection.await("ftp-init");

				InputStream inputStream = null;
				try {
					String ftpId = connection.get("ftp-id");
					int maxBlockSize = connection.get("ftp-item-block");
					int position = 0;
					inputStream = attachment.openInputSteamForSending();

					while (position < fileSize) {
						int left = fileSize - position;
						int blockSize = Math.min(left, maxBlockSize);

						connection.sendComplexBinary()
								.bytes(0x83)
								.wrap(N2OUtils.fromInt(0x68, 0x0a))
								.wrap(N2OUtils.fromString(0x64, "ftp"))
								.wrap(N2OUtils.fromString(0x6d, "00000.000"))
								.wrap(N2OUtils.fromString(0x6d, pageId))
								.wrap(N2OUtils.fromString(0x6d, ftpId))
								.wrap(N2OUtils.fromInt(0x68, 0x04))
								.wrap(N2OUtils.fromString(0x64, "meta"))
								.wrap(N2OUtils.fromString(0x6d, postImage))
								.wrap(N2OUtils.fromInt(0x62, imageSize.first))
								.wrap(N2OUtils.fromInt(0x62, imageSize.second))
								.wrap(N2OUtils.fromInt(0x62, fileSize))
								.wrap(N2OUtils.fromInt(0x62, position))
								.wrap(N2OUtils.fromInt(0x62, maxBlockSize))
								.wrap(N2OUtils.fromInt(0x6d, blockSize))
								.stream(inputStream, blockSize)
								.wrap(N2OUtils.fromString(0x6d, "send"))
								.send();

						position += blockSize;
						connection.await("ftp-send");
					}

					connection.await("ftp-complete");
				} catch (IOException e) {
					throw new RuntimeException(e);
				} finally {
					if (inputStream != null) {
						try {
							inputStream.close();
						} catch (IOException e) {
							// Ignore exception
						}
					}
				}
			}

			connection.store("task", "post");
			connection.sendComplexBinary()
					.bytes(0x83)
					.wrap(N2OUtils.fromInt(0x68, 0x04))
					.wrap(N2OUtils.fromString(0x64, "pickle"))
					.wrap(N2OUtils.fromString(0x6d, values[0]))
					.wrap(N2OUtils.fromString(0x6d, binData))
					.wrap(N2OUtils.fromInt(0x6c, 0x04))
					.wrap(N2OUtils.fromInt(0x68, 0x02))
					.wrap(N2OUtils.fromInt(0x68, 0x02))
					.wrap(N2OUtils.fromString(0x6b, values[0]))
					.wrap(N2OUtils.fromString(0x6d, "detail"))
					.bytes(0x6a)
					.wrap(N2OUtils.fromInt(0x68, 0x02))
					.wrap(N2OUtils.fromString(0x6b, values[1]))
					.wrap(builder -> {
						if (data.threadNumber == null) {
							return builder.wrap(N2OUtils.fromString(0x6b, StringUtils.emptyIfNull(data.subject)));
						} else {
							return builder.wrap(N2OUtils.fromString(0x6b, data.optionSage ? "true" : "false"));
						}
					}).wrap(N2OUtils.fromInt(0x68, 0x02))
					.wrap(N2OUtils.fromString(0x6b, values[2]))
					.wrap(N2OUtils.fromString(0x6b, StringUtils.emptyIfNull(comment)))
					.wrap(N2OUtils.fromInt(0x68, 0x02))
					.wrap(N2OUtils.fromString(0x6b, values[3]))
					.wrap(N2OUtils.fromString(0x6b, "on"))
					.bytes(0x6a)
					.send();
			connection.await("result");
		} finally {
			result = connection.close();
		}

		String errorMessage = result.get("error");
		if (errorMessage != null) {
			if (errorMessage.contains("Nothing publish")) {
				throw new ApiException(ApiException.SEND_ERROR_EMPTY_COMMENT);
			}
			CommonUtils.writeLog("Erlach send message", errorMessage);
			throw new ApiException(errorMessage);
		}
		String postNumber = result.get("post-id");
		if (postNumber != null) {
			postNumber = locator.convertToDecimalNumber(postNumber);
			String threadNumber = data.threadNumber;
			if (threadNumber == null) {
				threadNumber = postNumber;
				postNumber = null;
			}
			return new SendPostResult(threadNumber, postNumber);
		}
		throw new InvalidResponseException();
	}
}
