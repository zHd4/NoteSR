package app.notesr.service.migration;

import java.util.List;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AppMigrationService {
    private final List<AppMigration> migrations;

    public void run(int oldVersion, int newVersion) throws AppMigrationException {
        for (AppMigration migration : migrations) {
            if (migration.getFromVersion() >= oldVersion
                    && migration.getToVersion() <= newVersion) {
                migration.migrate();
            }
        }
    }
}
