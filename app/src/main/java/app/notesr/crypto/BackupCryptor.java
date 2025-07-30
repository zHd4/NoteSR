package app.notesr.crypto;

import app.notesr.App;
import app.notesr.dto.CryptoSecrets;

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

public class BackupCryptor {

    private static final int CHUNK_SIZE = 100000;

    private final FileInputStream sourceFileStream;
    private final FileOutputStream outputFileStream;
    private final CryptoSecrets cryptoKey;

    public BackupCryptor(FileInputStream sourceFileStream, FileOutputStream outputFileStream) {
        this.sourceFileStream = sourceFileStream;
        this.outputFileStream = outputFileStream;
        this.cryptoKey = getCryptoKey();
    }

    public void encrypt() throws IOException {
        Cipher cipher = getCipher(Cipher.ENCRYPT_MODE);
        CipherOutputStream outputCipherStream = new CipherOutputStream(outputFileStream, cipher);

        try (sourceFileStream; outputCipherStream) {
            byte[] chunk = new byte[CHUNK_SIZE];
            int bytesRead = sourceFileStream.read(chunk);

            while (bytesRead != -1) {
                if (bytesRead != CHUNK_SIZE) {
                    byte[] subChunk = new byte[bytesRead];
                    System.arraycopy(chunk, 0, subChunk, 0, bytesRead);

                    chunk = subChunk;
                }

                outputCipherStream.write(chunk);

                chunk = new byte[CHUNK_SIZE];
                bytesRead = sourceFileStream.read(chunk);
            }

            outputCipherStream.flush();
        }
    }

    public void decrypt() throws IOException {
        Cipher cipher = getCipher(Cipher.DECRYPT_MODE);
        CipherInputStream cipherInputStream = new CipherInputStream(sourceFileStream, cipher);

        try (cipherInputStream; outputFileStream) {
            byte[] chunk = new byte[CHUNK_SIZE];
            int bytesRead = cipherInputStream.read(chunk);

            while (bytesRead != -1) {
                if (bytesRead != CHUNK_SIZE) {
                    byte[] subChunk = new byte[bytesRead];
                    System.arraycopy(chunk, 0, subChunk, 0, bytesRead);

                    chunk = subChunk;
                }

                outputFileStream.write(chunk);

                chunk = new byte[CHUNK_SIZE];
                bytesRead = cipherInputStream.read(chunk);
            }
        }
    }

    private Cipher getCipher(int mode) {
        try {
            return AesCryptor.createCipher(cryptoKey.getKey(), cryptoKey.getSalt(), mode);
        } catch (NoSuchPaddingException
                 | NoSuchAlgorithmException
                 | InvalidAlgorithmParameterException
                 | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    private CryptoSecrets getCryptoKey() {
        return App.getAppContainer()
                .getCryptoManager()
                .getCryptoKeyInstance();
    }
}
