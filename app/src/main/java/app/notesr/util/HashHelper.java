package app.notesr.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.zip.CRC32;

public class HashHelper {
    public static byte[] toSha256Bytes(byte[] raw) throws NoSuchAlgorithmException {
        return getSha256Instance().digest(raw);
    }

    public static String toSha256String(byte[] raw) throws NoSuchAlgorithmException {
        byte[] hashBytes = getSha256Instance().digest(raw);

        StringBuilder hex = new StringBuilder(hashBytes.length * 2);

        for (byte b : hashBytes) {
            hex.append(String.format("%02x", b & 0xFF));
        }

        return hex.toString();
    }

    public static String toSha256String(String message) throws NoSuchAlgorithmException {
        return toSha256String(message.getBytes(StandardCharsets.UTF_8));
    }

    public static long getUUIDHash(UUID uuid) {
        CRC32 crc = new CRC32();
        crc.update(uuid.toString().getBytes());

        return crc.getValue();
    }

    private static MessageDigest getSha256Instance() throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("SHA-256");
    }
}
