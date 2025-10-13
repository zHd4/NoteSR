package app.notesr.core.security.crypto;

import java.security.GeneralSecurityException;

import app.notesr.core.security.exception.DecryptionFailedException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class ValueDecryptor {
    private final AesCryptor cryptor;

    public byte[] decrypt(byte[] encryptedValue) throws DecryptionFailedException {
        try {
            return cryptor.decrypt(encryptedValue);
        } catch (GeneralSecurityException e) {
            throw new DecryptionFailedException(e);
        }
    }
}
