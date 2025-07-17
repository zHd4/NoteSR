package app.notesr.service.android;

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

import app.notesr.App;
import app.notesr.R;
import app.notesr.db.notes.NotesDb;
import app.notesr.db.notes.dao.DataBlockDao;
import app.notesr.db.notes.dao.FileInfoDao;
import app.notesr.db.notes.dao.NoteDao;
import app.notesr.service.data.exporter.ExportResult;
import app.notesr.service.data.exporter.ExportService;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ExportAndroidService extends Service implements Runnable {
    private static final String TAG = ExportAndroidService.class.getName();
    public static final String EXPORT_DATA_BROADCAST = "export_data_broadcast";
    public static final String CANCEL_EXPORT_SIGNAL = "cancel_export_signal";
    private static final String CHANNEL_ID = "export_service_channel";
    private static final int BROADCAST_DELAY = 100;

    private Thread cancelThread;
    private File outputFile;
    private app.notesr.service.data.exporter.ExportService exportService;

    @Override
    public void onCreate() {
        Thread thread = new Thread(this);
        thread.start();
    }

    @Override
    public void run() {
        File outputDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        outputFile = getOutputFile(outputDir.getPath());
        exportService = getExportService(getApplicationContext(), outputFile,
                App.getAppContainer().getNotesDB());

        exportService.start();

        registerCancelSignalReceiver();
        broadcastLoop();

        // Delete file
        //outputFile.delete();

        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    private void broadcastLoop() {
        String status = exportService.getStatus();
        String outputPath = outputFile.getAbsolutePath();

        int progress = exportService.calculateProgress();

        while (exportService.getResult() == ExportResult.NONE) {
            try {
                status = exportService.getStatus();
                progress = exportService.calculateProgress();

                sendBroadcastData(progress, status, outputPath, false);

                Thread.sleep(BROADCAST_DELAY);
            } catch (InterruptedException e) {
                Log.e(TAG, "Thread interrupted", e);
            }
        }

        if (exportService.getResult() == ExportResult.FINISHED_SUCCESSFULLY) {
            sendBroadcastData(100, status, outputPath, false);
        } else if (exportService.getResult() == ExportResult.CANCELED) {
            sendBroadcastData(progress, status, outputPath, true);
        }
    }

    private void sendBroadcastData(int progress, String status, String outputPath, boolean canceled) {
        Intent intent = new Intent(EXPORT_DATA_BROADCAST)
                .putExtra("progress", progress)
                .putExtra("status", status)
                .putExtra("outputPath", outputPath)
                .putExtra("canceled", canceled);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void registerCancelSignalReceiver() {
        Runnable cancel = () -> {
            if (exportService == null) {
                throw new IllegalStateException("Service has not been started");
            }

            exportService.cancel();
        };

        LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if (cancelThread == null) {
                            cancelThread = new Thread(cancel);
                            cancelThread.start();
                        }
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

    private ExportService getExportService(Context context, File outputFile, NotesDb notesDb) {
        NoteDao noteDao = notesDb.getDao(NoteDao.class);
        FileInfoDao fileInfoDao = notesDb.getDao(FileInfoDao.class);
        DataBlockDao dataBlockDao = notesDb.getDao(DataBlockDao.class);

        return new ExportService(context, outputFile, noteDao, fileInfoDao, dataBlockDao);
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

        startForeground(startId, notification, type);

        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
