package notesr.service;

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
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import notesr.manager.importer.ImportManager;
import notesr.manager.importer.ImportResult;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class ImportService extends Service implements Runnable {

    private static final String TAG = ImportService.class.getName();
    private static final String CHANNEL_ID = "ImportChannel";
    private static final int LOOP_DELAY = 100;

    private ImportManager importManager;

    @Override
    public void onCreate() {
        Thread thread = new Thread(this);
        thread.start();
    }

    @Override
    public void run() {
        try {
            waitForImportManager();
            importManager.start();
            broadcastLoop();
        } catch (InterruptedException e) {
            Log.e(TAG, "Thread interrupted", e);
        }

        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    private void broadcastLoop() throws InterruptedException {
        while (importManager.getResult() == ImportResult.NONE) {
            sendBroadcastData(importManager.getStatus(), ImportResult.NONE);
            Thread.sleep(LOOP_DELAY);
        }

        sendBroadcastData(importManager.getStatus(), importManager.getResult());
    }

    private void sendBroadcastData(String status, ImportResult result) {
        Intent intent = new Intent("importDataBroadcast")
                .putExtra("status", status)
                .putExtra("result", result.toString());

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void waitForImportManager() throws InterruptedException {
        while (importManager == null) {
            Thread.sleep(LOOP_DELAY);
        }
    }

    private FileInputStream getFileStream(Uri uri) {
        try {
            return (FileInputStream) getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "FileNotFoundException", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Context context = this;

        Uri sourceUri = intent.getData();
        FileInputStream sourceStream = getFileStream(sourceUri);

        importManager = new ImportManager(context, sourceStream);

        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Importing",
                NotificationManager.IMPORTANCE_NONE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID).build();
        NotificationManager notificationManager = getSystemService(NotificationManager.class);

        notificationManager.createNotificationChannel(channel);

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
}
