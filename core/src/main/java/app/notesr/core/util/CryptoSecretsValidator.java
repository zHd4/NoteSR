/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.core.util;

import static app.notesr.core.security.dto.CryptoSecrets.MASTER_KEY_SIZE;
import static app.notesr.core.security.dto.CryptoSecrets.PASSWORD_MIN_LENGTH;

import app.notesr.core.security.dto.CryptoSecrets;

public final class CryptoSecretsValidator {
    /**
     * Validates the cryptographic secrets.
     * <p>
     * Combines validation checks for the key and password.
     * If any validation fails, an {@link IllegalArgumentException} is thrown.
     *
     * @param secrets the cryptographic secrets to validate
     * @throws IllegalArgumentException if any validation check fails
     * @see #validateKey(byte[])
     * @see #validatePassword(char[])
     */
    public static void validate(CryptoSecrets secrets) {
        validateKey(secrets.getKey());
        validatePassword(secrets.getPassword());
    }

    /**
     * Validates the master key.
     * <p>
     * Ensures that the key is not null or empty,
     * that it matches the expected 384-bit (48 bytes) length,
     * and that it is not nulled.
     *
     * @param key the master key to validate
     * @throws IllegalArgumentException if the key is invalid
     */
    public static void validateKey(byte[] key) {
        if (key == null || key.length == 0) {
            throw new IllegalArgumentException("CryptoSecrets key cannot be null or empty");
        }

        if (key.length != MASTER_KEY_SIZE) {
            throw new IllegalArgumentException("Key must be "
                    + MASTER_KEY_SIZE + " bytes long");
        }

        if (KeyUtils.isKeyNulled(key)) {
            throw new IllegalArgumentException("Key cannot be empty");
        }
    }

    /**
     * Validates the password.
     * <p>
     * Ensures that the password is not null or empty,
     * and that it is at least 4 characters long,
     * and that it contains at least one non-zero character.
     *
     * @param password the password to validate
     * @throws IllegalArgumentException if the password is invalid
     */
    public static void validatePassword(char[] password) {
        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("CryptoSecrets password cannot be null or empty");
        }

        if (password.length < PASSWORD_MIN_LENGTH) {
            throw new IllegalArgumentException("Password must be at least "
                    + PASSWORD_MIN_LENGTH + " characters long");
        }

        if (!CharUtils.hasNonZeroChars(password)) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
    }
}
