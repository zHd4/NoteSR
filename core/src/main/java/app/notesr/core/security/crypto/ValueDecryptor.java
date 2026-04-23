/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.core.security.crypto;

import java.security.GeneralSecurityException;
import java.util.Base64;

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

    public String decrypt(String encryptedValue) throws DecryptionFailedException {
        byte[] decryptedValue = decrypt(Base64.getDecoder().decode(encryptedValue));
        return new String(decryptedValue);
    }
}
