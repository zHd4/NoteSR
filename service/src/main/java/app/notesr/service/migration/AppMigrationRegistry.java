/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */
 
package app.notesr.service.migration;

import java.util.List;

import app.notesr.service.migration.changes.db.RoomIntegrationMigration;
import app.notesr.service.migration.changes.security.crypto.CbcToGcmKeyMigration;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AppMigrationRegistry {
    public static List<AppMigration> getAllMigrations() {
        return List.of(
                new CbcToGcmKeyMigration(1, 2),
                new RoomIntegrationMigration(1, 2)
        );
    }
}
