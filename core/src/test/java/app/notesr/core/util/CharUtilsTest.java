/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.core.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class CharUtilsTest {
    @Test
    void testCharsToBytesAndBackWithUtf8() throws CharacterCodingException {
        char[] original = {'H', 'e', 'l', 'l', 'o', '!', '1', '2', '3'};
        byte[] bytes = CharUtils.charsToBytes(Arrays.copyOf(original, original.length),
                StandardCharsets.UTF_8);

        assertNotNull(bytes, "Conversion to bytes should not return null");
        assertTrue(bytes.length > 0, "Byte array should not be empty");

        char[] result = CharUtils.bytesToChars(bytes, StandardCharsets.UTF_8);
        assertNotNull(result, "Conversion back to chars should not return null");
        assertArrayEquals(original, result, "Chars after conversion should match the original");
    }

    @Test
    void testCharsToBytesWithNullInput() throws CharacterCodingException {
        char[] input = null;
        byte[] result = CharUtils.charsToBytes(input, StandardCharsets.UTF_8);
        assertNull(result, "charsToBytes should return null when input is null");
    }

    @Test
    void testBytesToCharsWithNullInput() throws CharacterCodingException {
        byte[] input = null;
        char[] result = CharUtils.bytesToChars(input, StandardCharsets.UTF_8);
        assertNull(result, "bytesToChars should return null when input is null");
    }

    @Test
    void testConversionWithEmptyArray() throws CharacterCodingException {
        char[] emptyChars = new char[0];
        byte[] bytes = CharUtils.charsToBytes(emptyChars, StandardCharsets.UTF_8);
        assertNotNull(bytes, "Conversion of empty char array should not return null");
        assertEquals(0, bytes.length, "Byte array should be empty for empty char array");

        char[] chars = CharUtils.bytesToChars(bytes, StandardCharsets.UTF_8);
        assertNotNull(chars, "Conversion back from empty byte array should not return null");
        assertEquals(0, chars.length, "Char array should be empty after conversion from empty byte array");
    }

    @Test
    void testConversionWithSpecialCharacters() throws CharacterCodingException {
        char[] original = {'ñ', 'ü', 'Ω', 'Ж', 'あ'};
        byte[] bytes = CharUtils.charsToBytes(Arrays.copyOf(original, original.length),
                StandardCharsets.UTF_8);

        assertNotNull(bytes, "Byte array should not be null for special characters");

        char[] result = CharUtils.bytesToChars(bytes, StandardCharsets.UTF_8);
        assertArrayEquals(original, result, "Conversion should preserve all special characters");
    }

    @Test
    void testHasNonZeroCharsWithNull() {
        boolean result = CharUtils.hasNonZeroChars(null);
        assertFalse(result, "hasNonZeroChars should return false for null input");
    }

    @Test
    void testHasNonZeroCharsWithEmptyArray() {
        char[] emptyArray = new char[0];
        boolean result = CharUtils.hasNonZeroChars(emptyArray);
        assertFalse(result, "hasNonZeroChars should return false for empty array");
    }

    @Test
    void testHasNonZeroCharsWithAllZeroChars() {
        char[] allZeros = {'\0', '\0', '\0'};
        boolean result = CharUtils.hasNonZeroChars(allZeros);
        assertFalse(result,
                "hasNonZeroChars should return false when all characters are null");
    }

    @Test
    void testHasNonZeroCharsWithSingleZeroChar() {
        char[] singleZero = {'\0'};
        boolean result = CharUtils.hasNonZeroChars(singleZero);
        assertFalse(result,
                "hasNonZeroChars should return false for single null character");
    }

    @Test
    void testHasNonZeroCharsWithNonZeroAtStart() {
        char[] chars = {'a', '\0', '\0'};
        boolean result = CharUtils.hasNonZeroChars(chars);
        assertTrue(result,
                "hasNonZeroChars should return true when non-zero char is at start");
    }

    @Test
    void testHasNonZeroCharsWithNonZeroAtEnd() {
        char[] chars = {'\0', '\0', 'z'};
        boolean result = CharUtils.hasNonZeroChars(chars);
        assertTrue(result,
                "hasNonZeroChars should return true when non-zero char is at end");
    }

    @Test
    void testHasNonZeroCharsWithNonZeroInMiddle() {
        char[] chars = {'\0', 'b', '\0'};
        boolean result = CharUtils.hasNonZeroChars(chars);
        assertTrue(result,
                "hasNonZeroChars should return true when non-zero char is in middle");
    }

    @Test
    void testHasNonZeroCharsWithAllNonZeroChars() {
        char[] allNonZero = {'H', 'e', 'l', 'l', 'o'};
        boolean result = CharUtils.hasNonZeroChars(allNonZero);
        assertTrue(result,
                "hasNonZeroChars should return true when all characters are non-zero");
    }

    @Test
    void testHasNonZeroCharsWithSingleNonZeroChar() {
        char[] singleNonZero = {'x'};
        boolean result = CharUtils.hasNonZeroChars(singleNonZero);
        assertTrue(result,
                "hasNonZeroChars should return true for single non-zero character");
    }

    @Test
    void testHasNonZeroCharsWithSpecialCharacters() {
        char[] specialChars = {'\0', 'ñ', '\0'};
        boolean result = CharUtils.hasNonZeroChars(specialChars);
        assertTrue(result,
                "hasNonZeroChars should return true for special non-zero characters");
    }
}
