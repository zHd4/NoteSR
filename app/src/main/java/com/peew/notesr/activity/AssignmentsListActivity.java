package com.peew.notesr.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.peew.notesr.App;
import com.peew.notesr.R;
import com.peew.notesr.adapter.FilesListAdapter;
import com.peew.notesr.crypto.FilesCrypt;
import com.peew.notesr.db.notes.tables.FilesTable;
import com.peew.notesr.model.File;

//import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AssignmentsListActivity extends AppCompatActivity {
    private long noteId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assignments_list);

        noteId = getIntent().getLongExtra("note_id", -1);

        if (noteId == -1) {
            throw new RuntimeException("Note id didn't provided");
        }

        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.files);

//        fillTable();
        fillFilesList(findViewById(R.id.files_list_view), findViewById(R.id.missing_files_label));
    }

    private void fillFilesList(ListView filesView, TextView missingFilesLabel) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
        builder.setView(R.layout.progress_dialog_loading).setCancelable(false);

        AlertDialog progressDialog = builder.create();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            handler.post(progressDialog::show);

            FilesTable filesTable = App.getAppContainer().getNotesDatabase().getFilesTable();
            List<File> files = FilesCrypt.decrypt(filesTable.getByNoteId(noteId));

            if (!files.isEmpty()) {
                missingFilesLabel.setVisibility(View.INVISIBLE);
                FilesListAdapter adapter = new FilesListAdapter(
                        App.getContext(),
                        R.layout.files_list_item,
                        files);

                runOnUiThread(() -> filesView.setAdapter(adapter));
            }

            progressDialog.dismiss();
        });
    }

//    private void fillTable() {
//        FilesTable filesTable = App.getAppContainer().getNotesDatabase().getFilesTable();
//
//        if (filesTable.getByNoteId(1).isEmpty()) {
//            File file1 = new File("file1.txt", "foo".getBytes(StandardCharsets.UTF_8));
//            File file2 = new File("file2.txt", "bar".getBytes(StandardCharsets.UTF_8));
//            File file3 = new File("file3.txt", "baz".getBytes(StandardCharsets.UTF_8));
//
//            file1.setNoteId(1L);
//            file2.setNoteId(1L);
//            file3.setNoteId(1L);
//
//            filesTable.save(FilesCrypt.encrypt(file1));
//            filesTable.save(FilesCrypt.encrypt(file2));
//            filesTable.save(FilesCrypt.encrypt(file3));
//        }
//
//        List<File> files = FilesCrypt.decrypt(filesTable.getByNoteId(1L));
//        System.out.println();
//    }
}