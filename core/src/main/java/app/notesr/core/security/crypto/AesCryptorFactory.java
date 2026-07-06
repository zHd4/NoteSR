/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.core.security.crypto;

import static app.notesr.core.util.KeyUtils.getIvFromSecrets;
import static app.notesr.core.util.KeyUtils.getSecretKeyFromSecrets;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKey;

import app.notesr.core.security.dto.CryptoSecrets;

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

    public static AesGcmCryptor createAesGcmCryptor(CryptoSecrets secrets) {
        return new AesGcmCryptor(getSecretKeyFromSecrets(secrets));
    }

    public static AesCbcCryptor createAesCbcCryptor(CryptoSecrets secrets) {
        SecretKey key = getSecretKeyFromSecrets(secrets);
        byte[] iv = getIvFromSecrets(secrets);

        return new AesCbcCryptor(key, iv);
    }
}
