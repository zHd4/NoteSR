package com.notesr.controllers;

import android.util.Base64;
import com.notesr.encryption.provider.CryptoProvider;
import com.notesr.encryption.provider.KeyGenerator;
import com.notesr.encryption.provider.exceptions.CryptoKeyException;
import java.security.NoSuchAlgorithmException;

public class CryptoController {
    private static final int KEY_SIZE = 128;
    private static final int IV_SIZE = 32;

    public static byte[] genKey() {
        try {
            KeyGenerator keyGenerator = new KeyGenerator(KEY_SIZE);
            String password = Base64.encodeToString(keyGenerator.createKey(), Base64.DEFAULT);
            return AESController.genKey(password, ActivityTools.sha256(password));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return new byte[0];
    }

    public static byte[] genKey(String passphrase) {
        try {
            KeyGenerator keyGenerator = new KeyGenerator(KEY_SIZE);
            String password = Base64.encodeToString(keyGenerator.createKey(passphrase), Base64.DEFAULT);
            return AESController.genKey(password, ActivityTools.sha256(password));
        } catch (NoSuchAlgorithmException | CryptoKeyException e) {
            e.printStackTrace();
        }

        return new byte[0];
    }

    public static byte[] encrypt(byte[] data, String salt, byte[] key) throws Exception {
        try {
            KeyGenerator keyGenerator = new KeyGenerator(KEY_SIZE);
            CryptoProvider cryptoProvider = new CryptoProvider(keyGenerator.createKey(ActivityTools.sha256(
                    Base64.encodeToString(key, Base64.DEFAULT)
            )));

            cryptoProvider.initializeVector(salt, IV_SIZE);
            byte[] aesEncrypted = AESController.encrypt(data, key);

            return cryptoProvider.encrypt(aesEncrypted);
        } catch (Exception e) {
            throw e;
        }
    }

    public static byte[] decrypt(byte[] encrypted, String salt, byte[] key) throws Exception {
        try {
            KeyGenerator keyGenerator = new KeyGenerator(KEY_SIZE);
            CryptoProvider cryptoProvider = new CryptoProvider(keyGenerator.createKey(ActivityTools.sha256(
                    Base64.encodeToString(key, Base64.DEFAULT)
            )));

            cryptoProvider.initializeVector(salt, IV_SIZE);

            return AESController.decrypt(cryptoProvider.decrypt(encrypted), key);
        } catch (Exception e) {
            throw e;
        }
    }
}
