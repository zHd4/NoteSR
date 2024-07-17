package com.peew.notesr.activity.data;

import android.widget.TextView;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import com.peew.notesr.App;
import com.peew.notesr.R;
import com.peew.notesr.db.notes.table.FilesInfoTable;
import com.peew.notesr.db.notes.table.NotesTable;

public class ExportActivity extends AppCompatActivity {

    private ActionBar actionBar;

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
    }

    private long getNotesCount() {
        return App.getAppContainer().getNotesDB().getTable(NotesTable.class).getRowsCount();
    }

    private long getFilesCount() {
        return App.getAppContainer().getNotesDB().getTable(FilesInfoTable.class).getRowsCount();
    }
}