package com.peew.notesr.ui;

import static androidx.core.view.inputmethod.EditorInfoCompat.IME_FLAG_NO_PERSONALIZED_LEARNING;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.BlendMode;
import android.graphics.BlendModeColorFilter;
import android.graphics.ColorFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import com.peew.notesr.db.notes.NotesDatabase;

import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SetupKeyActivity extends ExtendedAppCompatActivity {
    public static final int FIRST_RUN_MODE = 0;
    public static final int REGENERATION_MODE = 1;
    private int mode;
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

        mode = getIntent().getIntExtra("mode", -1);
        password = getIntent().getStringExtra("password");

        if (mode == -1) {
            throw new RuntimeException("Mode didn't provided");
        } else if (mode == REGENERATION_MODE) {
            disableBackButton();
        }

        if (password == null) {
            throw new RuntimeException("Password didn't provided");
        }

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
                    proceedKeyImportFail(importKeyField);
                    return;
                }
            }

            if (mode == FIRST_RUN_MODE) {
                try {
                    cryptoManager.applyNewKey(key);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                startActivity(new Intent(App.getContext(), MainActivity.class));
            } else if (mode == REGENERATION_MODE) {
                proceedRegeneration();
            }
        };
    }

    private void proceedRegeneration() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);

        executor.execute(() -> {
            handler.post(() -> {
                builder.setView(R.layout.progress_dialog_re_encryption);
                builder.setCancelable(false);
                builder.create().show();
            });

            try {
                CryptoKey oldCryptoKey = cryptoManager.getCryptoKeyInstance().copy();
                cryptoManager.applyNewKey(key);

                NotesDatabase.getInstance().reEncryptAllTables(oldCryptoKey);

                startActivity(new Intent(App.getContext(), MainActivity.class));
                finish();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void proceedKeyImportFail(EditText importKeyField) {
        int importFailedColor = ContextCompat.getColor(
                App.getContext(),
                R.color.key_import_failed_color);

        ColorFilter colorFilter = new BlendModeColorFilter(
                importFailedColor,
                BlendMode.SRC_ATOP);

        importKeyField.getBackground().setColorFilter(colorFilter);
    }
}
