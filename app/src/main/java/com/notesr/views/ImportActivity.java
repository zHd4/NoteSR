package com.notesr.views;

import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import com.notesr.R;
import com.notesr.controllers.CryptoController;
import com.notesr.controllers.DatabaseController;
import com.notesr.models.ActivityTools;
import com.notesr.models.Config;

public class ImportActivity extends AppCompatActivity {

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.import_activity);

        Button importButton = findViewById(R.id.importButton);

        final EditText importDataText = findViewById(R.id.importDataText);
        final DatabaseController db = new DatabaseController(getApplicationContext());

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
}
