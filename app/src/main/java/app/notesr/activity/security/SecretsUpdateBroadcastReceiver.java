/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.security;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import app.notesr.service.security.rotation.SecretsUpdateAndroidService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class SecretsUpdateBroadcastReceiver extends BroadcastReceiver {
    private final Runnable onSecretsUpdateComplete;
    private final Runnable onSecretsUpdateFailed;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (SecretsUpdateAndroidService.BROADCAST_ACTION.equals(intent.getAction())) {
            boolean isCompleted = intent.getBooleanExtra(SecretsUpdateAndroidService.EXTRA_COMPLETE,
                    false);

            boolean isFailed = intent.getBooleanExtra(SecretsUpdateAndroidService.EXTRA_FAIL,
                    false);

            if (isCompleted) {
                onSecretsUpdateComplete.run();
            } else if (isFailed) {
                onSecretsUpdateFailed.run();
            } else {
                throw new IllegalStateException("Unexpected intent received: " + intent);
            }
        }
    }
}
