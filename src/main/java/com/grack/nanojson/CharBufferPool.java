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

import java.nio.CharBuffer;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicInteger;

public final class CharBufferPool {

    private static final AtomicInteger SIZE = new AtomicInteger(0);
    private static final int MAX_SIZE = 1000;
    private static final int MAX_RETAINED_BUFFER_SIZE = 16 * 1024; // 16KB limit for pooled buffers

    private static final PriorityQueue<CharBuffer> BUFFERS = new PriorityQueue<>();

    private CharBufferPool() {
    }

    public static CharBuffer get(int capacity) {
        synchronized (BUFFERS) {
            if (!BUFFERS.isEmpty()) {
                CharBuffer buffer = BUFFERS.poll();
                if (buffer.capacity() < capacity) {
                    return CharBuffer.allocate(capacity);
                }
                return buffer;
            }
        }

        if (SIZE.incrementAndGet() > MAX_SIZE) {
            SIZE.decrementAndGet();
            throw new IllegalStateException("Buffer pool size limit exceeded");
        }

        return CharBuffer.allocate(capacity);
    }

    public static void release(CharBuffer buffer) {
        if (buffer == null || buffer.capacity() <= 0) {
            return;
        }

        if (buffer.limit() > MAX_RETAINED_BUFFER_SIZE) {
            // If the buffer is too large, decrement SIZE so another can be created
            SIZE.decrementAndGet();
            return;
        }

        buffer.clear();

        synchronized (BUFFERS) {
            BUFFERS.add(buffer);
        }
    }
}
