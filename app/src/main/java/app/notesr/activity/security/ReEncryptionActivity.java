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
import app.notesr.service.security.crypto.update.SecretsUpdateAndroidService;
import app.notesr.service.security.crypto.update.SecretsUpdateAndroidServiceStarter;

public final class ReEncryptionActivity extends ActivityBase {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_re_encryption);
        applyInsets(findViewById(R.id.main));
        disableBackButton(this);

        ReEncryptionBroadcastReceiver broadcastReceiver =
                new ReEncryptionBroadcastReceiver(this::onReEncryptionComplete,
                        this::onReEncryptionFailed);

        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver,
                new IntentFilter(SecretsUpdateAndroidService.BROADCAST_ACTION));

        startReEncryptionService();
    }

    @Override
    protected boolean requiresSession() {
        return false;
    }

    private void startReEncryptionService() {
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

    private void onReEncryptionComplete() {
        startActivity(new Intent(getApplicationContext(), NotesListActivity.class));
        finish();
    }

    private void onReEncryptionFailed() {
        DialogFactory dialogFactory = new DialogFactory(this);
        dialogFactory.getThemedAlertDialogBuilder(R.layout.dialog_re_encryption_failed)
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
