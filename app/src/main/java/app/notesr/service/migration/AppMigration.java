package app.notesr.service.migration;

public interface AppMigration {
    int getFromVersion();

    int getToVersion();

    void migrate();
}
