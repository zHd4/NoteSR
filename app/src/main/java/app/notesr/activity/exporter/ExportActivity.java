package app.notesr.activity.exporter;

import static java.util.concurrent.Executors.newSingleThreadExecutor;

import static app.notesr.core.util.ActivityUtils.disableBackButton;
import static app.notesr.core.util.ActivityUtils.showToastMessage;
import static app.notesr.core.util.KeyUtils.getSecretKeyFromSecrets;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.ActionBar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import app.notesr.App;
import app.notesr.R;
import app.notesr.ActivityBase;
import app.notesr.core.security.crypto.AesCryptor;
import app.notesr.core.security.crypto.AesGcmCryptor;
import app.notesr.core.security.crypto.CryptoManagerProvider;
import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.core.util.FilesUtils;
import app.notesr.data.AppDatabase;
import app.notesr.data.DatabaseProvider;
import app.notesr.service.exporter.ExportAndroidService;
import app.notesr.service.exporter.ExportStatus;
import app.notesr.service.file.FileService;
import app.notesr.activity.note.NotesListActivity;
import app.notesr.service.note.NoteService;

public final class ExportActivity extends ActivityBase {

    private NoteService noteService;
    private FileService fileService;
    private ActionBar actionBar;
    private Button startStopButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export);

        Context context = getApplicationContext();
        AppDatabase db = DatabaseProvider.getInstance(this);
        CryptoSecrets secrets = CryptoManagerProvider.getInstance(context).getSecrets();
        AesCryptor cryptor = new AesGcmCryptor(getSecretKeyFromSecrets(secrets));

        noteService = new NoteService(db);
        fileService = new FileService(context, db, cryptor, new FilesUtils());

        actionBar = getSupportActionBar();

        ExportBroadcastReceiver dataReceiver = new ExportBroadcastReceiver(
                this::onOutputPathReceived,
                this::onExportRunning,
                this::onExportComplete
        );

        LocalBroadcastManager.getInstance(this).registerReceiver(dataReceiver,
                new IntentFilter(ExportAndroidService.BROADCAST_ACTION));

        startStopButton = findViewById(R.id.start_stop_export_button);
        startStopButton.setOnClickListener(startStopButtonOnClick());

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
    }

    private View.OnClickListener startStopButtonOnClick() {
        return view -> newSingleThreadExecutor().execute(() -> {
            if (noteService.getCount() == 0) {
                runOnUiThread(() -> {
                    String messageText = getString(R.string.no_notes);
                    showToastMessage(this, messageText, Toast.LENGTH_SHORT);
                });

                return;
            }

            runOnUiThread(() -> {
                if (!isExportRunning()) {
                    actionBar.setDisplayHomeAsUpEnabled(false);
                    actionBar.setTitle(getString(R.string.exporting));

                    disableBackButton(this);
                    startForegroundService(new Intent(this,
                            ExportAndroidService.class));

                    setCancelButton();
                } else {
                    view.setEnabled(false);
                    cancelExport();
                }
            });
        });
    }

    private void onOutputPathReceived(String outputPath) {
        TextView outputPathView = findViewById(R.id.export_output_path_view);

        outputPathView.setVisibility(View.VISIBLE);
        outputPathView.setText(String.format("%s\n%s", getString(R.string.saving_in), outputPath));
    }

    private void onExportRunning(ExportStatus status, int progress) {
        ProgressBar progressBar = findViewById(R.id.export_progress_bar);
        progressBar.setProgress(progress);

        TextView percentageView = findViewById(R.id.export_percentage_view);
        String progressStr = progress + "%";

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
        if (status == ExportStatus.DONE) {
            showToastMessage(this, getString(R.string.exported), Toast.LENGTH_LONG);
        } else if (status == ExportStatus.ERROR) {
            showToastMessage(this, getString(R.string.export_failed), Toast.LENGTH_LONG);
        }

        startActivity(new Intent(getApplicationContext(), NotesListActivity.class));
        finish();
    }

    private void setCancelButton() {
        startStopButton.setText(getString(R.string.cancel));
        startStopButton.setTextColor(getColor(android.R.color.holo_red_light));
    }

    private boolean isExportRunning() {
        return App.getContext().isServiceRunning(ExportAndroidService.class);
    }

    private void cancelExport() {
        LocalBroadcastManager.getInstance(this)
                .sendBroadcast(new Intent(ExportAndroidService.CANCEL_EXPORT_SIGNAL));
    }
}
