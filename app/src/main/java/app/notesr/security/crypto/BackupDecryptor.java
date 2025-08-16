package app.notesr.security.crypto;

import static app.notesr.util.KeyUtils.getSecretKeyFromSecrets;

import android.content.ContentResolver;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;

import javax.crypto.SecretKey;

import app.notesr.exception.DecryptionFailedException;
import app.notesr.security.dto.CryptoSecrets;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BackupDecryptor {
    private static final String TAG = BackupDecryptor.class.getName();

    private final ContentResolver contentResolver;
    private final CryptoSecrets cryptoSecrets;
    private final Uri inputUri;
    private final File outputFile;

    public void decrypt() throws DecryptionFailedException, IOException {
        try {
            InputStream sourceStream = contentResolver.openInputStream(inputUri);
            FileOutputStream outputStream = new FileOutputStream(outputFile);

            tryGcmDecryption(sourceStream, outputStream);
            return;
        } catch (GeneralSecurityException e) {
            Log.e(TAG, "GCM decryption failed", e);
        }

        try {
            InputStream sourceStream = contentResolver.openInputStream(inputUri);
            FileOutputStream outputStream = new FileOutputStream(outputFile);

            tryCbcDecryption(sourceStream, outputStream);
        } catch (GeneralSecurityException e) {
            Log.e(TAG, "CBC decryption failed", e);
            throw new DecryptionFailedException(e);
        }
    }

    void tryGcmDecryption(InputStream sourceStream, FileOutputStream outputStream)
            throws GeneralSecurityException, IOException {
        SecretKey key = getSecretKeyFromSecrets(cryptoSecrets);
        AesGcmCryptor cryptor = new AesGcmCryptor(key);
        cryptor.decrypt(sourceStream, outputStream);
    }

    void tryCbcDecryption(InputStream sourceStream, FileOutputStream outputStream)
            throws GeneralSecurityException, IOException {
        SecretKey key = getSecretKeyFromSecrets(cryptoSecrets);
        byte[] iv = getIv(cryptoSecrets);

        AesCbcCryptor cryptor = new AesCbcCryptor(key, iv);
        cryptor.decrypt(sourceStream, outputStream);
    }

    byte[] getIv(CryptoSecrets cryptoSecrets) {
        int ivSize = cryptoSecrets.getKey().length - (AesCryptor.KEY_SIZE / 8);
        byte[] iv = new byte[ivSize];

        System.arraycopy(cryptoSecrets.getKey(), AesCryptor.KEY_SIZE / 8, iv, 0, ivSize);
        return iv;
    }
}
