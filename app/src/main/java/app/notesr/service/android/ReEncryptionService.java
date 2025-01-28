package app.notesr.service.android;

import static java.util.Objects.requireNonNull;

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
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import app.notesr.R;
import app.notesr.dto.CryptoKey;
import app.notesr.exception.ReEncryptionFailedException;
import app.notesr.service.activity.security.KeyUpdateService;

public class ReEncryptionService extends Service implements Runnable {

    public static final String BROADCAST_ACTION = "ReEncryptionServiceDataBroadcast";
    private static final String CHANNEL_ID = "ReEncryptionChannel";

    private KeyUpdateService keyUpdateService;
    private Thread jobThread;

    @Override
    public void onCreate() {
        Thread thread = new Thread(this);
        thread.start();
    }

    @Override
    public void run() {
        jobThread = new Thread(() -> {
            try {
                keyUpdateService.updateEncryptedData();
            } catch (Exception e) {
                jobThread.interrupt();
                throw new ReEncryptionFailedException(e);
            }
        });

        jobThread.start();
        broadcastLoop();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        CryptoKey newCryptoKey =
                requireNonNull((CryptoKey) intent.getSerializableExtra("newCryptoKey"));

        keyUpdateService = getKeyUpdateService(newCryptoKey);

        String channelName = getResources().getString(R.string.re_encrypting_data);
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, channelName,
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

    private void broadcastLoop() {
        int progress;

        do {
            progress = getProgressPercent();

            Intent intent = new Intent(BROADCAST_ACTION).putExtra("progress", progress);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        } while (progress < 100 && !jobThread.isInterrupted());
    }

    private KeyUpdateService getKeyUpdateService(CryptoKey newCryptoKey) {
        try {
            return new KeyUpdateService(newCryptoKey);
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    private int getProgressPercent() {
        long total = keyUpdateService.getTotal();
        long progress = keyUpdateService.getProgress();

        return Math.round((progress * 100f) / total);
    }
}
