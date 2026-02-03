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
import app.notesr.core.security.crypto.CryptoManagerProvider;

import java.util.function.Consumer;

public final class LockOnClick implements Consumer<ActivityBase> {
    @Override
    public void accept(ActivityBase activity) {
        Context context = activity.getApplicationContext();
        Intent authActivityIntent = new Intent(context, AuthActivity.class)
                .putExtra(AuthActivity.EXTRA_MODE, AuthActivity.Mode.AUTHORIZATION.toString());

        CryptoManager cryptoManager = CryptoManagerProvider.getInstance(context);
        cryptoManager.destroySecrets();

        SecretCache.clear();

        activity.startActivity(authActivityIntent);
        activity.finish();
    }
}
