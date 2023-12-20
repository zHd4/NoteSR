package com.peew.notesr.crypto;

import android.util.Log;

import com.peew.notesr.App;
import com.peew.notesr.tools.FileManager;

import java.io.File;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class CryptoManager {
    private static final CryptoManager INSTANCE = new CryptoManager();
    private static final String ENCRYPTED_KEY_FILENAME = "key.encrypted";
    private static final String ENCRYPTED_TEST_FILENAME = "test.encrypted";
    private static final String BLOCKED_FILENAME = ".blocked";
    private static final int ENCRYPTED_TEST_FILE_SIZE = 1024;
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
                    mainKeyBytes.length,
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

    private File getEncryptedTestFile() {
        return FileManager.getInternalFile(ENCRYPTED_TEST_FILENAME);
    }

    public CryptoKey generateNewKey(String password) throws NoSuchAlgorithmException {
        SecretKey mainKey = Aes.generateRandomKey();
        byte[] mainSalt = Aes.generateRandomSalt();

        return new CryptoKey(mainKey, mainSalt, password);
    }

    public CryptoKey createCryptoKey(byte[] keyBytes, byte[] salt, String password) throws
            Exception {
        SecretKey newKey = new SecretKeySpec(keyBytes, 0, keyBytes.length,
                Aes.KEY_GENERATOR_ALGORITHM);
        if (verifyImportedKey(newKey, salt)) {
            return new CryptoKey(newKey, salt, password);
        }

        throw new Exception("Wrong key");
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

        createEncryptedTestFile(newKey.getKey(), mainSalt);
    }

    private void createEncryptedTestFile(SecretKey key, byte[] salt) throws
            InvalidAlgorithmParameterException, NoSuchPaddingException,
            IllegalBlockSizeException, NoSuchAlgorithmException,
            BadPaddingException, InvalidKeyException, IOException {
        byte[] randomBytes = new byte[ENCRYPTED_TEST_FILE_SIZE];

        Random random = new Random();
        Aes aesInstance = new Aes(key, salt);

        random.nextBytes(randomBytes);
        randomBytes = aesInstance.encrypt(randomBytes);

        FileManager.writeFileBytes(getEncryptedTestFile(), randomBytes);
    }

    private boolean verifyImportedKey(SecretKey key, byte[] salt) {
        if (App.onAndroid()) {
            try {
                Aes aesInstance = new Aes(key, salt);
                aesInstance.decrypt(FileManager.readFileBytes(getBlockFile()));
            } catch (Exception e) {
                return false;
            }
        }

        return true;
    }

    public boolean isBlocked() {
        return getBlockFile().exists() && !getEncryptedKeyFile().exists();
    }

    public void block() {
        try {
            File encryptedTestFile = getEncryptedTestFile();
            byte[] testFileData = FileManager.readFileBytes(encryptedTestFile);

            FileManager.wipeFile(getEncryptedKeyFile());
            FileManager.writeFileBytes(getBlockFile(), testFileData);

            FileManager.wipeFile(encryptedTestFile);
        } catch (IOException e) {
            Log.e("CryptoManager.block error", e.toString());
        }
    }

    public void destroyKey() {
        cryptoKeyInstance = null;
    }
}
