package com.peew.notesr.activity.files;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.peew.notesr.App;
import com.peew.notesr.R;
import com.peew.notesr.activity.AppCompatActivityExtended;
import com.peew.notesr.adapter.FilesListAdapter;
import com.peew.notesr.crypto.FilesCrypt;
import com.peew.notesr.crypto.NotesCrypt;
import com.peew.notesr.db.notes.tables.FilesTable;
import com.peew.notesr.model.EncryptedNote;
import com.peew.notesr.model.FileInfo;
import com.peew.notesr.model.Note;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AssignmentsListActivity extends AppCompatActivityExtended {
    private Note note;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assignments_list);

        long noteId = getIntent().getLongExtra("note_id", -1);

        if (noteId == -1) {
            throw new RuntimeException("Note id didn't provided");
        }

        EncryptedNote encryptedNote = App.getAppContainer()
                .getNotesDatabase()
                .getNotesTable()
                .get(noteId);

        if (encryptedNote != null) {
            note = NotesCrypt.decrypt(encryptedNote);
        } else {
            throw new RuntimeException("Note with id " + noteId + " not found");
        }

        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle("Files of: " + note.getName());

        FloatingActionButton addFileButton = findViewById(R.id.add_file_button);
        addFileButton.setOnClickListener(view -> {
            Intent intent = new Intent(App.getContext(), AddFileActivity.class);

            intent.putExtra("note_id", noteId);
            startActivity(intent);
        });

        loadFiles();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void loadFiles() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
        builder.setView(R.layout.progress_dialog_loading).setCancelable(false);

        AlertDialog progressDialog = builder.create();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            handler.post(progressDialog::show);

            FilesTable filesTable = App.getAppContainer().getNotesDatabase().getFilesTable();
            fillFilesListView(FilesCrypt.decryptInfo(filesTable.getByNoteId(note.getId())));

            progressDialog.dismiss();
        });
    }

    private void fillFilesListView(List<FileInfo> files) {
        ListView filesView = findViewById(R.id.files_list_view);
        TextView missingFilesLabel = findViewById(R.id.missing_files_label);

        if (!files.isEmpty()) {
            missingFilesLabel.setVisibility(View.INVISIBLE);
            FilesListAdapter adapter = new FilesListAdapter(
                    App.getContext(),
                    R.layout.files_list_item,
                    files);

            runOnUiThread(() -> filesView.setAdapter(adapter));
        }
    }
}