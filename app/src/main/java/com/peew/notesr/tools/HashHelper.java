package com.peew.notesr.tools;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashHelper {
    public static byte[] toSha256Bytes(byte[] raw) throws NoSuchAlgorithmException {
        return getSha256Instance().digest(raw);
    }

    public static String toSha256String(String message) throws NoSuchAlgorithmException {
        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
        byte[] hashBytes = getSha256Instance().digest(messageBytes);

        StringBuilder hex = new StringBuilder(hashBytes.length * 2);

        for (byte b : hashBytes) {
            hex.append(String.format("%02x", b & 0xFF));
        }

        return hex.toString();
    }

    private static MessageDigest getSha256Instance() throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("SHA-256");
    }
}
