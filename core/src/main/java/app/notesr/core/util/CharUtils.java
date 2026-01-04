/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */
 
package app.notesr.core.util;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.Arrays;

public final class CharUtils {
    public static byte[] charsToBytes(char[] chars, Charset charset)
            throws CharacterCodingException {

        if (chars == null) {
            return null;
        }

        CharsetEncoder encoder = charset.newEncoder();
        CharBuffer charBuffer = CharBuffer.wrap(chars);
        ByteBuffer byteBuffer = encoder.encode(charBuffer);

        byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytes);

        if (charBuffer.hasArray()) {
            Arrays.fill(charBuffer.array(), '\0');
        }

        if (byteBuffer.hasArray()) {
            Arrays.fill(byteBuffer.array(), (byte) 0);
        }

        return bytes;
    }

    public static char[] bytesToChars(byte[] bytes, Charset charset)
            throws CharacterCodingException {

        if (bytes == null) {
            return null;
        }

        CharsetDecoder decoder = charset.newDecoder();
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        CharBuffer charBuffer = decoder.decode(byteBuffer);

        char[] chars = new char[charBuffer.remaining()];
        charBuffer.get(chars);

        if (charBuffer.hasArray()) {
            Arrays.fill(charBuffer.array(), '\0');
        }

        if (byteBuffer.hasArray()) {
            Arrays.fill(byteBuffer.array(), (byte) 0);
        }

        return chars;
    }
}
