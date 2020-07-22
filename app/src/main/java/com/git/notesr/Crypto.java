package com.git.notesr;

import android.util.Base64;
import com.git.notesr.encryption.provider.CryptoProvider;
import com.git.notesr.encryption.provider.KeyGenerator;
import com.git.notesr.encryption.provider.exceptions.CryptoKeyException;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;

public class Crypto {
    public static int OLD_KEY_SIZE = 128;
    public static int NEW_KEY_SIZE = 2048;

    public static byte[] genKey(int keySize) {
        try {
            KeyGenerator keyGenerator = new KeyGenerator(keySize);
            return keyGenerator.createKey();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return new byte[0];
    }

    public static byte[] genKey(String passphrase, int keySize) {
        try {
            KeyGenerator keyGenerator = new KeyGenerator(keySize);
            return keyGenerator.createKey(passphrase);
        } catch (NoSuchAlgorithmException | CryptoKeyException e) {
            e.printStackTrace();
        }

        return new byte[0];
    }

    public static String encrypt(String text, String salt, byte[] key) throws Exception {
        try {
            CryptoProvider cryptoProvider = new CryptoProvider(key);

            cryptoProvider.initializeVector(salt);
            return Base64.encodeToString(cryptoProvider.encrypt(text.getBytes()), Base64.DEFAULT);
        } catch (Exception e) {
            throw e;
        }
    }

    public static String decrypt(String encrypted, String salt, byte[] key) throws Exception {
        try {
            CryptoProvider cryptoProvider = new CryptoProvider(key);

            cryptoProvider.initializeVector(salt);
            return new String(
                    cryptoProvider.decrypt(Base64.decode(encrypted, Base64.DEFAULT)),
                    StandardCharsets.UTF_8
            );
        } catch (Exception e) {
            throw e;
        }
    }
}
