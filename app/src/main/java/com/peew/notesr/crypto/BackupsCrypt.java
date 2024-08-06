package com.peew.notesr.crypto;

import com.peew.notesr.App;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class BackupsCrypt {

    private static final int CHUNK_SIZE = 100000;

    private final FileInputStream sourceFileStream;
    private final FileOutputStream outputFileStream;
    private final CryptoKey cryptoKey;

    public BackupsCrypt(FileInputStream sourceFileStream, FileOutputStream outputFileStream) {
        this.sourceFileStream = sourceFileStream;
        this.outputFileStream = outputFileStream;
        this.cryptoKey = getCryptoKey();
    }

    public void encrypt() throws IOException {
        transform(Cipher.ENCRYPT_MODE);
    }

    public void decrypt() throws IOException {
        transform(Cipher.DECRYPT_MODE);
    }

    private void transform(int mode) throws IOException {
        Cipher cipher = getCipher(mode);

        CipherInputStream sourceCipherStream = new CipherInputStream(sourceFileStream, cipher);
        CipherOutputStream outputCipherStream = new CipherOutputStream(outputFileStream, cipher);

        try (sourceCipherStream) {
            try (outputCipherStream) {
                byte[] chunk = new byte[CHUNK_SIZE];
                int bytesRead = sourceFileStream.read(chunk);

                while (bytesRead != -1) {
                    if (bytesRead != CHUNK_SIZE) {
                        byte[] subChunk = new byte[bytesRead];
                        System.arraycopy(chunk, 0, subChunk, 0, bytesRead);

                        chunk = subChunk;
                    }

                    outputFileStream.write(chunk);

                    chunk = new byte[CHUNK_SIZE];
                    bytesRead = sourceFileStream.read(chunk);
                }
            }
        }
    }

    private Cipher getCipher(int mode) {
        try {
            return Aes.createCipher(cryptoKey.key(), cryptoKey.salt(), mode);
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidAlgorithmParameterException |
                 InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    private CryptoKey getCryptoKey() {
        return App.getAppContainer()
                .getCryptoManager()
                .getCryptoKeyInstance();
    }
}
