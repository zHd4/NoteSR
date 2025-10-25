package app.notesr.data.migration;

import androidx.room.migration.Migration;

import java.util.List;

import app.notesr.data.migration.changes.DropTempFilesTableMigration;

public final class MigrationRegistry {

    public static List<Migration> getAllMigrations() {
        return List.of(
                new DropTempFilesTableMigration(1, 2)
        );
    }
}
