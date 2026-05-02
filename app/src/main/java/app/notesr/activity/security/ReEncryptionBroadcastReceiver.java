/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.security;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import app.notesr.service.security.crypto.update.SecretsUpdateAndroidService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class ReEncryptionBroadcastReceiver extends BroadcastReceiver {
    private final Runnable onReEncryptionComplete;
    private final Runnable onReEncryptionFailed;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (SecretsUpdateAndroidService.BROADCAST_ACTION.equals(intent.getAction())) {
            boolean isCompleted = intent.getBooleanExtra(SecretsUpdateAndroidService.EXTRA_COMPLETE,
                    false);

            boolean isFailed = intent.getBooleanExtra(SecretsUpdateAndroidService.EXTRA_FAIL,
                    false);

            if (isCompleted) {
                onReEncryptionComplete.run();
            } else if (isFailed) {
                onReEncryptionFailed.run();
            }
        }
    }
}
