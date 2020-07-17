package com.git.notesr;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {

    public static String[][] notes = new String[0][0];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        try {
            configureForm();
        } catch (Exception e) {
            e.printStackTrace();
        }

        FloatingActionButton addNoteButton = findViewById(R.id.add_note_button);

        addNoteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ChangeActivity.arg = ChangeActivity.CREATE_NOTE;

                startActivity(ActivityTools.getIntent(
                        getApplicationContext(),
                        ChangeActivity.class
                ));
            }
        });

        Button settingsButton = findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(
                        ActivityTools.getIntent(getApplicationContext(), SettingsActivity.class)
                );
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("RestrictedApi")
    private void configureForm() throws Exception {
        Database db = new Database(getApplicationContext());
        Button settingsButton = findViewById(R.id.settingsButton);
        FloatingActionButton addNoteButton = findViewById(R.id.add_note_button);

        boolean isNotesExists = db.getAllNotes().length > 0;
        boolean keyExists = Storage.readFile(
                getApplicationContext(),
                Config.keyBinFileName
        ).length() > 0;

        if((isNotesExists && keyExists) || (!isNotesExists && keyExists)) {
            if(Config.pinCode != null){
                if(Storage.isFileExists(getApplicationContext(), Config.notesJsonFilename)) {
                    convertJsonToDatabase();
                }

                addNoteButton.setVisibility(View.VISIBLE);
                settingsButton.setVisibility(View.VISIBLE);

                fillTable();
            } else {
                startActivity(ActivityTools.getIntent(getApplicationContext(), AccessActivity.class));
            }
        }

        if(isNotesExists && !keyExists) {
            startActivity(ActivityTools.getIntent(getApplicationContext(), RecoveryActivity.class));
        }

        if(!isNotesExists && !keyExists) {
            startActivity(ActivityTools.getIntent(getApplicationContext(), SetupActivity.class));
        }
    }

    private void fillTable() throws Exception {
        TableLayout notesTable = findViewById(R.id.notes_table);

        notes = Notes.getNotes(getApplicationContext());

        int noteColor = Color.rgb(37, 40, 47);
        int beforeLineColor = Color.rgb(25, 28, 33);

        if (!notes.equals(new String[0][0])) {
            for (int i = 0; i < notes.length; i++) {
                TableRow trData = new TableRow(this);

                trData.setBackgroundColor(noteColor);
                trData.setLayoutParams(new TableLayout.LayoutParams(
                        TableLayout.LayoutParams.FILL_PARENT,
                        TableLayout.LayoutParams.WRAP_CONTENT));

                final TextView notesElement = new TextView(this);

                notesElement.setId(i);
                notesElement.setText(notes[i][0]);
                notesElement.setTextColor(Color.WHITE);
                notesElement.setTextSize(24);
                notesElement.setPadding(15, 35, 15, 35);
                trData.addView(notesElement);

                TableRow beforeLine = new TableRow(this);

                beforeLine.setBackgroundColor(beforeLineColor);
                beforeLine.setPadding(15, 2, 15, 2);

                notesTable.addView(trData, new TableLayout.LayoutParams(
                        TableLayout.LayoutParams.FILL_PARENT,
                        TableLayout.LayoutParams.WRAP_CONTENT));

                notesTable.addView(beforeLine, new TableLayout.LayoutParams(
                        TableLayout.LayoutParams.FILL_PARENT,
                        TableLayout.LayoutParams.WRAP_CONTENT));

                final int finalIndex = i;

                trData.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        int id = finalIndex;
                        String noteTitle = notesElement.getText().toString();

                        ChangeActivity.noteId = id;
                        ChangeActivity.noteTitle = noteTitle;

                        ChangeActivity.arg = ChangeActivity.EDIT_NOTE;

                        startActivity(ActivityTools.getIntent(
                                getApplicationContext(),
                                ChangeActivity.class
                        ));
                    }
                });
            }
        }
    }

    private void convertJsonToDatabase() throws Exception {
        String notesData = Storage.readFile(getApplicationContext(), Config.notesJsonFilename);
        String decryptedNotes = AES.decrypt(
                notesData,
                Base64.decode(Config.aesKey, Base64.DEFAULT)
        );

        Database db = new Database(getApplicationContext());

        db.importFromJsonString(getApplicationContext(), decryptedNotes);
    }
}