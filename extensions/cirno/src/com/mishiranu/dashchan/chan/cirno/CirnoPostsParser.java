package com.mishiranu.dashchan.chan.cirno;

import android.annotation.SuppressLint;
import chan.content.WakabaPostsParser;
import chan.text.ParseException;
import chan.text.TemplateParser;
import chan.util.StringUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Objects;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CirnoPostsParser extends WakabaPostsParser
		<CirnoChanConfiguration, CirnoChanLocator, CirnoPostsParser> {
	private boolean hasPostBlock = false;
	private boolean hasPostBlockName = false;
	private boolean hasPostBlockEmail = false;
	private boolean hasPostBlockFile = false;
	private boolean hasSpoilerCheckBox = false;

	public static final SimpleDateFormat DATE_FORMAT;

	static {
		DateFormatSymbols symbols = new DateFormatSymbols();
		symbols.setShortWeekdays(new String[] {"", "Вс", "Пн", "Вт", "Ср", "Чт", "Пт", "Сб"});
		symbols.setMonths(new String[] {"января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа",
				"сентября", "октября", "ноября", "декабря"});
		@SuppressLint("SimpleDateFormat")
		SimpleDateFormat dateFormat = new SimpleDateFormat("EE dd MMMM yy HH:mm:ss", symbols);
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+3"));
		DATE_FORMAT = dateFormat;
	}

	private static final Pattern ADMIN_NAME = Pattern.compile("<span class=\"adminname\">(.*)</span>");
	private static final Pattern BUMP_LIMIT = Pattern.compile("Максимальное количество бампов треда: (\\d+).");

	public CirnoPostsParser(Object linked, String boardName) {
		super(PARSER, DATE_FORMAT, linked, boardName);
	}

	@Override
	protected void parseThis(TemplateParser<CirnoPostsParser> parser, InputStream input)
			throws IOException, ParseException {
		parser.parse(new InputStreamReader(input), this);
	}

	@Override
	protected void updateConfiguration() {
		if (hasPostBlock) {
			configuration.storeNamesEmailsImagesSpoilersEnabled(boardName,
					hasPostBlockName, hasPostBlockEmail, hasPostBlockFile, hasSpoilerCheckBox);
		}
	}

	@Override
	protected void setNameEmail(String nameHtml, String email) {
		Matcher matcher = ADMIN_NAME.matcher(nameHtml);
		if (matcher.matches()) {
			post.setCapcode("Admin");
			nameHtml = matcher.group(1);
		}
		post.setName(StringUtils.nullIfEmpty(StringUtils.clearHtml(nameHtml).trim()));
	}

	@Override
	protected void storeBoardTitle(String title) {
		int index = title.indexOf("— ");
		if (index >= 0) {
			title = title.substring(index + 2);
		}
		super.storeBoardTitle(title);
	}

	private static final TemplateParser<CirnoPostsParser> PARSER = WakabaPostsParser
			.<CirnoPostsParser>createParserBuilder()
			.equals("div", "class", "rules")
			.content((instance, holder, text) -> {
				Matcher matcher = BUMP_LIMIT.matcher(text);
				if (matcher.find()) {
					int bumpLimit = Integer.parseInt(Objects.requireNonNull(matcher.group(1)));
					holder.configuration.storeBumpLimit(holder.boardName, bumpLimit);
				}
			})
			.equals("td", "class", "postblock")
			.content((instance, holder, text) -> {
				holder.hasPostBlock = true;
				if ("Имя".equals(text)) {
					holder.hasPostBlockName = true;
				} else if ("E-mail".equals(text)) {
					holder.hasPostBlockEmail = true;
				} else if ("Файл".equals(text)) {
					holder.hasPostBlockFile = true;
				}
			})
			.equals("input", "name", "spoiler")
			.open((instance, holder, tagName, attributes) -> {
				holder.hasSpoilerCheckBox = true;
				return false;
			})
			.equals("meta", "http-equiv", "Refresh")
			.open((instance, holder, tagName, attributes) -> {
				String content = attributes.get("content");
				if (content != null) {
					throw new RedirectException(content);
				}
				return false;
			})
			.prepare();

	public static class RedirectException extends ParseException {
		public final String content;

		public RedirectException(String content) {
			this.content = content;
		}
	}
}
