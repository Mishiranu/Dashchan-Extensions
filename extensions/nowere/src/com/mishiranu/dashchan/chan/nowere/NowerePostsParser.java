package com.mishiranu.dashchan.chan.nowere;

import chan.content.WakabaPostsParser;
import chan.text.ParseException;
import chan.text.TemplateParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

public class NowerePostsParser extends WakabaPostsParser
		<NowereChanConfiguration, NowereChanLocator, NowerePostsParser> {
	private static final SimpleDateFormat DATE_FORMAT;

	static {
		DATE_FORMAT = new SimpleDateFormat("yy/MM/dd(EEE)hh:mm", Locale.US);
		DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT+3"));
	}

	public NowerePostsParser(Object linked, String boardName) {
		super(PARSER, DATE_FORMAT, linked, boardName);
	}

	@Override
	protected void parseThis(TemplateParser<NowerePostsParser> parser, InputStream input)
			throws IOException, ParseException {
		parser.parse(new InputStreamReader(input), this);
	}

	private static final TemplateParser<NowerePostsParser> PARSER = WakabaPostsParser
			.<NowerePostsParser>createParserBuilder().prepare();
}
