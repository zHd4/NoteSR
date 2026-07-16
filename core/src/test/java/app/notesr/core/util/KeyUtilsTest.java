/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.core.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import javax.crypto.SecretKey;

import app.notesr.core.security.crypto.AesCryptor;
import app.notesr.core.security.dto.CryptoSecrets;

class KeyUtilsTest {

    @Test
    void getSecretKeyFromSecretsTruncatesOrPadsCorrectly() {
        int keyLength = AesCryptor.KEY_SIZE / 8;
        byte[] longKey = new byte[40];

        for (int i = 0; i < longKey.length; i++) {
            longKey[i] = (byte) i;
        }

        CryptoSecrets secrets = new CryptoSecrets(longKey, "pass".toCharArray());
        SecretKey secretKey = KeyUtils.getSecretKeyFromSecrets(secrets);

        assertEquals(AesCryptor.KEY_GENERATOR_ALGORITHM, secretKey.getAlgorithm(),
                "SecretKey algorithm should match expected algorithm");

        byte[] expected = Arrays.copyOfRange(longKey, 0, keyLength);
        assertArrayEquals(expected, secretKey.getEncoded(),
                "SecretKey should be truncated to correct length");
    }

    @Test
    void getSecretKeyFromSecretsPadsWithZerosIfShorter() {
        int keyLength = AesCryptor.KEY_SIZE / 8;

        byte[] shortKey = new byte[8];
        Arrays.fill(shortKey, (byte) 0x5A);

        CryptoSecrets secrets = new CryptoSecrets(shortKey, "pass".toCharArray());
        SecretKey secretKey = KeyUtils.getSecretKeyFromSecrets(secrets);

        byte[] expected = new byte[keyLength];
        Arrays.fill(expected, 0, shortKey.length, (byte) 0x5A);

        assertEquals(AesCryptor.KEY_GENERATOR_ALGORITHM, secretKey.getAlgorithm(),
                "SecretKey algorithm should match expected algorithm");
        assertArrayEquals(expected, secretKey.getEncoded(),
                "Short key should be padded with zeros to correct length");
    }

    @Test
    void getKeyHexFromSecretsFormatsCorrectly() {
        byte[] key = new byte[]{
                0x01, 0x23, 0x45, 0x67,
                (byte) 0x89, (byte) 0xAB,
                (byte) 0xCD, (byte) 0xEF
        };

        String expected = "01 23 45 67 \n89 AB CD EF";
        String actual = KeyUtils.getHexFromKeyBytes(key);

        assertEquals(expected, actual,
                "Hex conversion should format key with spaces and newlines correctly");
    }

    @Test
    void getKeyBytesFromHexParsesCorrectly() {
        char[] hex = "01 23 45 67\n89 AB CD EF".toCharArray();

        byte[] expected = new byte[]{
                0x01, 0x23, 0x45, 0x67,
                (byte) 0x89, (byte) 0xAB,
                (byte) 0xCD, (byte) 0xEF
        };

        byte[] actual = KeyUtils.getKeyBytesFromHex(hex);
        assertArrayEquals(expected, actual,
                "Hex parsing should correctly convert formatted hex string to bytes");
    }

    @Test
    void getKeyBytesFromHexHandlesExtraWhitespace() {
        char[] hex = "  0a   1b  \t2c\n3d\r\n4e 5f ".toCharArray();

        byte[] expected = new byte[]{0x0a, 0x1b, 0x2c, 0x3d, 0x4e, 0x5f};
        byte[] actual = KeyUtils.getKeyBytesFromHex(hex);

        assertArrayEquals(expected, actual,
                "Hex parsing should handle extra whitespace (spaces, tabs, newlines)");
    }

    @Test
    void getKeyBytesFromHexThrowsOnInvalidHex() {
        char[] invalidHex = "zz yy xx".toCharArray();

        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                KeyUtils.getKeyBytesFromHex(invalidHex),
                "Should throw IllegalArgumentException for invalid hex characters");

