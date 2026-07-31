/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.core.security.dto;

import java.util.Arrays;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Data transfer object containing cryptographic secrets.
 * <p>
 * This class holds a byte array key and a character array password,
 * providing a method to securely wipe the sensitive information from memory.
 */
@AllArgsConstructor
@Data
@EqualsAndHashCode
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
        if (key != null) {
            Arrays.fill(key, (byte) 0);
        }

        if (password != null) {
            Arrays.fill(password, '\0');
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

        byte[] key = secrets.key != null
                ? Arrays.copyOf(secrets.key, secrets.key.length)
                : null;

        char[] password = secrets.password != null
                ? Arrays.copyOf(secrets.password, secrets.password.length)
                : null;

        return new CryptoSecrets(key, password);
    }
}
