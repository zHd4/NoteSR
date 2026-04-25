/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.core.security.crypto;

import java.security.GeneralSecurityException;
import java.util.Base64;

import app.notesr.core.security.exception.DecryptionFailedException;
import lombok.RequiredArgsConstructor;

/**
 * High-level utility for decrypting data using {@link AesCryptor}.
 * <p>
 * This class provides convenient methods to decrypt both byte arrays and
 * Base64-encoded strings, handling cryptographic exceptions by wrapping
 * them into {@link DecryptionFailedException}.
 */
@RequiredArgsConstructor
public final class ValueDecryptor {
    private final AesCryptor cryptor;

    /**
     * Decrypts a byte array.
     *
     * @param encryptedValue the encrypted data as a byte array
     * @return the decrypted data as a byte array
     * @throws DecryptionFailedException if an error occurs during the decryption process
     */
    public byte[] decrypt(byte[] encryptedValue) throws DecryptionFailedException {
        try {
            return cryptor.decrypt(encryptedValue);
        } catch (GeneralSecurityException e) {
            throw new DecryptionFailedException(e);
        }
    }

    /**
     * Decrypts a Base64-encoded string.
     *
     * @param encryptedValue the Base64-encoded encrypted string
     * @return the decrypted data as a clear-text string
     * @throws DecryptionFailedException if the input is not valid Base64 or decryption fails
     */
    public String decrypt(String encryptedValue) throws DecryptionFailedException {
        try {
            byte[] decryptedValue = decrypt(Base64.getDecoder().decode(encryptedValue));
            return new String(decryptedValue);
        } catch (IllegalArgumentException e) {
            throw new DecryptionFailedException(e);
        }
    }
}
