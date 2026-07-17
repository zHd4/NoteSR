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

/**
 * Utility class for character and byte conversion operations with secure memory handling.
 * 
 * <p>This utility provides methods to convert between character and byte arrays using
 * specified charsets, with automatic memory cleanup to minimize sensitive data exposure.
 * All buffers are securely cleared after use to prevent information leakage.
 */
public final class CharUtils {
    /**
     * Converts a character array to a byte array using the specified charset.
     * 
     * <p>This method encodes the given character array to bytes using the provided charset.
     * Both the character buffer and byte buffer are securely cleared after extraction to
     * minimize sensitive data exposure in memory.
     * 
     * @param chars the character array to convert. May be {@code null}.
     * @param charset the charset to use for encoding
     * @return a new byte array containing the encoded bytes, or {@code null} if the
     *         input character array is {@code null}
     * @throws CharacterCodingException if the character sequence cannot be encoded
     *         using the specified charset
     */
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

    /**
     * Converts a byte array to a character array using the specified charset.
     * 
     * <p>This method decodes the given byte array to characters using the provided charset.
     * Both the byte buffer and character buffer are securely cleared after extraction to
     * minimize sensitive data exposure in memory.
     * 
     * @param bytes the byte array to convert. May be {@code null}.
     * @param charset the charset to use for decoding
     * @return a new character array containing the decoded characters, or {@code null} if the
     *         input byte array is {@code null}
     * @throws CharacterCodingException if the byte sequence cannot be decoded
     *         using the specified charset
     */
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

    /**
     * Checks whether the given character array contains any non-zero characters.
     * 
     * <p>This method iterates through the character array and returns {@code true} if
     * any character is not the null character ({@code '\0'}). This is useful for
     * determining if sensitive data (like passwords) still exists in memory.
     * 
     * @param chars the character array to check. May be {@code null}.
     * @return {@code true} if the array contains at least one non-zero character,
     *         {@code false} if the array is {@code null}, empty, or contains only null characters
     */
    public static boolean hasNonZeroChars(char[] chars) {
        if (chars == null) {
            return false;
        }

        for (char c : chars) {
            if (c != '\0') {
                return true;
            }
        }

        return false;
    }
}
