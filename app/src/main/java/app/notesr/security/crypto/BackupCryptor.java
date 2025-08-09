package app.notesr.security.crypto;

import android.util.Log;

import app.notesr.security.dto.CryptoSecrets;
import app.notesr.exception.DecryptionFailedException;
import app.notesr.exception.EncryptionFailedException;
import lombok.RequiredArgsConstructor;

import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@RequiredArgsConstructor
public class BackupCryptor {
    private static final String TAG = BackupCryptor.class.getName();
    private static final int CHUNK_SIZE = 100000;

    private final FileInputStream sourceFileStream;
    private final FileOutputStream outputFileStream;
    private final CryptoSecrets cryptoSecrets;

    public void encrypt() throws EncryptionFailedException, IOException {
        AesGcmCryptor cryptor = new AesGcmCryptor(getKey(cryptoSecrets));

        try {
            CipherOutputStream outputCipherStream = cryptor.encrypt(outputFileStream);

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
        } catch (NoSuchAlgorithmException | NoSuchPaddingException |
                 InvalidAlgorithmParameterException | InvalidKeyException e) {
            throw new EncryptionFailedException(e);
        }
    }

    public void decrypt() throws DecryptionFailedException, IOException {
        try {
            AesGcmCryptor cryptor = new AesGcmCryptor(getKey(cryptoSecrets));
            decrypt(cryptor.decrypt(sourceFileStream));
            return;
        } catch (InvalidAlgorithmParameterException | NoSuchPaddingException |
                 NoSuchAlgorithmException | InvalidKeyException e) {
            Log.e(TAG, "GCM decryption failed", e);
        }

        try {
            AesCbcCryptor cryptor = new AesCbcCryptor(getKey(cryptoSecrets), getIv(cryptoSecrets));
            decrypt(cryptor.decrypt(sourceFileStream));
        } catch (InvalidAlgorithmParameterException | NoSuchPaddingException |
                 NoSuchAlgorithmException | InvalidKeyException e) {
            Log.e(TAG, "CBC decryption failed", e);
            throw new DecryptionFailedException(e);
        }
    }

    private void decrypt(CipherInputStream cipherInputStream) throws IOException {
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

    private SecretKey getKey(CryptoSecrets cryptoSecrets) {
        byte[] keyBytes = new byte[AesCryptor.KEY_SIZE / 8];
        System.arraycopy(cryptoSecrets.getKey(), 0, keyBytes, 0, keyBytes.length);

        return new SecretKeySpec(keyBytes, AesCryptor.KEY_GENERATOR_ALGORITHM);
    }

    private byte[] getIv(CryptoSecrets cryptoSecrets) {
        int ivSize = cryptoSecrets.getKey().length - (AesCryptor.KEY_SIZE / 8);
        byte[] iv = new byte[ivSize];

        System.arraycopy(cryptoSecrets.getKey(), AesCryptor.KEY_SIZE / 8, iv, 0, ivSize);
        return iv;
    }
}
