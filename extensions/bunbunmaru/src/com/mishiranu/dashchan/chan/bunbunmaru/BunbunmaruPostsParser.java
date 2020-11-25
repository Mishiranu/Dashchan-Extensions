package com.mishiranu.dashchan.chan.bunbunmaru;

import android.net.Uri;
import chan.content.WakabaPostsParser;
import chan.text.ParseException;
import chan.text.TemplateParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

public class BunbunmaruPostsParser extends WakabaPostsParser
		<BunbunmaruChanConfiguration, BunbunmaruChanLocator, BunbunmaruPostsParser> {
	private boolean reflinkParsing = false;

	private static final SimpleDateFormat DATE_FORMAT;

	static {
		DATE_FORMAT = new SimpleDateFormat("yy/MM/dd(EEE)hh:mm", Locale.US);
		DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT-8"));
	}

	public BunbunmaruPostsParser(Object linked, String boardName) {
		super(PARSER, DATE_FORMAT, linked, boardName);
		originalNameFromLink = true;
	}

	@Override
	protected void parseThis(TemplateParser<BunbunmaruPostsParser> parser, InputStream input)
			throws IOException, ParseException {
		parser.parse(new InputStreamReader(input), this);
	}

	private static final TemplateParser<BunbunmaruPostsParser> PARSER = WakabaPostsParser
			.<BunbunmaruPostsParser>createParserBuilder()
			.name("label")
			.open((instance, holder, tagName, attributes) -> {
				if (holder.post != null) {
					holder.headerHandling = true;
				}
				return false;
			})
			.equals("span", "class", "reflink")
			.open((instance, holder, tagName, attributes) -> {
				holder.reflinkParsing = true;
				return false;
			})
			.name("a")
			.open((instance, holder, tagName, attributes) -> {
				if (holder.reflinkParsing) {
					holder.reflinkParsing = false;
					if (holder.post != null && holder.post.getParentPostNumber() == null) {
						Uri uri = Uri.parse(attributes.get("href"));
						String threadNumber = holder.locator.getThreadNumber(uri);
						if (threadNumber != null && !threadNumber.equals(holder.post.getPostNumber())) {
							holder.post.setParentPostNumber(threadNumber);
						}
					}
				}
				return false;
			})
			.prepare();
}
