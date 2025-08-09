package app.notesr.migration.service;

public interface AppMigration {
    int getFromVersion();

    int getToVersion();

    void migrate();
}
