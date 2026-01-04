/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */
 
package app.notesr.activity.security;

import static app.notesr.core.util.CharUtils.charsToBytes;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import app.notesr.BuildConfig;
import app.notesr.activity.migration.MigrationActivity;
import app.notesr.activity.note.NotesListActivity;
import app.notesr.core.security.SecretCache;
import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.service.security.SecretsSetupService;
import app.notesr.service.migration.DataVersionManager;
import app.notesr.service.security.SecretsUpdateAndroidService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class KeySetupCompletionHandler {
    private final Activity activity;
    private final SecretsSetupService secretsSetupService;
    private final KeySetupMode mode;

    public void handle() {
        switch (mode) {
            case FIRST_RUN -> proceedFirstRun();
            case REGENERATION -> proceedRegeneration();
            default -> throw new RuntimeException("Unknown mode: " + mode);
        }
    }

    private void proceedFirstRun() {
        try {
            secretsSetupService.apply();

            Context context = activity.getApplicationContext();
            Intent nextIntent = new Intent(context, NotesListActivity.class);

            DataVersionManager dataVersionManager = new DataVersionManager(context);

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
        CryptoSecrets secrets = secretsSetupService.getCryptoSecrets();

        try {
            byte[] keyBytes = Arrays.copyOf(secrets.getKey(), secrets.getKey().length);
            char[] password = Arrays.copyOf(secrets.getPassword(), secrets.getPassword().length);
            byte[] passwordBytes = charsToBytes(password, StandardCharsets.UTF_8);

            SecretCache.put(SecretsUpdateAndroidService.NEW_KEY, keyBytes);
            SecretCache.put(SecretsUpdateAndroidService.PASSWORD, passwordBytes);
        } catch (CharacterCodingException e) {
            throw new RuntimeException(e);
        }

        secrets.destroy();

        Context context = activity.getApplicationContext();
        activity.startActivity(new Intent(context, ReEncryptionActivity.class));
        activity.finish();
    }
}
