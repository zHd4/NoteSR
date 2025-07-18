package app.notesr.service.android;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import app.notesr.App;
import app.notesr.file.viewer.FileViewerActivityBase;
import app.notesr.db.service.dao.TempFileDao;
import app.notesr.model.TempFile;
import app.notesr.util.Wiper;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CacheCleanerAndroidService extends Service implements Runnable {

    private static final String TAG = CacheCleanerAndroidService.class.getName();
    private static final String CHANNEL_ID = "cache_cleaner_service_channel";
    private static final int DELAY = 2000;

    private final Map<TempFile, Thread> runningJobs = new LinkedHashMap<>();
    private Thread thread;

    @Override
    public void onCreate() {
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void run() {
        try {
            while (thread.isAlive()) {
                if (!FileViewerActivityBase.isRunning()) {
                    clearCache();

                    if (runningJobs.isEmpty()) {
                        thread.interrupt();

                        stopForeground(STOP_FOREGROUND_REMOVE);
                        stopSelf();

                        if (!App.getContext().isAnyActivityVisible()) {
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
        List<TempFile> tempFiles = getTempFileDao().getAll();

        tempFiles.stream()
                .filter(tempFile -> !runningJobs.containsKey(tempFile))
                .forEach(tempFile -> {
                    Thread thread = new Thread(wipeFile(tempFile));

                    runningJobs.put(tempFile, thread);
                    thread.start();
                });
    }

    private Runnable wipeFile(TempFile tempFile) {
        return () -> {
            File file = new File(Objects.requireNonNull(tempFile.getUri().getPath()));

            if (file.exists()) {
                try {
                    boolean removed = Wiper.wipeFile(file);

                    if (!removed) {
                        Log.e(TAG, "File " + file + " not removed");
                    }
                } catch (IOException e) {
                    Log.e(TAG, "IOException while clearing cache", e);
                }
            }

            getTempFileDao().delete(tempFile.getId());
            runningJobs.remove(tempFile);
        };
    }

    private TempFileDao getTempFileDao() {
        return App.getAppContainer()
                .getServicesDB()
                .getDao(TempFileDao.class);
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

        startForeground(startId, notification, type);

        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
