package com.notesr.controllers.activities;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.notesr.R;
import com.notesr.controllers.crypto.CryptoController;
import com.notesr.controllers.db.DatabaseController;
import com.notesr.controllers.NotesController;
import com.notesr.controllers.ActivityHelper;
import com.notesr.models.Config;
import com.notesr.models.Note;
import com.notesr.models.OpenNoteOperation;

import java.util.Arrays;

import static android.view.inputmethod.EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING;

/** @noinspection resource*/
public class NoteActivity extends AppCompatActivity {
    public static OpenNoteOperation operation = OpenNoteOperation.NONE;

    private static int noteId;
    private static String noteTitle;

    private Context activityContext;

    public static void setNoteId(int id) {
        noteId = id;
    }

    public static void setNoteTitle(String title) {
        noteTitle = title;
    }

    @SuppressLint({"RestrictedApi", "InlinedApi"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.note_activity);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        ActivityHelper.checkReady(getApplicationContext(), this);

        ActionBar actionBar = getSupportActionBar();

        assert actionBar != null;

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(getResources().getString(R.string.new_note));

        this.activityContext = this;

        final EditText titleText = findViewById(R.id.titleText);
        final EditText textText = findViewById(R.id.textText);

        titleText.setImeOptions(IME_FLAG_NO_PERSONALIZED_LEARNING);
        textText.setImeOptions(IME_FLAG_NO_PERSONALIZED_LEARNING);

        final FloatingActionButton applyButton = findViewById(R.id.applyButton);
        final FloatingActionButton showAttachmentsButton = findViewById(R.id.showAttachmentsButton);
        final FloatingActionButton deleteButton = findViewById(R.id.deleteButton);

        final DialogInterface.OnClickListener deleteDialogClickListener = (dialog, which) -> {
            if(which == DialogInterface.BUTTON_POSITIVE) {
                try {
                    DatabaseController db = new DatabaseController(getApplicationContext());
                    byte[] key = Base64.decode(Config.cryptoKey, Base64.DEFAULT);

                    String encryptedNoteTitle = Base64.encodeToString(
                            CryptoController.encrypt(
                                    noteTitle.getBytes(),
                                    ActivityHelper.sha256(Config.cryptoKey),
                                    key),
                            Base64.DEFAULT);

                    db.deleteNote(encryptedNoteTitle);
                    startActivity(ActivityHelper.getIntent(getApplicationContext(), MainActivity.class));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        if(operation == OpenNoteOperation.EDIT_NOTE){
            actionBar.setTitle(getResources().getString(R.string.edit_note));
            titleText.setText(noteTitle);
            textText.setText(MainActivity.getNotes()[noteId].getText());
            deleteButton.setVisibility(View.VISIBLE);
        }

        applyButton.setOnClickListener(view -> {
            if(!titleText.getText().toString().equals("") && !textText.getText().toString().equals("")) {
                    int newNoteId = MainActivity.getNotes().length;
                    Note note = new Note(newNoteId, titleText.getText().toString(), textText.getText().toString());

                    MainActivity.setNotes(addToNotesArray(MainActivity.getNotes(), note));
            } else {
                MainActivity.getNotes()[noteId].setName(titleText.getText().toString());
                MainActivity.getNotes()[noteId].setText(textText.getText().toString());
            }

            try {
                NotesController.setNotes(getApplicationContext(), MainActivity.getNotes());
            } catch (Exception e) {
                e.printStackTrace();
            }
            startActivity(ActivityHelper.getIntent(getApplicationContext(), MainActivity.class));
        });

        showAttachmentsButton.setOnClickListener(v -> {

        });

        deleteButton.setOnClickListener(v -> {
            if(operation == OpenNoteOperation.EDIT_NOTE) {
                AlertDialog.Builder builder = new AlertDialog.Builder(activityContext);

                builder.setMessage(getResString(R.string.if_yes_cannot_undo))
                        .setPositiveButton(getResString(R.string.yes), deleteDialogClickListener)
                        .setNegativeButton(getResString(R.string.no), deleteDialogClickListener)
                        .show();
            }
        });
    }
    @Override
    public boolean onSupportNavigateUp() {
        final EditText titleText = findViewById(R.id.titleText);
        final EditText textText = findViewById(R.id.textText);

        if(MainActivity.getNotes()[noteId].getName().equals(titleText.getText().toString()) &&
                MainActivity.getNotes()[noteId].getText().equals(textText.getText().toString())) {
            finish();
        } else {
            startActivity(ActivityHelper.getIntent(getApplicationContext(), MainActivity.class));
        }

        return true;
    }

    private static Note[] addToNotesArray(final Note[] array, final Note note) {
        Note[] result = Arrays.copyOf(array, array.length + 1);
        result[result.length - 1] = note;

        return result;
    }

    private String getResString(int id) {
        return getResources().getString(id);
    }
}