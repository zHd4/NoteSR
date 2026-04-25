/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.core.security.crypto;

import java.security.GeneralSecurityException;
import java.util.Base64;

import app.notesr.core.security.exception.EncryptionFailedException;
import lombok.RequiredArgsConstructor;

/**
 * Component responsible for encrypting values using AES.
 * <p>
 * This class acts as a high-level wrapper around {@link AesCryptor},
 * providing convenient methods for encrypting both byte arrays and strings.
 */
@RequiredArgsConstructor
public final class ValueEncryptor {

    private final AesCryptor cryptor;

    /**
     * Encrypts the provided byte array.
     *
     * @param plainValue the raw data to be encrypted
     * @return the encrypted byte array
     * @throws EncryptionFailedException if the encryption process fails due to security configuration issues
     */
    public byte[] encrypt(byte[] plainValue) throws EncryptionFailedException {
        try {
            return cryptor.encrypt(plainValue);
        } catch (GeneralSecurityException e) {
            throw new EncryptionFailedException(e);
        }
    }

    /**
     * Encrypts the provided string and returns the result as a Base64-encoded string.
     *
     * @param plainValue the string to be encrypted
     * @return a Base64-encoded representation of the encrypted data
     * @throws EncryptionFailedException if the encryption process fails
     */
    public String encrypt(String plainValue) throws EncryptionFailedException {
        byte[] encryptedValue = encrypt(plainValue.getBytes());
        return Base64.getEncoder().encodeToString(encryptedValue);
    }
}
