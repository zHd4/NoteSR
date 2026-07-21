/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.service.security.crypto.setup;

import android.content.Context;

import app.notesr.core.security.crypto.CryptoManager;
import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.core.security.exception.EncryptionFailedException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public final class SecretsSetupService {
    private final Context context;
    private final CryptoManager cryptoManager;

    public CryptoSecrets getSecretsWithRandomKey(char[] password) {
        return cryptoManager.generateSecrets(password);
    }

    public void applySecrets(CryptoSecrets cryptoSecrets) throws EncryptionFailedException {
        cryptoManager.setSecrets(context, cryptoSecrets);
        cryptoSecrets.destroy();
    }
}
