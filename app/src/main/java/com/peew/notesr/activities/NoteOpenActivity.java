package com.peew.notesr.activities;

import static androidx.core.view.inputmethod.EditorInfoCompat.IME_FLAG_NO_PERSONALIZED_LEARNING;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.peew.notesr.App;
import com.peew.notesr.R;
import com.peew.notesr.db.notes.NotesDatabase;
import com.peew.notesr.db.notes.tables.NotesTable;
import com.peew.notesr.models.Note;

public class NoteOpenActivity extends ExtendedAppCompatActivity {
    public static final Integer NEW_NOTE_MODE = 0;
    public static final Integer EDIT_NOTE_MODE = 1;

    private long noteId;

    /** @noinspection DataFlowIssue*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_open);

        int mode = getIntent().getIntExtra("mode", NEW_NOTE_MODE);
        noteId = getIntent().getLongExtra("note_id", -1);

        if (noteId < 0) {
            throw new RuntimeException("Note id didn't provided");
        }

        EditText nameField = findViewById(R.id.note_name_field);
        EditText textField = findViewById(R.id.note_text_field);

        FloatingActionButton saveButton = findViewById(R.id.save_note_button);
        FloatingActionButton deleteButton = findViewById(R.id.delete_note_button);

        nameField.setImeOptions(IME_FLAG_NO_PERSONALIZED_LEARNING);
        textField.setImeOptions(IME_FLAG_NO_PERSONALIZED_LEARNING);

        saveButton.setOnClickListener(saveNoteOnClick(nameField, textField));

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        if (mode == EDIT_NOTE_MODE) {
            actionBar.setTitle(getResources().getString(R.string.edit_note));
            deleteButton.setOnClickListener(deleteNoteOnClick());
        } else {
            deleteButton.setVisibility(View.INVISIBLE);
        }

        configureForm(nameField, textField);
    }

    /** @noinspection deprecation*/
    @Override
    public boolean onSupportNavigateUp() {
        super.onBackPressed();
        return true;
    }

    private void configureForm(EditText nameField, EditText textField) {
        NotesTable notesTable = NotesDatabase.getInstance().getNotesTable();

        if (notesTable.exists(noteId)) {
            Note note = notesTable.get(noteId);

            nameField.setText(note.getName());
            textField.setText(note.getText());
        }
    }

    private View.OnClickListener saveNoteOnClick(EditText nameField, EditText textField) {
        return view -> {
            String name = nameField.getText().toString();
            String text = textField.getText().toString();

            if (!name.isBlank() && !text.isBlank()) {
                Note note = new Note(noteId, name, text);
                NotesTable notesTable = NotesDatabase.getInstance().getNotesTable();

                if (notesTable.exists(noteId)) {
                    notesTable.update(note);
                } else {
                    notesTable.add(note);
                }

                startActivity(new Intent(App.getContext(), MainActivity.class));
            }
        };
    }

    private View.OnClickListener deleteNoteOnClick() {
        return view -> {
            int textColor = ContextCompat.getColor(App.getContext(), R.color.text_color);
            int dialogStyleId = R.style.AlertDialogTheme;

            String dialogTextColor = toHexColor(textColor);
            String messageHtmlFormat = "<font color='%s'>%s</font>";

            String messageText = getString(R.string.this_action_cannot_be_undo_are_you_sure);
            String message = String.format(messageHtmlFormat, dialogTextColor, messageText);

            Spanned messageSpanned = Html.fromHtml(message, Html.FROM_HTML_MODE_LEGACY);

            DialogInterface.OnClickListener listener = deleteNoteDialogOnClick();
            AlertDialog.Builder builder = new AlertDialog.Builder(this, dialogStyleId);

            builder.setMessage(messageSpanned)
                    .setPositiveButton(getString(R.string.delete), listener)
                    .setNegativeButton(getString(R.string.no), listener);

            builder.create().show();
        };
    }

    private DialogInterface.OnClickListener deleteNoteDialogOnClick() {
        return (dialog, result) -> {
            if (result == DialogInterface.BUTTON_POSITIVE) {
                NotesDatabase.getInstance().getNotesTable().delete(noteId);
                startActivity(new Intent(App.getContext(), MainActivity.class));
            }
        };
    }

    private String toHexColor(int color) {
        return "#" + Integer.toHexString(color).substring(2);
    }
}
