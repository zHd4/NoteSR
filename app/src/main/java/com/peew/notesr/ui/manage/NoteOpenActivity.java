package com.peew.notesr.ui.manage;

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
import com.peew.notesr.ui.ExtendedAppCompatActivity;
import com.peew.notesr.ui.MainActivity;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class NoteOpenActivity extends ExtendedAppCompatActivity {
    public static final int NEW_NOTE_MODE = 0;
    public static final int EDIT_NOTE_MODE = 1;
    private final Map<Integer, Consumer<?>> menuItemsMap = new HashMap<>();
    private long noteId;
    private int mode;

    /** @noinspection DataFlowIssue*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_open);

        mode = getIntent().getIntExtra("mode", NEW_NOTE_MODE);
        noteId = getIntent().getLongExtra("note_id", -1);

        if (noteId < 0) {
            throw new RuntimeException("Note id didn't provided");
        }

        EditText nameField = findViewById(R.id.note_name_field);
        EditText textField = findViewById(R.id.note_text_field);

        nameField.setImeOptions(IME_FLAG_NO_PERSONALIZED_LEARNING);
        textField.setImeOptions(IME_FLAG_NO_PERSONALIZED_LEARNING);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        switch (mode) {
            case NEW_NOTE_MODE -> actionBar.setTitle(getResources().getString(R.string.new_note));
            case EDIT_NOTE_MODE -> actionBar.setTitle(getResources().getString(R.string.edit_note));
            default -> throw new RuntimeException("Unknown mode");
        }

        configureForm(nameField, textField);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        EditText nameField = findViewById(R.id.note_name_field);
        EditText textField = findViewById(R.id.note_text_field);

        getMenuInflater().inflate(R.menu.menu_note_open, menu);

        menuItemsMap.put(R.id.save_note_button, action -> saveNoteOnClick(nameField, textField));

        switch (mode) {
            case NEW_NOTE_MODE -> {
                MenuItem deleteButton = menu.findItem(R.id.delete_note_button);

                deleteButton.setEnabled(false);
                deleteButton.setVisible(false);
            }
            
            case EDIT_NOTE_MODE -> {
                Consumer<?> deleteButtonOnClick = action -> deleteNoteOnClick();
                menuItemsMap.put(R.id.delete_note_button, deleteButtonOnClick);
            }

            default -> throw new RuntimeException("Unknown mode");
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

    private void configureForm(EditText nameField, EditText textField) {
        NotesTable notesTable = NotesDatabase.getInstance().getNotesTable();

        if (notesTable.exists(noteId)) {
            Note note = NotesCrypt.decrypt(notesTable.get(noteId));

            nameField.setText(note.getName());
            textField.setText(note.getText());
        }
    }

    private void saveNoteOnClick(EditText nameField, EditText textField) {
        String name = nameField.getText().toString();
        String text = textField.getText().toString();

        if (!name.isBlank() && !text.isBlank()) {
            EncryptedNote encryptedNote = NotesCrypt.encrypt(new Note(name, text));
            NotesTable notesTable = NotesDatabase.getInstance().getNotesTable();

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

    private DialogInterface.OnClickListener deleteNoteDialogOnClick() {
        return (dialog, result) -> {
            if (result == DialogInterface.BUTTON_POSITIVE) {
                NotesDatabase.getInstance().getNotesTable().delete(noteId);
                startActivity(new Intent(App.getContext(), MainActivity.class));
            }
        };
    }
}
