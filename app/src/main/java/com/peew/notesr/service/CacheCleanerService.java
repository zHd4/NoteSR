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
import com.peew.notesr.tools.FileManager;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class CacheCleanerService extends Service implements Runnable {

    private static final String TAG = CacheCleanerService.class.getName();
    private static final String CHANNEL_ID = "CacheCleanerChannel";
    private static final int DELAY = 2000;

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
        while (thread.isAlive()) {
            if (!BaseFileViewerActivity.isRunning()) {
                clearCache();

                thread.interrupt();
                stopSelf();
            }

            try {
                Thread.sleep(DELAY);
            } catch (InterruptedException e) {
                Log.e(TAG, "Service interrupted", e);
            }
        }
    }

    private void clearCache() {
        List<TempFile> tempFiles = getTempFilesTable().getAll();
        CountDownLatch latch = new CountDownLatch(tempFiles.size());

        tempFiles.forEach(tempFile -> {
            Thread thread = new Thread(wipeFile(tempFile, latch));
            thread.start();
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            Log.e(TAG, "CountDownLatch interrupted", e);
        }
    }

    private Runnable wipeFile(TempFile tempFile, CountDownLatch latch) {
        return () -> {
            File file = new File(tempFile.getUri().getPath());

            if (file.exists()) {
                try {
                    FileManager.wipeFile(file);
                } catch (IOException e) {
                    Log.e(TAG, "IOException while clearing cache", e);
                }
            }

            getTempFilesTable().delete(tempFile.getId());
            latch.countDown();
        };
    }

    private TempFilesTable getTempFilesTable() {
        return App.getAppContainer()
                .getServicesDB()
                .getTable(TempFilesTable.class);
    }
}
