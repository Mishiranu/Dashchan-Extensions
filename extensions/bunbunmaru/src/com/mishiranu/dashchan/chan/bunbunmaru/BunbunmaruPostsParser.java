package com.mishiranu.dashchan.chan.bunbunmaru;

import chan.content.WakabaPostsParser;
import chan.text.ParseException;
import chan.text.TemplateParser;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

public class BunbunmaruPostsParser extends WakabaPostsParser
		<BunbunmaruChanConfiguration, BunbunmaruChanLocator, BunbunmaruPostsParser> {
	private static final SimpleDateFormat DATE_FORMAT;

	static {
		DATE_FORMAT = new SimpleDateFormat("yy/MM/dd(EEE)hh:mm", Locale.US);
		DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT-8"));
	}

	public BunbunmaruPostsParser(String source, Object linked, String boardName) {
		super(PARSER, DATE_FORMAT, source, linked, boardName);
		originalNameFromLink = true;
	}

	@Override
	protected void parseThis(TemplateParser<BunbunmaruPostsParser> parser, String source) throws ParseException {
		parser.parse(source, this);
	}

	private static final TemplateParser<BunbunmaruPostsParser> PARSER = WakabaPostsParser
			.<BunbunmaruPostsParser>createParserBuilder().prepare();
}
