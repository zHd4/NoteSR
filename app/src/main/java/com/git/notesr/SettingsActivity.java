package com.git.notesr;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class SettingsActivity extends AppCompatActivity {

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        if (!Storage.isExternalStorageAvailable() || !Storage.isExternalStorageReadOnly()) {
            ActivityTools.RequirePermission(SettingsActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE);
            ActivityTools.RequirePermission(SettingsActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        Button exportButton = findViewById(R.id.exportButton);
        Button exportBinButton = findViewById(R.id.exportBinButton);

        Button importButton = findViewById(R.id.importButton);
        Button importBinButton = findViewById(R.id.importBinButton);

        final EditText importDataText = findViewById(R.id.importDataText);

        exportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String notesData = Storage.ReadFile(getApplicationContext(),
                        Config.notesJsonFileName);

                ActivityTools.clipboard =
                        (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("", notesData);
                ActivityTools.clipboard.setPrimaryClip(clip);

                ActivityTools.ShowTextMessage("Copied!", Toast.LENGTH_SHORT,
                        getApplicationContext());
            }
        });

        exportBinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                @SuppressLint("SimpleDateFormat")
                String datetime =
                        new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")
                                .format(Calendar.getInstance().getTime());

                File path = new File(
                        Environment.getExternalStoragePublicDirectory(
                                Environment.DIRECTORY_DOWNLOADS),
                        String.format("notesr_export_" + datetime + ".nsrbak"));

                String notesData = Storage.ReadFile(getApplicationContext(),
                        Config.notesJsonFileName);

                if (Storage.ExternalWriteFile(path, notesData)) {
                    ActivityTools.ShowTextMessage("Saved to " + path.getAbsolutePath(),
                            Toast.LENGTH_SHORT, getApplicationContext());
                } else {
                    ActivityTools.RequirePermission(SettingsActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    ActivityTools.ShowTextMessage("Please allow storage access and try again",
                            Toast.LENGTH_SHORT, getApplicationContext());
                }
            }
        });

        importButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String newNotesData = importDataText.getText().toString();

                if(!newNotesData.equals("")){
                    String notesData = Storage.ReadFile(getApplicationContext(),
                            Config.notesJsonFileName);

                    Storage.WriteFile(getApplicationContext(),
                            Config.notesJsonFileName, newNotesData);

                    try {
                        String[][] data = Notes.GetNotes(getApplicationContext());

                        if(data.equals(new String[0][0])){
                            Storage.WriteFile(getApplicationContext(), Config.notesJsonFileName,
                                    notesData);
                        }else{
                            startActivity(ActivityTools.GetIntent(getApplicationContext(),
                                    MainActivity.class));
                        }
                    } catch (Exception e) {
                        Storage.WriteFile(getApplicationContext(), Config.notesJsonFileName,
                                notesData);
                    }
                }
            }
        });

        importBinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ChooseFileActivity.safeCalled = true;
                startActivity(ActivityTools.GetIntent(getApplicationContext(), ChooseFileActivity.class));
            }
        });
    }
}
