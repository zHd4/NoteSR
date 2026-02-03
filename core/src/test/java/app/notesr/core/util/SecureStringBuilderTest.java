/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.core.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SecureStringBuilderTest {
    @Test
    void testAppendChar() {
        SecureStringBuilder builder = new SecureStringBuilder();
        builder.append('A');

        assertEquals(1, builder.length(),
                "Length should be 1 after appending one char");
        assertArrayEquals(new char[]{'A'}, builder.toCharArray(),
                "Char array should contain the appended char");
    }

    @Test
    void testAppendCharArray() {
        SecureStringBuilder builder = new SecureStringBuilder();
        builder.append(new char[]{'a', 'b', 'c'});

        assertEquals(3, builder.length(),
                "Length should match number of appended chars");
        assertArrayEquals(new char[]{'a', 'b', 'c'}, builder.toCharArray(),
                "Char array should contain appended chars");
    }

    @Test
    void testAppendCharSequence() {
        SecureStringBuilder builder = new SecureStringBuilder();
        builder.append("test");

        assertEquals(4, builder.length(),
                "Length should equal the appended CharSequence length");
        assertArrayEquals(new char[]{'t', 'e', 's', 't'}, builder.toCharArray(),
                "Buffer should match appended sequence");
    }

    @Test
    void testAppendNullCharArrayDoesNothing() {
        SecureStringBuilder builder = new SecureStringBuilder();
        builder.append((char[]) null);

        assertEquals(0, builder.length(),
                "Appending null char array should not change length");
    }

    @Test
    void testAppendNullCharSequenceDoesNothing() {
        SecureStringBuilder builder = new SecureStringBuilder();
        builder.append((CharSequence) null);

        assertEquals(0, builder.length(),
                "Appending null CharSequence should not change length");
    }

    @Test
    void testEnsureCapacityExpandsBuilderBuffer() {
        SecureStringBuilder builder = new SecureStringBuilder(1);
        builder.append("abcd");

        assertTrue(builder.length() >= 4,
                "Buffer should expand to fit appended chars");
        assertEquals("abcd", builder.toString(),
                "String representation should match appended text");
    }

    @Test
    void testToCharArrayReturnsCopy() {
        SecureStringBuilder builder = new SecureStringBuilder();
        builder.append("xyz");

        char[] arr = builder.toCharArray();
        arr[0] = 'A';

        assertEquals('x', builder.toCharArray()[0],
                "Modifying returned array should not affect internal buffer");
    }

    @Test
    void testToStringCreatesNewString() {
        SecureStringBuilder builder = new SecureStringBuilder();
        builder.append("abc");

        assertEquals("abc", builder.toString(),
                "toString() should return correct text representation");
    }

    @Test
    void testWipeClearsData() {
        SecureStringBuilder builder = new SecureStringBuilder();
        builder.append("secret");
        builder.wipe();
        assertEquals(0, builder.length(), "Length should be 0 after wipe");
        assertArrayEquals(new char[0], builder.toCharArray(),
                "Char array should be empty after wipe");
    }

    @Test
    void testIllegalInitialCapacityThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new SecureStringBuilder(0),
                "Creating builder with zero capacity should throw IllegalArgumentException");
    }

    @Test
    void testDeleteCharAtRemovesCharacter() {
        SecureStringBuilder builder = new SecureStringBuilder();
        builder.append("abc");
        builder.deleteCharAt(1);

        assertEquals(2, builder.length(),
                "Length should decrease after deleting a char");
        assertEquals("ac", builder.toString(),
                "Character at index should be removed");
    }

    @Test
    void testDeleteCharAtFirstAndLast() {
        SecureStringBuilder builder = new SecureStringBuilder();
        builder.append("abcd");

        builder.deleteCharAt(0);
        assertEquals("bcd", builder.toString(),
                "First character should be removed");

        builder.deleteCharAt(builder.length() - 1);
        assertEquals("bc", builder.toString(),
                "Last character should be removed");
    }

    @Test
    void testDeleteCharAtInvalidIndexThrows() {
        SecureStringBuilder builder = new SecureStringBuilder();
        builder.append("abc");

        assertThrows(IndexOutOfBoundsException.class, () -> builder.deleteCharAt(-1),
                "Negative index should throw");
        assertThrows(IndexOutOfBoundsException.class, () -> builder.deleteCharAt(3),
                "Index equal to length should throw");
    }
}
