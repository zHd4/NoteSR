package app.notesr.cleaner.service;

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

import app.notesr.App;
import app.notesr.cleaner.model.TempFile;
import app.notesr.db.DatabaseProvider;
import app.notesr.file.activity.viewer.FileViewerActivityBase;
import app.notesr.util.Wiper;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class CacheCleanerAndroidService extends Service implements Runnable {

    private static final String TAG = CacheCleanerAndroidService.class.getName();
    private static final String CHANNEL_ID = "cache_cleaner_service_channel";
    private static final int DELAY = 2000;
    private final Map<TempFile, Thread> runningJobs = new LinkedHashMap<>();
    private Thread thread;
    private TempFileService tempFileService;

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
        tempFileService = new TempFileService(DatabaseProvider.getInstance(context));

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
                    clearCache();

                    if (runningJobs.isEmpty()) {
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

    private void clearCache() {
        tempFileService.getAll().stream()
                .filter(tempFile -> !runningJobs.containsKey(tempFile))
                .forEach(tempFile -> {
                    Thread thread = new Thread(deleteTempFile(tempFile));

                    runningJobs.put(tempFile, thread);
                    thread.start();
                });
    }

    private Runnable deleteTempFile(TempFile tempFile) {
        return () -> {
            File file = new File(Objects.requireNonNull(tempFile.getUri().getPath()));

            if (file.exists()) {
                try {
                    boolean removed = Wiper.wipeFile(file);

                    if (!removed) {
                        Log.e(TAG, "Temp file cannot be removed: " + file);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "IOException while clearing cache", e);
                }
            }

            tempFileService.delete(tempFile.getId());
            runningJobs.remove(tempFile);
        };
    }
}
