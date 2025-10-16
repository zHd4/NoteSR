package app.notesr.service.cachecleaner;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import app.notesr.activity.App;
import app.notesr.data.AppDatabase;
import app.notesr.data.DatabaseProvider;
import app.notesr.activity.file.viewer.FileViewerActivityBase;

public final class CacheCleanerAndroidService extends Service implements Runnable {

    private static final String TAG = CacheCleanerAndroidService.class.getCanonicalName();
    private static final String CHANNEL_ID = "cache_cleaner_service_channel";
    private static final int DELAY = 2000;

    private Thread thread;
    private CacheCleanerService cacheCleanerService;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Cleaning cache",
                NotificationManager.IMPORTANCE_NONE);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .build();

        int type = 0;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            type = ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC;
        }

        Context context = getApplicationContext();
        AppDatabase db = DatabaseProvider.getInstance(context);
        TempFileService tempFileService = new TempFileService(db);

        cacheCleanerService = new CacheCleanerService(tempFileService);
        thread = new Thread(this);

        thread.start();
        startForeground(startId, notification, type);

        return START_STICKY;
    }

    @Override
    public void run() {
        try {
            while (thread.isAlive()) {
                Activity currentActivity = App.getContext().getCurrentActivity();

                if (!(currentActivity instanceof FileViewerActivityBase)) {
                    cacheCleanerService.cleanupTempFilesAsync();

                    if (!cacheCleanerService.hasRunningJobs()) {
                        thread.interrupt();

                        stopForeground(STOP_FOREGROUND_REMOVE);
                        stopSelf();

                        if (currentActivity == null) {
                            System.exit(0);
                        }
                    }
                }

                Thread.sleep(DELAY);
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "Thread interrupted", e);
        }
    }
}
