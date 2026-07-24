/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.security;

import static app.notesr.core.util.CharUtils.charsToBytes;

import android.content.Context;
import android.content.Intent;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import app.notesr.BuildConfig;
import app.notesr.R;
import app.notesr.activity.ActivityBase;
import app.notesr.activity.DialogFactory;
import app.notesr.activity.migration.MigrationActivity;
import app.notesr.activity.note.list.NotesListActivity;
import app.notesr.core.security.SecretCache;
import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.service.security.AppSecurityService;
import app.notesr.service.migration.DataVersionManager;
import app.notesr.service.security.crypto.update.SecretsUpdateAndroidService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class KeySetupCompletionHandler {
    private final ActivityBase activity;
    private final AppSecurityService appSecurityService;
    private final KeySetupMode mode;
    private final CryptoSecrets cryptoSecrets;

    public void handle() {
        switch (mode) {
            case FIRST_RUN -> proceedFirstRun();
            case REGENERATION -> proceedRegeneration();
            default -> throw new RuntimeException("Unknown mode: " + mode);
        }
    }

    private void proceedFirstRun() {
        try {
            appSecurityService.setSecrets(cryptoSecrets);

            Context context = activity.getApplicationContext();
            Intent nextIntent = new Intent(context, NotesListActivity.class);

            var dataVersionManager = new DataVersionManager(context);

            int lastMigrationVersion = dataVersionManager.getCurrentVersion();
            int currentDataSchemaVersion = BuildConfig.DATA_SCHEMA_VERSION;

            if (lastMigrationVersion == DataVersionManager.DEFAULT_FIRST_VERSION) {
                dataVersionManager.setCurrentVersion(currentDataSchemaVersion);
            } else if (lastMigrationVersion < currentDataSchemaVersion) {
                nextIntent = new Intent(context, MigrationActivity.class);
            }

            activity.startActivity(nextIntent);
            activity.finish();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void proceedRegeneration() {
        new DialogFactory(activity)
                .getThemedAlertDialogBuilder(R.layout.dialog_re_encryption_warning)
                .setTitle(R.string.warning)
                .setPositiveButton(R.string.yes,
                        (dialog, which) -> onRegenerationConfirmed())
                .setNegativeButton(R.string.no, null)
                .create()
                .show();
    }

    private void onRegenerationConfirmed() {
        byte[] keyBytes = Arrays.copyOf(cryptoSecrets.getKey(),
                cryptoSecrets.getKey().length);

        char[] password = Arrays.copyOf(cryptoSecrets.getPassword(),
                cryptoSecrets.getPassword().length);

        try {
            byte[] passwordBytes = charsToBytes(password, StandardCharsets.UTF_8);

            SecretCache.put(SecretsUpdateAndroidService.NEW_KEY, keyBytes);
            SecretCache.put(SecretsUpdateAndroidService.PASSWORD, passwordBytes);
        } catch (CharacterCodingException e) {
            throw new RuntimeException(e);
        }

        cryptoSecrets.destroy();

        Intent reEncryptionIntent = new Intent(activity.getApplicationContext(),
                ReEncryptionActivity.class);
        activity.startActivity(reEncryptionIntent);
        activity.finish();
    }
}
