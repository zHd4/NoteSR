package com.git.notesr;

import android.util.Base64;
import com.git.notesr.encryption.provider.CryptoProvider;
import com.git.notesr.encryption.provider.KeyGenerator;
import com.git.notesr.encryption.provider.exceptions.CryptoKeyException;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;

public class Crypto {
    private static final int KEY_SIZE = 128;
    private static final int IV_SIZE = 32;

    public static byte[] genKey() {
        try {
            KeyGenerator keyGenerator = new KeyGenerator(KEY_SIZE);
            String password = Base64.encodeToString(keyGenerator.createKey(), Base64.DEFAULT);
            return AES.genKey(password, ActivityTools.sha256(password));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return new byte[0];
    }

    public static byte[] genKey(String passphrase) {
        try {
            KeyGenerator keyGenerator = new KeyGenerator(KEY_SIZE);
            String password = Base64.encodeToString(keyGenerator.createKey(passphrase), Base64.DEFAULT);
            return AES.genKey(password, ActivityTools.sha256(password));
        } catch (NoSuchAlgorithmException | CryptoKeyException e) {
            e.printStackTrace();
        }

        return new byte[0];
    }

    public static String encrypt(String text, String salt, byte[] key) throws Exception {
        try {
            CryptoProvider cryptoProvider = new CryptoProvider(key);
            cryptoProvider.initializeVector(salt, IV_SIZE);

            text = AES.encrypt(text, key);
            return Base64.encodeToString(cryptoProvider.encrypt(text.getBytes()), Base64.DEFAULT);
        } catch (Exception e) {
            throw e;
        }
    }

    public static String decrypt(String encrypted, String salt, byte[] key) throws Exception {
        try {
            CryptoProvider cryptoProvider = new CryptoProvider(key);
            cryptoProvider.initializeVector(salt, IV_SIZE);

            encrypted = new String(
                    cryptoProvider.decrypt(Base64.decode(encrypted, Base64.DEFAULT)),
                    StandardCharsets.UTF_8
            );

            return AES.decrypt(encrypted, key);
        } catch (Exception e) {
            throw e;
        }
    }
}
