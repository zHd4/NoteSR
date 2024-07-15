package com.peew.notesr.service;

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
import com.peew.notesr.App;
import com.peew.notesr.activity.files.viewer.BaseFileViewerActivity;
import com.peew.notesr.db.services.tables.TempFilesTable;
import com.peew.notesr.model.TempFile;
import com.peew.notesr.tools.FileWiper;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CacheCleanerService extends Service implements Runnable {

    private static final String TAG = CacheCleanerService.class.getName();
    private static final String CHANNEL_ID = "CacheCleanerChannel";
    private static final int DELAY = 2000;

    private final Map<TempFile, Thread> runningJobs = new LinkedHashMap<>();
    private Thread thread;

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

        startForeground(startId, notification, type);

        return START_STICKY;
    }
    @Override
    public void onCreate() {
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void run() {
        try {
            while (thread.isAlive()) {
                if (!BaseFileViewerActivity.isRunning()) {
                    clearCache();

                    if (runningJobs.isEmpty()) {
                        thread.interrupt();
                        stopSelf();
                    }
                }

                //noinspection BusyWait
                Thread.sleep(DELAY);
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "Thread interrupted", e);
        }
    }

    private void clearCache() {
        List<TempFile> tempFiles = getTempFilesTable().getAll();

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
            File file = new File(tempFile.getUri().getPath());

            if (file.exists()) {
                try {
                    FileWiper wiper = new FileWiper(file);
                    boolean removed = wiper.wipeFile();

                    if (!removed) {
                        Log.e(TAG, "File " + file + " not removed");
                    }
                } catch (IOException e) {
                    Log.e(TAG, "IOException while clearing cache", e);
                }
            }

            getTempFilesTable().delete(tempFile.getId());
            runningJobs.remove(tempFile);
        };
    }

    private TempFilesTable getTempFilesTable() {
        return App.getAppContainer()
                .getServicesDB()
                .getTable(TempFilesTable.class);
    }
}
