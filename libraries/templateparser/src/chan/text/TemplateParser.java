package chan.text;

import android.util.Pair;
import chan.util.StringUtils;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * <p>HTML text parser. This parser is a convenient wrapper over the {@link GroupParser}. Read about wrapped
 * parser before using this one.</p>
 *
 * <p>You can define parsing rules using the following methods:</p>
 *
 * <ul>
 * <li>{@link SimpleRuleBuilder#name(String)}</li>
 * <li>{@link ComplexRuleBuilder#equals(String, String, String)}</li>
 * <li>{@link ComplexRuleBuilder#starts(String, String, String)}</li>
 * <li>{@link ComplexRuleBuilder#contains(String, String, String)}</li>
 * <li>{@link ComplexRuleBuilder#ends(String, String, String)}</li>
 * </ul>
 *
 * <p>And define reaction rules:</p>
 *
 * <ul>
 * <li>{@link OpenBuilder#open(OpenCallback)}</li>
 * <li>{@link ContentBuilder#content(ContentCallback)}</li>
 * <li>{@link SimpleBuilder#close(CloseCallback)}</li>
 * <li>{@link InitialBuilder#text(TextCallback)}</li>
 * </ul>
 *
 * <p>After defining parsing rules you should call {@link InitialBuilder#prepare()} method.
 * Then you can use your parsing calling {@link #parse(String, Object)} method.</p>
 */
public final class TemplateParser<H> {
	private final HashMap<String, ArrayList<AttributeMatcher<H>>> openMatchers = new HashMap<>();
	private final HashMap<String, ArrayList<AttributeMatcher<H>>> closeMatchers = new HashMap<>();
	private final ArrayList<TextCallback<H>> textCallbacks = new ArrayList<>();
	private boolean ready;

	private final ArrayList<Pair<String, AttributeMatcher<H>>> buildingMatchers = new ArrayList<>();
	private OpenCallback<H> openCallback;
	private ContentCallback<H> contentCallback;
	private CloseCallback<H> closeCallback;

	private TemplateParser() {}

	/**
	 * <p>Creates a new parser builder.</p>
	 *
	 * @param <H> Holder object type.
	 * @return Template parser builder.
	 */
	public static <H> InitialBuilder<H> builder() {
		return new TemplateParser<H>().contentBuilder;
	}

	private static class AttributeMatcher<H> {
		public enum Method {EQUALS, STARTS, CONTAINS, ENDS}

		private final String attribute;
		private final String value;
		private final Method method;

		public OpenCallback<H> openCallback;
		public ContentCallback<H> contentCallback;
		public CloseCallback<H> closeCallback;

		public AttributeMatcher(String attribute, String value, Method method) {
			this.attribute = attribute;
			this.value = value;
			this.method = method;
		}

		public boolean match(Attributes attributes) {
			if (method == null) {
				return true;
			}
			String value = attributes.get(attribute);
			switch (method) {
				case EQUALS: {
					return StringUtils.equals(value, this.value);
				}
				case STARTS: {
					return value != null && value.startsWith(this.value);
				}
				case CONTAINS: {
					return value != null && value.contains(this.value);
				}
				case ENDS: {
					return value != null && value.endsWith(this.value);
				}
			}
			throw new RuntimeException();
		}
	}

	private void copyCallbacks() {
		if (openCallback != null || contentCallback != null || closeCallback != null) {
			if ((openCallback != null || contentCallback != null) && closeCallback != null) {
				throw new IllegalStateException("OpenCallback and ContentCallback can not be defined "
						+ "with CloseCallback at once");
			}
			for (Pair<String, AttributeMatcher<H>> pair : buildingMatchers) {
				if (closeCallback != null && pair.second.attribute != null) {
					throw new IllegalStateException("Attributed tag definition is not supported for closing tags");
				}
				HashMap<String, ArrayList<AttributeMatcher<H>>> map = closeCallback != null
						? closeMatchers : openMatchers;
				ArrayList<AttributeMatcher<H>> matchers = map.get(pair.first);
				if (matchers == null) {
					matchers = new ArrayList<>();
					map.put(pair.first, matchers);
				}
				pair.second.openCallback = openCallback;
				pair.second.contentCallback = contentCallback;
				pair.second.closeCallback = closeCallback;
				matchers.add(pair.second);
			}
			buildingMatchers.clear();
			openCallback = null;
			contentCallback = null;
			closeCallback = null;
		}
	}

	private void normalize() {
		for (ArrayList<AttributeMatcher<H>> matchers : openMatchers.values()) {
			for (int i = 0, j = matchers.size(); i < j; i++) {
				AttributeMatcher<H> matcher = matchers.get(i);
				if (matcher.attribute == null) {
					// Move to end
					matchers.remove(i);
					matchers.add(matcher);
					j--;
				}
			}
		}
	}

	private void checkReady() {
		if (ready) {
			throw new IllegalStateException("You can not call this method after prepare() call");
		}
	}

	private void name(String tagName) {
		tag(tagName, null, null, null);
	}

	private void equals(String tagName, String attribute, String value) {
		tag(tagName, attribute, value, AttributeMatcher.Method.EQUALS);
	}

	private void starts(String tagName, String attribute, String value) {
		tag(tagName, attribute, value, AttributeMatcher.Method.STARTS);
	}

	private void contains(String tagName, String attribute, String value) {
		tag(tagName, attribute, value, AttributeMatcher.Method.CONTAINS);
	}

	private void ends(String tagName, String attribute, String value) {
		tag(tagName, attribute, value, AttributeMatcher.Method.ENDS);
	}

	private void tag(String tagName, String attribute, String value, AttributeMatcher.Method method) {
		checkReady();
		copyCallbacks();
		if (attribute == null) {
			value = null;
		}
		buildingMatchers.add(new Pair<>(tagName, new AttributeMatcher<>(attribute, value, method)));
	}

	private void open(OpenCallback<H> openCallback) {
		checkReady();
		checkHasMatchers();
		this.openCallback = openCallback;
	}

	private void content(ContentCallback<H> contentCallback) {
		checkReady();
		checkHasMatchers();
		this.contentCallback = contentCallback;
	}

	private void close(CloseCallback<H> closeCallback) {
		checkReady();
		checkHasMatchers();
		this.closeCallback = closeCallback;
	}

	private void checkHasMatchers() {
		if (buildingMatchers.isEmpty()) {
			throw new IllegalStateException("You must define at least one parsing rule before adding this callback");
		}
	}

	private void text(TextCallback<H> textCallback) {
		checkReady();
		copyCallbacks();
		if (!buildingMatchers.isEmpty()) {
			throw new IllegalStateException("This callback can not be used with any parsing rules");
		}
		textCallbacks.add(textCallback);
	}

	private void prepare() {
		checkReady();
		copyCallbacks();
		normalize();
		ready = true;
	}

	/**
	 * <p>Starts a new parsing process.</p>
	 *
	 * @param source String to parse.
	 * @param holder Intermediate data holder during parsing process.
	 * @throws ParseException when parsing process was interrupted.
	 */
	public void parse(String source, H holder) throws ParseException {
		if (!ready) {
			throw new IllegalStateException("prepare() was not called");
		}
		try {
			GroupParser.parse(source, new Implementation<>(this, holder));
		} catch (FinishException e) {
			// finish() was called
		}
	}

	/**
	 * <p>Attributes holder and parser.</p>
	 */
	public static final class Attributes {
		private static final Object NULL = new Object();

		private GroupParser parser;
		private String attributes;

		private final HashMap<String, Object> lastValues = new HashMap<>();

		private Attributes() {}

		/**
		 * <p>Parses the attribute and returns its value if attribute exists.</p>
		 *
		 * @param attribute Attribute name.
		 * @return Attribute value.
		 */
		public String get(String attribute) {
			Object value = lastValues.get(attribute);
			if (value != null) {
				return value == NULL ? null : (String) value;
			}
			String stringValue = parser.getAttr(attributes, attribute);
			lastValues.put(attribute, stringValue != null ? stringValue : NULL);
			return stringValue;
		}

		private void set(GroupParser parser, String attributes) {
			this.parser = parser;
			this.attributes = attributes;
			lastValues.clear();
		}
	}

	/**
	 * <p>Parsing process holder.</p>
	 */
	public static final class Instance<H> {
		private final Implementation<H> implementation;

		private Instance(Implementation<H> implementation) {
			this.implementation = implementation;
		}

		/**
		 * <p>Finishes the parsing process. Calling this method doesn't interrupt your working callback.</p>
		 */
		public void finish() {
			implementation.finish = true;
		}
	}

	/**
	 * <p>Tag open callback.</p>
	 */
	public interface OpenCallback<H> {
		/**
		 * <p>Tag open callback method. See {@link OpenBuilder#open(OpenCallback)}.</p>
		 *
		 * @param instance Parser instance holder.
		 * @param holder Intermediate data holder.
		 * @param tagName Tag name.
		 * @param attributes Attributes holder.
		 * @return True if parser should parser full tag content.
		 * @throws ParseException to interrupt parsing process.
		 */
		boolean onOpen(Instance<H> instance, H holder, String tagName, Attributes attributes) throws ParseException;
	}

	/**
	 * <p>Tag full content callback.</p>
	 */
	public interface ContentCallback<H> {
		/**
		 * <p>Tag full content method. See {@link ContentBuilder#content(ContentCallback)}.</p>
		 *
		 * @param instance Parser instance holder.
		 * @param holder Intermediate data holder.
		 * @param text Full tag content.
		 * @throws ParseException to interrupt parsing process.
		 */
		void onContent(Instance<H> instance, H holder, String text) throws ParseException;
	}

	/**
	 * <p>Tag close callback.</p>
	 */
	public interface CloseCallback<H> {
		/**
		 * <p>Tag close callback method. See {@link SimpleBuilder#close(CloseCallback)}.</p>
		 *
		 * @param instance Parser instance holder.
		 * @param holder Intermediate data holder.
		 * @param tagName Tag name.
		 * @throws ParseException to interrupt parsing process.
		 */
		void onClose(Instance<H> instance, H holder, String tagName) throws ParseException;
	}

	/**
	 * <p>Text between tags callback.</p>
	 */
	public interface TextCallback<H> {
		/**
		 * <p>Text between tags callback method. See {@link InitialBuilder#text(TextCallback)}.</p>
		 *
		 * @param instance Parser instance holder.
		 * @param holder Intermediate data holder.
		 * @param source Source string.
		 * @param start Start index of text.
		 * @param end End index of text.
		 * @throws ParseException to interrupt parsing process.
		 */
		void onText(Instance<H> instance, H holder, String source, int start, int end) throws ParseException;
	}

	private static class FinishException extends ParseException {}

	private static class Implementation<H> implements GroupParser.Callback {
		private final TemplateParser<H> parser;
		private final H holder;

		private final Attributes attributes = new Attributes();
		private final Instance<H> instance = new Instance<>(this);

		private AttributeMatcher<H> workMatcher;
		private boolean finish = false;

		public Implementation(TemplateParser<H> parser, H holder) {
			this.parser = parser;
			this.holder = holder;
		}

		private void checkFinish() throws FinishException {
			if (finish) {
				throw new FinishException();
			}
		}

		@Override
		public boolean onStartElement(GroupParser parser, String tagName, String attrs) throws ParseException {
			ArrayList<AttributeMatcher<H>> matchers = this.parser.openMatchers.get(tagName);
			if (matchers != null) {
				attributes.set(parser, attrs);
				for (AttributeMatcher<H> matcher : matchers) {
					if (matcher.match(attributes)) {
						boolean readContent;
						if (matcher.openCallback != null) {
							readContent = matcher.openCallback.onOpen(instance, holder, tagName, attributes);
							checkFinish();
						} else {
							readContent = true;
						}
						if (readContent) {
							workMatcher = matcher;
							return true;
						}
					}
				}
			}
			return false;
		}

		@Override
		public void onEndElement(GroupParser parser, String tagName) throws ParseException {
			ArrayList<AttributeMatcher<H>> matchers = this.parser.closeMatchers.get(tagName);
			if (matchers != null) {
				for (AttributeMatcher<H> matcher : matchers) {
					matcher.closeCallback.onClose(instance, holder, tagName);
					checkFinish();
				}
			}
		}

		@Override
		public void onGroupComplete(GroupParser parser, String text) throws ParseException {
			if (workMatcher.contentCallback != null) {
				workMatcher.contentCallback.onContent(instance, holder, text);
				checkFinish();
			}
		}

		@Override
		public void onText(GroupParser parser, String source, int start, int end) throws ParseException {
			ArrayList<TextCallback<H>> textCallbacks = this.parser.textCallbacks;
			for (TextCallback<H> textCallback : textCallbacks) {
				textCallback.onText(instance, holder, source, start, end);
				checkFinish();
			}
		}
	}

	/**
	 * <p>Parser builder.</p>
	 */
	public interface SimpleRuleBuilder<H> {
		/**
		 * <p>Indicates the parser to react on {@code tagName} tags.</p>
		 *
		 * @param tagName Tag name.
		 * @return Parser builder.
		 */
		SimpleBuilder<H> name(String tagName);
	}

	/**
	 * <p>Parser builder.</p>
	 */
	public interface ComplexSimpleRuleBuilder<H> {
		/**
		 * <p>Indicates the parser to react on {@code tagName} tags.</p>
		 *
		 * @param tagName Tag name.
		 * @return Parser builder.
		 */
		ComplexBuilder<H> name(String tagName);
	}

	/**
	 * <p>Parser builder.</p>
	 */
	public interface ComplexRuleBuilder<H> {
		/**
		 * <p>Indicates the parser to react on {@code tagName} tags which has an {@code attribute}
		 * equals {@code value}.</p>
		 *
		 * @param tagName Tag name.
		 * @param attribute Attribute name.
		 * @param value Attribute value.
		 * @return Parser builder.
		 */
		ComplexBuilder<H> equals(String tagName, String attribute, String value);

		/**
		 * <p>Indicates the parser to react on {@code tagName} tags which has an {@code attribute}
		 * starts with {@code value}.</p>
		 *
		 * @param tagName Tag name.
		 * @param attribute Attribute name.
		 * @param value Attribute value.
		 * @return Parser builder.
		 */
		ComplexBuilder<H> starts(String tagName, String attribute, String value);

		/**
		 * <p>Indicates the parser to react on {@code tagName} tags which has an {@code attribute}
		 * contains {@code value}.</p>
		 *
		 * @param tagName Tag name.
		 * @param attribute Attribute name.
		 * @param value Attribute value.
		 * @return Parser builder.
		 */
		ComplexBuilder<H> contains(String tagName, String attribute, String value);

		/**
		 * <p>Indicates the parser to react on {@code tagName} tags which has an {@code attribute}
		 * ends with {@code value}.</p>
		 *
		 * @param tagName Tag name.
		 * @param attribute Attribute name.
		 * @param value Attribute value.
		 * @return Parser builder.
		 */
		ComplexBuilder<H> ends(String tagName, String attribute, String value);
	}

	/**
	 * <p>Parser builder.</p>
	 */
	public interface OpenBuilder<H> {
		/**
		 * <p>Defines a reaction callback when tag opened. This callback determines whether parser should parse
		 * the full tag content and call content callback or not depending on the return value. If you don't
		 * specify this callback parser will parse full content anyway.</p>
		 *
		 * @param openCallback Tag open callback.
		 * @return Parser builder.
		 * @see GroupParser.Callback#onStartElement(GroupParser, String, String)
		 */
		ContentBuilder<H> open(OpenCallback<H> openCallback);

		/**
		 * <p>Defines a reaction callback when full tag content parsed. This callback may be not called if
		 * open callback returned a {@code false} value.</p>
		 *
		 * @param contentCallback Tag content callback.
		 * @return Parser builder.
		 * @see GroupParser.Callback#onGroupComplete(GroupParser, String)
		 */
		InitialBuilder<H> content(ContentCallback<H> contentCallback);
	}

	/**
	 * <p>Parser builder.</p>
	 */
	public interface InitialBuilder<H> extends SimpleRuleBuilder<H>, ComplexRuleBuilder<H> {
		/**
		 * <p>Defines a reaction callback on text between tags. This callback doesn't depend on any rules.</p>
		 *
		 * @param textCallback Text between tags callback.
		 * @return Parser builder.
		 * @see GroupParser.Callback#onText(GroupParser, String, int, int)
		 */
		InitialBuilder<H> text(TextCallback<H> textCallback);

		/**
		 * <p>Creates a parser from builder.</p>
		 *
		 * @return Parser instance.
		 */
		TemplateParser<H> prepare();
	}

	/**
	 * <p>Parser builder.</p>
	 */
	public interface SimpleBuilder<H> extends SimpleRuleBuilder<H>, ComplexRuleBuilder<H>, OpenBuilder<H> {
		/**
		 * <p>Defines a reaction callback when tag closed. Parser can react only on {@link #name(String)} rule.</p>
		 *
		 * @param closeCallback Tag close callback.
		 * @return Parser builder.
		 * @see GroupParser.Callback#onEndElement(GroupParser, String)
		 */
		InitialBuilder<H> close(CloseCallback<H> closeCallback);
	}

	/**
	 * <p>Parser builder.</p>
	 */
	public interface ComplexBuilder<H> extends ComplexSimpleRuleBuilder<H>, ComplexRuleBuilder<H>, OpenBuilder<H> {}

	/**
	 * <p>Parser builder.</p>
	 */
	public interface ContentBuilder<H> extends InitialBuilder<H> {
		/**
		 * <p>Defines a reaction callback when full tag content parsed. This callback may be not called if
		 * open callback returned a {@code false} value.</p>
		 *
		 * @param contentCallback Tag content callback.
		 * @return Parser builder.
		 * @see GroupParser.Callback#onGroupComplete(GroupParser, String)
		 */
		InitialBuilder<H> content(ContentCallback<H> contentCallback);
	}

	private final ContentBuilder<H> contentBuilder = new ContentBuilder<H>() {
		@Override
		public SimpleBuilder<H> name(String tagName) {
			TemplateParser.this.name(tagName);
			return simpleBuilder;
		}

		@Override
		public ComplexBuilder<H> equals(String tagName, String attribute, String value) {
			TemplateParser.this.equals(tagName, attribute, value);
			return complexBuilder;
		}

		@Override
		public ComplexBuilder<H> starts(String tagName, String attribute, String value) {
			TemplateParser.this.starts(tagName, attribute, value);
			return complexBuilder;
		}

		@Override
		public ComplexBuilder<H> contains(String tagName, String attribute, String value) {
			TemplateParser.this.contains(tagName, attribute, value);
			return complexBuilder;
		}

		@Override
		public ComplexBuilder<H> ends(String tagName, String attribute, String value) {
			TemplateParser.this.ends(tagName, attribute, value);
			return complexBuilder;
		}

		@Override
		public InitialBuilder<H> text(TextCallback<H> textCallback) {
			TemplateParser.this.text(textCallback);
			return contentBuilder;
		}

		@Override
		public InitialBuilder<H> content(ContentCallback<H> contentCallback) {
			TemplateParser.this.content(contentCallback);
			return contentBuilder;
		}

		@Override
		public TemplateParser<H> prepare() {
			TemplateParser.this.prepare();
			return TemplateParser.this;
		}
	};

	private final SimpleBuilder<H> simpleBuilder = new SimpleBuilder<H>() {
		@Override
		public SimpleBuilder<H> name(String tagName) {
			TemplateParser.this.name(tagName);
			return simpleBuilder;
		}

		@Override
		public ComplexBuilder<H> equals(String tagName, String attribute, String value) {
			TemplateParser.this.equals(tagName, attribute, value);
			return complexBuilder;
		}

		@Override
		public ComplexBuilder<H> starts(String tagName, String attribute, String value) {
			TemplateParser.this.starts(tagName, attribute, value);
			return complexBuilder;
		}

		@Override
		public ComplexBuilder<H> contains(String tagName, String attribute, String value) {
			TemplateParser.this.contains(tagName, attribute, value);
			return complexBuilder;
		}

		@Override
		public ComplexBuilder<H> ends(String tagName, String attribute, String value) {
			TemplateParser.this.ends(tagName, attribute, value);
			return complexBuilder;
		}

		@Override
		public ContentBuilder<H> open(OpenCallback<H> openCallback) {
			TemplateParser.this.open(openCallback);
			return contentBuilder;
		}

		@Override
		public InitialBuilder<H> content(ContentCallback<H> contentCallback) {
			TemplateParser.this.content(contentCallback);
			return contentBuilder;
		}

		@Override
		public InitialBuilder<H> close(CloseCallback<H> closeCallback) {
			TemplateParser.this.close(closeCallback);
			return contentBuilder;
		}
	};

	private final ComplexBuilder<H> complexBuilder = new ComplexBuilder<H>() {
		@Override
		public ComplexBuilder<H> name(String tagName) {
			TemplateParser.this.name(tagName);
			return complexBuilder;
		}

		@Override
		public ComplexBuilder<H> equals(String tagName, String attribute, String value) {
			TemplateParser.this.equals(tagName, attribute, value);
			return complexBuilder;
		}

		@Override
		public ComplexBuilder<H> starts(String tagName, String attribute, String value) {
			TemplateParser.this.starts(tagName, attribute, value);
			return complexBuilder;
		}

		@Override
		public ComplexBuilder<H> contains(String tagName, String attribute, String value) {
			TemplateParser.this.contains(tagName, attribute, value);
			return complexBuilder;
		}

		@Override
		public ComplexBuilder<H> ends(String tagName, String attribute, String value) {
			TemplateParser.this.ends(tagName, attribute, value);
			return complexBuilder;
		}

		@Override
		public ContentBuilder<H> open(OpenCallback<H> openCallback) {
			TemplateParser.this.open(openCallback);
			return contentBuilder;
		}

		@Override
		public InitialBuilder<H> content(ContentCallback<H> contentCallback) {
			TemplateParser.this.content(contentCallback);
			return contentBuilder;
		}
	};
}
