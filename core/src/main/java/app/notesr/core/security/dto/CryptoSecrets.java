/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */
 
package app.notesr.core.security.dto;

import java.util.Arrays;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public final class CryptoSecrets {

    private byte[] key;
    private char[] password;

    public void destroy() {
        Arrays.fill(key, (byte) 0);
        Arrays.fill(password, '\0');
    }

    public static CryptoSecrets from(CryptoSecrets secrets) {
        return new CryptoSecrets(Arrays.copyOf(secrets.key, secrets.key.length),
                Arrays.copyOf(secrets.password, secrets.password.length));
    }
}
