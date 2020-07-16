package com.git.notesr;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
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
            ActivityTools.requirePermission(
                    SettingsActivity.this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            );

            ActivityTools.requirePermission(
                    SettingsActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            );
        }

        Button exportButton = findViewById(R.id.exportButton);
        Button exportBinButton = findViewById(R.id.exportBinButton);

        Button importButton = findViewById(R.id.importButton);
        Button importBinButton = findViewById(R.id.importBinButton);

        final EditText importDataText = findViewById(R.id.importDataText);
        final Database db = new Database(getApplicationContext());

        exportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    String notesData = AES.encrypt(
                            db.exportToJsonString(getApplicationContext()),
                            Base64.decode(Config.aesKey, Base64.DEFAULT)
                    );

                    ActivityTools.clipboard = (ClipboardManager)
                            getSystemService(Context.CLIPBOARD_SERVICE);

                    ClipData clip = ClipData.newPlainText("", notesData);
                    ActivityTools.clipboard.setPrimaryClip(clip);

                    ActivityTools.showTextMessage(
                            "Copied!",
                            Toast.LENGTH_SHORT,
                            getApplicationContext()
                    );
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        exportBinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    @SuppressLint("SimpleDateFormat")
                    String datetime =
                            new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")
                                    .format(Calendar.getInstance().getTime());

                    File path = new File(
                            Environment.getExternalStoragePublicDirectory(
                                    Environment.DIRECTORY_DOWNLOADS),
                            String.format("notesr_export_" + datetime + ".nsrbak"));

                    String notesData = AES.encrypt(
                            db.exportToJsonString(getApplicationContext()),
                            Base64.decode(Config.aesKey, Base64.DEFAULT)
                    );

                    if (Storage.externalWriteFile(path, notesData)) {
                        ActivityTools.showTextMessage("Saved to " + path.getAbsolutePath(),
                                Toast.LENGTH_SHORT, getApplicationContext());
                    } else {
                        ActivityTools.requirePermission(
                                SettingsActivity.this,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                        );

                        ActivityTools.showTextMessage(
                                "Please allow storage access and try again",
                                Toast.LENGTH_SHORT,
                                getApplicationContext()
                        );
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        importButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String notesData = importDataText.getText().toString();

                if (notesData.length() > 0) {
                    try {
                        Database db = new Database(getApplicationContext());
                        String decryptedNotes = AES.decrypt(
                                notesData,
                                Base64.decode(Config.aesKey, Base64.DEFAULT)
                        ).replace("\\n", "");

                        db.importFromJsonString(getApplicationContext(), decryptedNotes);
                        startActivity(ActivityTools.getIntent(getApplicationContext(),
                                MainActivity.class));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        importBinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ChooseFileActivity.safeCalled = true;
                startActivity(ActivityTools.getIntent(
                        getApplicationContext(),
                        ChooseFileActivity.class
                ));
            }
        });
    }
}
