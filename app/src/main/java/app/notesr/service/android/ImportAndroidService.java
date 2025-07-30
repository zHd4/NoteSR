package app.notesr.service.android;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import app.notesr.R;
import app.notesr.db.AppDatabase;
import app.notesr.db.DatabaseProvider;
import app.notesr.service.data.importer.ImportStatus;
import app.notesr.service.data.importer.ImportService;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Set;

public class ImportAndroidService extends Service implements Runnable {

    private static final String TAG = ImportAndroidService.class.getName();
    public static final String BROADCAST_ACTION = "import_data_broadcast";
    public static final String STATUS_EXTRA = "status";

    public static final Set<ImportStatus> FINISH_STATUSES = Set.of(
            ImportStatus.DONE,
            ImportStatus.DECRYPTION_FAILED,
            ImportStatus.IMPORT_FAILED
    );

    private static final String CHANNEL_ID = "import_service_channel";
    private static final int BROADCAST_LOOP_DELAY = 100;


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

        AppDatabase db = DatabaseProvider.getInstance(getApplicationContext());
        Uri sourceUri = intent.getData();
        FileInputStream sourceStream = getFileStream(sourceUri);

        importService = new ImportService(this, db, sourceStream);
        Thread thread = new Thread(this);

        thread.start();
        startForeground(startId, notification, type);

        return START_STICKY;
    }

    @Override
    public void run() {
        Thread jobThread = new Thread(() -> importService.doImport());
        jobThread.start();

        try {
            broadcastLoop();
        } catch (InterruptedException e) {
            Log.e(TAG, "Broadcast loop interrupted", e);
        }

        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    private void broadcastLoop() throws InterruptedException {
        ImportStatus status;

        do {
            status = importService.getStatus();

            sendBroadcastData(status);
            Thread.sleep(BROADCAST_LOOP_DELAY);
        } while (status != null && !FINISH_STATUSES.contains(status));
    }

    private void sendBroadcastData(ImportStatus status) {
        Intent intent = new Intent(BROADCAST_ACTION)
                .putExtra(STATUS_EXTRA, status);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private FileInputStream getFileStream(Uri uri) {
        try {
            return (FileInputStream) getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "FileNotFoundException", e);
            throw new RuntimeException(e);
        }
    }
}
