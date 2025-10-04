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

import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Attempts to test that numbers are correctly round-tripped.
 */
class JsonNumberTest {
    // CHECKSTYLE_OFF: MagicNumber
    // CHECKSTYLE_OFF: JavadocMethod
    @Test
    void basicNumberRead() throws JsonParserException {
        JsonArray array = JsonParser.array().from("[1, 1.0, 1.00]");
        assertEquals(1, ((Number) array.get(0)).intValue());
        assertEquals(1.0, ((Number) array.get(1)).doubleValue(), 0.0);
        assertEquals(1.0, ((Number) array.get(2)).doubleValue(), 0.0);
    }

    @Test
    void basicNumberWrite() {
        JsonArray array = JsonArray.from(1, 1.0, 1.0f);
        assertEquals("[1,1.0,1.0]", JsonWriter.string().array(array).done());
    }

    @Test
    void largeIntRead() throws JsonParserException {
        JsonArray array = JsonParser.array().from("[-300000000,300000000]");
        assertEquals(-300000000, ((Number) array.get(0)).intValue());
        assertEquals(300000000, ((Number) array.get(1)).intValue());
    }

    @Test
    void largeIntWrite() {
        JsonArray array = JsonArray.from(-300000000, 300000000);
        assertEquals("[-300000000,300000000]", JsonWriter.string().array(array)
                .done());
    }

    @Test
    void longRead() throws JsonParserException {
        JsonArray array = JsonParser.array().from("[-3000000000,3000000000]");
        assertEquals(-3000000000L, ((Number) array.get(0)).longValue());
        assertEquals(3000000000L, ((Number) array.get(1)).longValue());
    }

    @Test
    void longWrite() {
        JsonArray array = JsonArray.from(1L, -3000000000L, 3000000000L);
        assertEquals("[1,-3000000000,3000000000]",
                JsonWriter.string().array(array).done());
    }

    @Test
    void bigIntRead() throws JsonParserException {
        JsonArray array = JsonParser.array().from(
                "[-30000000000000000000,30000000000000000000]");
        // cast to ensure it's a number
        assertEquals("-30000000000000000000", ((Number) array.get(0)).toString());
        assertEquals("30000000000000000000", ((Number) array.get(1)).toString());
    }

    @Test
    void bigIntWrite() {
        JsonArray array = JsonArray.from(BigInteger.ONE, new BigInteger(
                        "-30000000000000000000"),
                new BigInteger("30000000000000000000"));
        assertEquals("[1,-30000000000000000000,30000000000000000000]",
                JsonWriter.string().array(array).done());
    }

    /**
     * Tests a bug where longs were silently truncated to floats.
     */
    @Test
    void longBuilder() {
        JsonObject o = JsonObject.builder().value("long", 0xffffffffffffL)
                .done();
        assertEquals(0xffffffffffffL, o.getNumber("long").longValue());
    }

    /**
     * Test around the edges of the integral types.
     */
    @Test
    void aroundEdges() throws JsonParserException {
        JsonArray array = JsonArray.from(Integer.MAX_VALUE,
                ((long) Integer.MAX_VALUE) + 1, Integer.MIN_VALUE,
                ((long) Integer.MIN_VALUE) - 1, Long.MAX_VALUE, BigInteger
                        .valueOf(Long.MAX_VALUE).add(BigInteger.ONE),
                Long.MIN_VALUE,
                BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.ONE));
        String json = JsonWriter.string().array(array).done();
        assertEquals(
                "[2147483647,2147483648,-2147483648,-2147483649,9223372036854775807,"
                        + "9223372036854775808,-9223372036854775808,-9223372036854775809]",
                json);
        JsonArray array2 = JsonParser.array().from(json);
        String json2 = JsonWriter.string().array(array2).done();
        assertEquals(json, json2);
    }

    @Test
    void trailingDecimalLazy() throws JsonParserException {
        Object value = JsonParser.any().withLazyNumbers().from("1.000");
        String json = JsonWriter.string().value(value).done();
        assertEquals("1.000", json);
    }
}
