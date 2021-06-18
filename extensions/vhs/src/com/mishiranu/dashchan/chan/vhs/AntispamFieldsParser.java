package com.mishiranu.dashchan.chan.vhs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import android.util.Pair;

import chan.http.RequestEntity;
import chan.text.ParseException;
import chan.text.TemplateParser;
import chan.util.StringUtils;

public class AntispamFieldsParser {
	private final HashSet<String> ignoreFields = new HashSet<>();
	private final ArrayList<Pair<String, String>> fields = new ArrayList<>();

	private boolean formParsing;
	private String fieldName;

	private AntispamFieldsParser(String source, RequestEntity entity, String... ignoreFields) throws ParseException {
		Collections.addAll(this.ignoreFields, ignoreFields);
		PARSER.parse(source, this);
		for (Pair<String, String> field : fields) {
			entity.add(field.first, field.second);
		}
	}

	public static void parseAndApply(String source, RequestEntity entity, String... ignoreFields)
			throws ParseException {
		new AntispamFieldsParser(source, entity, ignoreFields);
	}

	private static final TemplateParser<AntispamFieldsParser> PARSER = TemplateParser.<AntispamFieldsParser>builder()
			.equals("form", "name", "post").open((instance, holder, tagName, attributes) -> {
		holder.formParsing = true;
		return false;
	}).name("input").open((instance, holder, tagName, attributes) -> {
		if (holder.formParsing) {
			String name = attributes.get("name");
			if (!holder.ignoreFields.contains(name)) {
				String value = StringUtils.unescapeHtml(attributes.get("value"));
				holder.fields.add(new Pair<>(name, value));
			}
		}
		return false;
	}).name("textarea").open((instance, holder, tagName, attributes) -> {
		if (holder.formParsing) {
			String name = attributes.get("name");
			if (!holder.ignoreFields.contains(name)) {
				holder.fieldName = name;
				return true;
			}
		}
		return false;
	}).content((instance, holder, text) -> {
		String value = StringUtils.unescapeHtml(text);
		holder.fields.add(new Pair<>(holder.fieldName, value));
	}).name("form").close((instance, holder, tagName) -> {
		if (holder.formParsing) {
			instance.finish();
		}
	}).prepare();
}
