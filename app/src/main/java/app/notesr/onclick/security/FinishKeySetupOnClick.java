package app.notesr.onclick.security;

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
import app.notesr.App;
import app.notesr.R;
import app.notesr.activity.notes.NotesListActivity;
import app.notesr.activity.security.SetupKeyActivity;
import app.notesr.model.CryptoKey;
import app.notesr.crypto.CryptoManager;
import app.notesr.crypto.CryptoTools;
import app.notesr.manager.KeyUpdateManager;
import lombok.AllArgsConstructor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@AllArgsConstructor
public class FinishKeySetupOnClick implements View.OnClickListener {

    private final SetupKeyActivity activity;
    private final String password;
    private CryptoKey key;

    @Override
    public void onClick(View v) {
        EditText importKeyField = activity.findViewById(R.id.importKeyField);
        String hexKeyToImport = importKeyField.getText().toString();

        if (!hexKeyToImport.isBlank()) {
            try {
                key = CryptoTools.hexToCryptoKey(hexKeyToImport, password, true);
            } catch (Exception e) {
                Log.e("hexToCryptoKey" , e.toString());
                proceedKeyImportFail(importKeyField);
                return;
            }
        }

        if (activity.getMode() == SetupKeyActivity.Mode.FIRST_RUN) {
            try {
                getCryptoManager().applyNewKey(key);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            activity.startActivity(new Intent(App.getContext(), NotesListActivity.class));
        } else if (activity.getMode() == SetupKeyActivity.Mode.REGENERATION) {
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

                CryptoKey oldKey = cryptoManager.getCryptoKeyInstance().clone();

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
