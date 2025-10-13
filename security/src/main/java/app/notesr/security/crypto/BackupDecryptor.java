package app.notesr.security.crypto;

import static app.notesr.security.util.KeyUtils.getIvFromSecrets;
import static app.notesr.security.util.KeyUtils.getSecretKeyFromSecrets;

import android.content.ContentResolver;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;

import javax.crypto.SecretKey;

import app.notesr.security.dto.CryptoSecrets;
import app.notesr.security.exception.DecryptionFailedException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class BackupDecryptor {
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
        byte[] iv = getIvFromSecrets(cryptoSecrets);

        AesCbcCryptor cryptor = new AesCbcCryptor(key, iv);
        cryptor.decrypt(sourceStream, outputStream);
    }
}
