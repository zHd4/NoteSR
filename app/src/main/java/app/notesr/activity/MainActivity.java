/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */
 
package app.notesr.activity;

import android.content.Intent;
import android.os.Bundle;

import java.util.List;
import java.util.function.Supplier;

import app.notesr.R;
import app.notesr.core.security.crypto.CryptoManager;
import app.notesr.core.security.crypto.CryptoManagerProvider;
import app.notesr.activity.exporter.ExportActivity;
import app.notesr.activity.importer.ImportActivity;
import app.notesr.activity.migration.MigrationActivity;
import app.notesr.service.lifecycle.AppCloseAndroidService;
import app.notesr.service.migration.AppMigrationAndroidService;
import app.notesr.service.exporter.ExportAndroidService;
import app.notesr.service.importer.ImportAndroidService;
import app.notesr.activity.note.NotesListActivity;
import app.notesr.activity.security.AuthActivity;
import app.notesr.activity.security.KeyRecoveryActivity;
import app.notesr.activity.security.ReEncryptionActivity;
import app.notesr.service.security.SecretsUpdateAndroidService;

public final class MainActivity extends ActivityBase {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CryptoManager cryptoManager = CryptoManagerProvider.getInstance(getApplicationContext());
        List<Supplier<Intent>> intentSuppliers = getIntentSuppliers(App.getContext(),
                cryptoManager);
        Intent defaultIntent = new Intent(getApplicationContext(), NotesListActivity.class);

        startAppCloseService();
        startActivity(new StartupIntentResolver(intentSuppliers, defaultIntent).resolve());
        finish();
    }

    private List<Supplier<Intent>> getIntentSuppliers(App context, CryptoManager cryptoManager) {
        return List.of(
                () -> cryptoManager.isBlocked(getApplicationContext())
                        ? new Intent(context, KeyRecoveryActivity.class)
                        : null,
                () -> !cryptoManager.isKeyExists(getApplicationContext())
                        ? new Intent(context, StartActivity.class)
                        : null,
                () -> context.isServiceRunning(AppMigrationAndroidService.class)
                        ? new Intent(context, MigrationActivity.class)
                        : null,

                () -> context.isServiceRunning(ExportAndroidService.class)
                        ? new Intent(context, ExportActivity.class)
                        : null,

                () -> context.isServiceRunning(ImportAndroidService.class)
                        ? new Intent(context, ImportActivity.class)
                        : null,

                () -> context.isServiceRunning(SecretsUpdateAndroidService.class)
                        ? new Intent(context, ReEncryptionActivity.class)
                        : null,

                () -> {
                    if (!cryptoManager.isConfigured()) {
                        Intent intent = new Intent(context, AuthActivity.class);
                        intent.putExtra("mode", AuthActivity.Mode.AUTHORIZATION.toString());

                        return intent;
                    }

                    return null;
                }
        );
    }

    private void startAppCloseService() {
        if (!App.getContext().isServiceRunning(AppCloseAndroidService.class)) {
            Intent serviceIntent = new Intent(getApplicationContext(),
                    AppCloseAndroidService.class);

            startForegroundService(serviceIntent);
        }
    }
}
