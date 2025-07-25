package app.notesr.crypto;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import app.notesr.BuildConfig;
import app.notesr.data.MigrationActivity;
import app.notesr.data.ReEncryptionActivity;
import app.notesr.note.NoteListActivity;
import app.notesr.service.crypto.KeySetupService;
import app.notesr.service.migration.DataVersionManager;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class KeySetupCompletionHandler {
    private final Activity activity;
    private final KeySetupService keySetupService;
    private final KeySetupMode mode;

    public void handle() {
        switch (mode) {
            case FIRST_RUN -> proceedFirstRun();
            case REGENERATION -> proceedRegeneration();
        }
    }

    private void proceedFirstRun() {
        try {
            keySetupService.apply();

            Context context = activity.getApplicationContext();
            Intent nextIntent = new Intent(context, NoteListActivity.class);

            DataVersionManager dataVersionManager = new DataVersionManager(context);

            int lastMigrationVersion = dataVersionManager.getCurrentVersion();
            int currentDataSchemaVersion = BuildConfig.DATA_SCHEMA_VERSION;

            if (lastMigrationVersion == DataVersionManager.DEFAULT_FIRST_VERSION) {
                dataVersionManager.setCurrentVersion(currentDataSchemaVersion);
            } else if (lastMigrationVersion < currentDataSchemaVersion) {
                nextIntent = new Intent(context, MigrationActivity.class);
            }

            activity.startActivity(nextIntent);
            activity.finish();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void proceedRegeneration() {
        Context context = activity.getApplicationContext();
        Intent intent = new Intent(context, ReEncryptionActivity.class)
                .putExtra("newCryptoKey", keySetupService.getCryptoKey());

        activity.startActivity(intent);
        activity.finish();
    }
}
