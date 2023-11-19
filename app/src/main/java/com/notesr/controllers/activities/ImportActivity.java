package com.notesr.controllers.activities;

import static android.view.inputmethod.EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Base64;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.notesr.R;
import com.notesr.controllers.ActivityHelper;
import com.notesr.controllers.crypto.CryptoController;
import com.notesr.controllers.db.DatabaseController;
import com.notesr.models.Config;

/** @noinspection resource*/
public class ImportActivity extends AppCompatActivity {

    @SuppressLint("InlinedApi")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.import_activity);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
        );

        ActivityHelper.checkReady(getApplicationContext(), this);

        ActionBar actionBar = getSupportActionBar();

        assert actionBar != null;

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(getResources().getString(R.string.import_notes_title));

        Button importButton = findViewById(R.id.importButton);

        final EditText importDataText = findViewById(R.id.importDataText);
        final DatabaseController db = new DatabaseController(getApplicationContext());

        importDataText.setImeOptions(IME_FLAG_NO_PERSONALIZED_LEARNING);

        importButton.setOnClickListener(view -> {
            String notesData = importDataText.getText().toString();

            if (notesData.length() > 0) {
                try {
                    //noinspection resource
                    DatabaseController db1 = new DatabaseController(getApplicationContext());
                    String decryptedNotes = new String(CryptoController.decrypt(
                            Base64.decode(notesData, Base64.DEFAULT),
                            ActivityHelper.sha256(Config.cryptoKey),
                            Base64.decode(Config.cryptoKey, Base64.DEFAULT)
                    )).replace("\\n", "");

                    db1.importFromJsonString(getApplicationContext(), decryptedNotes);
                    startActivity(ActivityHelper.getIntent(getApplicationContext(),
                            MainActivity.class));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        startActivity(ActivityHelper.getIntent(getApplicationContext(), MainActivity.class));
        return true;
    }
}
