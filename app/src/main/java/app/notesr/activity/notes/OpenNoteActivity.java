package app.notesr.activity.notes;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;

import app.notesr.App;
import app.notesr.R;
import app.notesr.activity.ExtendedAppCompatActivity;
import app.notesr.activity.files.AssignmentsListActivity;
import app.notesr.service.FilesService;
import app.notesr.service.NotesService;
import app.notesr.model.Note;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import static androidx.core.view.inputmethod.EditorInfoCompat.IME_FLAG_NO_PERSONALIZED_LEARNING;

public class OpenNoteActivity extends ExtendedAppCompatActivity {

    private static final long MAX_COUNT_IN_BADGE = 9;

    private final Map<Integer, Consumer<?>> menuItemsMap = new HashMap<>();
    private Note note;

    private boolean noteModified;

    /**
     * @noinspection DataFlowIssue
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_note);

        String noteId = getIntent().getStringExtra("noteId");
        note = getNotesManager().get(noteId);

        noteModified = getIntent().getBooleanExtra("modified", false);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        EditText nameField = findViewById(R.id.noteNameField);
        EditText textField = findViewById(R.id.noteTextField);

        nameField.setImeOptions(IME_FLAG_NO_PERSONALIZED_LEARNING);
        textField.setImeOptions(IME_FLAG_NO_PERSONALIZED_LEARNING);

        if (note != null) {
            nameField.setText(note.getName());
            textField.setText(note.getText());

            actionBar.setTitle(getResources().getString(R.string.edit_note));
        } else {
            actionBar.setTitle(getResources().getString(R.string.new_note));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        EditText nameField = findViewById(R.id.noteNameField);
        EditText textField = findViewById(R.id.noteTextField);

        getMenuInflater().inflate(R.menu.menu_open_note, menu);

        MenuItem saveNoteButton = menu.findItem(R.id.saveNoteButton);
        MenuItem openAssignmentsButton = menu.findItem(R.id.openAssignmentsButton);
        MenuItem deleteNoteButton = menu.findItem(R.id.deleteNoteButton);

        menuItemsMap.put(saveNoteButton.getItemId(),
                action -> saveNoteOnClick(nameField, textField));

        if (note != null) {
            menuItemsMap.put(openAssignmentsButton.getItemId(),
                    action -> openAssignmentsOnClick());

            menuItemsMap.put(deleteNoteButton.getItemId(),
                    action -> deleteNoteOnClick());

            FilesService filesService = App.getAppContainer().getFilesService();
            long filesCount = filesService.getFilesCount(note.getId());

            if (filesCount > 0) {
                openAssignmentsButton.setActionView(R.layout.button_open_assignments);

                View view = Objects.requireNonNull(openAssignmentsButton.getActionView());
                TextView badge = view.findViewById(R.id.attachedAssignmentsCountBadge);

                String badgeText = filesCount <= MAX_COUNT_IN_BADGE
                        ? String.valueOf(filesCount)
                        : MAX_COUNT_IN_BADGE + "+";

                badge.setText(badgeText);
                badge.setVisibility(View.VISIBLE);

                view.setOnClickListener(v -> openAssignmentsOnClick());
            }
        } else {
            disableMenuItem(openAssignmentsButton);
            disableMenuItem(deleteNoteButton);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            if (noteModified) {
                Intent intent = new Intent(App.getContext(), NotesListActivity.class);
                startActivity(intent);
            } else {
                finish();
            }
            return true;
        }

        Consumer<?> action = menuItemsMap.get(id);

        if (action != null) {
            action.accept(null);
        }

        return super.onOptionsItemSelected(item);
    }

    private void saveNoteOnClick(EditText nameField, EditText textField) {
        String name = nameField.getText().toString();
        String text = textField.getText().toString();

        if (!name.isBlank() && !text.isBlank()) {
            LocalDateTime now = LocalDateTime.now();

            if (note != null) {
                note.setName(name);
                note.setText(text);
            } else {
                note = new Note(name, text);
            }

            note.setUpdatedAt(now);

            getNotesManager().save(note);
            startActivity(new Intent(App.getContext(), NotesListActivity.class));
        }
    }

    private void deleteNoteOnClick() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);

        builder.setView(R.layout.dialog_action_cannot_be_undo);
        builder.setTitle(R.string.warning);

        builder.setPositiveButton(R.string.delete, deleteNoteDialogOnClick());
        builder.setNegativeButton(R.string.no, deleteNoteDialogOnClick());

        builder.create().show();
    }

    private void openAssignmentsOnClick() {
        Intent intent = new Intent(App.getContext(), AssignmentsListActivity.class);

        intent.putExtra("noteId", note.getId());
        startActivity(intent);
    }

    private DialogInterface.OnClickListener deleteNoteDialogOnClick() {
        return (dialog, result) -> {
            if (result == DialogInterface.BUTTON_POSITIVE) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this,
                        R.style.AlertDialogTheme);

                builder.setView(R.layout.progress_dialog_loading).setCancelable(false);

                AlertDialog progressDialog = builder.create();
                ExecutorService executor = Executors.newSingleThreadExecutor();

                executor.execute(() -> {
                    runOnUiThread(progressDialog::show);
                    getNotesManager().delete(note.getId());

                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        startActivity(new Intent(App.getContext(), NotesListActivity.class));
                    });
                });
            }
        };
    }

    private void disableMenuItem(MenuItem item) {
        item.setEnabled(false);
        item.setVisible(false);
    }

    private NotesService getNotesManager() {
        return App.getAppContainer().getNotesService();
    }
}
