/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.security;

import static app.notesr.util.ActivityUtils.disableBackButton;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.nio.charset.CharacterCodingException;

import app.notesr.R;
import app.notesr.activity.ActivityBase;
import app.notesr.activity.DialogFactory;
import app.notesr.activity.note.list.NotesListActivity;
import app.notesr.service.AndroidServiceRegistry;
import app.notesr.service.security.rotation.SecretsUpdateAndroidService;
import app.notesr.service.security.rotation.SecretsUpdateAndroidServiceStarter;

public final class SecretsUpdateActivity extends ActivityBase {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_secrets_update);
        applyInsets(findViewById(R.id.main));
        disableBackButton(this);

        SecretsUpdateBroadcastReceiver broadcastReceiver =
                new SecretsUpdateBroadcastReceiver(this::onSecretsUpdateComplete,
                        this::onSecretsUpdateFailed);

        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                new IntentFilter(SecretsUpdateAndroidService.BROADCAST_ACTION));

        startSecretsUpdateService();
    }

    private void startSecretsUpdateService() {
        AndroidServiceRegistry serviceRegistry = AndroidServiceRegistry
                .getInstance(getApplicationContext());

        if (!serviceRegistry.isServiceRunning(SecretsUpdateAndroidService.class)) {
            try {
                new SecretsUpdateAndroidServiceStarter().start(getApplicationContext());
            } catch (CharacterCodingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void onSecretsUpdateComplete() {
        startActivity(new Intent(getApplicationContext(), NotesListActivity.class));
        finish();
    }

    private void onSecretsUpdateFailed() {
        if (isFinishing() || isDestroyed()) {
            startActivity(new Intent(getApplicationContext(), NotesListActivity.class));
            finish();
            return;
        }

        DialogFactory dialogFactory = new DialogFactory(this);
        dialogFactory.getThemedAlertDialogBuilder(R.layout.dialog_secrets_update_failed)
                .setTitle(R.string.error)
                .setCancelable(false)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    startActivity(new Intent(getApplicationContext(), NotesListActivity.class));
                    finish();
                })
                .create()
                .show();
    }
}
