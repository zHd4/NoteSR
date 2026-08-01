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

import java.nio.charset.CharacterCodingException;

import app.notesr.R;
import app.notesr.activity.ActivityBase;
import app.notesr.activity.DialogFactory;
import app.notesr.activity.note.list.NotesListActivity;
import app.notesr.service.AndroidServiceRegistry;
import app.notesr.service.security.rotation.SecretsUpdateAndroidService;
import app.notesr.service.security.rotation.SecretsRotationAndroidServiceStarter;

public final class SecretsRotationActivity extends ActivityBase {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_secrets_rotation);
        applyInsets(findViewById(R.id.main));
        disableBackButton(this);

        SecretsRotationBroadcastReceiver broadcastReceiver =
                new SecretsRotationBroadcastReceiver(this::onSecretsRotationComplete,
                        this::onSecretsRotationFailed);

        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                new IntentFilter(SecretsUpdateAndroidService.BROADCAST_ACTION));

        startSecretsRotationService();
    }

    @Override
    protected boolean requiresSession() {
        return false;
    }

    private void startSecretsRotationService() {
        AndroidServiceRegistry serviceRegistry = AndroidServiceRegistry
                .getInstance(getApplicationContext());

        if (!serviceRegistry.isServiceRunning(SecretsUpdateAndroidService.class)) {
            try {
                new SecretsRotationAndroidServiceStarter().start(getApplicationContext());
            } catch (CharacterCodingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void onSecretsRotationComplete() {
        startActivity(new Intent(getApplicationContext(), NotesListActivity.class));
        finish();
    }

    private void onSecretsRotationFailed() {
        DialogFactory dialogFactory = new DialogFactory(this);
        dialogFactory.getThemedAlertDialogBuilder(R.layout.dialog_secrets_rotation_failed)
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
