package app.notesr.security.util;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import app.notesr.security.crypto.AesCryptor;
import app.notesr.security.dto.CryptoSecrets;

public final class KeyUtils {
    private static final int HEX_LINE_SIZE_LIMIT = 4;

    public static SecretKey getSecretKeyFromSecrets(CryptoSecrets cryptoSecrets) {
        int keyLength = AesCryptor.KEY_SIZE / 8;
        byte[] keyBytes = Arrays.copyOfRange(cryptoSecrets.getKey(), 0, keyLength);

        return new SecretKeySpec(keyBytes, AesCryptor.KEY_GENERATOR_ALGORITHM);
    }

    public static String getKeyHexFromSecrets(CryptoSecrets cryptoSecrets) {
        return getHexFromKeyBytes(cryptoSecrets.getKey());
    }

    public static CryptoSecrets getSecretsFromHex(String hex, String password) {
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

    public static byte[] getKeyBytesFromHex(String hex) {
        try {
            requireNonNull(hex, "hex must not be null");

            String[] hexArray = hex.toLowerCase().trim().split("\\s+");
            byte[] key = new byte[hexArray.length];

            for (int i = 0; i < hexArray.length; i++) {
                key[i] = (byte) Integer.parseInt(hexArray[i], 16);
            }

            return key;
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
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
}
