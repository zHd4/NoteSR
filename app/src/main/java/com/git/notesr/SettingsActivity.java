package com.git.notesr;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        Button exportButton = findViewById(R.id.exportButton);
        Button importButton = findViewById(R.id.importButton);
        final EditText importDataText = findViewById(R.id.importDataText);

        exportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String notesData = Storage.ReadFile(getApplicationContext(), "notes.json");

                GenkeysActivity.clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("", notesData);
                GenkeysActivity.clipboard.setPrimaryClip(clip);

                ShowTextMessage("Copied!");
            }
        });

        importButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String newNotesData = importDataText.getText().toString();

                if(!newNotesData.equals("")){
                    String notesData = Storage.ReadFile(getApplicationContext(), "notes.json");

                    Storage.WriteFile(getApplicationContext(), "notes.json", newNotesData);

                    try {
                        String[][] data = Notes.GetNotes(getApplicationContext());

                        if(data == new String[0][0]){
                            Storage.WriteFile(getApplicationContext(), "notes.json", notesData);
                        }else{
                            StartMainActivity();
                        }
                    } catch (Exception e) {
                        Storage.WriteFile(getApplicationContext(), "notes.json", notesData);
                    }
                }
            }
        });
    }

    public void ShowTextMessage(String text) {
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }

    public void StartMainActivity()
    {
        Intent saIntent = new Intent(this, MainActivity.class);
        startActivity(saIntent);
    }
}
