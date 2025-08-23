/*
 * Copyright 2011 The nanojson Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.grack.nanojson;

import ch.randelshofer.fastdoubleparser.JavaDoubleParser;
import ch.randelshofer.fastdoubleparser.JavaFloatParser;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.BitSet;

/**
 * Streaming reader for JSON documents.
 */
public final class JsonReader implements Closeable {
	private JsonTokener tokener;
	private int token;
	private BitSet states = new BitSet();
	private int stateIndex = 0;
	private boolean inObject;
	private boolean first = true;
	private CharBuffer key = CharBufferPool.get(1024);

	/**
	 * The type of value that the {@link JsonReader} is positioned over.
	 */
	public enum Type {
		/**
		 * An object.
		 */
		OBJECT,
		/**
		 * An array.
		 */
		ARRAY,
		/**
		 * A string.
		 */
		STRING,
		/**
		 * A number.
		 */
		NUMBER,
		/**
		 * A boolean value (true or false).
		 */
		BOOLEAN,
		/**
		 * A null value.
		 */
		NULL,
	};

	/**
	 * Create a {@link JsonReader} from an {@link InputStream}.
	 */
	public static JsonReader from(InputStream in) throws JsonParserException {
		return new JsonReader(new JsonTokener(in));
	}

	/**
	 * Create a {@link JsonReader} from a {@link String}.
	 */
	public static JsonReader from(String s) throws JsonParserException {
		return new JsonReader(new JsonTokener(new StringReader(s)));
	}

	/**
	 * Create a {@link JsonReader} from a {@link Reader}.
	 */
	public static JsonReader from(Reader reader) throws JsonParserException {
		return new JsonReader(new JsonTokener(reader));
	}

	/**
	 * Internal constructor.
	 */
	JsonReader(JsonTokener tokener) throws JsonParserException {
		this.tokener = tokener;
		token = tokener.advanceToToken();
	}

	/**
	 * Returns to the array or object structure above the current one, and
	 * advances to the next key or value.
	 */
	public boolean pop() throws JsonParserException {
		// CHECKSTYLE_OFF: EmptyStatement
		while (!next())
			;
		// CHECKSTYLE_ON: EmptyStatement
		first = false;
		inObject = states.get(--stateIndex);
		return token != JsonTokener.TOKEN_EOF;
	}

	/**
	 * Returns the current type of the value.
	 */
	public Type current() throws JsonParserException {
		switch (token) {
			case JsonTokener.TOKEN_TRUE:
			case JsonTokener.TOKEN_FALSE:
				return Type.BOOLEAN;
			case JsonTokener.TOKEN_NULL:
				return Type.NULL;
			case JsonTokener.TOKEN_NUMBER:
				return Type.NUMBER;
			case JsonTokener.TOKEN_STRING:
				return Type.STRING;
			case JsonTokener.TOKEN_OBJECT_START:
				return Type.OBJECT;
			case JsonTokener.TOKEN_ARRAY_START:
				return Type.ARRAY;
			default:
				throw createTokenMismatchException(JsonTokener.TOKEN_NULL, JsonTokener.TOKEN_TRUE,
						JsonTokener.TOKEN_FALSE, JsonTokener.TOKEN_NUMBER, JsonTokener.TOKEN_STRING,
						JsonTokener.TOKEN_OBJECT_START, JsonTokener.TOKEN_ARRAY_START);
		}
	}

	/**
	 * Starts reading an object at the current value.
	 */
	public void object() throws JsonParserException {
		if (token != JsonTokener.TOKEN_OBJECT_START)
			throw createTokenMismatchException(JsonTokener.TOKEN_OBJECT_START);
		states.set(stateIndex++, inObject);
		inObject = true;
		first = true;
	}

	/**
	 * Reads the key for the object at the current value. Does not advance to the
	 * next value.
	 */
	public String key() throws JsonParserException {
		if (!inObject)
			throw tokener.createParseException(null, "Not reading an object", true);
		char[] chars = key.array();
		chars = Arrays.copyOf(chars, key.position());
		return new String(chars);
	}

	/**
	 * Starts reading an array at the current value.
	 */
	public void array() throws JsonParserException {
		if (token != JsonTokener.TOKEN_ARRAY_START)
			throw createTokenMismatchException(JsonTokener.TOKEN_ARRAY_START);
		states.set(stateIndex++, inObject);
		inObject = false;
		first = true;
	}

	/**
	 * Returns the current value.
	 */
	public Object value() throws JsonParserException {
		switch (token) {
			case JsonTokener.TOKEN_TRUE:
				return true;
			case JsonTokener.TOKEN_FALSE:
				return false;
			case JsonTokener.TOKEN_NULL:
				return null;
			case JsonTokener.TOKEN_NUMBER:
				return number();
			case JsonTokener.TOKEN_STRING:
				return string();
			default:
				throw createTokenMismatchException(JsonTokener.TOKEN_NULL, JsonTokener.TOKEN_TRUE,
						JsonTokener.TOKEN_FALSE,
						JsonTokener.TOKEN_NUMBER, JsonTokener.TOKEN_STRING);
		}
	}

	/**
	 * Parses the current value as a null.
	 */
	public void nul() throws JsonParserException {
		if (token != JsonTokener.TOKEN_NULL)
			throw createTokenMismatchException(JsonTokener.TOKEN_NULL);
	}

