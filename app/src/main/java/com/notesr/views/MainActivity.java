package com.notesr.views;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.*;
import android.widget.TableLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.notesr.R;
import com.notesr.controllers.*;
import com.notesr.models.Config;
import com.notesr.models.Note;
import com.notesr.models.OpenNoteOperation;

public class MainActivity extends AppCompatActivity {

    private static Note[] notes = new Note[0];

    private Context activityContext;

    public static Note[] getNotes() {
        return notes;
    }

    public static void setNotes(final Note[] notesArray) {
        notes = notesArray;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main_activity);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,WindowManager.LayoutParams.FLAG_SECURE);

        this.activityContext = this;

        ActivityCompat.requestPermissions(MainActivity.this, new String[] {
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        }, 23);

        try {
            configureForm();
        } catch (Exception e) {
            e.printStackTrace();
        }

        FloatingActionButton addNoteButton = findViewById(R.id.add_note_button);
        FloatingActionButton lockScreenButton = findViewById(R.id.lock_screen_button);

        addNoteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NoteActivity.operation = OpenNoteOperation.CREATE_NOTE;

                startActivity(ActivityTools.getIntent(
                        getApplicationContext(),
                        NoteActivity.class
                ));
            }
        });

        lockScreenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(1);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.searchMenuItem:
                startActivity(ActivityTools.getIntent(getApplicationContext(), SearchActivity.class));
                break;

            case R.id.exportToFile:
                Exporter exporter = new Exporter();
                exporter.exportToFile(getApplicationContext());
                break;

            case R.id.importFromFile:
                Importer importer = new Importer();
                importer.importFromFile(getApplicationContext(), MainActivity.this);
                break;

            case R.id.exportToClipoard:
                exporter = new Exporter();
                exporter.exportToClipboard(
                        getApplicationContext(),
                        (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE)
                );
                break;

            case R.id.importFromClipoard:
                startActivity(ActivityTools.getIntent(getApplicationContext(), ImportActivity.class));
                break;

            case R.id.secretPin:
                AccessActivity.operation = AccessActivity.SECRET_CODE;
                startActivity(ActivityTools.getIntent(getApplicationContext(), AccessActivity.class));

                break;

            case R.id.changePin:
                AccessActivity.operation = AccessActivity.CREATE_CODE;
                startActivity(ActivityTools.getIntent(getApplicationContext(), AccessActivity.class));

                break;

            case R.id.regenerateKey:
                final DialogInterface.OnClickListener warningDialogClickListener =
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(which == DialogInterface.BUTTON_POSITIVE) {
                            Bundle setupBundle = new Bundle();
                            setupBundle.putBoolean(SetupActivity.regenerateKey, true);

                            startActivity(ActivityTools.getIntent(
                                    getApplicationContext(),
                                    SetupActivity.class
                            ).putExtras(setupBundle));
                        }
                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(activityContext);

                builder.setMessage(getResources().getString(R.string.warning_of_losing_data))
                        .setPositiveButton(getResources().getString(R.string.yes), warningDialogClickListener)
                        .setNegativeButton(getResources().getString(R.string.no), warningDialogClickListener)
                        .show();

                break;

            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    @SuppressLint("RestrictedApi")
    private void configureForm() throws Exception {
        DatabaseController db = new DatabaseController(getApplicationContext());
        FloatingActionButton addNoteButton = findViewById(R.id.add_note_button);
        FloatingActionButton lockScreenButton = findViewById(R.id.lock_screen_button);

        boolean isNotesExists = db.getAllNotes().length > 0;
        boolean keyExists = StorageController.readFile(
                getApplicationContext(),
                Config.keyBinFileName
        ).length() > 0;

        if((isNotesExists && keyExists) || (!isNotesExists && keyExists)) {
            if(Config.passwordCode != null){
                addNoteButton.setVisibility(View.VISIBLE);
                lockScreenButton.setVisibility(View.VISIBLE);

                fillTable();
            } else {
                startActivity(ActivityTools.getIntent(getApplicationContext(), AccessActivity.class));
            }
        }

        if(isNotesExists && !keyExists) {
            startActivity(ActivityTools.getIntent(getApplicationContext(), RecoveryActivity.class));
        }

        if(!isNotesExists && !keyExists) {
            startActivity(ActivityTools.getIntent(getApplicationContext(), StartActivity.class));
        }
    }

    private void fillTable() throws Exception {
        TableLayout notesTable = findViewById(R.id.notes_table);

        notes = NotesController.getNotes(getApplicationContext());
        new TableGenerator().fillTable(
                this,
                this,
                notesTable,
                notes,
                getResources().getColor(R.color.buttonBackground),
                getWindowManager().getDefaultDisplay().getWidth() / 33
        );
    }
}