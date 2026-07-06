/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.core.security.crypto;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

public final class AesCryptorFactory {
    public AesCryptor createAesCryptor(char[] password, Class<? extends AesCryptor> cryptorClass)
            throws InvalidKeySpecException, NoSuchAlgorithmException {
        byte[] salt = AesCryptor.generatePasswordBasedSalt(password);

        if (cryptorClass == AesGcmCryptor.class) {
            return new AesGcmCryptor(password, salt);
        } else if (cryptorClass == AesCbcCryptor.class) {
            return new AesCbcCryptor(password, salt);
        } else {
            throw new IllegalArgumentException("Unsupported cryptor class: " + cryptorClass);
        }
    }
}
