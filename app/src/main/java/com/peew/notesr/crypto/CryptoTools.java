package com.peew.notesr.crypto;

import java.nio.ByteBuffer;

public class CryptoTools {
    private static final int HEX_LINE_SIZE_LIMIT = 4;

    public static String cryptoKeyToHex(CryptoKey key) {
        byte[] keyBytes = key.getKey().getEncoded();
        byte[] salt = key.getSalt();
        byte[] bytes = new byte[keyBytes.length + salt.length];

        StringBuilder result = new StringBuilder();
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        buffer.put(keyBytes);
        buffer.put(salt);

        bytes = buffer.array();
        int lineLength = 0;

        for (int i = 0; i < bytes.length; i++) {
            String hex = byteToHex(bytes[i]);

            if (lineLength < HEX_LINE_SIZE_LIMIT) {
                result.append(hex);
                if (i < bytes.length - 1) result.append(' ');

                lineLength++;
            } else {
                result.append('\n').append(hex).append(' ');
                lineLength = 1;
            }
        }

        return result.toString().toUpperCase();
    }

    public static CryptoKey hexToCryptoKey(String hex, String password) {
        String[] hexArray = hex.toLowerCase()
                .replace("\n", "")
                .split(" ");

        byte[] bytes = new byte[hexArray.length];

        byte[] keyBytes = new byte[Aes.KEY_SIZE / 8];
        byte[] salt = new byte[Aes.SALT_SIZE];

        for (int i = 0; i < hexArray.length; i++) {
            String hexDigit = hexArray[i];
            if (!hexDigit.isBlank()) {
                bytes[i] = (byte) Integer.parseInt(hexDigit, 16);
            }
        }

        System.arraycopy(bytes, 0, keyBytes, 0, keyBytes.length);
        System.arraycopy(bytes, keyBytes.length, salt, 0, salt.length);

        return CryptoManager.getInstance().createCryptoKey(keyBytes, salt, password);
    }

    private static String byteToHex(byte value) {
        char[] hexDigits = new char[2];

        hexDigits[0] = Character.forDigit((value >> 4) & 0xF, 16);
        hexDigits[1] = Character.forDigit((value & 0xF), 16);

        return String.valueOf(hexDigits);
    }
}
