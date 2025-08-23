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

import java.util.Arrays;

public class LazyString implements CharSequence {

    private final int length;

    private char[] value;
    private String stringValue;

    private final Object lock = new Object();

    public LazyString(char[] value) {
        this.value = value;
        this.length = value.length;
    }

    public LazyString(String value) {
        this.stringValue = value;
        this.length = value.length();
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    public char charAt(int index) {
        String str = stringValue; // Local ref copy to avoid race
        if (str != null) {
            return str.charAt(index);
        }
        char[] arr = value; // Local ref copy to avoid race
        if (arr != null) {
            return arr[index];
        }
        // Fallback if value was nullified
        return toString().charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        String str = stringValue; // Local ref copy to avoid race
        if (str != null) {
            return str.subSequence(start, end);
        }
        char[] arr = value; // Local ref copy to avoid race
        if (arr != null) {
            return new LazyString(Arrays.copyOfRange(arr, start, end));
        }
        // Fallback to string-based subsequence
        return toString().subSequence(start, end);
    }

    public String toString() {
        if (stringValue != null) {
            return stringValue;
        }
        synchronized (lock) {
            if (stringValue == null) {
                stringValue = new String(value);
            }
            value = null; // Clear the char array to save memory
        }
        return stringValue;
    }

    @Override
    public int hashCode() {
        String str = stringValue; // Local ref copy to avoid race
        if (str != null) {
            return str.hashCode();
        }
        char[] arr = value; // Local ref copy to avoid race
        if (arr != null) {
            return Arrays.hashCode(arr);
        }
        return toString().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof CharSequence))
            return false;
        if (obj instanceof LazyString other) {
            String str = stringValue; // Local ref copy to avoid race
            String otherStr = other.stringValue; // Local ref copy to avoid race
            if (str != null && otherStr != null) {
                return str.equals(otherStr);
            } else if (str != null) {
                return str.contentEquals((CharSequence) other);
            } else if (otherStr != null) {
                return otherStr.contentEquals((CharSequence) this);
            }
            // Both are LazyString without stringValue
            char[] arr = value; // Local ref copy to avoid race
            char[] otherArr = other.value; // Local ref copy to avoid race
            if (arr != null && otherArr != null) {
                return Arrays.equals(arr, otherArr);
            }
            // Fallback to string comparison
            return toString().equals(other.toString());
        }
        return toString().contentEquals((CharSequence) obj);
    }
}
