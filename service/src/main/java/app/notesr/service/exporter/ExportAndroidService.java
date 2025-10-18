package app.notesr.service.exporter;

import static java.util.Objects.requireNonNull;
import static app.notesr.core.util.KeyUtils.getSecretKeyFromSecrets;

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

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import app.notesr.service.R;
import app.notesr.core.security.crypto.AesCryptor;
import app.notesr.core.security.crypto.AesGcmCryptor;
import app.notesr.core.security.crypto.CryptoManagerProvider;
import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.core.util.FilesUtils;
import app.notesr.data.AppDatabase;
import app.notesr.data.DatabaseProvider;
import app.notesr.service.file.FileService;
import app.notesr.service.note.NoteService;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.function.BiConsumer;

public final class ExportAndroidService extends Service implements Runnable {
    public static final String BROADCAST_ACTION = "export_data_broadcast";
    public static final String EXTRA_STATUS = "status";
    public static final String EXTRA_PROGRESS = "progress";
    public static final String EXTRA_OUTPUT_PATH = "output_path";
    public static final String CANCEL_EXPORT_SIGNAL = "cancel_export_signal";
    private static final String CHANNEL_ID = "export_service_channel";

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

        String appVersion = intent.getStringExtra("app_version");
        requireNonNull(appVersion, "App version not provided");

        File outputDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS);

        outputFile = getOutputFile(outputDir.getPath());
        exportService = getExportService(outputFile, this::onUpdateCallback, appVersion);

        Thread thread = new Thread(this);

        thread.start();
        startForeground(startId, notification, type);

        return START_STICKY;
    }

    private void onUpdateCallback(Integer progress, ExportStatus status) {
        Intent broadcast = new Intent(BROADCAST_ACTION)
                .putExtra(EXTRA_STATUS, status)
                .putExtra(EXTRA_PROGRESS, progress);

        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast);
    }

    @Override
    public void run() {
        registerCancelSignalReceiver();
        broadcastOutputPath(outputFile.getPath());
        exportService.doExport();

        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
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

    private ExportService getExportService(
            File backupOutputFile,
            BiConsumer<Integer, ExportStatus> updateCallback,
            String appVersion) {

        Context context = getApplicationContext();

        AppDatabase db = DatabaseProvider.getInstance(this);
        NoteService noteService = new NoteService(db);

        CryptoSecrets secrets = CryptoManagerProvider.getInstance(context).getSecrets();
        AesCryptor cryptor = new AesGcmCryptor(getSecretKeyFromSecrets(secrets));

        FileService fileService = new FileService(context, db, cryptor, new FilesUtils());
        ExportStatusHolder statusHolder = new ExportStatusHolder(updateCallback);

        return new ExportService(
                secrets,
                backupOutputFile,
                appVersion,
                context,
                db,
                noteService,
                fileService,
                statusHolder
        );
    }

    private File getOutputFile(String dirPath) {
        LocalDateTime now = LocalDateTime.now();
        String nowStr = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));

        String filename = "nsr_export_" + nowStr + ".notesr.bak";
        Path outputPath = Paths.get(dirPath, filename);

        return new File(outputPath.toUri());
    }
}
