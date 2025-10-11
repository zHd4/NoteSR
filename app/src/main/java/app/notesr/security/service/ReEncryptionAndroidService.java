package app.notesr.security.service;

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

import java.io.IOException;

import app.notesr.R;
import app.notesr.exception.DecryptionFailedException;
import app.notesr.exception.EncryptionFailedException;
import app.notesr.security.crypto.CryptoManager;
import app.notesr.security.crypto.CryptoManagerProvider;
import app.notesr.db.DatabaseProvider;
import app.notesr.security.dto.CryptoSecrets;
import app.notesr.util.FilesUtils;

public final class ReEncryptionAndroidService extends Service implements Runnable {

    private static final String TAG = ReEncryptionAndroidService.class.getName();
    public static final String BROADCAST_ACTION = "re_encryption_service_broadcast";
    public static final String EXTRA_COMPLETE = "re_encryption_complete";
    public static final String EXTRA_NEW_SECRETS = "new_secrets";
    private static final String CHANNEL_ID = "re_encryption_service_channel";

    private SecretsUpdateService secretsUpdateService;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
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

        CryptoManager cryptoManager = CryptoManagerProvider.getInstance();
        CryptoSecrets newSecrets = (CryptoSecrets) intent.getSerializableExtra(EXTRA_NEW_SECRETS);
        requireNonNull(newSecrets);

        FilesUtils filesUtils = new FilesUtils();

        secretsUpdateService = new SecretsUpdateService(
                getApplicationContext(),
                DatabaseProvider.DB_NAME,
                cryptoManager,
                newSecrets,
                filesUtils
        );

        Thread thread = new Thread(this);
        thread.start();
        startForeground(startId, notification, type);

        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void run() {
        try {
            secretsUpdateService.update();
            onComplete();
        } catch (EncryptionFailedException | DecryptionFailedException | IOException e) {
            Log.e(TAG, "Filed to update database", e);
            throw new RuntimeException("Filed to update database", e);
        } finally {
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
        }
    }

    private void onComplete() {
        Intent broadcastIntent = new Intent(BROADCAST_ACTION).putExtra(EXTRA_COMPLETE, true);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);
    }
}
