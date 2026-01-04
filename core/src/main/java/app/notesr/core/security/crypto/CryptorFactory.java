/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.core.security.crypto;

public interface CryptorFactory {
    AesCryptor create(char[] password, Class<? extends AesCryptor> cryptorClass) throws Exception;
}
