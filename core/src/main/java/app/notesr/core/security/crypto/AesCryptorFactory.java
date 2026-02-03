/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.core.security.crypto;

import java.lang.reflect.InvocationTargetException;
import java.security.NoSuchAlgorithmException;

public final class AesCryptorFactory implements CryptorFactory {
    @Override
    public AesCryptor create(char[] password, Class<? extends AesCryptor> cryptorClass)
            throws NoSuchAlgorithmException {
        byte[] salt = AesCryptor.generatePasswordBasedSalt(password);

        try {
            return cryptorClass.getConstructor(char[].class, byte[].class)
                    .newInstance(password, salt);
        } catch (IllegalAccessException
                 | InstantiationException
                 | InvocationTargetException
                 | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
}
