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
    private static final String ENCRYPTED_KEY_FILENAME = "key.encrypted";
    private static final String BLOCKED_FILENAME = ".blocked";
    private static final int KEY_BYTES_COUNT = Aes.KEY_SIZE / 8;
    private CryptoKey cryptoKeyInstance;

    private CryptoManager() {}

    public boolean configure(String password) {
        try {
            byte[] secondarySalt = Aes.generatePasswordBasedSalt(password);
            Aes aesInstance = new Aes(password, secondarySalt);

            byte[] encryptedKeyFileBytes = FileManager.readFileBytes(getEncryptedKeyFile());
            byte[] keyFileBytes = aesInstance.decrypt(encryptedKeyFileBytes);

            byte[] mainKeyBytes = new byte[KEY_BYTES_COUNT];
            byte[] mainSaltBytes = new byte[Aes.SALT_SIZE];

            System.arraycopy(keyFileBytes, 0, mainKeyBytes, 0,KEY_BYTES_COUNT);
            System.arraycopy(keyFileBytes, KEY_BYTES_COUNT, mainSaltBytes, 0,Aes.SALT_SIZE);

            SecretKey mainKey = new SecretKeySpec(
                    mainKeyBytes,
                    0,
                    mainSaltBytes.length,
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
        return !getEncryptedKeyFile().exists() && !getBlockFile().exists();
    }

    public static CryptoManager getInstance() {
        return INSTANCE;
    }

    public CryptoKey getCryptoKeyInstance() {
        return cryptoKeyInstance;
    }

    private File getEncryptedKeyFile() {
        return FileManager.getInternalFile(ENCRYPTED_KEY_FILENAME);
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

        byte[] mainKey = newKey.getKey().getEncoded();
        byte[] mainSalt = newKey.getSalt();

        byte[] secondarySalt = Aes.generatePasswordBasedSalt(password);
        byte[] keyFileData = new byte[KEY_BYTES_COUNT + Aes.SALT_SIZE];

        System.arraycopy(mainKey, 0, keyFileData, 0, mainKey.length);
        System.arraycopy(mainSalt, 0, keyFileData, mainKey.length, mainSalt.length);

        Aes aesInstance = new Aes(password, secondarySalt);
        FileManager.writeFileBytes(getEncryptedKeyFile(), aesInstance.encrypt(keyFileData));

        cryptoKeyInstance = newKey;
        File blockFile = getBlockFile();

        if (blockFile.exists()) {
            //noinspection ResultOfMethodCallIgnored
            blockFile.delete();
        }
    }

    public boolean isBlocked() {
        return getBlockFile().exists() && !getEncryptedKeyFile().exists();
    }

    public void block() {
        try {
            FileManager.wipeFile(getEncryptedKeyFile());
            FileManager.writeFileBytes(getBlockFile(), new byte[0]);
        } catch (IOException e) {
            Log.e("CryptoManager.block error", e.toString());
        }
    }
}