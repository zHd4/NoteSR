package app.notesr.exporter.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import app.notesr.R;
import app.notesr.db.DatabaseProvider;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

public class ExportAndroidService extends Service implements Runnable {
    private static final String TAG = ExportAndroidService.class.getName();
    public static final String BROADCAST_ACTION = "export_data_broadcast";
    public static final String EXTRA_STATUS = "status";
    public static final String EXTRA_PROGRESS = "progress";
    public static final String EXTRA_OUTPUT_PATH = "output_path";
    public static final String CANCEL_EXPORT_SIGNAL = "cancel_export_signal";
    private static final String CHANNEL_ID = "export_service_channel";
    private static final int BROADCAST_DELAY = 100;

    public static final Set<ExportStatus> FINISH_STATUSES = Set.of(
            ExportStatus.DONE,
            ExportStatus.CANCELED,
            ExportStatus.ERROR
    );

    private File outputFile;
    private ExportService exportService;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String channelName = getResources().getString(R.string.exporting);
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, channelName,
                NotificationManager.IMPORTANCE_NONE);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID).build();

        int type = 0;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            type = ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC;
        }

        File outputDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS);

        outputFile = getOutputFile(outputDir.getPath());
        exportService = new ExportService(getApplicationContext(),
                DatabaseProvider.getInstance(this), outputFile);

        Thread thread = new Thread(this);

        thread.start();
        startForeground(startId, notification, type);

        return START_STICKY;
    }

    @Override
    public void run() {
        registerCancelSignalReceiver();
        broadcastOutputPath(outputFile.getPath());
        exportService.doExport();

        try {
            broadcastLoop();
        } catch (InterruptedException e) {
            Log.e(TAG, "Broadcast loop interrupted", e);
        }

        // Delete output file
        //outputFile.delete();

        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    private void broadcastLoop() throws InterruptedException {
        ExportStatus status;
        int progress;

        do {
            status = exportService.getStatus();
            progress = exportService.calculateProgress();

            Intent broadcast = new Intent(BROADCAST_ACTION)
                    .putExtra(EXTRA_STATUS, status)
                    .putExtra(EXTRA_PROGRESS, progress);

            LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
            Thread.sleep(BROADCAST_DELAY);
        } while (status != null && !FINISH_STATUSES.contains(status));
    }

    private void broadcastOutputPath(String outputPath) {
        Intent broadcast = new Intent(BROADCAST_ACTION).putExtra(EXTRA_OUTPUT_PATH, outputPath);
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
    }

    private void registerCancelSignalReceiver() {
        LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                exportService.cancel();
            }
        }, new IntentFilter(CANCEL_EXPORT_SIGNAL));
    }

    private File getOutputFile(String dirPath) {
        LocalDateTime now = LocalDateTime.now();
        String nowStr = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));

        String filename = "nsr_export_" + nowStr + ".notesr.bak";
        Path outputPath = Paths.get(dirPath, filename);

        return new File(outputPath.toUri());
    }
}
