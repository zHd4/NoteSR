package app.notesr.security.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import app.notesr.BuildConfig;
import app.notesr.migration.activity.MigrationActivity;
import app.notesr.note.activity.NotesListActivity;
import app.notesr.security.service.SecretsSetupService;
import app.notesr.migration.service.DataVersionManager;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class KeySetupCompletionHandler {
    private final Activity activity;
    private final SecretsSetupService keySetupService;
    private final KeySetupMode mode;

    public void handle() {
        switch (mode) {
            case FIRST_RUN -> proceedFirstRun();
            case REGENERATION -> proceedRegeneration();
            default -> throw new RuntimeException("Unknown mode: " + mode);
        }
    }

    private void proceedFirstRun() {
        try {
            keySetupService.apply();

            Context context = activity.getApplicationContext();
            Intent nextIntent = new Intent(context, NotesListActivity.class);

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

        Intent intent = new Intent(context, ReEncryptionActivity.class);
        intent.putExtra(ReEncryptionActivity.EXTRA_NEW_SECRETS, keySetupService.getCryptoSecrets());

        activity.startActivity(intent);
        activity.finish();
    }
}
