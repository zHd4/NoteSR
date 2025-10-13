package app.notesr.core.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.UUID;

class HashUtilsTest {
    private static final int MAX_DATA_SIZE = 100000;
    private final Random random = new Random();

    @Test
    void testSha256Bytes() throws NoSuchAlgorithmException {
        byte[] data = new byte[random.nextInt(MAX_DATA_SIZE)];
        random.nextBytes(data);

        byte[] expected = MessageDigest.getInstance("SHA-256").digest(data);
        byte[] actual = HashUtils.toSha256Bytes(data);

        assertArrayEquals(expected, actual, "Actual hash different");
    }

    @Test
    void testToSha256StringFromBytes() throws NoSuchAlgorithmException {
        byte[] data = "hello".getBytes(StandardCharsets.UTF_8);

        String expected = bytesToHex(MessageDigest.getInstance("SHA-256").digest(data));
        String actual = HashUtils.toSha256String(data);

        assertEquals(expected, actual, "Actual hex string different");
    }

    @Test
    void testToSha256StringFromString() throws NoSuchAlgorithmException {
        String message = "hello world";

        byte[] hash = MessageDigest.getInstance("SHA-256")
                .digest(message.getBytes(StandardCharsets.UTF_8));

        String expected = bytesToHex(hash);
        String actual = HashUtils.toSha256String(message);

        assertEquals(expected, actual, "Actual hex string from string different");
    }

    @Test
    void testFromSha256HexString() throws NoSuchAlgorithmException {
        String message = "some test input";

        byte[] hashBytes = MessageDigest.getInstance("SHA-256")
                .digest(message.getBytes(StandardCharsets.UTF_8));

        String hex = bytesToHex(hashBytes);

        byte[] actual = HashUtils.fromSha256HexString(hex);

        assertArrayEquals(hashBytes, actual, "Restored bytes do not match original hash");
    }

    @Test
    void testFromSha256HexStringWithInvalidLengthThrowsException() {
        String invalidHex = "abc";

        assertThrows(IllegalArgumentException.class, () ->
                HashUtils.fromSha256HexString(invalidHex));
    }

    @Test
    void testFromSha256HexStringWithInvalidCharacterThrowsException() {
        String invalidHex = "zzzz";

        assertThrows(NumberFormatException.class, () -> HashUtils.fromSha256HexString(invalidHex));
    }

    @Test
    void testGetUUIDHash() {
        UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");

        long expected = 3121068470L;
        long actual = HashUtils.getUUIDHash(uuid);

        assertEquals(expected, actual, "Actual hash different");
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);

        for (byte b : bytes) {
            hex.append(String.format("%02x", b & 0xFF));
        }

        return hex.toString();
    }
}
