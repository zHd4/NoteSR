package com.peew.notesr.onclick.security;

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
import com.peew.notesr.activity.notes.NotesListActivity;
import com.peew.notesr.activity.security.SetupKeyActivity;
import com.peew.notesr.crypto.CryptoKey;
import com.peew.notesr.crypto.CryptoManager;
import com.peew.notesr.crypto.CryptoTools;
import com.peew.notesr.manager.KeyUpdateManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FinishKeySetupOnClick implements View.OnClickListener {
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
        EditText importKeyField = activity.findViewById(R.id.importKeyField);
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
                getCryptoManager().applyNewKey(key);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            activity.startActivity(new Intent(App.getContext(), NotesListActivity.class));
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
                CryptoManager cryptoManager = getCryptoManager();
                KeyUpdateManager keyUpdateManager = getKeyUpdateManager();

                CryptoKey oldKey = cryptoManager.getCryptoKeyInstance().copy();

                cryptoManager.applyNewKey(key);
                keyUpdateManager.updateEncryptedData(oldKey, key);

                activity.startActivity(new Intent(App.getContext(), NotesListActivity.class));
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

    private CryptoManager getCryptoManager() {
        return App.getAppContainer().getCryptoManager();
    }

    private KeyUpdateManager getKeyUpdateManager() {
        return App.getAppContainer().getKeyUpdateManager();
    }
}
