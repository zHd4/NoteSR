/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.service.lifecycle;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import app.notesr.core.security.crypto.CryptoManager;
import app.notesr.core.security.crypto.CryptoManagerProvider;
import app.notesr.service.AndroidService;
import app.notesr.service.AndroidServiceEntry;
import app.notesr.service.AndroidServiceRegistry;

public final class AppCloseAndroidService extends AndroidService {

    private static final String CHANNEL_ID = "app_close_service_channel";
    private static final String CHANNEL_NAME = "App Close Service Channel";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(notificationChannel);

        Notification notification = new NotificationCompat.Builder(getApplicationContext(),
                CHANNEL_ID).build();

        int type = 0;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            type = ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC;
        }

        startForeground(1005, notification, type);
        register();

        return START_NOT_STICKY;
    }

    @NonNull
    @Override
    protected AndroidServiceEntry getEntry() {
        return entryBuilder(AppCloseAndroidServiceStarter.class).build();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        if (getCurrentRunningServicesCount() == 0) {
            CryptoManager cryptoManager = CryptoManagerProvider.getInstance(getApplicationContext());
            cryptoManager.destroySecrets();

            stopForeground(true);
            stopSelf();

            super.onTaskRemoved(rootIntent);
            System.exit(0);
        } else {
            stopForeground(true);
            stopSelf();
        }
    }

    private long getCurrentRunningServicesCount() {
        return AndroidServiceRegistry.getInstance(getApplicationContext())
                .getSet()
                .stream()
                .filter(serviceEntry ->
                        serviceEntry.getServiceClass() != getClass())
                .count();
    }
}