	/**
	 * Parses the current value as a string.
	 */
	public String string() throws JsonParserException {
		if (token == JsonTokener.TOKEN_NULL)
			return null;
		if (token != JsonTokener.TOKEN_STRING)
			throw createTokenMismatchException(JsonTokener.TOKEN_NULL, JsonTokener.TOKEN_STRING);
		char[] chars = tokener.reusableBuffer.array();
		chars = Arrays.copyOf(chars, tokener.reusableBuffer.position());
		return new String(chars);
	}

	/**
	 * Parses the current value as a boolean.
	 */
	public boolean bool() throws JsonParserException {
		if (token == JsonTokener.TOKEN_TRUE)
			return true;
		else if (token == JsonTokener.TOKEN_FALSE)
			return false;
		else
			throw createTokenMismatchException(JsonTokener.TOKEN_TRUE, JsonTokener.TOKEN_FALSE);
	}

	/**
	 * Parses the current value as a {@link Number}.
	 */
	public Number number() throws JsonParserException {
		if (token == JsonTokener.TOKEN_NULL)
			return null;
		char[] chars = tokener.reusableBuffer.array();
		chars = Arrays.copyOf(chars, tokener.reusableBuffer.position());
		return new JsonLazyNumber(chars, tokener.isDouble);
	}

	/**
	 * Parses the current value as a long.
	 */
	public long longVal() throws JsonParserException {
		char[] chars = tokener.reusableBuffer.array();
		chars = Arrays.copyOf(chars, tokener.reusableBuffer.position());
		return tokener.isDouble ? (long) JavaDoubleParser.parseDouble(chars) : Long.parseLong(new String(chars));
	}

	/**
	 * Parses the current value as an integer.
	 */
	public int intVal() throws JsonParserException {
		char[] chars = tokener.reusableBuffer.array();
		chars = Arrays.copyOf(chars, tokener.reusableBuffer.position());
		return tokener.isDouble ? (int) JavaDoubleParser.parseDouble(chars) : Integer.parseInt(new String(chars));
	}

	/**
	 * Parses the current value as a float.
	 */
	public float floatVal() throws JsonParserException {
		char[] chars = tokener.reusableBuffer.array();
		chars = Arrays.copyOf(chars, tokener.reusableBuffer.position());
		return JavaFloatParser.parseFloat(chars);
	}

	/**
	 * Parses the current value as a double.
	 */
	public double doubleVal() throws JsonParserException {
		char[] chars = tokener.reusableBuffer.array();
		chars = Arrays.copyOf(chars, tokener.reusableBuffer.position());
		return JavaDoubleParser.parseDouble(chars);
	}

	/**
	 * Advance to the next value in this array or object. If no values remain,
	 * return to the parent array or object.
	 *
	 * @return true if we still have values to read in this array or object,
	 *         false if we have completed this object (and implicitly moved back
	 *         to the parent array or object)
	 */
	public boolean next() throws JsonParserException {
		if (stateIndex == 0) {
			throw tokener.createParseException(null, "Unabled to call next() at the root", true);
		}

		token = tokener.advanceToToken();

		if (inObject) {
			if (token == JsonTokener.TOKEN_OBJECT_END) {
				inObject = states.get(--stateIndex);
				first = false;
				return false;
			}

			if (!first) {
				if (token != JsonTokener.TOKEN_COMMA)
					throw createTokenMismatchException(JsonTokener.TOKEN_COMMA, JsonTokener.TOKEN_OBJECT_END);
				token = tokener.advanceToToken();
			}

			if (token != JsonTokener.TOKEN_STRING)
				throw createTokenMismatchException(JsonTokener.TOKEN_STRING);
			key.clear();
			char[] chars = tokener.reusableBuffer.array();
			chars = Arrays.copyOf(chars, tokener.reusableBuffer.position());
			key.put(chars);
			if ((token = tokener.advanceToToken()) != JsonTokener.TOKEN_COLON)
				throw createTokenMismatchException(JsonTokener.TOKEN_COLON);
			token = tokener.advanceToToken();
		} else {
			if (token == JsonTokener.TOKEN_ARRAY_END) {
				inObject = states.get(--stateIndex);
				first = false;
				return false;
			}
			if (!first) {
				if (token != JsonTokener.TOKEN_COMMA)
					throw createTokenMismatchException(JsonTokener.TOKEN_COMMA, JsonTokener.TOKEN_ARRAY_END);
				token = tokener.advanceToToken();
			}
		}

		if (token != JsonTokener.TOKEN_NULL && token != JsonTokener.TOKEN_STRING
				&& token != JsonTokener.TOKEN_NUMBER && token != JsonTokener.TOKEN_TRUE
				&& token != JsonTokener.TOKEN_FALSE && token != JsonTokener.TOKEN_OBJECT_START
				&& token != JsonTokener.TOKEN_ARRAY_START)
			throw createTokenMismatchException(JsonTokener.TOKEN_NULL, JsonTokener.TOKEN_STRING,
					JsonTokener.TOKEN_NUMBER, JsonTokener.TOKEN_TRUE, JsonTokener.TOKEN_FALSE,
					JsonTokener.TOKEN_OBJECT_START, JsonTokener.TOKEN_ARRAY_START);

		first = false;

		return true;
	}

	/**
	 * Releases resources used by this JsonReader. Should be called when done
	 * reading.
	 */
	@Override
	public void close() throws IOException {
		if (key != null) {
			CharBufferPool.release(key);
			key = null;
		}
		if (tokener != null) {
			tokener.close();
		}
	}

	private JsonParserException createTokenMismatchException(int... t) {
		return tokener.createParseException(null, "token mismatch (expected " + Arrays.toString(t)
				+ ", was " + token + ")",
				true);
	}
}
