package app.notesr.service.importer.v3;

import java.security.GeneralSecurityException;

import app.notesr.core.security.exception.DecryptionFailedException;
import app.notesr.core.security.crypto.AesCryptor;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class BackupDecryptor {
    private final AesCryptor cryptor;

    public byte[] decrypt(byte[] encrypted) throws DecryptionFailedException {
        try {
            return cryptor.decrypt(encrypted);
        } catch (GeneralSecurityException e) {
            throw new DecryptionFailedException(e);
        }
    }

    public String decryptJsonObject(byte[] encrypted) throws DecryptionFailedException {
        try {
            return new String(cryptor.decrypt(encrypted));
        } catch (GeneralSecurityException e) {
            throw new DecryptionFailedException(e);
        }
    }
}
