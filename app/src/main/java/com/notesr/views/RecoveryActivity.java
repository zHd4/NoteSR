package com.notesr.views;

import static android.view.inputmethod.EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Base64;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.notesr.R;
import com.notesr.controllers.ActivityTools;
import com.notesr.controllers.CryptoController;
import com.notesr.controllers.DatabaseController;
import com.notesr.models.Config;

/** @noinspection resource*/
public class RecoveryActivity extends AppCompatActivity {

    @SuppressLint("InlinedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.recovery_activity);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        final EditText keyText = findViewById(R.id.keyText);
        final Button decryptButton = findViewById(R.id.decryptButton);

        keyText.setImeOptions(IME_FLAG_NO_PERSONALIZED_LEARNING);

        decryptButton.setOnClickListener(view -> {
            try {
                String keyString = ActivityTools.hexToKey(keyText.getText().toString());

                byte[] key = Base64.decode(keyString, Base64.DEFAULT);

                DatabaseController db = new DatabaseController(getApplicationContext());

                CryptoController.decrypt(
                        Base64.decode(db.getAllNotes()[0].getName(), Base64.DEFAULT),
                        ActivityTools.sha256(keyString), key);

                Config.cryptoKey = Base64.encodeToString(key, Base64.DEFAULT);
                AccessActivity.operation = AccessActivity.CREATE_CODE;

                startActivity(ActivityTools.getIntent(getApplicationContext(), AccessActivity.class));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
