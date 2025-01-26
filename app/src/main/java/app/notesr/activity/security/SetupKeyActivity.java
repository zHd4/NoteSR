package app.notesr.activity.security;

import static java.util.Objects.requireNonNull;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import app.notesr.App;
import app.notesr.R;
import app.notesr.activity.ExtendedAppCompatActivity;
import app.notesr.activity.notes.NotesListActivity;
import app.notesr.service.activity.security.KeySetupService;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
public class SetupKeyActivity extends ExtendedAppCompatActivity {

    @AllArgsConstructor
    public enum Mode {
        FIRST_RUN("first_run"),
        REGENERATION("regeneration");

        public final String mode;
    }

    private static final String TAG = SetupKeyActivity.class.getName();
    private static final int LOW_SCREEN_HEIGHT = 800;
    private static final float KEY_VIEW_TEXT_SIZE_FOR_LOW_SCREEN_HEIGHT = 16;

    private Mode mode;
    private String password;
    private KeySetupService keySetupService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_setup_key);
        setMode();

        password = requireNonNull(getIntent().getStringExtra("password"));
        keySetupService = new KeySetupService(password);

        TextView keyView = findViewById(R.id.aesKeyHex);
        keyView.setText(keySetupService.getHexKey());

        adaptKeyView();

        Button copyToClipboardButton = findViewById(R.id.copyAesKeyHex);
        Button importButton = findViewById(R.id.importHexKeyButton);
        Button nextButton = findViewById(R.id.keySetupNextButton);

        copyToClipboardButton.setOnClickListener(copyKeyButtonOnClick());
        importButton.setOnClickListener(importKeyButtonOnClick());
        nextButton.setOnClickListener(nextButtonOnClick());
    }

    private void setMode() {
        String modeName = getIntent().getStringExtra("mode");

        try {
            mode = Mode.valueOf(modeName);

            if (mode == Mode.REGENERATION) {
                disableBackButton();
            }
        } catch (NullPointerException e) {
            Log.e(TAG, "Mode didn't provided", e);
            throw new RuntimeException(e);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid mode: " + modeName, e);
            throw new RuntimeException(e);
        }
    }

    private void adaptKeyView() {
        TextView keyView = findViewById(R.id.aesKeyHex);

        if (getResources().getDisplayMetrics().heightPixels <= LOW_SCREEN_HEIGHT) {
            keyView.setTextSize(KEY_VIEW_TEXT_SIZE_FOR_LOW_SCREEN_HEIGHT);
        }
    }

    private View.OnClickListener copyKeyButtonOnClick() {
        return view -> {
            String keyHex = ((TextView) findViewById(R.id.aesKeyHex)).getText().toString();

            copyToClipboard("", keyHex);
            showToastMessage(getString(R.string.copied), Toast.LENGTH_SHORT);
        };
    }

    private View.OnClickListener importKeyButtonOnClick() {
        return view -> {
            Intent intent = new Intent(getApplicationContext(), ImportKeyActivity.class)
                    .putExtra("password", password);

            startActivity(intent);
        };
    }

    private View.OnClickListener nextButtonOnClick() {
        return view -> {
            if (mode == Mode.FIRST_RUN) proceedFirstRun();
            else if (mode == Mode.REGENERATION) proceedRegeneration();
        };
    }

    private void proceedFirstRun() {
        try {
            keySetupService.apply();
            startActivity(new Intent(getApplicationContext(), NotesListActivity.class));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
                App.getAppContainer()
                        .getKeyUpdateService()
                        .updateEncryptedData(keySetupService.getCryptoKey());
                startActivity(new Intent(getApplicationContext(), NotesListActivity.class));
                finish();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
