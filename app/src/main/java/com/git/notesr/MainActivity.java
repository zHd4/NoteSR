package com.git.notesr;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
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

import java.io.File;

public class MainActivity extends AppCompatActivity {

    public static String[][] notes_arr = new String[0][0];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        File dir = new File(getApplicationContext().getFilesDir(), "data");

        if (dir.isDirectory())
        {
            String[] children = dir.list();

            for (int i = 0; i < children.length; i++)
            {
                new File(dir, children[i]).delete();
            }
        }

        try {
            ConfigureForm();
        } catch (Exception e) {
            e.printStackTrace();
        }

        FloatingActionButton add_note_button = findViewById(R.id.add_note_button);

        add_note_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                StartChangeActivity(ChangeActivity.CREATE_NOTE);
            }
        });

        Button settingsButton = findViewById(R.id.settingsButton);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(ActivityTools.GetIntent(getApplicationContext(), SettingsActivity.class));
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
    public void ConfigureForm() throws Exception {
        Button settingsButton = findViewById(R.id.settingsButton);
        FloatingActionButton add_note_button = findViewById(R.id.add_note_button);

        String keys = Storage.ReadFile(getApplicationContext(), "key.bin");
        String notes = Storage.ReadFile(getApplicationContext(), "notes.json");

        if((!keys.equals("") && !notes.equals("")) || (!keys.equals("") && notes.equals(""))) {
            if(Config.pinCode.equals("")){
                Intent saIntent = new Intent(this, AccessActivity.class);
                startActivity(saIntent);
            } else {
                add_note_button.setVisibility(View.VISIBLE);
                settingsButton.setVisibility(View.VISIBLE);

                FillTable();
            }
        }

        if(keys.equals("") && notes.equals("")){
            startActivity(ActivityTools.GetIntent(getApplicationContext(), SetupActivity.class));
        }

        if(keys.equals("") && !notes.equals("")){
            startActivity(ActivityTools.GetIntent(getApplicationContext(), RecoveryActivity.class));
        }
    }

    public void FillTable() throws Exception {
        TableLayout notes_table = findViewById(R.id.notes_table);

        notes_arr = Notes.GetNotes(getApplicationContext());

        int noteColor = Color.rgb(25, 28, 33);
        int beforeLineColor = Color.rgb(9, 10, 13);

        if (!notes_arr.equals(new String[0][0])) {
            for (int i = 0; i < notes_arr.length; i++) {
                TableRow tr_data = new TableRow(this);

                tr_data.setBackgroundColor(noteColor);
                tr_data.setLayoutParams(new TableLayout.LayoutParams(
                        TableLayout.LayoutParams.FILL_PARENT,
                        TableLayout.LayoutParams.WRAP_CONTENT));

                final TextView n_element = new TextView(this);

                n_element.setId(i);
                n_element.setText(notes_arr[i][0]);
                n_element.setTextColor(Color.WHITE);
                n_element.setTextSize(24);
                n_element.setPadding(15, 35, 15, 35);
                tr_data.addView(n_element);

                TableRow before_line = new TableRow(this);

                before_line.setBackgroundColor(beforeLineColor);
                before_line.setPadding(15, 2, 15, 2);

                notes_table.addView(tr_data, new TableLayout.LayoutParams(
                        TableLayout.LayoutParams.FILL_PARENT,
                        TableLayout.LayoutParams.WRAP_CONTENT));

                notes_table.addView(before_line, new TableLayout.LayoutParams(
                        TableLayout.LayoutParams.FILL_PARENT,
                        TableLayout.LayoutParams.WRAP_CONTENT));

                final int finalI = i;

                tr_data.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        int id = finalI;
                        String n_label = n_element.getText().toString();

                        ChangeActivity.n_id = id;
                        ChangeActivity.n_label = n_label;

                        StartChangeActivity(ChangeActivity.EDIT_NOTE);
                    }
                });
            }
        }
    }

    private void StartChangeActivity(int arg) {
        ChangeActivity.arg = arg;

        Intent intent = new Intent(this, ChangeActivity.class);
        startActivity(intent);
    }
}