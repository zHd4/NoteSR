package app.notesr.service.lifecycle;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import app.notesr.core.security.crypto.CryptoManager;
import app.notesr.core.security.crypto.CryptoManagerProvider;

public class AppCloseAndroidService extends Service {

    private static final String CHANNEL_ID = "app_close_service_channel";
    private static final String CHANNEL_NAME = "App Close Service Channel";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW );

        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);

        Notification notification = new NotificationCompat.Builder(getApplicationContext(),
                CHANNEL_ID).build();

        int type = 0;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            type = ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC;
        }

        startForeground(startId, notification, type);

        return START_NOT_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        CryptoManager cryptoManager = CryptoManagerProvider.getInstance(getApplicationContext());
        cryptoManager.destroySecrets();

        super.onTaskRemoved(rootIntent);
        stopForeground(true);
        stopSelf();
    }
}
