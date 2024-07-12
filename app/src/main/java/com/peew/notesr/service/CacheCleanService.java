package com.peew.notesr.service;

import android.app.ForegroundServiceStartNotAllowedException;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.peew.notesr.activity.files.viewer.FileViewerActivityBase;

public class CacheCleanService extends Service {

    private static final String CHANNEL_ID = "CacheCleanChannel";

    private Handler handler;
    private Runnable runnable;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
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
        } catch (Exception e) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    e instanceof ForegroundServiceStartNotAllowedException
            ) {
                throw new IllegalStateException(e);
            }
        }

        return START_STICKY;
    }
    @Override
    public void onCreate() {
        handler = new Handler(Looper.getMainLooper());
        runnable = () -> {
            boolean assignmentOpened = FileViewerActivityBase.isRunning();

            handler.postDelayed(runnable, 500);
        };

        handler.postDelayed(runnable, 500);
    }
}
