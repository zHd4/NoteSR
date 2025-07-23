package app.notesr.crypto;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;

import app.notesr.BuildConfig;
import app.notesr.data.MigrationActivity;
import app.notesr.data.ReEncryptionActivity;
import app.notesr.note.NoteListActivity;
import app.notesr.service.crypto.KeySetupService;
import app.notesr.service.migration.DataVersionManager;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class FinishKeySetupOnClick implements View.OnClickListener {
    private final Activity parentActivity;
    private final KeySetupService keySetupService;
    private final KeySetupMode mode;
    
    @Override
    public void onClick(View view) {
        switch (mode) {
            case FIRST_RUN -> proceedFirstRun();
            case REGENERATION -> proceedRegeneration();
        }
    }

    private void proceedFirstRun() {
        try {
            keySetupService.apply();

            Context context = parentActivity.getApplicationContext();
            Intent nextIntent = new Intent(context, NoteListActivity.class);

            DataVersionManager dataVersionManager = new DataVersionManager(context);

            int lastMigrationVersion = dataVersionManager.getCurrentVersion();
            int currentDataSchemaVersion = BuildConfig.DATA_SCHEMA_VERSION;

            if (lastMigrationVersion == DataVersionManager.DEFAULT_FIRST_VERSION) {
                dataVersionManager.setCurrentVersion(currentDataSchemaVersion);
            } else if (lastMigrationVersion < currentDataSchemaVersion) {
                nextIntent = new Intent(context, MigrationActivity.class);
            }

            parentActivity.startActivity(nextIntent);
            parentActivity.finish();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void proceedRegeneration() {
        Context context = parentActivity.getApplicationContext();
        Intent intent = new Intent(context, ReEncryptionActivity.class)
                .putExtra("newCryptoKey", keySetupService.getCryptoKey());

        parentActivity.startActivity(intent);
        parentActivity.finish();
    }
}
