package com.peew.notesr.crypto;

import android.content.Context;
import android.util.Log;

import com.peew.notesr.App;
import com.peew.notesr.tools.FileManager;
import com.peew.notesr.tools.HashHelper;

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
    private static final String MAIN_KEY_FILENAME = "key.enc";
    private static final String MAIN_SALT_FILENAME = "iv.enc";
    private CryptoKey cryptoKeyInstance;

    private CryptoManager() {}

    public boolean configure(String password) {
        try {
            Context context = App.getContext();

            File encryptedKeyFile = new File(context.getFilesDir(), MAIN_KEY_FILENAME);
            File encryptedSaltFile = new File(context.getFilesDir(), MAIN_SALT_FILENAME);

            byte[] encryptedKey = FileManager.readFileBytes(encryptedKeyFile);
            byte[] encryptedSalt = FileManager.readFileBytes(encryptedSaltFile);

            byte[] secondarySalt = HashHelper.toSha256Bytes(password.getBytes());
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
        Context context = App.getContext();
        File encryptedKeyFile = new File(context.getFilesDir(), MAIN_KEY_FILENAME);
        File encryptedSaltFile = new File(context.getFilesDir(), MAIN_SALT_FILENAME);

        return !encryptedKeyFile.exists() || !encryptedSaltFile.exists();
    }

    public static CryptoManager getInstance() {
        return INSTANCE;
    }

    public CryptoKey getCryptoKeyInstance() {
        return cryptoKeyInstance;
    }

    public CryptoKey generateNewKey(String password) throws NoSuchAlgorithmException {
        SecretKey mainKey = Aes.generateRandomKey();
        byte[] mainSalt = Aes.generateRandomSalt();

        return new CryptoKey(mainKey, mainSalt, password);
    }

    public void applyNewKey(CryptoKey newKey) throws
            NoSuchAlgorithmException, InvalidKeySpecException,
            InvalidAlgorithmParameterException, NoSuchPaddingException,
            IllegalBlockSizeException, BadPaddingException,
            InvalidKeyException, IOException {
        String password = newKey.getPassword();
        SecretKey mainKey = newKey.getKey();

        byte[] mainSalt = newKey.getSalt();
        byte[] secondarySalt = HashHelper.toSha256Bytes(password.getBytes());

        cryptoKeyInstance = newKey;
        Context context = App.getContext();

        Aes aesInstance = new Aes(password, secondarySalt);

        File encryptedKeyFile = new File(context.getFilesDir(), MAIN_KEY_FILENAME);
        File encryptedSaltFile = new File(context.getFilesDir(), MAIN_SALT_FILENAME);

        FileManager.writeFileBytes(encryptedKeyFile, aesInstance.encrypt(mainKey.getEncoded()));
        FileManager.writeFileBytes(encryptedSaltFile, aesInstance.encrypt(mainSalt));
    }
}
