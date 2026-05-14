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
import app.notesr.data.DatabaseProvider;
import app.notesr.service.AndroidService;
import app.notesr.service.AndroidServiceEntry;
import app.notesr.service.AndroidServiceRegistry;

/**
 * A foreground {@link app.notesr.service.AndroidService} responsible for handling the application's
 * lifecycle cleanup when the task is removed
 * (e.g., when the app is swiped away from the recent apps list).
 *
 * <p>This service ensures that sensitive data is cleared and database connections are
 * properly closed to maintain data integrity and security. If no other application services
 * are running, it performs a full cleanup and terminates the process.</p>
 *
 * <p>The service operates as a foreground service to ensure the system grants it sufficient
 * time to execute cleanup logic during the task removal phase.</p>
 */
public final class AppCloseAndroidService extends AndroidService {

    private static final int NOTIFICATION_ID = 1005;

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

        startForeground(NOTIFICATION_ID, notification, type);
        register(null, null);

        return START_NOT_STICKY;
    }

    @NonNull
    @Override
    protected AndroidServiceEntry getEntry(String payload, String state) {
        return entryBuilder(AppCloseAndroidServiceStarter.class).build();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        if (getOtherRunningServicesCount() == 0) {
            closeDatabase();
            destroySecrets();

            stopForegroundService();
            stopService();

            callSuperOnTaskRemoved(rootIntent);
            exitProcess();
        } else {
            stopForegroundService();
            stopService();
        }
    }

    void callSuperOnTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
    }

    void stopForegroundService() {
        stopForeground(true);
    }

    void stopService() {
        stopSelf();
    }

    long getOtherRunningServicesCount() {
        return AndroidServiceRegistry.getInstance(getApplicationContext())
                .getSet()
                .stream()
                .filter(serviceEntry ->
                        serviceEntry.getServiceClass() != getClass())
                .count();
    }

    void closeDatabase() {
        DatabaseProvider.close();
    }

    void destroySecrets() {
        CryptoManagerProvider.getInstance(getApplicationContext()).destroySecrets();
    }

    void exitProcess() {
        System.exit(0);
    }
}
