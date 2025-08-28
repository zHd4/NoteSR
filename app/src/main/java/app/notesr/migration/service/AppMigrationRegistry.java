package app.notesr.migration.service;

import java.util.List;

import app.notesr.migration.changes.db.RoomIntegrationMigration;
import app.notesr.migration.changes.security.crypto.CbcToGcmKeyMigration;
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
