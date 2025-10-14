package app.notesr.activity.migration;

import static app.notesr.service.migration.AppMigrationAndroidService.EXTRA_COMPLETE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import app.notesr.service.migration.AppMigrationAndroidService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class MigrationBroadcastReceiver extends BroadcastReceiver {
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
