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

import ch.randelshofer.fastdoubleparser.JavaBigDecimalParser;
import ch.randelshofer.fastdoubleparser.JavaDoubleParser;
import ch.randelshofer.fastdoubleparser.JavaFloatParser;

/**
 * Lazily-parsed number for performance.
 */
@SuppressWarnings("serial")
class JsonLazyNumber extends Number {
	private char[] value;
	private boolean isDouble;

	JsonLazyNumber(char[] number, boolean isDoubleValue) {
		this.value = number;
		this.isDouble = isDoubleValue;
	}

	@Override
	public double doubleValue() {
		return JavaDoubleParser.parseDouble(value);
	}

	@Override
	public float floatValue() {
		return JavaFloatParser.parseFloat(value);
	}

	@Override
	public int intValue() {
		return isDouble ? (int)JavaDoubleParser.parseDouble(value) : Integer.parseInt(new String(value));
	}

	@Override
	public long longValue() {
		return isDouble ? (long) JavaDoubleParser.parseDouble(value) : Long.parseLong(new String(value));
	}

	@Override
	public String toString() {
		return new String(value);
	}

	/**
	 * Avoid serializing {@link JsonLazyNumber}.
	 */
	private Object writeReplace() {
		return JavaBigDecimalParser.parseBigDecimal(value);
	}
}
