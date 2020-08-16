package com.notesr.views;

import android.annotation.SuppressLint;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.notesr.R;
import com.notesr.controllers.Database;
import com.notesr.controllers.Notes;
import com.notesr.models.ActivityTools;
import com.notesr.models.Config;
import com.notesr.controllers.Storage;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.notesr.models.Exporter;
import com.notesr.models.Importer;

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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.exportToFile:
                Exporter exporter = new Exporter(MainActivity.this);
                exporter.exportToFile(getApplicationContext());
                break;

            case R.id.importFromFile:
                Importer importer = new Importer();
                importer.importFromFile(getApplicationContext(), MainActivity.this);
                break;

            case R.id.exportToClipoard:
                exporter = new Exporter(MainActivity.this);
                exporter.exportToClipboard(
                        getApplicationContext(),
                        (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE)
                );
                break;

            case R.id.importFromClipoard:
                startActivity(ActivityTools.getIntent(getApplicationContext(), SettingsActivity.class));
                break;

            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    @SuppressLint("RestrictedApi")
    private void configureForm() throws Exception {
        Database db = new Database(getApplicationContext());
        FloatingActionButton addNoteButton = findViewById(R.id.add_note_button);

        boolean isNotesExists = db.getAllNotes().length > 0;
        boolean keyExists = Storage.readFile(
                getApplicationContext(),
                Config.keyBinFileName
        ).length() > 0;

        if((isNotesExists && keyExists) || (!isNotesExists && keyExists)) {
            if(Config.pinCode != null){
                addNoteButton.setVisibility(View.VISIBLE);

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

        final int maxTitleVisualSize = 28;

        if (!notes.equals(new String[0][0])) {
            for (int i = 0; i < notes.length; i++) {
                TableRow trData = new TableRow(this);

                trData.setBackgroundColor(noteColor);
                trData.setLayoutParams(new TableLayout.LayoutParams(
                        TableLayout.LayoutParams.FILL_PARENT,
                        TableLayout.LayoutParams.WRAP_CONTENT));

                final TextView notesElement = new TextView(this);

                String title = notes[i][0];

                if(title.length() > maxTitleVisualSize) {
                    title = title.substring(0, maxTitleVisualSize) + "...";
                }

                notesElement.setId(i);

                notesElement.setText(notes[i][0].length() > maxTitleVisualSize
                        ? notes[i][0].substring(0, maxTitleVisualSize) + "..."
                        : notes[i][0]);

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
                        String noteTitle = notes[id][0];

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
}