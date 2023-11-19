package com.notesr.controllers.activities;

import static android.view.inputmethod.EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Base64;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.notesr.R;
import com.notesr.controllers.ActivityHelper;
import com.notesr.controllers.crypto.CryptoController;
import com.notesr.controllers.db.DatabaseController;
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
                String keyString = ActivityHelper.hexToKey(keyText.getText().toString());

                byte[] key = Base64.decode(keyString, Base64.DEFAULT);

                DatabaseController db = new DatabaseController(getApplicationContext());

                CryptoController.decrypt(
                        Base64.decode(db.getAllNotes()[0].getName(), Base64.DEFAULT),
                        ActivityHelper.sha256(keyString), key);

                Config.cryptoKey = Base64.encodeToString(key, Base64.DEFAULT);
                AccessActivity.operation = AccessActivity.CREATE_CODE;

                startActivity(ActivityHelper.getIntent(getApplicationContext(), AccessActivity.class));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
