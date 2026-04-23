/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.service.security;

import android.content.Context;
import android.content.Intent;

import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.service.AndroidServiceStarter;

public final class SecretsUpdateAndroidServiceStarter implements AndroidServiceStarter {

    @Override
    public void start(Context context) {
        context.startForegroundService(buildIntent(context));
    }

    @Override
    public void start(Context context, CryptoSecrets secrets, String payload) {
        throw new UnsupportedOperationException();
    }

    private Intent buildIntent(Context context) {
        return new Intent(context, SecretsUpdateAndroidService.class);
    }
}
