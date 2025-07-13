package app.notesr.service.migration;

import java.util.List;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AppMigrationRegistry {
    public static List<AppMigration> getAllMigrations() {
        return List.of();
    }
}
