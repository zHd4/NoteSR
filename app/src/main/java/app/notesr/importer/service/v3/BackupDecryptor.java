package app.notesr.importer.service.v3;

import java.security.GeneralSecurityException;

import app.notesr.exception.DecryptionFailedException;
import app.notesr.security.crypto.AesCryptor;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BackupDecryptor {
    private final AesCryptor cryptor;

    public String decryptJsonObject(byte[] encrypted) throws DecryptionFailedException {
        try {
            return new String(cryptor.decrypt(encrypted));
        } catch (GeneralSecurityException e) {
            throw new DecryptionFailedException(e);
        }
    }
}
