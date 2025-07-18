package app.notesr.crypto;

import static java.util.Objects.requireNonNull;

import static app.notesr.util.ActivityUtils.copyToClipboard;
import static app.notesr.util.ActivityUtils.disableBackButton;
import static app.notesr.util.ActivityUtils.showToastMessage;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import app.notesr.R;
import app.notesr.ActivityBase;
import app.notesr.service.crypto.KeySetupService;
import app.notesr.data.ReEncryptionActivity;
import app.notesr.note.NoteListActivity;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
public class SetupKeyActivity extends ActivityBase {

    @AllArgsConstructor
    public enum Mode {
        FIRST_RUN("first_run"),
        REGENERATION("regeneration");

        public final String mode;
    }

    private static final int LOW_SCREEN_HEIGHT = 800;
    private static final float KEY_VIEW_TEXT_SIZE_FOR_LOW_SCREEN_HEIGHT = 16;

    private Mode mode;
    private String password;
    private KeySetupService keySetupService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_key);

        mode = Mode.valueOf(requireNonNull(getIntent().getStringExtra("mode")));

        if (mode == Mode.REGENERATION) {
            disableBackButton(this);
        }

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

    private void adaptKeyView() {
        TextView keyView = findViewById(R.id.aesKeyHex);

        if (getResources().getDisplayMetrics().heightPixels <= LOW_SCREEN_HEIGHT) {
            keyView.setTextSize(KEY_VIEW_TEXT_SIZE_FOR_LOW_SCREEN_HEIGHT);
        }
    }

    private View.OnClickListener copyKeyButtonOnClick() {
        return view -> {
            String keyHex = ((TextView) findViewById(R.id.aesKeyHex)).getText().toString();

            copyToClipboard(this, keyHex);
            showToastMessage(this, getString(R.string.copied), Toast.LENGTH_SHORT);
        };
    }

    private View.OnClickListener importKeyButtonOnClick() {
        return view -> {
            Intent intent = new Intent(getApplicationContext(), ImportKeyActivity.class)
                    .putExtra("mode", mode.toString())
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
            startActivity(new Intent(getApplicationContext(), NoteListActivity.class));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void proceedRegeneration() {
        Intent intent = new Intent(getApplicationContext(), ReEncryptionActivity.class)
                .putExtra("newCryptoKey", keySetupService.getCryptoKey());
        
        startActivity(intent);
        finish();
    }
}
