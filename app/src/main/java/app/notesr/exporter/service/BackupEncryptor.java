package app.notesr.exporter.service;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;

import app.notesr.core.security.crypto.AesCryptor;
import app.notesr.core.security.exception.EncryptionFailedException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class BackupEncryptor {
    private final AesCryptor cryptor;

    public byte[] encrypt(String json) throws EncryptionFailedException, IOException {
        try {
            return cryptor.encrypt(json.getBytes());
        } catch (GeneralSecurityException e) {
            throw new EncryptionFailedException(e);
        }
    }

    public byte[] encrypt(byte[] data) throws EncryptionFailedException, IOException {
        try {
            return cryptor.encrypt(data);
        } catch (GeneralSecurityException e) {
            throw new EncryptionFailedException(e);
        }
    }

    public void encrypt(FileInputStream inputStream, FileOutputStream outputStream)
            throws EncryptionFailedException, IOException {
        try {
            cryptor.encrypt(inputStream, outputStream);
        } catch (GeneralSecurityException e) {
            throw new EncryptionFailedException(e);
        }
    }
}
