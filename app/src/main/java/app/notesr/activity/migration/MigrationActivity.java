/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.migration;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import app.notesr.BuildConfig;
import app.notesr.R;
import app.notesr.activity.ActivityBase;
import app.notesr.activity.note.NotesListActivity;
import app.notesr.service.AndroidServiceRegistry;
import app.notesr.service.migration.AppMigrationAndroidService;
import app.notesr.service.migration.AppMigrationAndroidServiceStarter;
import app.notesr.service.migration.DataVersionManager;

public final class MigrationActivity extends ActivityBase {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_migration);
        applyInsets(findViewById(R.id.main));

        MigrationBroadcastReceiver receiver = new MigrationBroadcastReceiver(
                this::onMigrationComplete);

        LocalBroadcastManager.getInstance(getApplicationContext())
                .registerReceiver(receiver,
                        new IntentFilter(AppMigrationAndroidService.BROADCAST_ACTION));

        startMigrationService();
    }

    @Override
    protected boolean requiresSession() {
        return false;
    }

    private void startMigrationService() {
        AndroidServiceRegistry serviceRegistry = AndroidServiceRegistry
                .getInstance(getApplicationContext());

        if (!serviceRegistry.isServiceRunning(AppMigrationAndroidService.class)) {
            new AppMigrationAndroidServiceStarter(
                    new AppMigrationAndroidServiceStarter.Payload(BuildConfig.DATA_SCHEMA_VERSION))
                    .start(getApplicationContext());
        }
    }

    private void onMigrationComplete() {
        DataVersionManager dataVersionManager = new DataVersionManager(getApplicationContext());
        dataVersionManager.setCurrentVersion(BuildConfig.DATA_SCHEMA_VERSION);

        startActivity(new Intent(getApplicationContext(), NotesListActivity.class));
        finish();
    }
}
