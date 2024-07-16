package com.peew.notesr.crypto;

import com.peew.notesr.App;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class BackupsCrypt {

    private final File source;
    private final File output;
    private final CryptoKey cryptoKey;

    public BackupsCrypt(File source, File output) {
        this.source = source;
        this.output = output;
        this.cryptoKey = getCryptoKey();
    }

    public void encrypt() throws IOException {
        try (FileInputStream sourceStream = new FileInputStream(source)) {
            try (FileOutputStream outputStream = new FileOutputStream(output)) {

            }
        }
    }

    private byte[] encryptData(byte[] data) {
        try {
            Aes aes = new Aes(cryptoKey.key(), cryptoKey.salt());
            return aes.encrypt(data);
        } catch (NoSuchPaddingException | InvalidAlgorithmParameterException | NoSuchAlgorithmException |
                 InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] decryptData(byte[] data) {
        try {
            Aes aes = new Aes(cryptoKey.key(), cryptoKey.salt());
            return aes.decrypt(data);
        } catch (NoSuchPaddingException | InvalidAlgorithmParameterException | NoSuchAlgorithmException |
                 InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    private long getAvailableMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory());
    }

    private CryptoKey getCryptoKey() {
        return App.getAppContainer()
                .getCryptoManager()
                .getCryptoKeyInstance();
    }
}
