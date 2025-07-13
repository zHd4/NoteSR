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
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import app.notesr.R;
import app.notesr.dto.CryptoKey;
import app.notesr.exception.ReEncryptionFailedException;
import app.notesr.service.ServiceHandler;
import app.notesr.service.activity.security.KeyUpdateService;

public class ReEncryptionService extends Service implements Runnable {

    public static final String BROADCAST_ACTION = "re_encryption_service_data_broadcast";

    private static final String TAG = ReEncryptionService.class.getName();
    private static final String CHANNEL_ID = "re_encryption_service_channel";
    private static final int LOOP_DELAY = 100;

    private final ServiceHandler<KeyUpdateService> keyUpdateServiceServiceHandler =
            new ServiceHandler<>();
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
                keyUpdateServiceServiceHandler.waitForService();
                keyUpdateServiceServiceHandler.getService().updateEncryptedData();
            } catch (Exception e) {
                jobThread.interrupt();
                throw new ReEncryptionFailedException(e);
            }
        });

        jobThread.start();

        try {
            broadcastLoop();
        } catch (InterruptedException e) {
            Log.e(TAG, "Broadcast loop interrupted", e);
        } finally {
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        CryptoKey newCryptoKey =
                requireNonNull((CryptoKey) intent.getSerializableExtra("newCryptoKey"));

        keyUpdateServiceServiceHandler.setService(createKeyUpdateService(newCryptoKey));

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

    private void broadcastLoop() throws InterruptedException {
        Integer progress;
        boolean threadInterrupted;

        do {
            progress = getProgressPercent();
            threadInterrupted = jobThread.isInterrupted();

            Log.i(TAG, "Progress: " + progress + "%, is job thread interrupted: "
                    + threadInterrupted);

            if (progress != null) {
                Intent intent = new Intent(BROADCAST_ACTION).putExtra("progress", progress);
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            }

            Thread.sleep(LOOP_DELAY);
        } while (progress == null || (progress < 100 && !threadInterrupted));
    }

    private KeyUpdateService createKeyUpdateService(CryptoKey newCryptoKey) {
        try {
            return new KeyUpdateService(newCryptoKey);
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

    private Integer getProgressPercent() {
        if (keyUpdateServiceServiceHandler.getService() == null) {
            return null;
        }

        long total = keyUpdateServiceServiceHandler.getService().getTotal();
        long progress = keyUpdateServiceServiceHandler.getService().getProgress();

        return Math.round((progress * 100f) / total);
    }
}
