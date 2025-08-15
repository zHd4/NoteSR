package app.notesr.security.crypto;

import static app.notesr.util.KeyUtils.getSecretKeyFromSecrets;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;

import app.notesr.exception.EncryptionFailedException;
import app.notesr.security.dto.CryptoSecrets;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BackupEncryptor {
    private final CryptoSecrets cryptoSecrets;
    private final FileInputStream sourceFileStream;
    private final FileOutputStream outputFileStream;

    public void encrypt() throws EncryptionFailedException, IOException {
        AesGcmCryptor cryptor = new AesGcmCryptor(getSecretKeyFromSecrets(cryptoSecrets));

        try {
            cryptor.encrypt(sourceFileStream, outputFileStream);
        } catch (GeneralSecurityException e) {
            throw new EncryptionFailedException(e);
        }
    }
}
