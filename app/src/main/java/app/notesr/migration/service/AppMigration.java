package app.notesr.migration.service;

import android.content.Context;

public interface AppMigration {
    int getFromVersion();

    int getToVersion();

    void migrate(Context context);
}
