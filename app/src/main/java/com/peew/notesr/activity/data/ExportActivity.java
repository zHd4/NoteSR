package com.peew.notesr.activity.data;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.ActionBar;
import com.peew.notesr.App;
import com.peew.notesr.R;
import com.peew.notesr.activity.ExtendedAppCompatActivity;
import com.peew.notesr.activity.notes.NotesListActivity;
import com.peew.notesr.db.notes.table.FilesInfoTable;
import com.peew.notesr.db.notes.table.NotesTable;
import com.peew.notesr.service.ExportService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExportActivity extends ExtendedAppCompatActivity {


    private ActionBar actionBar;
    private ExportService exportService;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export);

        exportService = ExportService.getInstance();

        actionBar = getSupportActionBar();
        assert actionBar != null;

        if (exportService == null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(getString(R.string.export));
        } else {
            actionBar.setTitle(getString(R.string.exporting));
            disableBackButton();
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
            startActivityWorker();
        };
    }

    private void startActivityWorker() {
        int delay = 500;

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(activityWorker(delay));
    }

    private Runnable activityWorker(int delay) {
        ProgressBar progressBar = findViewById(R.id.export_progress_bar);

        return () -> {
            while (true) {
                int progress = exportService.getProgress();

                runOnUiThread(() -> progressBar.setProgress(progress));

                if (progress == 100) {
                    break;
                }

                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            runOnUiThread(onActivityWorkerFinished());
        };
    }

    private Runnable onActivityWorkerFinished() {
        return () -> {
            String path = exportService.getOutputPath();
            String message = String.format(getString(R.string.saved_to), path);

            showToastMessage(message, Toast.LENGTH_LONG);
            startActivity(new Intent(this, NotesListActivity.class));

            finish();
        };
    }


    private long getNotesCount() {
        return App.getAppContainer().getNotesDB().getTable(NotesTable.class).getRowsCount();
    }

    private long getFilesCount() {
        return App.getAppContainer().getNotesDB().getTable(FilesInfoTable.class).getRowsCount();
    }
}