package com.peew.notesr.activity.data;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.ActionBar;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.peew.notesr.App;
import com.peew.notesr.R;
import com.peew.notesr.activity.ExtendedAppCompatActivity;
import com.peew.notesr.activity.notes.NotesListActivity;
import com.peew.notesr.db.notes.table.FilesInfoTable;
import com.peew.notesr.db.notes.table.NotesTable;
import com.peew.notesr.service.ExportService;

public class ExportActivity extends ExtendedAppCompatActivity {

    private ActionBar actionBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export);

        actionBar = getSupportActionBar();
        assert actionBar != null;

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(dataReceiver(), new IntentFilter("ExportDataBroadcast"));

        if (App.getContext().serviceRunning(ExportService.class)) {
            actionBar.setTitle(getString(R.string.exporting));
            disableBackButton();
        } else {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(getString(R.string.export));
        }

        TextView notesCountLabel = findViewById(R.id.notes_count_label);
        TextView filesCountLabel = findViewById(R.id.files_count_label);

        long notesCount = getNotesCount();
        long filesCount = getFilesCount();

        notesCountLabel.setText(String.format(getString(R.string.d_notes), notesCount));
        filesCountLabel.setText(String.format(getString(R.string.d_files), filesCount));

        Button startStopButton = findViewById(R.id.start_stop_export_button);
        startStopButton.setOnClickListener(startStopButtonOnClick());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private View.OnClickListener startStopButtonOnClick() {
        return view -> {
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setTitle(getString(R.string.exporting));

            disableBackButton();
            startForegroundService(new Intent(this, ExportService.class));

            view.setEnabled(false);
        };
    }

    private BroadcastReceiver dataReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int progress = intent.getIntExtra("progress", 0);

                String status = intent.getStringExtra("status");
                String outputPath = intent.getStringExtra("outputPath");

                updateViews(progress, status, outputPath);

                if (progress == 100) {
                    finishExporting();
                }
            }
        };
    }

    private void updateViews(int progress, String status, String outputPath) {
        ProgressBar progressBar = findViewById(R.id.export_progress_bar);
        progressBar.setProgress(progress);

        String progressStr = progress + "%";

        TextView percentageLabel = findViewById(R.id.export_percentage_label);
        TextView statusLabel = findViewById(R.id.export_status_label);
        TextView outputPathLabel = findViewById(R.id.export_output_path_label);

        makeViewVisible(percentageLabel);
        makeViewVisible(statusLabel);
        makeViewVisible(outputPathLabel);

        percentageLabel.setText(progressStr);
        statusLabel.setText(status);
        outputPathLabel.setText(outputPath);
    }

    private void finishExporting() {
        showToastMessage(getString(R.string.exported), Toast.LENGTH_LONG);
        startActivity(new Intent(getApplicationContext(), NotesListActivity.class));

        finish();
    }

    private void makeViewVisible(View view) {
        if (view.getVisibility() == View.INVISIBLE) {
            view.setVisibility(View.VISIBLE);
        }
    }

    private long getNotesCount() {
        return App.getAppContainer().getNotesDB().getTable(NotesTable.class).getRowsCount();
    }

    private long getFilesCount() {
        return App.getAppContainer().getNotesDB().getTable(FilesInfoTable.class).getRowsCount();
    }
}