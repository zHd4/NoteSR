/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.core.util;

import androidx.annotation.NonNull;

import java.util.Arrays;

public final class SecureStringBuilder {
    private static final int INITIAL_CAPACITY = 32;

    private char[] buffer;
    private int length = 0;

    public SecureStringBuilder() {
        this(INITIAL_CAPACITY);
    }

    public SecureStringBuilder(int initialCapacity) {
        if (initialCapacity <= 0) {
            throw new IllegalArgumentException("Capacity must be > 0");
        }

        buffer = new char[initialCapacity];
    }

    public SecureStringBuilder append(char c) {
        ensureCapacity(length + 1);
        buffer[length++] = c;

        return this;
    }

    public SecureStringBuilder append(char[] chars) {
        if (chars == null) {
            return this;
        }

        ensureCapacity(length + chars.length);
        System.arraycopy(chars, 0, buffer, length, chars.length);
        length += chars.length;

        return this;
    }

    public SecureStringBuilder append(CharSequence seq) {
        if (seq == null) {
            return this;
        }

        ensureCapacity(length + seq.length());

        for (int i = 0; i < seq.length(); i++) {
            buffer[length++] = seq.charAt(i);
        }

        return this;
    }

    public SecureStringBuilder deleteCharAt(int index) {
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException("Index out of bounds");
        }

        System.arraycopy(buffer, index + 1, buffer, index, length - index - 1);
        buffer[length - 1] = '\0';
        length--;
        return this;
    }

    public char[] toCharArray() {
        return Arrays.copyOf(buffer, length);
    }

    public int length() {
        return length;
    }

    public void wipe() {
        if (buffer != null) {
            Arrays.fill(buffer, '\0');
            buffer = new char[0];
            length = 0;
        }
    }

    private void ensureCapacity(int minCapacity) {
        if (minCapacity <= buffer.length) {
            return;
        }

        int newCapacity = Math.max(buffer.length * 2, minCapacity);
        char[] newBuffer = new char[newCapacity];

        System.arraycopy(buffer, 0, newBuffer, 0, length);
        Arrays.fill(buffer, '\0');

        buffer = newBuffer;
    }

    @NonNull
    @Override
    public String toString() {
        return new String(buffer, 0, length);
    }
}
