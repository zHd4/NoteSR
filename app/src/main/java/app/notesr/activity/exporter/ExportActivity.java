/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.exporter;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

import static app.notesr.core.util.ActivityUtils.disableBackButton;
import static app.notesr.core.util.ActivityUtils.showToastMessage;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import app.notesr.R;
import app.notesr.activity.ActivityBase;
import app.notesr.core.security.crypto.AesCryptor;
import app.notesr.core.security.crypto.AesCryptorFactory;
import app.notesr.core.util.FileExifDataResolver;
import app.notesr.core.util.FilesUtils;
import app.notesr.data.DatabaseProvider;
import app.notesr.service.AndroidServiceRegistry;
import app.notesr.service.exporter.ExportAndroidService;
import app.notesr.service.exporter.ExportAndroidServiceStarter;
import app.notesr.service.exporter.ExportStatus;
import app.notesr.service.file.FileService;
import app.notesr.activity.note.list.NotesListActivity;
import app.notesr.service.note.NoteService;
import app.notesr.service.security.AppSecurityService;
import app.notesr.util.VersionFetcherImpl;

public final class ExportActivity extends ActivityBase {

    private static final String TAG = ExportActivity.class.getSimpleName();

    private NoteService noteService;
    private FileService fileService;

    private ActionBar actionBar;
    private Button startStopButton;
    private TextView outputFileNameView;

    private ActivityResultLauncher<String> exportDestinationPicker;

    private boolean isExportCompleted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isFinishing()) {
            return;
        }

        setContentView(R.layout.activity_export);
        applyInsets(findViewById(R.id.main));

        var context = getApplicationContext();
        var db = DatabaseProvider.getInstance(this);

        AesCryptor cryptor = AesCryptorFactory.createAesGcmCryptor(
                new AppSecurityService(context).getActualSecrets());

        noteService = new NoteService(db);
        fileService = new FileService(context, db, cryptor, new FilesUtils());

        actionBar = getSupportActionBar();

        var serviceBroadcastReceiver = new ExportBroadcastReceiver(
                this::onExportRunning,
                this::onExportComplete
        );

        LocalBroadcastManager.getInstance(this).registerReceiver(serviceBroadcastReceiver,
                new IntentFilter(ExportAndroidService.BROADCAST_ACTION));

        startStopButton = findViewById(R.id.start_stop_export_button);
        startStopButton.setOnClickListener(startStopButtonOnClick());

        outputFileNameView = findViewById(R.id.output_file_name_view);

        if (isExportRunning()) {
            actionBar.setTitle(getString(R.string.exporting));

            disableBackButton(this);
            setCancelButton();
        } else {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(getString(R.string.export));
        }

        TextView notesCountLabel = findViewById(R.id.notes_count_view);
        TextView filesCountLabel = findViewById(R.id.files_count_label);

        newSingleThreadExecutor().execute(() -> {
            long notesCount = noteService.getCount();
            long filesCount = fileService.getFilesCount();

            runOnUiThread(() -> {
                notesCountLabel.setText(String.format(getString(R.string.d_notes), notesCount));
                filesCountLabel.setText(String.format(getString(R.string.d_files), filesCount));
            });
        });

        exportDestinationPicker = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("application/octet-stream"),
                this::startExporting
        );
    }

    private View.OnClickListener startStopButtonOnClick() {
        return view -> newSingleThreadExecutor().execute(() -> {
            if (noteService.getCount() == 0) {
                runOnUiThread(() -> {
                    var messageText = getString(R.string.no_notes);
                    showToastMessage(this, messageText, Toast.LENGTH_SHORT);
                });

                return;
            }

            runOnUiThread(() -> {
                if (!isExportRunning()) {
                    pickSaveLocation();
                } else {
                    view.setEnabled(false);
                    cancelExport();
                }
            });
        });
    }

    private void pickSaveLocation() {
        var now = LocalDateTime.now();
        var nowStr = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        var filename = "nsr_export_" + nowStr + ".notesr.bak";

        exportDestinationPicker.launch(filename);
    }

    private void startExporting(Uri uri) {
        if (uri != null) {
            startExportService(uri);

            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setTitle(getString(R.string.exporting));

            disableBackButton(this);
            setCancelButton();

            String outputFileName = new FileExifDataResolver(this, new FilesUtils(), uri)
                    .getFileName();

            outputFileNameView.setVisibility(View.VISIBLE);
            outputFileNameView.setText(
                    String.format("%s %s", getString(R.string.saving_in), outputFileName));
        }
    }

    private void startExportService(Uri uri) {
        var versionFetcher = new VersionFetcherImpl();

        try {
            var appVersion = versionFetcher.fetchVersionName(this, false);

            var exportServicePayload = new ExportAndroidServiceStarter.Payload(
                    appVersion, uri.toString());
            new ExportAndroidServiceStarter(exportServicePayload).start(getApplicationContext());
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void onExportRunning(ExportStatus status, int progress) {
        ProgressBar progressBar = findViewById(R.id.export_progress_bar);
        progressBar.setProgress(progress);

        TextView percentageView = findViewById(R.id.export_percentage_view);
        var progressStr = progress + "%";

        percentageView.setText(progressStr);
        percentageView.setVisibility(View.VISIBLE);

        TextView statusView = findViewById(R.id.export_status_view);

        switch (status) {
            case INITIALIZING -> statusView.setText(getString(R.string.initializing));
            case EXPORTING_DATA -> statusView.setText(getString(R.string.exporting_data));
            case ENCRYPTING_DATA -> statusView.setText(getString(R.string.encrypting_data));
            case WIPING_TEMP_DATA -> statusView.setText(getString(R.string.wiping_temp_data));
            case CANCELLING -> statusView.setText(getString(R.string.cancelling));
            default -> throw new IllegalStateException("Unexpected value: " + status);
        }

        statusView.setVisibility(View.VISIBLE);
    }

    private void onExportComplete(ExportStatus status) {
        if (!isExportCompleted) {
            isExportCompleted = true;

            if (status == ExportStatus.DONE) {
                showToastMessage(this, getString(R.string.exported), Toast.LENGTH_LONG);
            } else if (status == ExportStatus.ERROR) {
                showToastMessage(this, getString(R.string.export_failed), Toast.LENGTH_LONG);
            }

            startActivity(new Intent(getApplicationContext(), NotesListActivity.class));
            finish();
        } else {
            Log.i(TAG, "Export already completed, ignoring duplicate completion signal");
        }
    }

    private void setCancelButton() {
        startStopButton.setText(getString(R.string.cancel));
        startStopButton.setTextColor(getColor(android.R.color.holo_red_light));
    }

    private boolean isExportRunning() {
        return AndroidServiceRegistry.getInstance(getApplicationContext())
                .isServiceRunning(ExportAndroidService.class);
    }

    private void cancelExport() {
        LocalBroadcastManager.getInstance(this)
                .sendBroadcast(new Intent(ExportAndroidService.CANCEL_EXPORT_SIGNAL));
    }
}
