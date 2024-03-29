package com.peew.notesr.ui.onclick;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.BlendMode;
import android.graphics.BlendModeColorFilter;
import android.graphics.ColorFilter;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import androidx.core.content.ContextCompat;

import com.peew.notesr.App;
import com.peew.notesr.R;
import com.peew.notesr.crypto.CryptoKey;
import com.peew.notesr.crypto.CryptoManager;
import com.peew.notesr.crypto.CryptoTools;
import com.peew.notesr.db.notes.NotesDatabase;
import com.peew.notesr.ui.MainActivity;
import com.peew.notesr.ui.setup.SetupKeyActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FinishKeySetupOnClick implements View.OnClickListener {
    private final CryptoManager cryptoManager = CryptoManager.getInstance();
    private final SetupKeyActivity activity;
    private final int mode;
    private final String password;
    private CryptoKey key;

    public FinishKeySetupOnClick(SetupKeyActivity activity, int mode, String password, CryptoKey key) {
        this.activity = activity;
        this.mode = mode;
        this.password = password;
        this.key = key;
    }

    @Override
    public void onClick(View v) {
        EditText importKeyField = activity.findViewById(R.id.import_key_field);
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

        if (mode == SetupKeyActivity.FIRST_RUN_MODE) {
            try {
                cryptoManager.applyNewKey(key);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            activity.startActivity(new Intent(App.getContext(), MainActivity.class));
        } else if (mode == SetupKeyActivity.REGENERATION_MODE) {
            proceedRegeneration();
        }
    }

    private void proceedRegeneration() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.AlertDialogTheme);

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

                activity.startActivity(new Intent(App.getContext(), MainActivity.class));
                activity.finish();
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
