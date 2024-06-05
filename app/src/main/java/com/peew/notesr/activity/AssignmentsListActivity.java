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

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AssignmentsListActivity extends AppCompatActivity {
//    private static final Set<String> FILES_TYPES = Set.of(
//            "text/txt", "text/log",
//            "image/gif", "image/png",
//            "image/jpg", "image/jpeg",
//            "image/svg", "image/bmp",
//            "video/mp4", "video/avi",
//            "video/mov", "video/webm",
//            "audio/mp3", "audio/wav",
//            "audio/ogg", "audio/m4a"
//    );
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
        loadFiles();
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
            List<File> filesList = FilesCrypt.decrypt(filesTable.getByNoteId(noteId));

            fillFilesListView(filesList);
            progressDialog.dismiss();
        });
    }

    private void fillFilesListView(List<File> files) {
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

//    private String getFileType(File file) {
//        String name = file.getName();
//        String extension = new LinkedList<>(Arrays.asList(name.split("\\."))).getLast();
//
//        return FILES_TYPES.stream()
//                .map(type -> type.split("/"))
//                .filter(type -> type[1].equals(extension))
//                .map(type -> type[0])
//                .findFirst()
//                .orElse(null);
//    }

//    private void fillTable() {
//        FilesTable filesTable = App.getAppContainer().getNotesDatabase().getFilesTable();
//
//        if (filesTable.getByNoteId(1).isEmpty()) {
//            File file1 = new File("file1.txt", "text", "foo".getBytes(StandardCharsets.UTF_8));
//            File file2 = new File("file2.txt", "text", "bar".getBytes(StandardCharsets.UTF_8));
//            File file3 = new File("file3.txt", "text", "baz".getBytes(StandardCharsets.UTF_8));
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