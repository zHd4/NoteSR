package app.notesr.activity.migration;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import app.notesr.activity.App;
import app.notesr.BuildConfig;
import app.notesr.R;
import app.notesr.activity.ActivityBase;
import app.notesr.activity.note.NotesListActivity;
import app.notesr.service.migration.AppMigrationAndroidService;
import app.notesr.service.migration.DataVersionManager;

public final class MigrationActivity extends ActivityBase {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_migration);

        MigrationBroadcastReceiver receiver = new MigrationBroadcastReceiver(
                this::onMigrationComplete);

        LocalBroadcastManager.getInstance(getApplicationContext())
                .registerReceiver(receiver,
                        new IntentFilter(AppMigrationAndroidService.BROADCAST_ACTION));

        startMigrationService();
    }

    private void startMigrationService() {
        if (!App.getContext().isServiceRunning(AppMigrationAndroidService.class)) {
            Intent serviceIntent = new Intent(getApplicationContext(),
                    AppMigrationAndroidService.class);

            serviceIntent.putExtra(AppMigrationAndroidService.EXTRA_CURRENT_DATA_SCHEMA_VERSION,
                    BuildConfig.DATA_SCHEMA_VERSION);

            startForegroundService(serviceIntent);
        }
    }

    private void onMigrationComplete() {
        DataVersionManager dataVersionManager = new DataVersionManager(getApplicationContext());
        dataVersionManager.setCurrentVersion(BuildConfig.DATA_SCHEMA_VERSION);

        startActivity(new Intent(getApplicationContext(), NotesListActivity.class));
        finish();
    }
}
