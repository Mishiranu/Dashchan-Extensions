package com.mishiranu.dashchan.chan.fourchan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ReportReason {
	public final String category;
	public final String value;
	public final String title;

	public ReportReason(String category, String value, String title) {
		this.category = category;
		this.value = value;
		this.title = title;
	}

	public String getKey() {
		JSONObject jsonObject = new JSONObject();
		try {
			jsonObject.put("category", category);
			jsonObject.put("value", value);
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
		return jsonObject.toString();
	}

	public static ReportReason fromKey(String key) {
		if (key == null) {
			return null;
		}
		try {
			JSONObject jsonObject = new JSONObject(key);
			String category = jsonObject.getString("category");
			String value = jsonObject.getString("value");
			return new ReportReason(category, value, "");
		} catch (JSONException e) {
			return null;
		}
	}

	public static List<ReportReason> parse(String json) {
		try {
			JSONArray jsonArray = new JSONArray(json);
			ArrayList<ReportReason> reportReasons = new ArrayList<>(jsonArray.length());
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject jsonObject = jsonArray.getJSONObject(i);
				String category = jsonObject.getString("category");
				String value = jsonObject.getString("value");
				String title = jsonObject.getString("title");
				reportReasons.add(new ReportReason(category, value, title));
			}
			return reportReasons;
		} catch (JSONException e) {
			return Collections.emptyList();
		}
	}

	public static String serialize(List<ReportReason> reportReasons) {
		if (reportReasons == null || reportReasons.isEmpty()) {
			return null;
		}
		JSONArray jsonArray = new JSONArray();
		for (ReportReason reportReason : reportReasons) {
			JSONObject jsonObject = new JSONObject();
			try {
				jsonObject.put("category", reportReason.category);
				jsonObject.put("value", reportReason.value);
				jsonObject.put("title", reportReason.title);
			} catch (JSONException e) {
				throw new RuntimeException(e);
			}
			jsonArray.put(jsonObject);
		}
		return jsonArray.toString();
	}
}
