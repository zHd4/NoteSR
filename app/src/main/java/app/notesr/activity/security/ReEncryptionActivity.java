/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */
 
package app.notesr.activity.security;

import static app.notesr.core.util.ActivityUtils.disableBackButton;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import app.notesr.activity.App;
import app.notesr.R;
import app.notesr.activity.ActivityBase;
import app.notesr.activity.note.NotesListActivity;
import app.notesr.service.security.SecretsUpdateAndroidService;

public final class ReEncryptionActivity extends ActivityBase {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_re_encryption);

        disableBackButton(this);

        ReEncryptionBroadcastReceiver broadcastReceiver =
                new ReEncryptionBroadcastReceiver(this::onReEncryptionComplete);

        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                new IntentFilter(SecretsUpdateAndroidService.BROADCAST_ACTION));

        startReEncryptionService();
    }

    private void startReEncryptionService() {
        if (!App.getContext().isServiceRunning(SecretsUpdateAndroidService.class)) {
            Intent serviceIntent = new Intent(getApplicationContext(),
                    SecretsUpdateAndroidService.class);

            startForegroundService(serviceIntent);
        }
    }

    private void onReEncryptionComplete() {
        startActivity(new Intent(getApplicationContext(), NotesListActivity.class));
        finish();
    }
}
