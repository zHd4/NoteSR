package app.notesr.data;

import static app.notesr.util.ActivityUtils.disableBackButton;
import static app.notesr.util.ActivityUtils.showToastMessage;

import android.content.BroadcastReceiver;
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
import app.notesr.note.NoteListActivity;
import app.notesr.db.notes.dao.FileInfoDao;
import app.notesr.db.notes.dao.NoteDao;
import app.notesr.service.android.ExportAndroidService;

public class ExportActivity extends ActivityBase {

    private ActionBar actionBar;
    private Button startStopButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export);

        actionBar = getSupportActionBar();
        assert actionBar != null;

        LocalBroadcastManager.getInstance(this).registerReceiver(dataReceiver(),
                new IntentFilter(ExportAndroidService.EXPORT_DATA_BROADCAST));

        startStopButton = findViewById(R.id.startStopExportButton);
        startStopButton.setOnClickListener(startStopButtonOnClick());

        if (exportRunning()) {
            actionBar.setTitle(getString(R.string.exporting));

            disableBackButton(this);
            setCancelButton();
        } else {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(getString(R.string.export));
        }

        TextView notesCountLabel = findViewById(R.id.notesCountLabel);
        TextView filesCountLabel = findViewById(R.id.filesCountLabel);

        long notesCount = getNotesCount();
        long filesCount = getFilesCount();

        notesCountLabel.setText(String.format(getString(R.string.d_notes), notesCount));
        filesCountLabel.setText(String.format(getString(R.string.d_files), filesCount));
    }

    private View.OnClickListener startStopButtonOnClick() {
        return view -> {
            if (getNotesCount() == 0) {
                showToastMessage(this, getString(R.string.no_notes), Toast.LENGTH_SHORT);
                return;
            }

            if (!exportRunning()) {
                actionBar.setDisplayHomeAsUpEnabled(false);
                actionBar.setTitle(getString(R.string.exporting));

                disableBackButton(this);
                startForegroundService(new Intent(this, ExportAndroidService.class));

                setCancelButton();
            } else {
                view.setEnabled(false);
                cancelExport();
            }
        };
    }

    private BroadcastReceiver dataReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int progress = intent.getIntExtra("progress", 0);
                boolean canceled = intent.getBooleanExtra("canceled", false);

                String status = intent.getStringExtra("status");
                String outputPath = intent.getStringExtra("outputPath");

                updateViews(progress, status, outputPath);

                if (progress == 100) {
                    finishExporting(false);
                }

                if (canceled) {
                    finishExporting(true);
                }
            }
        };
    }

    private void updateViews(int progress, String status, String outputPath) {
        ProgressBar progressBar = findViewById(R.id.exportProgressBar);
        progressBar.setProgress(progress);

        String progressStr = progress + "%";

        TextView percentageLabel = findViewById(R.id.exportPercentageLabel);
        TextView statusLabel = findViewById(R.id.exportStatusLabel);
        TextView outputPathLabel = findViewById(R.id.exportOutputPathLabel);

        makeViewVisible(percentageLabel);
        makeViewVisible(statusLabel);
        makeViewVisible(outputPathLabel);

        percentageLabel.setText(progressStr);
        statusLabel.setText(status);

        outputPathLabel.setText(String.format("%s\n%s", getString(R.string.saving_in), outputPath));
    }

    private void setCancelButton() {
        startStopButton.setText(getString(R.string.cancel));
        startStopButton.setTextColor(getColor(android.R.color.holo_red_light));
    }

    private void finishExporting(boolean canceled) {
        if (!canceled) {
            showToastMessage(this, getString(R.string.exported), Toast.LENGTH_LONG);
        }

        startActivity(new Intent(getApplicationContext(), NoteListActivity.class));
        finish();
    }

    private void makeViewVisible(View view) {
        if (view.getVisibility() == View.INVISIBLE) {
            view.setVisibility(View.VISIBLE);
        }
    }

    private boolean exportRunning() {
        return App.getContext().isServiceRunning(ExportAndroidService.class);
    }

    private void cancelExport() {
        LocalBroadcastManager.getInstance(this)
                .sendBroadcast(new Intent(ExportAndroidService.CANCEL_EXPORT_SIGNAL));
    }

    private long getNotesCount() {
        return App.getAppContainer().getNotesDB().getDao(NoteDao.class).getRowsCount();
    }

    private long getFilesCount() {
        return App.getAppContainer().getNotesDB().getDao(FileInfoDao.class).getRowsCount();
    }
}