package com.peew.notesr.crypto;

import com.peew.notesr.App;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class BackupsCrypt {

    private static final int CHUNK_SIZE = 100000;
    private static final int ENCRYPTION_MODE = 0;
    private static final int DECRYPTION_MODE = 1;

    private final FileInputStream sourceStream;
    private final FileOutputStream outputStream;
    private final CryptoKey cryptoKey;

    public BackupsCrypt(FileInputStream sourceStream, FileOutputStream outputStream) {
        this.sourceStream = sourceStream;
        this.outputStream = outputStream;
        this.cryptoKey = getCryptoKey();
    }

    public void encrypt() throws IOException {
        transform(ENCRYPTION_MODE);
    }

    public void decrypt() throws IOException {
        transform(DECRYPTION_MODE);
    }

    private void transform(int mode) throws IOException {
        try (sourceStream) {
            try (outputStream) {
                byte[] chunk = new byte[CHUNK_SIZE];
                int bytesRead = sourceStream.read(chunk);

                while (bytesRead != -1) {
                    if (bytesRead != CHUNK_SIZE) {
                        byte[] subChunk = new byte[bytesRead];
                        System.arraycopy(chunk, 0, subChunk, 0, bytesRead);

                        chunk = subChunk;
                    }

                    if (mode == ENCRYPTION_MODE) {
                        chunk = encryptData(chunk);
                    } else if (mode == DECRYPTION_MODE) {
                        chunk = decryptData(chunk);
                    } else {
                        throw new IllegalArgumentException("Invalid mode " + mode);
                    }

                    outputStream.write(chunk);

                    chunk = new byte[CHUNK_SIZE];
                    bytesRead = sourceStream.read(chunk);
                }
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

    private CryptoKey getCryptoKey() {
        return App.getAppContainer()
                .getCryptoManager()
                .getCryptoKeyInstance();
    }
}
