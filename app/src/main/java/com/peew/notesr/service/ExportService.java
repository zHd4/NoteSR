package com.peew.notesr.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.peew.notesr.App;
import com.peew.notesr.manager.export.ExportManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ExportService extends Service implements Runnable {
    private static final String TAG = CacheCleanerService.class.getName();
    private static final String CHANNEL_ID = "ExportChannel";
    private static final int DELAY = 500;

    private Thread serviceThread;
    private Thread workerThread;
    private File outputFile;
    private ExportManager exportManager;

    @Override
    public void onCreate() {
        serviceThread = new Thread(this);
        serviceThread.start();
    }

    @Override
    public void run() {
        workerThread = new Thread(worker());
        workerThread.start();

        do {
            try {
                sendProgress();
                Thread.sleep(DELAY);
            } catch (InterruptedException e) {
                Log.e(TAG, "Thread interrupted", e);
            }
        } while (workerThread.isAlive());

        sendOutputPath();
    }

    private Runnable worker() {
        return () -> {
            Context context = App.getContext();
            File outputDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

            outputFile = getOutputFile(outputDir.getPath());
            exportManager = new ExportManager(context);

            try {
                exportManager.export(outputFile);
            } catch (IOException e) {
                Log.e(TAG, "IOException", e);
                throw new RuntimeException(e);
            }
        };
    }

    private void sendProgress() {
        if (exportManager != null) {
            Intent intent = new Intent("ProgressUpdate");
            intent.putExtra("progress", exportManager.calculateProgress());

            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }
    }

    private void sendOutputPath() {
        if (outputFile != null) {
            Intent intent = new Intent("ExportOutputPath");
            intent.putExtra("path", outputFile.getAbsolutePath());

            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }
    }

    private File getOutputFile(String dirPath) {
        LocalDateTime now = LocalDateTime.now();
        String nowStr = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));

        //noinspection SpellCheckingInspection
        String filename = "nsr_export_" + nowStr + ".notesr.bak";
        Path outputPath = Paths.get(dirPath, filename);

        return new File(outputPath.toUri());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Exporting",
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