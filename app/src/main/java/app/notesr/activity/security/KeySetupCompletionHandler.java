/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.security;

import static app.notesr.core.util.CharUtils.bytesToChars;
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
import app.notesr.service.security.rotation.SecretsUpdateAndroidService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class KeySetupCompletionHandler {
    private final ActivityBase activity;
    private final AppSecurityService appSecurityService;
    private final KeySetupMode mode;
    private final byte[] keyBytes;

    public void handle() {
        switch (mode) {
            case FIRST_RUN -> proceedFirstRun();
            case REGENERATION -> proceedRegeneration();
            default -> throw new RuntimeException("Unknown mode: " + mode);
        }
    }

    private void proceedFirstRun() {
        try {
            char[] password = getCurrentPassword();

            CryptoSecrets newSecrets = new CryptoSecrets(keyBytes, password);
            appSecurityService.setSecrets(newSecrets);

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
                .getThemedAlertDialogBuilder(R.layout.dialog_secrets_rotation_warning)
                .setTitle(R.string.warning)
                .setPositiveButton(R.string.yes,
                        (dialog, which) -> onRegenerationConfirmed())
                .setNegativeButton(R.string.no,
                        (dialog, which) -> onRegenerationCanceled())
                .create()
                .show();
    }

    private void onRegenerationConfirmed() {
        try {
            char[] password = getCurrentPassword();
            byte[] passwordBytes = charsToBytes(password, StandardCharsets.UTF_8);

            SecretCache.put(SecretsUpdateAndroidService.NEW_KEY, keyBytes);
            SecretCache.put(SecretsUpdateAndroidService.PASSWORD, passwordBytes);
        } catch (CharacterCodingException e) {
            throw new RuntimeException(e);
        }

        Intent secretsRotationIntent = new Intent(activity.getApplicationContext(),
                SecretsRotationActivity.class);

        activity.startActivity(secretsRotationIntent);
        activity.finish();
    }

    private void onRegenerationCanceled() {
        if (keyBytes != null) {
            Arrays.fill(keyBytes, (byte) 0);
        }
    }

    private char[] getCurrentPassword() throws CharacterCodingException {
        if (appSecurityService.isAuthConfigured()) {
            CryptoSecrets cryptoSecrets = appSecurityService.getActualSecrets();

            char[] password = cryptoSecrets.getPassword();
            char[] passwordCopy = Arrays.copyOf(password, password.length);

            cryptoSecrets.destroy();
            return passwordCopy;
        } else {
            if (SecretCache.contains(SetupKeyActivity.CACHE_KEY_PASSWORD)) {
                return bytesToChars(SecretCache.take(SetupKeyActivity.CACHE_KEY_PASSWORD),
                        StandardCharsets.UTF_8);
            }
        }

        throw new IllegalStateException("App authentication is not configured" +
                " and password is not found in cache");
    }
}
