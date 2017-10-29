package com.mishiranu.dashchan.chan.wizardchan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.util.Pair;

import chan.text.GroupParser;
import chan.text.ParseException;
import chan.util.StringUtils;

public class TinyboardAntispamFieldsParser implements GroupParser.Callback {
	private final String source;
	private final ArrayList<Pair<String, String>> fields = new ArrayList<>();

	private boolean formParsing;
	private String fieldName;

	/*
	 * This is array of real fields. Other fields are fake and can have hash values from server.
	 * See https://github.com/savetheinternet/Tinyboard/blob/master/inc/config.php, "Flood/spam settings" category
	 */
	private static final List<String> VALID_FIELDS = Arrays.asList("board", "thread", "name", "email",
			"subject", "post", "body", "file", "password", "spoiler", "json_response");

	public TinyboardAntispamFieldsParser(String source) {
		this.source = source;
	}

	public ArrayList<Pair<String, String>> convert() throws ParseException {
		try {
			GroupParser.parse(source, this);
		} catch (FinishedException e) {
			// Ignore exception
		}
		return fields;
	}

	private static class FinishedException extends ParseException {
		private static final long serialVersionUID = 1L;
	}

	@Override
	public boolean onStartElement(GroupParser parser, String tagName, String attrs) {
		if ("form".equals(tagName)) {
			String name = parser.getAttr(attrs, "name");
			if ("post".equals(name)) {
				formParsing = true;
			}
		} else if (formParsing) {
			boolean input = "input".equals(tagName);
			boolean textarea = "textarea".equals(tagName);
			if (input || textarea) {
				String fieldName = StringUtils.unescapeHtml(parser.getAttr(attrs, "name"));
				if (!VALID_FIELDS.contains(fieldName)) {
					if (textarea) {
						this.fieldName = fieldName;
						return true;
					} else {
						String value = StringUtils.unescapeHtml(parser.getAttr(attrs, "value"));
						fields.add(new Pair<>(fieldName, value));
					}
				}
			}
		}
		return false;
	}

	@Override
	public void onEndElement(GroupParser parser, String tagName) throws ParseException {
		if ("form".equals(tagName) && formParsing) {
			throw new FinishedException();
		}
	}

	@Override
	public void onText(GroupParser parser, String source, int start, int end) {}

	@Override
	public void onGroupComplete(GroupParser parser, String text) {
		String value = StringUtils.unescapeHtml(text);
		fields.add(new Pair<>(fieldName, value));
	}
}
