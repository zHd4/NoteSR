package com.notesr.views;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.notesr.R;
import com.notesr.controllers.*;
import com.notesr.models.ActivityTools;
import com.notesr.models.Config;
import com.notesr.models.FileAttachment;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;

import static android.view.inputmethod.EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING;

public class NoteActivity extends AppCompatActivity {
    public static int arg = 0;

    public static int CREATE_NOTE = 0;
    public static int EDIT_NOTE = 1;

    public static int noteId;
    public static String noteTitle;

    private Context activityContext;

    @SuppressLint("RestrictedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.note_activity);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        ActivityTools.checkReady(getApplicationContext(), this);

        ActionBar actionBar = getSupportActionBar();

        assert actionBar != null;

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(getResources().getString(R.string.new_note));

        this.activityContext = this;

        final EditText titleText = findViewById(R.id.titleText);
        final EditText textText = findViewById(R.id.textText);

        titleText.setImeOptions(IME_FLAG_NO_PERSONALIZED_LEARNING);
        textText.setImeOptions(IME_FLAG_NO_PERSONALIZED_LEARNING);

        final TextView filePathField = findViewById(R.id.filePathField);
        final Button addFileButton = findViewById(R.id.addFileButton);

        final FloatingActionButton applyButton = findViewById(R.id.applyButton);
        final FloatingActionButton deleteButton = findViewById(R.id.deleteButton);

        final DialogInterface.OnClickListener deleteDialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(which == DialogInterface.BUTTON_POSITIVE) {
                    try {
                        DatabaseController db = new DatabaseController(getApplicationContext());
                        byte[] key = Base64.decode(Config.cryptoKey, Base64.DEFAULT);

                        db.deleteNote(CryptoController.encrypt(noteTitle, ActivityTools.sha256(Config.cryptoKey), key));
                        startActivity(ActivityTools.getIntent(getApplicationContext(), MainActivity.class));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        if(arg == EDIT_NOTE){
            actionBar.setTitle(getResources().getString(R.string.edit_note));
            titleText.setText(noteTitle);
            textText.setText(parseNoteText(MainActivity.notes[noteId][1]));
            deleteButton.setVisibility(View.VISIBLE);
        }

        applyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!titleText.getText().toString().equals("") &&
                        !textText.getText().toString().equals("")) {
                    if(arg == CREATE_NOTE) {
                        String[][] note =
                                { {
                                    titleText.getText().toString(), textText.getText().toString()
                                } };
                        MainActivity.notes = concat(MainActivity.notes, note);
                    } else {
                        MainActivity.notes[noteId][0] = titleText.getText().toString();
                        MainActivity.notes[noteId][1] = textText.getText().toString();
                    }

                    try {
                        NotesController.setNotes(getApplicationContext(), MainActivity.notes);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    startActivity(ActivityTools.getIntent(getApplicationContext(),
                            MainActivity.class));
                }
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(arg == EDIT_NOTE) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(activityContext);

                    builder.setMessage(getResString(R.string.if_yes_cannot_undo))
                            .setPositiveButton(getResString(R.string.yes), deleteDialogClickListener)
                            .setNegativeButton(getResString(R.string.no), deleteDialogClickListener)
                            .show();
                }
            }
        });

        addFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String filePath = filePathField.getText().toString();

                if(!filePath.equals("")) {
                    try {
                        File file = new File(filePath);
                        String noteText = MainActivity.notes[noteId][1];

                        byte[] filedata = StorageController.externalReadFile(file, new FileInputStream(file));

                        int lastIndex = NoteFilesController.getLastIndex(noteText);
                        String fileMarkup = NoteFilesController.generateMarkup(file.getName(), filedata, lastIndex);

                        MainActivity.notes[noteId][1] = NoteFilesController.setLastIndex(noteText, lastIndex);
                        MainActivity.notes[noteId][1] += fileMarkup;
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        final EditText titleText = findViewById(R.id.titleText);
        final EditText textText = findViewById(R.id.textText);

        if(MainActivity.notes[noteId][0].equals(titleText.getText().toString()) &&
                MainActivity.notes[noteId][1].equals(textText.getText().toString())) {
            finish();
        } else {
            startActivity(ActivityTools.getIntent(getApplicationContext(), MainActivity.class));
        }

        return true;
    }

    private String parseNoteText(final String raw) {
        String result = raw;
        TableLayout filesTable = findViewById(R.id.filesTable);

        FileAttachment[] files = NoteFilesController.findMarkups(result);

        if(files != null) {
            filesTable.removeAllViews();
            fillFilesTable(filesTable, files);
        }

        return result;
    }

    private void fillFilesTable(TableLayout filesTable, FileAttachment[] files) {
        final Button addFileButton = findViewById(R.id.addFileButton);

        final int beforeLineColor = Color.rgb(25, 28, 33);
        final int maxFilenameVisualSize = getWindowManager().getDefaultDisplay().getWidth() / 33;

        for(int i = 0; i < files.length; i++) {
            final TextView filenameView = new TextView(this);

            TableRow fileRow = new TableRow(this);
            TableRow afterLine = new TableRow(this);

            fileRow.setLayoutParams(new TableLayout.LayoutParams(
                    TableLayout.LayoutParams.FILL_PARENT,
                    TableLayout.LayoutParams.WRAP_CONTENT));

            filenameView.setId(i);
            filenameView.setText(files[i].getName().length() > maxFilenameVisualSize
                    ? files[i].getName().substring(0, maxFilenameVisualSize) + "..."
                    : files[i].getName());

            filenameView.setTextColor(Color.WHITE);
            filenameView.setTextSize(24);
            filenameView.setPadding(15, 35, 15, 35);

            afterLine.setBackgroundColor(beforeLineColor);
            afterLine.setPadding(15, 2, 15, 2);

            filesTable.addView(fileRow, new TableLayout.LayoutParams(
                    TableLayout.LayoutParams.FILL_PARENT,
                    TableLayout.LayoutParams.WRAP_CONTENT));

            filesTable.addView(afterLine, new TableLayout.LayoutParams(
                    TableLayout.LayoutParams.FILL_PARENT,
                    TableLayout.LayoutParams.WRAP_CONTENT));
        }
    }

    private static <T> T[] concat(T[] first, T[] second) {
        T[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);

        return result;
    }

    private String getResString(int id) {
        return getResources().getString(id);
    }
}