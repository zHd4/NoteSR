package com.peew.notesr.activity.data;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.ActionBar;
import com.peew.notesr.App;
import com.peew.notesr.R;
import com.peew.notesr.activity.ExtendedAppCompatActivity;
import com.peew.notesr.db.notes.table.FilesInfoTable;
import com.peew.notesr.db.notes.table.NotesTable;

public class ExportActivity extends ExtendedAppCompatActivity {

    private static boolean running;

    private ActionBar actionBar;

    public static boolean isRunning() {
        return running;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export);

        actionBar = getSupportActionBar();
        assert actionBar != null;

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(getString(R.string.export));

        TextView notesCountLabel = findViewById(R.id.notes_count_label);
        TextView filesCountLabel = findViewById(R.id.files_count_label);

        long notesCount = getNotesCount();
        long filesCount = getFilesCount();

        notesCountLabel.setText(String.format(getString(R.string.d_notes), notesCount));
        filesCountLabel.setText(String.format(getString(R.string.d_files), filesCount));

        Button startStopButton = findViewById(R.id.start_stop_export_button);

        startStopButton.setOnClickListener(view -> {
            actionBar.setDisplayHomeAsUpEnabled(false);
            disableBackButton();
        });
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

    @Override
    public void onPause() {
        running = false;
        super.onPause();
    }

    @Override
    public void onDestroy() {
        running = false;
        super.onDestroy();
    }

    @Override
    public void onResume() {
        running = true;
        super.onResume();
    }

    private long getNotesCount() {
        return App.getAppContainer().getNotesDB().getTable(NotesTable.class).getRowsCount();
    }

    private long getFilesCount() {
        return App.getAppContainer().getNotesDB().getTable(FilesInfoTable.class).getRowsCount();
    }
}