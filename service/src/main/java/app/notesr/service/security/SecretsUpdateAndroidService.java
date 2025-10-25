package app.notesr.service.security;

import static java.util.Objects.requireNonNull;

import static app.notesr.core.util.CharUtils.bytesToChars;

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
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;

import app.notesr.core.security.SecretCache;
import app.notesr.core.security.exception.DecryptionFailedException;
import app.notesr.core.security.exception.EncryptionFailedException;
import app.notesr.core.security.crypto.CryptoManager;
import app.notesr.core.security.crypto.CryptoManagerProvider;
import app.notesr.data.DatabaseProvider;
import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.core.util.FilesUtils;

public final class SecretsUpdateAndroidService extends Service implements Runnable {

    private static final String TAG = SecretsUpdateAndroidService.class.getCanonicalName();
    public static final String NEW_KEY = "new_key";
    public static final String PASSWORD = "password";
    public static final String BROADCAST_ACTION = "re_encryption_service_broadcast";
    public static final String EXTRA_COMPLETE = "re_encryption_complete";
    private static final String CHANNEL_ID = "re_encryption_service_channel";
    private static final String CHANNEL_NAME = "Re-encryption Service Channel";


    private SecretsUpdateService secretsUpdateService;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME,
                NotificationManager.IMPORTANCE_NONE);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(notificationChannel);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .build();

        int type = 0;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            type = ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC;
        }

        CryptoSecrets newSecrets = getNewSecrets();
        secretsUpdateService = getSecretsUpdateService(newSecrets);

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

    private CryptoSecrets getNewSecrets() {
        try {
            byte[] newKey = SecretCache.take(NEW_KEY);
            char[] newPassword = bytesToChars(SecretCache.take(PASSWORD),
                    StandardCharsets.UTF_8);

            requireNonNull(newKey);
            requireNonNull(newPassword);

            return new CryptoSecrets(newKey, newPassword);
        } catch (CharacterCodingException e) {
            throw new RuntimeException(e);
        }
    }

    private SecretsUpdateService getSecretsUpdateService(CryptoSecrets newSecrets) {
        Context context = getApplicationContext();
        CryptoManager cryptoManager = CryptoManagerProvider.getInstance(context);
        FilesUtils filesUtils = new FilesUtils();

        return new SecretsUpdateService(
                context,
                DatabaseProvider.DB_NAME,
                cryptoManager,
                newSecrets,
                filesUtils
        );
    }
}
