package com.peew.notesr.crypto;

public class CryptoTools {
    private static final int HEX_LINE_SIZE_LIMIT = 4;

    public static String keyBytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();

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

        return result.toString();
    }

    public static byte[] hexKeyToBytes(String hex) {
        String[] hexArray = hex.replace("\n", "").split(" ");
        byte[] bytes = new byte[hexArray.length];

        for (int i = 0; i < hexArray.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hexArray[i], 16);
        }

        return new byte[0];
    }
    private static String byteToHex(byte value) {
        char[] hexDigits = new char[2];

        hexDigits[0] = Character.forDigit((value >> 4) & 0xF, 16);
        hexDigits[1] = Character.forDigit((value & 0xF), 16);

        return String.valueOf(hexDigits);
    }
}
