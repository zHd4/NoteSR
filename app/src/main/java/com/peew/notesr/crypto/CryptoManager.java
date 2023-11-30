package com.peew.notesr.crypto;

import android.content.Context;

import com.peew.notesr.State;
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

public class CryptoManager {
    private static final String MAIN_KEY_FILENAME = "key.enc";
    private static final String MAIN_SALT_FILENAME = "iv.enc";
    private static final CryptoKey CRYPTO_KEY_INSTANCE = State.getInstance().getCryptoKeyInstance();

    private Context context;

    public CryptoManager(Context context) {
        this.context = context;
    }

    public void createNewKey(String password) throws
            NoSuchAlgorithmException, InvalidKeySpecException,
            InvalidAlgorithmParameterException, NoSuchPaddingException,
            IllegalBlockSizeException, BadPaddingException,
            InvalidKeyException, IOException {
        SecretKey mainKey = Aes256.generateRandomKey();
        byte[] mainSalt = Aes256.generateRandomSalt();

        byte[] secondarySalt = HashHelper.toSha256Bytes(password.getBytes());
        Aes256 aesInstance = new Aes256(password, secondarySalt);

        CRYPTO_KEY_INSTANCE.setPassword(password);
        CRYPTO_KEY_INSTANCE.setKey(mainKey);

        File encryptedKeyFile = new File(context.getFilesDir(), MAIN_KEY_FILENAME);
        File encryptedSaltFile = new File(context.getFilesDir(), MAIN_SALT_FILENAME);

        FileManager.writeFileBytes(encryptedKeyFile, aesInstance.encrypt(mainKey.getEncoded()));
        FileManager.writeFileBytes(encryptedSaltFile, aesInstance.encrypt(mainSalt));
    }
}
