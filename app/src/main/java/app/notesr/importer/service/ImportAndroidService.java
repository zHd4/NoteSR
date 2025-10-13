package app.notesr.importer.service;

import static app.notesr.core.util.KeyUtils.getSecretKeyFromSecrets;

import app.notesr.core.util.FilesUtils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import app.notesr.R;
import app.notesr.data.AppDatabase;
import app.notesr.data.DatabaseProvider;
import app.notesr.file.service.FileService;
import app.notesr.note.service.NoteService;
import app.notesr.core.security.crypto.AesCryptor;
import app.notesr.core.security.crypto.AesGcmCryptor;
import app.notesr.core.security.crypto.CryptoManagerProvider;
import app.notesr.core.security.dto.CryptoSecrets;

import java.util.Set;

public final class ImportAndroidService extends Service implements Runnable {

    public static final String BROADCAST_ACTION = "import_data_broadcast";
    public static final String EXTRA_STATUS = "status";

    public static final Set<ImportStatus> FINISH_STATUSES = Set.of(
            ImportStatus.DONE,
            ImportStatus.DECRYPTION_FAILED,
            ImportStatus.IMPORT_FAILED
    );

    private static final String CHANNEL_ID = "import_service_channel";

    private ImportService importService;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String channelName = getResources().getString(R.string.importing);
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, channelName,
                NotificationManager.IMPORTANCE_NONE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID).build();
        NotificationManager notificationManager = getSystemService(NotificationManager.class);

        notificationManager.createNotificationChannel(channel);

        int type = 0;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            type = ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC;
        }

        importService = getImportService(intent);

        Thread thread = new Thread(this);
        thread.start();

        startForeground(startId, notification, type);
        return START_STICKY;
    }

    @Override
    public void run() {
        importService.doImport();

        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    private void sendBroadcastData(ImportStatus status) {
        Intent intent = new Intent(BROADCAST_ACTION)
                .putExtra(EXTRA_STATUS, status);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private ImportService getImportService(Intent intent) {
        Context context = getApplicationContext();
        AppDatabase db = DatabaseProvider.getInstance(context);

        CryptoSecrets secrets = CryptoManagerProvider.getInstance(context).getSecrets();
        AesCryptor cryptor = new AesGcmCryptor(getSecretKeyFromSecrets(secrets));

        NoteService noteService = new NoteService(db);
        FileService fileService = new FileService(context, db, cryptor,
                new FilesUtils());

        Uri sourceUri = intent.getData();
        ImportStatusCallback statusCallback = new ImportStatusCallback(this::sendBroadcastData);

        return new ImportService(
                this,
                db,
                noteService,
                fileService,
                secrets,
                getContentResolver(),
                sourceUri,
                statusCallback
        );
    }
}
