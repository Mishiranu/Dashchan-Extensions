package com.mishiranu.dashchan.chan.brchan;

import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import chan.text.ParseException;
import chan.text.TemplateParser;
import chan.util.StringUtils;

public class BrchanBoardsParser
{
	private final String mSource;

	private String mCategory;
	private final LinkedHashMap<String, String> mBoards = new LinkedHashMap<>();

	private static final Pattern PATTERN_LINK = Pattern.compile("/(.*?)/");

	public BrchanBoardsParser(String source)
	{
		mSource = source;
	}

	public LinkedHashMap<String, String> convertMap() throws ParseException
	{
		PARSER.parse(mSource, this);
		return mBoards;
	}

	private static final TemplateParser<BrchanBoardsParser> PARSER = new TemplateParser<BrchanBoardsParser>()
			.name("li").open((i, h, t, a) -> a.get("title") == null).content((instance, holder, text) ->
	{
		holder.mCategory = StringUtils.clearHtml(text);

	}).name("a").open((instance, holder, tagName, attributes) ->
	{
		if (holder.mCategory != null)
		{
			String link = attributes.get("href");
			Matcher matcher = PATTERN_LINK.matcher(link);
			if (matcher.matches()) holder.mBoards.put(matcher.group(1), holder.mCategory);
		}
		return false;

	}).name("div").close((instance, holder, tagName) ->
	{
		if (holder.mCategory != null) instance.finish();

	}).prepare();
}