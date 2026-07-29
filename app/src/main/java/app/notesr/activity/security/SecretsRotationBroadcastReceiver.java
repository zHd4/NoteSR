/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.security;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import app.notesr.service.security.rotation.SecretsRotationAndroidService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class SecretsRotationBroadcastReceiver extends BroadcastReceiver {
    private final Runnable onSecretsRotationComplete;
    private final Runnable onSecretsRotationFailed;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (SecretsRotationAndroidService.BROADCAST_ACTION.equals(intent.getAction())) {
            boolean isCompleted = intent.getBooleanExtra(SecretsRotationAndroidService.EXTRA_COMPLETE,
                    false);

            boolean isFailed = intent.getBooleanExtra(SecretsRotationAndroidService.EXTRA_FAIL,
                    false);

            if (isCompleted) {
                onSecretsRotationComplete.run();
            } else if (isFailed) {
                onSecretsRotationFailed.run();
            }
        }
    }
}
