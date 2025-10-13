package app.notesr.core.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void testGetSecretKeyFromSecretsTruncatesOrPadsCorrectly() {
        int keyLength = AesCryptor.KEY_SIZE / 8;
        byte[] longKey = new byte[40];

        for (int i = 0; i < longKey.length; i++) {
            longKey[i] = (byte) i;
        }

        CryptoSecrets secrets = new CryptoSecrets(longKey, "pass");
        SecretKey secretKey = KeyUtils.getSecretKeyFromSecrets(secrets);

        assertEquals(AesCryptor.KEY_GENERATOR_ALGORITHM, secretKey.getAlgorithm());

        byte[] expected = Arrays.copyOfRange(longKey, 0, keyLength);
        assertArrayEquals(expected, secretKey.getEncoded());
    }

    @Test
    void testGetSecretKeyFromSecretsPadsWithZerosIfShorter() {
        int keyLength = AesCryptor.KEY_SIZE / 8;

        byte[] shortKey = new byte[8];
        Arrays.fill(shortKey, (byte) 0x5A);

        CryptoSecrets secrets = new CryptoSecrets(shortKey, "pass");
        SecretKey secretKey = KeyUtils.getSecretKeyFromSecrets(secrets);

        byte[] expected = new byte[keyLength];
        Arrays.fill(expected, 0, shortKey.length, (byte) 0x5A);

        assertEquals(AesCryptor.KEY_GENERATOR_ALGORITHM, secretKey.getAlgorithm());
        assertArrayEquals(expected, secretKey.getEncoded());
    }

    @Test
    void testGetKeyHexFromSecretsFormatsCorrectly() {
        byte[] key = new byte[] {
                0x01, 0x23, 0x45, 0x67,
                (byte) 0x89, (byte) 0xAB,
                (byte) 0xCD, (byte) 0xEF
        };

        String expected = "01 23 45 67 \n89 AB CD EF";
        String actual = KeyUtils.getHexFromKeyBytes(key);

        assertEquals(expected, actual);
    }

    @Test
    void testGetKeyBytesFromHexParsesCorrectly() {
        String hex = "01 23 45 67\n89 AB CD EF";
        byte[] expected = new byte[] {
                0x01, 0x23, 0x45, 0x67,
                (byte) 0x89, (byte) 0xAB,
                (byte) 0xCD, (byte) 0xEF
        };

        byte[] actual = KeyUtils.getKeyBytesFromHex(hex);
        assertArrayEquals(expected, actual);
    }

    @Test
    void testGetKeyBytesFromHexHandlesExtraWhitespace() {
        String hex = "  0a   1b  \t2c\n3d\r\n4e 5f ";

        byte[] expected = new byte[] {0x0a, 0x1b, 0x2c, 0x3d, 0x4e, 0x5f};
        byte[] actual = KeyUtils.getKeyBytesFromHex(hex);

        assertArrayEquals(expected, actual);
    }

    @Test
    void testGetKeyBytesFromHexThrowsOnInvalidHex() {
        String invalidHex = "zz yy xx";

        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                KeyUtils.getKeyBytesFromHex(invalidHex));

        String message = exception.getMessage();

        assertNotNull(message);
        assertTrue(message.contains("Invalid hex key"));
    }

    @Test
    void testGetKeyBytesFromHexThrowsOnNullInput() {
        Exception exception = assertThrows(NullPointerException.class, () ->
                KeyUtils.getKeyBytesFromHex(null));

        String message = exception.getMessage();
        assertNotNull(message);
        assertTrue(message.contains("hex must not be null"));
    }

    @Test
    void testHexConversionRoundTrip() {
        byte[] originalKey = new byte[] {0x10, 0x20, 0x30, 0x40, 0x50, 0x60};

        String hex = KeyUtils.getHexFromKeyBytes(originalKey);
        byte[] restoredKey = KeyUtils.getKeyBytesFromHex(hex);

        assertArrayEquals(originalKey, restoredKey);
    }

    @Test
    void testGetHexFromCryptoSecretsDelegatesCorrectly() {
        CryptoSecrets secrets = new CryptoSecrets(new byte[] {0x0A, 0x0B}, "password");
        String hex = KeyUtils.getKeyHexFromSecrets(secrets);

        assertEquals("0A 0B", hex);
    }

    @Test
    void testGetSecretsFromHexProducesCorrectObject() {
        String hex = "0C 0D 0E";
        String password = "secret";

        CryptoSecrets secrets = KeyUtils.getSecretsFromHex(hex, password);

        assertArrayEquals(new byte[] {0x0C, 0x0D, 0x0E}, secrets.getKey());
        assertEquals(password, secrets.getPassword());
    }


    @Test
    void testGetIvExtractsIvCorrectly() {
        int keyLength = 48;
        int ivLength = 16;

        byte[] key = new byte[keyLength];

        for (int i = 0; i < key.length; i++) {
            key[i] = (byte) i;
        }

        CryptoSecrets secrets = new CryptoSecrets(key, "password");

        byte[] iv = KeyUtils.getIvFromSecrets(secrets);

        assertEquals(ivLength, iv.length);
        assertArrayEquals(Arrays.copyOfRange(key, keyLength - ivLength, keyLength), iv);
    }
}
