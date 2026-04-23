/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.service.lifecycle;

import android.content.Context;
import android.content.Intent;

import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.service.AndroidServiceStarter;

public final class AppCloseAndroidServiceStarter implements AndroidServiceStarter {

    @Override
    public void start(Context context) {
        context.startForegroundService(new Intent(context, AppCloseAndroidService.class));
    }

    @Override
    public void start(Context context, CryptoSecrets secrets, String payload) {
        throw new UnsupportedOperationException();
    }
}
