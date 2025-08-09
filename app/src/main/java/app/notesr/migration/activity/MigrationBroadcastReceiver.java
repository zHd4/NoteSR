package app.notesr.migration.activity;

import static app.notesr.migration.service.AppMigrationAndroidService.EXTRA_COMPLETE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import app.notesr.migration.service.AppMigrationAndroidService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MigrationBroadcastReceiver extends BroadcastReceiver {
    private final Runnable onMigrationComplete;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (AppMigrationAndroidService.BROADCAST_ACTION.equals(intent.getAction())) {
            boolean isCompleted = intent.getBooleanExtra(EXTRA_COMPLETE, false);
            if (isCompleted) {
                onMigrationComplete.run();
            }
        }
    }
}
