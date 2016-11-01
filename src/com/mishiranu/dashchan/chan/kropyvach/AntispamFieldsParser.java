package com.mishiranu.dashchan.chan.kropyvach;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import android.util.Pair;

import chan.http.RequestEntity;
import chan.text.ParseException;
import chan.text.TemplateParser;
import chan.util.StringUtils;

public class AntispamFieldsParser
{
	private final HashSet<String> mIgnoreFields = new HashSet<>();
	private final ArrayList<Pair<String, String>> mFields = new ArrayList<>();

	private boolean mFormParsing;
	private String mFieldName;

	private AntispamFieldsParser(String source, RequestEntity entity, String... ignoreFields) throws ParseException
	{
		Collections.addAll(mIgnoreFields, ignoreFields);
		PARSER.parse(source, this);
		for (Pair<String, String> field : mFields) entity.add(field.first, field.second);
	}

	public static void parseAndApply(String source, RequestEntity entity, String... ignoreFields) throws ParseException
	{
		new AntispamFieldsParser(source, entity, ignoreFields);
	}

	private static final TemplateParser<AntispamFieldsParser> PARSER = new TemplateParser<AntispamFieldsParser>()
			.equals("form", "name", "post").open((instance, holder, tagName, attributes) ->
	{
		holder.mFormParsing = true;
		return false;

	}).name("input").open((instance, holder, tagName, attributes) ->
	{
		if (holder.mFormParsing)
		{
			String name = attributes.get("name");
			if (!holder.mIgnoreFields.contains(name))
			{
				String value = StringUtils.unescapeHtml(attributes.get("value"));
				holder.mFields.add(new Pair<>(name, value));
			}
		}
		return false;

	}).name("textarea").open((instance, holder, tagName, attributes) ->
	{
		if (holder.mFormParsing)
		{
			String name = attributes.get("name");
			if (!holder.mIgnoreFields.contains(name))
			{
				holder.mFieldName = name;
				return true;
			}
		}
		return false;

	}).content((instance, holder, text) ->
	{
		String value = StringUtils.unescapeHtml(text);
		holder.mFields.add(new Pair<>(holder.mFieldName, value));

	}).name("form").close((instance, holder, tagName) ->
	{
		if (holder.mFormParsing) instance.finish();

	}).prepare();
}