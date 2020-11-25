package com.mishiranu.dashchan.chan.fourchan;

import chan.text.ParseException;
import chan.text.TemplateParser;
import chan.util.StringUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class FourchanRulesParser {
	private final ArrayList<ReportReason> reportReasons = new ArrayList<>();
	private final ArrayList<ReportReason> categoryReportReasons = new ArrayList<>();

	private String category;
	private String categoryTitle;
	private String value;

	public List<ReportReason> parse(InputStream input) throws IOException, ParseException {
		PARSER.parse(new InputStreamReader(input), this);
		return reportReasons;
	}

	private void closeCategory() {
		if (category != null) {
			if (categoryReportReasons.isEmpty()) {
				if (categoryTitle != null) {
					reportReasons.add(new ReportReason(category, "", categoryTitle));
				}
			} else {
				reportReasons.addAll(categoryReportReasons);
				categoryReportReasons.clear();
			}
			category = null;
			categoryTitle = null;
		}
	}

	private static final TemplateParser<FourchanRulesParser> PARSER = TemplateParser
			.<FourchanRulesParser>builder()
			.equals("input", "name", "cat")
			.open(((instance, holder, tagName, attributes) -> {
				holder.closeCategory();
				holder.category = attributes.get("value");
				return false;
			}))
			.name("label")
			.open((instance, holder, tagName, attributes) -> holder.category != null)
			.content((instance, holder, text) -> holder.categoryTitle = StringUtils.clearHtml(text))
			.name("option")
			.open((instance, holder, tagName, attributes) -> {
				if (holder.category != null) {
					String value = attributes.get("value");
					if (value != null) {
						holder.value = value;
						return true;
					}
				}
				return false;
			})
			.content((instance, holder, text) -> {
				String title = StringUtils.clearHtml(text);
				if (!StringUtils.isEmpty(title)) {
					holder.categoryReportReasons.add(new ReportReason(holder.category, holder.value, title));
				}
			})
			.name("form")
			.close((instance, holder, tagName) -> holder.closeCategory())
			.prepare();
}
