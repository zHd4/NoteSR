package com.peew.notesr.activities;

import static androidx.core.view.inputmethod.EditorInfoCompat.IME_FLAG_NO_PERSONALIZED_LEARNING;

import android.content.Intent;
import android.graphics.BlendMode;
import android.graphics.BlendModeColorFilter;
import android.graphics.ColorFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.peew.notesr.App;
import com.peew.notesr.R;
import com.peew.notesr.crypto.CryptoKey;
import com.peew.notesr.crypto.CryptoManager;
import com.peew.notesr.crypto.CryptoTools;

import java.security.NoSuchAlgorithmException;

public class SetupKeyActivity extends ExtendedAppCompatActivity {
    private String password;
    private CryptoKey key;
    private final CryptoManager cryptoManager = CryptoManager.getInstance();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_key);

        TextView keyView = findViewById(R.id.aes_key_hex);
        EditText importKeyField = findViewById(R.id.import_key_field);

        Button copyToClipboardButton = findViewById(R.id.copy_aes_key_hex);
        Button nextButton = findViewById(R.id.key_setup_next_button);

        password = getIntent().getStringExtra("password");
        importKeyField.setImeOptions(IME_FLAG_NO_PERSONALIZED_LEARNING);

        try {
            key = cryptoManager.generateNewKey(password);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        String keyHex = CryptoTools.cryptoKeyToHex(key);

        keyView.setText(keyHex);
        copyToClipboardButton.setOnClickListener(copyToClipboardOnClick(keyHex));
        nextButton.setOnClickListener(nextButtonOnClick(importKeyField));
    }

    private View.OnClickListener copyToClipboardOnClick(String keyHex) {
        return view -> {
            copyToClipboard("", keyHex);
            showToastMessage(getString(R.string.copied), Toast.LENGTH_SHORT);
        };
    }

    private View.OnClickListener nextButtonOnClick(EditText importKeyField) {
        return view -> {
            String hexKeyToImport = importKeyField.getText().toString();

            if (!hexKeyToImport.isBlank()) {
                try {
                    key = CryptoTools.hexToCryptoKey(hexKeyToImport, password);
                } catch (Exception e) {
                    Log.e("hexToCryptoKey" , e.toString());

                    int importFailedColor = ContextCompat.getColor(
                            App.getContext(),
                            R.color.key_import_failed_color);

                    ColorFilter colorFilter = new BlendModeColorFilter(
                            importFailedColor,
                            BlendMode.SRC_ATOP);

                    importKeyField.getBackground().setColorFilter(colorFilter);
                    return;
                }
            }

            try {
                cryptoManager.applyNewKey(key);
            } catch (Exception e) {
                Log.e("applyNewKey" , e.toString());
            }

            startActivity(new Intent(App.getContext(), MainActivity.class));
        };
    }
}
