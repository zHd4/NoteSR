/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.service.migration.changes.security.crypto;

import android.content.Context;

import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.service.migration.AppMigration;
import app.notesr.service.migration.AppMigrationException;
import app.notesr.service.security.AppSecurityException;
import app.notesr.service.security.AppSecurityService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CbcToGcmKeyMigration implements AppMigration {

    @Getter
    private final int fromVersion;

    @Getter
    private final int toVersion;

    @Override
    public void migrate(Context context) {
        try {
            AppSecurityService appSecurityService = getAppSecurityService(context);
            CryptoSecrets secrets = appSecurityService.getActualSecrets();

            appSecurityService.setSecrets(secrets);
            secrets.destroy();
        } catch (AppSecurityException | IllegalArgumentException e) {
            throw new AppMigrationException("Failed to migrate key", e);
        }
    }

    AppSecurityService getAppSecurityService(Context context) {
        return new AppSecurityService(context);
    }
}
