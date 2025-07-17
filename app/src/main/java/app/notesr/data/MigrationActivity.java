package app.notesr.data;

import static app.notesr.service.android.AppMigrationAndroidService.EXTRA_COMPLETE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import app.notesr.BuildConfig;
import app.notesr.R;
import app.notesr.ActivityBase;
import app.notesr.note.NoteListActivity;
import app.notesr.service.android.AppMigrationAndroidService;
import app.notesr.service.migration.DataVersionManager;

public class MigrationActivity extends ActivityBase {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_migration);

        LocalBroadcastManager.getInstance(getApplicationContext())
                .registerReceiver(broadcastReceiver(),
                        new IntentFilter(AppMigrationAndroidService.BROADCAST_ACTION));

        Intent serviceIntent = new Intent(getApplicationContext(), AppMigrationAndroidService.class);
        startForegroundService(serviceIntent);
    }

    private BroadcastReceiver broadcastReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (AppMigrationAndroidService.BROADCAST_ACTION.equals(intent.getAction())) {
                    boolean isCompleted = intent.getBooleanExtra(EXTRA_COMPLETE, false);

                    if (isCompleted) {
                        new DataVersionManager(getApplicationContext())
                                .setCurrentVersion(BuildConfig.DATA_SCHEMA_VERSION);

                        startActivity(new Intent(getApplicationContext(), NoteListActivity.class));
                        finish();
                    }
                }
            }
        };
    }
}