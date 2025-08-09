package app.notesr.migration.activity;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import app.notesr.App;
import app.notesr.BuildConfig;
import app.notesr.R;
import app.notesr.ActivityBase;
import app.notesr.note.activity.NotesListActivity;
import app.notesr.migration.service.AppMigrationAndroidService;
import app.notesr.migration.service.DataVersionManager;

public class MigrationActivity extends ActivityBase {

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

    protected void startMigrationService() {
        if (!App.getContext().isServiceRunning(AppMigrationAndroidService.class)) {
            Intent serviceIntent = new Intent(getApplicationContext(),
                    AppMigrationAndroidService.class);

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