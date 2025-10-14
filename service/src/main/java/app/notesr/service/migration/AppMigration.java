package app.notesr.service.migration;

import android.content.Context;

public interface AppMigration {
    int getFromVersion();

    int getToVersion();

    void migrate(Context context);
}
