/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.core.security.crypto;

import java.security.GeneralSecurityException;
import java.util.Base64;

import app.notesr.core.security.exception.EncryptionFailedException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class ValueEncryptor {

    private final AesCryptor cryptor;

    public byte[] encrypt(byte[] plainValue) throws EncryptionFailedException {
        try {
            return cryptor.encrypt(plainValue);
        } catch (GeneralSecurityException e) {
            throw new EncryptionFailedException(e);
        }
    }

    public String encrypt(String plainValue) throws EncryptionFailedException {
        byte[] encryptedValue = encrypt(plainValue.getBytes());
        return Base64.getEncoder().encodeToString(encryptedValue);
    }
}
