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

import app.notesr.R;
import app.notesr.crypto.CryptoManager;
import app.notesr.db.AppDatabase;
import app.notesr.db.DatabaseProvider;
import app.notesr.dto.CryptoSecrets;
import app.notesr.service.crypto.SecretsUpdateService;

public class ReEncryptionAndroidService extends Service implements Runnable {

    private static final String TAG = ReEncryptionAndroidService.class.getName();
    public static final String BROADCAST_ACTION = "re_encryption_service_broadcast";
    public static final String EXTRA_COMPLETE = "re_encryption_complete";
    public static final String EXTRA_NEW_SECRETS = "new_secrets";
    private static final String CHANNEL_ID = "re_encryption_service_channel";

    private SecretsUpdateService secretsUpdateService;


    @Override
    public void onCreate() {
        Thread thread = new Thread(this);
        thread.start();
    }

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

        CryptoSecrets newSecrets =
                requireNonNull((CryptoSecrets) intent.getSerializableExtra(EXTRA_NEW_SECRETS));

        AppDatabase db = DatabaseProvider.getInstance(getApplicationContext());
        CryptoManager cryptoManager = CryptoManager.getInstance(getApplicationContext());

        secretsUpdateService = new SecretsUpdateService(db, cryptoManager, newSecrets);
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
        secretsUpdateService.update();
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }
}
