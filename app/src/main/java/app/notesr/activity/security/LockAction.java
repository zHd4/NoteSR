/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.security;

import android.content.Context;
import android.content.Intent;

import app.notesr.activity.ActivityBase;
import app.notesr.core.security.SecretCache;
import app.notesr.core.security.crypto.CryptoManager;
import app.notesr.data.DatabaseProvider;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class LockAction {

    private final ActivityBase activity;
    private final CryptoManager cryptoManager;

    public void lock() {
        Context context = activity.getApplicationContext();
        Intent authActivityIntent = new Intent(context, AuthActivity.class)
                .putExtra(AuthActivity.EXTRA_MODE, AuthActivity.Mode.AUTHORIZATION.toString());

        clearSecrets();

        activity.startActivity(authActivityIntent);
        activity.finish();
    }

    private void clearSecrets() {
        DatabaseProvider.close();
        cryptoManager.destroySecrets();
        SecretCache.clear();
    }
}
