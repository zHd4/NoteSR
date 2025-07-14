package app.notesr.service.android;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import app.notesr.BuildConfig;
import app.notesr.R;
import app.notesr.service.migration.AppMigrationRegistry;
import app.notesr.service.migration.AppMigrationService;
import app.notesr.service.migration.DataVersionManager;

public class AppMigrationAndroidService extends Service implements Runnable {
    public static final String BROADCAST_ACTION = "app_migration_service_broadcast";
    public static final String EXTRA_COMPLETE = "migration_complete";
    private static final String CHANNEL_ID = "app_migration_service_channel";

    private AppMigrationService appMigrationService;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        appMigrationService = new AppMigrationService(AppMigrationRegistry.getAllMigrations());

        String channelName = getResources().getString(R.string.updating);
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, channelName,
                NotificationManager.IMPORTANCE_NONE);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);

        Notification notification = new NotificationCompat.Builder(getApplicationContext(),
                CHANNEL_ID).build();

        int type = 0;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            type = ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC;
        }

        startForeground(startId, notification, type);

        return START_STICKY;
    }

    @Override
    public void run() {
        Context context = getApplicationContext();

        int lastMigrationVersion = new DataVersionManager(context).getCurrentVersion();
        int currentDataSchemaVersion = BuildConfig.DATA_SCHEMA_VERSION;

        appMigrationService.run(lastMigrationVersion, currentDataSchemaVersion);
        Intent broadcastIntent = new Intent(BROADCAST_ACTION)
                .putExtra(EXTRA_COMPLETE, true);

        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);
    }
}
