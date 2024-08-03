package com.peew.notesr.service;

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
import com.peew.notesr.manager.importer.ImportManager;

import java.io.File;

public class ImportService extends Service implements Runnable {

    private static final String TAG = ImportService.class.getName();
    private static final String CHANNEL_ID = "ImportChannel";

    private Thread thread;
    private ImportManager importManager;

    @Override
    public void onCreate() {
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void run() {
        if (importManager == null) {
            RuntimeException e = new UnsupportedOperationException("Service has not been started");

            Log.e(TAG, "UnsupportedOperationException", e);
            throw e;
        }

        importManager.start();
        broadcastLoop();

        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    private void broadcastLoop() {

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Importing",
                NotificationManager.IMPORTANCE_NONE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID).build();
        NotificationManager notificationManager = getSystemService(NotificationManager.class);

        notificationManager.createNotificationChannel(channel);

        int type = 0;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            type = ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC;
        }

        Context context = this;
        File sourceFile = new File(intent.getStringExtra("sourceFileUri"));

        importManager = new ImportManager(context, sourceFile);

        startForeground(startId, notification, type);

        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
