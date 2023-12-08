package com.peew.notesr.crypto;

import android.util.Log;

import com.peew.notesr.tools.FileManager;

import java.io.File;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class CryptoManager {
    private static final CryptoManager INSTANCE = new CryptoManager();
    private static final String MAIN_KEY_FILENAME = "key.encrypted";
    private static final String MAIN_SALT_FILENAME = "iv.encrypted";
    private static final String BLOCKED_FILENAME = "blocked";
    private CryptoKey cryptoKeyInstance;

    private CryptoManager() {}

    public boolean configure(String password) {
        try {
            byte[] encryptedKey = FileManager.readFileBytes(getEncryptedKeyFile());
            byte[] encryptedSalt = FileManager.readFileBytes(getEncryptedSaltFile());

            byte[] secondarySalt = Aes.generatePasswordBasedSalt(password);
            Aes aesInstance = new Aes(password, secondarySalt);

            byte[] mainKeyBytes = aesInstance.decrypt(encryptedKey);
            byte[] mainSaltBytes = aesInstance.decrypt(encryptedSalt);

            SecretKey mainKey = new SecretKeySpec(mainKeyBytes, 0, mainSaltBytes.length,
                    Aes.KEY_GENERATOR_ALGORITHM);
            cryptoKeyInstance = new CryptoKey(mainKey, mainSaltBytes, password);

            return true;
        } catch (Exception e) {
            Log.e("CryptoManager configuration error", e.toString());
            return false;
        }
    }

    public boolean ready() {
        return cryptoKeyInstance != null;
    }

    public boolean isFirstRun() {
        return (!getEncryptedKeyFile().exists() || !getEncryptedSaltFile().exists()) &&
                !getBlockFile().exists();
    }

    public static CryptoManager getInstance() {
        return INSTANCE;
    }

    public CryptoKey getCryptoKeyInstance() {
        return cryptoKeyInstance;
    }

    private File getEncryptedKeyFile() {
        return FileManager.getInternalFile(MAIN_KEY_FILENAME);
    }

    private File getEncryptedSaltFile() {
        return FileManager.getInternalFile(MAIN_SALT_FILENAME);
    }

    private File getBlockFile() {
        return FileManager.getInternalFile(BLOCKED_FILENAME);
    }

    public CryptoKey generateNewKey(String password) throws NoSuchAlgorithmException {
        SecretKey mainKey = Aes.generateRandomKey();
        byte[] mainSalt = Aes.generateRandomSalt();

        return new CryptoKey(mainKey, mainSalt, password);
    }

    public CryptoKey createCryptoKey(byte[] keyBytes, byte[] salt, String password) {
        SecretKey newKey = new SecretKeySpec(keyBytes, 0, keyBytes.length,
                Aes.KEY_GENERATOR_ALGORITHM);
        return new CryptoKey(newKey, salt, password);
    }

    public void applyNewKey(CryptoKey newKey) throws
            NoSuchAlgorithmException, InvalidKeySpecException,
            InvalidAlgorithmParameterException, NoSuchPaddingException,
            IllegalBlockSizeException, BadPaddingException,
            InvalidKeyException, IOException {
        String password = newKey.getPassword();
        SecretKey mainKey = newKey.getKey();

        byte[] mainSalt = newKey.getSalt();
        byte[] secondarySalt = Aes.generatePasswordBasedSalt(password);

        cryptoKeyInstance = newKey;
        Aes aesInstance = new Aes(password, secondarySalt);

        File encryptedKeyFile = getEncryptedKeyFile();
        File encryptedSaltFile = getEncryptedSaltFile();

        FileManager.writeFileBytes(encryptedKeyFile, aesInstance.encrypt(mainKey.getEncoded()));
        FileManager.writeFileBytes(encryptedSaltFile, aesInstance.encrypt(mainSalt));
    }

    public boolean isBlocked() {
        return getBlockFile().exists() &&
                !getEncryptedKeyFile().exists() &&
                !getEncryptedSaltFile().exists();
    }

    public void block() {
        try {
            FileManager.wipeFile(getEncryptedKeyFile());
            FileManager.wipeFile(getEncryptedSaltFile());

            FileManager.writeFileBytes(getBlockFile(), new byte[0]);
        } catch (IOException e) {
            Log.e("CryptoManager.block error", e.toString());
        }
    }
}
