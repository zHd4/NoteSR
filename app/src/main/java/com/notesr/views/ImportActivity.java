package com.notesr.views;

import android.view.WindowManager;
import com.notesr.R;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import com.notesr.controllers.CryptoController;
import com.notesr.controllers.DatabaseController;
import com.notesr.controllers.ActivityTools;
import com.notesr.models.Config;

import static android.view.inputmethod.EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING;

public class ImportActivity extends AppCompatActivity {

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.import_activity);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
        );

        ActivityTools.checkReady(getApplicationContext(), this);

        ActionBar actionBar = getSupportActionBar();

        assert actionBar != null;

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(getResources().getString(R.string.import_notes_title));

        Button importButton = findViewById(R.id.importButton);

        final EditText importDataText = findViewById(R.id.importDataText);
        final DatabaseController db = new DatabaseController(getApplicationContext());

        importDataText.setImeOptions(IME_FLAG_NO_PERSONALIZED_LEARNING);

        importButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String notesData = importDataText.getText().toString();

                if (notesData.length() > 0) {
                    try {
                        DatabaseController db = new DatabaseController(getApplicationContext());
                        String decryptedNotes = CryptoController.decrypt(
                                notesData,
                                ActivityTools.sha256(Config.cryptoKey),
                                Base64.decode(Config.cryptoKey, Base64.DEFAULT)
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
    }

    @Override
    public boolean onSupportNavigateUp() {
        startActivity(ActivityTools.getIntent(getApplicationContext(), MainActivity.class));
        return true;
    }
}
