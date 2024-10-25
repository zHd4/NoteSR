package app.notesr.activity.files;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import app.notesr.App;
import app.notesr.R;
import app.notesr.activity.ExtendedAppCompatActivity;
import app.notesr.adapter.FilesListAdapter;
import app.notesr.manager.AssignmentsManager;
import app.notesr.manager.NotesManager;
import app.notesr.model.FileInfo;
import app.notesr.model.Note;
import app.notesr.onclick.files.OpenFileOnClick;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AssignmentsListActivity extends ExtendedAppCompatActivity {
    private Note note;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assignments_list);

        long noteId = getIntent().getLongExtra("noteId", -1);

        if (noteId == -1) {
            throw new RuntimeException("Note id didn't provided");
        }

        note = getNotesManager().get(noteId);

        if (note == null) {
            throw new RuntimeException("Note with id " + noteId + " not found");
        }

        configureActionBar();
        loadFiles();

        FloatingActionButton addFileButton = findViewById(R.id.addFileButton);

        addFileButton.setOnClickListener(view -> {
            Intent intent = new Intent(App.getContext(), AddFilesActivity.class);

            intent.putExtra("noteId", noteId);
            startActivity(intent);
        });

        ListView filesListView = findViewById(R.id.filesListView);
        filesListView.setOnItemClickListener(new OpenFileOnClick(this));
    }

    private void configureActionBar() {
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;

        long filesCount = getAssignmentsManager().getFilesCount(note.getId());

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
            fillFilesListView(getAssignmentsManager().getFilesInfo(note.getId()));
            progressDialog.dismiss();
        });
    }

    private void fillFilesListView(List<FileInfo> files) {
        ListView filesView = findViewById(R.id.filesListView);
        TextView missingFilesLabel = findViewById(R.id.missingFilesLabel);

        if (!files.isEmpty()) {
            missingFilesLabel.setVisibility(View.INVISIBLE);
            FilesListAdapter adapter = new FilesListAdapter(
                    App.getContext(),
                    R.layout.files_list_item,
                    files);

            runOnUiThread(() -> filesView.setAdapter(adapter));
        }
    }

    private NotesManager getNotesManager() {
        return App.getAppContainer().getNotesManager();
    }

    private AssignmentsManager getAssignmentsManager() {
        return App.getAppContainer().getAssignmentsManager();
    }
}