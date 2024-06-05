package com.peew.notesr.activity;

import static androidx.core.view.inputmethod.EditorInfoCompat.IME_FLAG_NO_PERSONALIZED_LEARNING;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;

import com.peew.notesr.App;
import com.peew.notesr.R;
import com.peew.notesr.crypto.NotesCrypt;
import com.peew.notesr.db.notes.NotesDatabase;
import com.peew.notesr.db.notes.tables.NotesTable;
import com.peew.notesr.model.EncryptedNote;
import com.peew.notesr.model.Note;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class NoteOpenActivity extends AppCompatActivityExtended {
    private final Map<Integer, Consumer<?>> menuItemsMap = new HashMap<>();
    private Note note;

    /** @noinspection DataFlowIssue*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_open);

        long noteId = getIntent().getLongExtra("note_id", -1);
        note = findNote(noteId);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        EditText nameField = findViewById(R.id.note_name_field);
        EditText textField = findViewById(R.id.note_text_field);

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
        EditText nameField = findViewById(R.id.note_name_field);
        EditText textField = findViewById(R.id.note_text_field);

        getMenuInflater().inflate(R.menu.menu_note_open, menu);

        menuItemsMap.put(R.id.save_note_button, action -> saveNoteOnClick(nameField, textField));
        menuItemsMap.put(R.id.open_assignments_button, action -> openAssignmentsOnClick());

        if (note == null) {
            MenuItem deleteButton = menu.findItem(R.id.delete_note_button);

            deleteButton.setEnabled(false);
            deleteButton.setVisible(false);
        } else {
            menuItemsMap.put(R.id.delete_note_button, action -> deleteNoteOnClick());
        }

        return true;
    }

    /** @noinspection deprecation*/
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        Consumer<?> action = menuItemsMap.get(id);

        if (action != null) {
            action.accept(null);
        } else {
            super.onBackPressed();
        }

        return true;
    }

    private void saveNoteOnClick(EditText nameField, EditText textField) {
        String name = nameField.getText().toString();
        String text = textField.getText().toString();

        if (!name.isBlank() && !text.isBlank()) {
            if (note != null) {
                note.setName(name);
                note.setText(text);
            } else {
                note = new Note(name, text);
            }

            EncryptedNote encryptedNote = NotesCrypt.encrypt(note);
            NotesTable notesTable = getNotesDatabase().getNotesTable();

            notesTable.save(encryptedNote);
            startActivity(new Intent(App.getContext(), MainActivity.class));
        }
    }

    private void deleteNoteOnClick() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);

        builder.setView(R.layout.dialog_delete_note);
        builder.setTitle(R.string.warning);

        builder.setPositiveButton(R.string.delete, deleteNoteDialogOnClick());
        builder.setNegativeButton(R.string.no, deleteNoteDialogOnClick());

        builder.create().show();
    }

    private void openAssignmentsOnClick() {
        Intent intent = new Intent(App.getContext(), AssignmentsListActivity.class);

        intent.putExtra("note_id", note.getId());
        startActivity(intent);
    }

    private DialogInterface.OnClickListener deleteNoteDialogOnClick() {
        return (dialog, result) -> {
            if (result == DialogInterface.BUTTON_POSITIVE) {
                getNotesDatabase().getNotesTable().delete(note.getId());
                startActivity(new Intent(App.getContext(), MainActivity.class));
            }
        };
    }

    private Note findNote(long id) {
        EncryptedNote encryptedNote = getNotesDatabase().getNotesTable().get(id);
        return encryptedNote != null ? NotesCrypt.decrypt(encryptedNote) : null;
    }
    
    private NotesDatabase getNotesDatabase() {
        return App.getAppContainer().getNotesDatabase();
    }
}
