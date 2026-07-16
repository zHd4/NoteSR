/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.core.security.dto;

import java.util.Arrays;

import app.notesr.core.util.CharUtils;
import app.notesr.core.util.KeyUtils;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Data transfer object containing cryptographic secrets.
 * <p>
 * This class holds a byte array key and a character array password,
 * providing a method to securely wipe the sensitive information from memory.
 */
@AllArgsConstructor
@Data
public final class CryptoSecrets {

    public static final int MASTER_KEY_SIZE = 48;
    public static final int PASSWORD_MIN_LENGTH = 4;

    /**
     * The cryptographic 384-bit master key (48 bytes).
     */
    private byte[] key;

    /**
     * The password used for cryptographic operations.
     */
    private char[] password;

    /**
     * Securely clears the secrets by filling the underlying arrays with zeros.
     */
    public void destroy() {
        Arrays.fill(key, (byte) 0);
        Arrays.fill(password, '\0');
    }

    /**
     * Validates the integrity of the cryptographic secrets.
     * <p>
     * Ensures that both the key and password arrays are not null or empty,
     * and that the key matches the expected 384-bit (48 bytes) length requirement
     * and that the password is at least 4 characters long.
     *
     * @throws IllegalArgumentException if any validation check fails
     */
    public void validate() {
        if (key == null || key.length == 0) {
            throw new IllegalStateException("CryptoSecrets key cannot be null or empty");
        }

        if (password == null || password.length == 0) {
            throw new IllegalStateException("CryptoSecrets password cannot be null or empty");
        }

        if (key.length != MASTER_KEY_SIZE) {
            throw new IllegalStateException("Key must be "
                    + MASTER_KEY_SIZE + " bytes long");
        }

        if (password.length < PASSWORD_MIN_LENGTH) {
            throw new IllegalStateException("Password must be at least "
                    + PASSWORD_MIN_LENGTH + " characters long");
        }

        if (KeyUtils.isKeyNulled(key)) {
            throw new IllegalStateException("Key cannot be empty");
        }

        if (!CharUtils.hasNonZeroChars(password)) {
            throw new IllegalStateException("Password cannot be empty");
        }
    }

    /**
     * Creates a deep copy of the provided {@link CryptoSecrets} instance.
     *
     * @param secrets the secrets to copy, may be {@code null}
     * @return a new {@link CryptoSecrets} instance with copied arrays,
     * or {@code null} if the input was {@code null}
     */
    public static CryptoSecrets from(CryptoSecrets secrets) {
        if (secrets == null) {
            return null;
        }

        return new CryptoSecrets(Arrays.copyOf(secrets.key, secrets.key.length),
                Arrays.copyOf(secrets.password, secrets.password.length));
    }
}