        assertNotNull(exception.getMessage(), "Exception message should not be null");
        assertTrue(exception.getMessage().contains("Invalid hex key"),
                "Exception message should contain 'Invalid hex key'");
    }

    @Test
    void getKeyBytesFromHexThrowsOnNullInput() {
        Exception exception = assertThrows(NullPointerException.class, () ->
                KeyUtils.getKeyBytesFromHex(null),
                "Should throw NullPointerException for null hex input");

        assertNotNull(exception.getMessage(), "Exception message should not be null");
        assertTrue(exception.getMessage().contains("hexChars must not be null"),
                "Exception message should contain 'hexChars must not be null'");
    }

    @Test
    void hexConversionRoundTrip() {
        byte[] originalKey = new byte[]{0x10, 0x20, 0x30, 0x40, 0x50, 0x60};

        String hex = KeyUtils.getHexFromKeyBytes(originalKey);
        byte[] restoredKey = KeyUtils.getKeyBytesFromHex(hex.toCharArray());

        assertArrayEquals(originalKey, restoredKey,
                "Round-trip hex conversion should restore original key bytes");
    }

    @Test
    void getHexFromCryptoSecretsDelegatesCorrectly() {
        CryptoSecrets secrets = new CryptoSecrets(new byte[]{0x0A, 0x0B}, "password".toCharArray());
        String hex = KeyUtils.getKeyHexFromSecrets(secrets);

        assertEquals("0A 0B", hex,
                "getKeyHexFromSecrets should correctly delegate to getHexFromKeyBytes");
    }

    @Test
    void getSecretsFromHexProducesCorrectObject() {
        char[] hex = "0C 0D 0E".toCharArray();
        String password = "secret";

        CryptoSecrets secrets = KeyUtils.getSecretsFromHex(hex, password.toCharArray());

        assertArrayEquals(new byte[]{0x0C, 0x0D, 0x0E}, secrets.getKey(),
                "CryptoSecrets key should match parsed hex");
        assertArrayEquals(password.toCharArray(), secrets.getPassword(),
                "CryptoSecrets password should match input password");
    }

    @Test
    void getIvExtractsIvCorrectly() {
        int keyLength = 48;
        int ivLength = 16;

        byte[] key = new byte[keyLength];
        for (int i = 0; i < key.length; i++) {
            key[i] = (byte) i;
        }

        CryptoSecrets secrets = new CryptoSecrets(key, "password".toCharArray());
        byte[] iv = KeyUtils.getIvFromSecrets(secrets);

        assertEquals(ivLength, iv.length, "Extracted IV should have correct length");
        assertArrayEquals(Arrays.copyOfRange(key, keyLength - ivLength, keyLength), iv,
                "IV should be extracted from end of key");
    }

    @Test
    void isKeyNulledReturnsFalseForNull() {
        assertFalse(KeyUtils.isKeyNulled(null),
                "isKeyNulled should return false for null input");
    }

    @Test
    void isKeyNulledReturnsTrueForEmptyArray() {
        byte[] emptyKey = new byte[0];
        assertTrue(KeyUtils.isKeyNulled(emptyKey),
                "isKeyNulled should return true for empty array");
    }

    @Test
    void isKeyNulledReturnsTrueForAllZeros() {
        byte[] nulledKey = new byte[32];
        Arrays.fill(nulledKey, (byte) 0);

        assertTrue(KeyUtils.isKeyNulled(nulledKey),
                "isKeyNulled should return true for array filled with zeros");
    }

    @Test
    void isKeyNulledReturnsFalseForSingleNonZeroByte() {
        byte[] key = new byte[32];
        Arrays.fill(key, (byte) 0);
        key[15] = (byte) 1;

        assertFalse(KeyUtils.isKeyNulled(key),
                "isKeyNulled should return false when array contains a single non-zero byte");
    }

    @Test
    void isKeyNulledReturnsFalseForNonZeroAtStart() {
        byte[] key = new byte[16];
        key[0] = (byte) 0xFF;
        Arrays.fill(key, 1, key.length, (byte) 0);

        assertFalse(KeyUtils.isKeyNulled(key),
                "isKeyNulled should return false when array has non-zero byte at start");
    }

    @Test
    void isKeyNulledReturnsFalseForNonZeroAtEnd() {
        byte[] key = new byte[16];
        Arrays.fill(key, 0, key.length - 1, (byte) 0);
        key[key.length - 1] = (byte) 0x80;

        assertFalse(KeyUtils.isKeyNulled(key),
                "isKeyNulled should return false when array has non-zero byte at end");
    }

    @Test
    void isKeyNulledReturnsFalseForMultipleNonZeroBytes() {
        byte[] key = new byte[]{0x01, 0x00, 0x02, 0x00, 0x03};

        assertFalse(KeyUtils.isKeyNulled(key),
                "isKeyNulled should return false when array contains multiple non-zero bytes");
    }
}
