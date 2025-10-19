package app.notesr.core.util;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import app.notesr.core.security.crypto.AesCryptor;
import app.notesr.core.security.dto.CryptoSecrets;

public final class KeyUtils {
    private static final int HEX_LINE_SIZE_LIMIT = 4;

    public static SecretKey getSecretKeyFromSecrets(CryptoSecrets cryptoSecrets) {
        int keyLength = AesCryptor.KEY_SIZE / 8;
        byte[] keyBytes = Arrays.copyOfRange(cryptoSecrets.getKey(), 0, keyLength);

        SecretKey secretKey = new SecretKeySpec(keyBytes, AesCryptor.KEY_GENERATOR_ALGORITHM);
        Arrays.fill(keyBytes, (byte) 0);

        return secretKey;
    }

    public static String getKeyHexFromSecrets(CryptoSecrets cryptoSecrets) {
        return getHexFromKeyBytes(cryptoSecrets.getKey());
    }

    public static CryptoSecrets getSecretsFromHex(char[] hex, char[] password) {
        return new CryptoSecrets(getKeyBytesFromHex(hex), password);
    }

    public static String getHexFromKeyBytes(byte[] key) {
        StringBuilder result = new StringBuilder();
        int lineLength = 0;

        for (int i = 0; i < key.length; i++) {
            String hex = byteToHex(key[i]);

            if (lineLength < HEX_LINE_SIZE_LIMIT) {
                result.append(hex);

                if (i < key.length - 1) {
                    result.append(' ');
                }

                lineLength++;
            } else {
                result.append('\n').append(hex);

                if (i < key.length - 1) {
                    result.append(' ');
                }

                lineLength = 1;
            }
        }

        return result.toString().toUpperCase();
    }

    public static byte[] getKeyBytesFromHex(char[] hexChars) {
        requireNonNull(hexChars, "hexChars must not be null");

        try {
            int tokenCount = countHexTokens(hexChars);
            byte[] key = new byte[tokenCount];

            parseHexTokens(hexChars, key);

            Arrays.fill(hexChars, '\0');
            return key;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid hex key", e);
        }
    }

    public static byte[] getIvFromSecrets(CryptoSecrets cryptoSecrets) {
        int ivSize = cryptoSecrets.getKey().length - (AesCryptor.KEY_SIZE / 8);
        byte[] iv = new byte[ivSize];

        System.arraycopy(cryptoSecrets.getKey(), AesCryptor.KEY_SIZE / 8, iv, 0, ivSize);
        return iv;
    }

    private static String byteToHex(byte value) {
        char[] hexDigits = new char[2];

        hexDigits[0] = Character.forDigit((value >> 4) & 0xF, 16);
        hexDigits[1] = Character.forDigit((value & 0xF), 16);

        return String.valueOf(hexDigits);
    }

    private static int countHexTokens(char[] chars) {
        int count = 0;
        boolean inToken = false;

        for (char c : chars) {
            if (Character.isWhitespace(c)) {
                if (inToken) {
                    count++;
                    inToken = false;
                }
            } else {
                inToken = true;
            }
        }

        if (inToken) {
            count++;
        }

        return count;
    }

    private static void parseHexTokens(char[] chars, byte[] output) {
        int keyIndex = 0;
        int nibbleCount = 0;
        int value = 0;

        boolean hasDigit = false;

        for (char c : chars) {
            if (Character.isWhitespace(c)) {
                if (hasDigit) {
                    output[keyIndex++] = (byte) value;
                    nibbleCount = 0;
                    value = 0;
                    hasDigit = false;
                }

                continue;
            }

            int digit = hexDigitToInt(c);

            if (digit < 0) {
                throw new IllegalArgumentException("Invalid hex character: " + c);
            }

            value = (value << 4) | digit;
            nibbleCount++;
            hasDigit = true;

            if (nibbleCount == 2) {
                output[keyIndex++] = (byte) value;
                nibbleCount = 0;
                value = 0;
                hasDigit = false;
            }
        }

        if (hasDigit) {
            output[keyIndex] = (byte) value;
        }
    }

    private static int hexDigitToInt(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }

        if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        }

        if (c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        }

        return -1;
    }
}
