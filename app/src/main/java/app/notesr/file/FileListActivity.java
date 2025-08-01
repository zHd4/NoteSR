package app.notesr.file;

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
import app.notesr.App;
import app.notesr.R;
import app.notesr.ActivityBase;
import app.notesr.note.OpenNoteActivity;
import app.notesr.service.note.NoteService;
import app.notesr.model.FileInfo;
import app.notesr.model.Note;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileListActivity extends ActivityBase {
    private final Map<Long, String> filesIdsMap = new HashMap<>();

    private Note note;
    private boolean noteModified;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_list);

        String noteId = getIntent().getStringExtra("noteId");

        if (noteId == null) {
            throw new RuntimeException("Note id didn't provided");
        }

        noteModified = getIntent().getBooleanExtra("modified", false);
        note = getNotesManager().get(noteId);

        if (note == null) {
            throw new RuntimeException("Note with id " + noteId + " not found");
        }

        configureActionBar();
        loadFiles();

        FloatingActionButton addFileButton = findViewById(R.id.addFileButton);

        addFileButton.setOnClickListener(view -> {
            Intent intent = new Intent(App.getContext(), AddFileActivity.class);

            intent.putExtra("noteId", noteId);
            startActivity(intent);
        });

        ListView filesListView = findViewById(R.id.filesListView);
        filesListView.setOnItemClickListener(new OpenFileOnClick(this, filesIdsMap));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (noteModified) {
                Intent intent = new Intent(App.getContext(), OpenNoteActivity.class)
                        .putExtra("noteId", note.getId())
                        .putExtra("modified", true)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                startActivity(intent);
            } else {
                finish();
            }

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void configureActionBar() {
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;

        long filesCount = App.getAppContainer().getFileService().getFilesCount(note.getId());

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle("(" + filesCount + ") Files of: " + note.getName());
    }

    private void loadFiles() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
        builder.setView(R.layout.progress_dialog_loading).setCancelable(false);

        AlertDialog progressDialog = builder.create();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            handler.post(progressDialog::show);

            List<FileInfo> filesInfo = App.getAppContainer()
                    .getFileService()
                    .getFilesInfo(note.getId());

            filesInfo.forEach(
                    fileInfo -> filesIdsMap.put(fileInfo.getDecimalId(), fileInfo.getId())
            );

            fillFilesListView(filesInfo);
            progressDialog.dismiss();
        });
    }

    private void fillFilesListView(List<FileInfo> filesInfo) {
        ListView filesView = findViewById(R.id.filesListView);
        TextView missingFilesLabel = findViewById(R.id.missingFilesLabel);

        if (!filesInfo.isEmpty()) {
            missingFilesLabel.setVisibility(View.INVISIBLE);
            FileListAdapter adapter = new FileListAdapter(
                    App.getContext(),
                    R.layout.files_list_item,
                    filesInfo);

            runOnUiThread(() -> filesView.setAdapter(adapter));
        }
    }

    private NoteService getNotesManager() {
        return App.getAppContainer().getNoteService();
    }
}