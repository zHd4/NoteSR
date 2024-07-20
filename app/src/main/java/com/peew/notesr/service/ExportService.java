package com.peew.notesr.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ExportService extends Service implements Runnable {
    private static final String TAG = CacheCleanerService.class.getName();
    private static final String CHANNEL_ID = "ExportChannel";
    private static final int BROADCAST_DELAY = 100;

    private Thread serviceThread;
    private Thread exportWorkerThread;
    private Thread cancelThread;
    private File outputFile;
    private ExportManager exportManager;

    @Override
    public void onCreate() {
        serviceThread = new Thread(this);
        serviceThread.start();
    }

    @Override
    public void run() {
        Context context = App.getContext();

        File outputDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        outputFile = getOutputFile(outputDir.getPath());

        exportManager = new ExportManager(context, outputFile);
        exportWorkerThread = new Thread(exportWorker());

        registerCancelSignalReceiver();

        exportWorkerThread.start();
        broadcastWorker().run();

        // Delete file
        outputFile.delete();
    }

    private Runnable exportWorker() {
        return () -> {
            exportManager.export();
            stop();
        };
    }

    private Runnable broadcastWorker() {
        return () -> {
            String outputPath = outputFile.getAbsolutePath();

            do {
                try {
                    String status = exportManager.getStatus();

                    int progress = exportManager.calculateProgress();
                    boolean wasCanceled = exportManager.getResult() == ExportManager.CANCELED;

                    sendBroadcastData(progress, status, outputPath, wasCanceled);

                    Thread.sleep(BROADCAST_DELAY);
                } catch (InterruptedException e) {
                    Log.e(TAG, "Thread interrupted", e);
                }
            } while (exportManager.getResult() == ExportManager.NONE);
        };
    }

    private void sendBroadcastData(int progress, String status, String outputPath, boolean stopped) {
        Intent intent = new Intent("ExportDataBroadcast")
                .putExtra("progress", progress)
                .putExtra("status", status)
                .putExtra("outputPath", outputPath)
                .putExtra("stopped", stopped);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void registerCancelSignalReceiver() {
        Runnable cancel = () -> {
            if (exportManager == null) {
                throw new IllegalStateException("Service has not been started");
            }

            exportManager.cancel();
            stop();
        };

        LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if (cancelThread == null) {
                            cancelThread = new Thread(cancel);
                            cancelThread.start();
                        }
                    }
                }, new IntentFilter("CancelExportSignal"));
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

    private void stop() {
        serviceThread.interrupt();
        exportWorkerThread.interrupt();

        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }
}
