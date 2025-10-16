package app.notesr.activity.security;

import static java.util.Objects.requireNonNull;

import static app.notesr.core.util.ActivityUtils.copyToClipboard;
import static app.notesr.core.util.ActivityUtils.disableBackButton;
import static app.notesr.core.util.ActivityUtils.showToastMessage;
import static app.notesr.core.util.CharUtils.bytesToChars;
import static app.notesr.core.util.CharUtils.charsToBytes;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;

import app.notesr.R;
import app.notesr.activity.ActivityBase;
import app.notesr.core.security.SecretCache;
import app.notesr.core.security.crypto.CryptoManager;
import app.notesr.core.security.crypto.CryptoManagerProvider;
import app.notesr.service.security.SecretsSetupService;
import app.notesr.core.util.KeyUtils;
import lombok.Getter;

@Getter
public final class SetupKeyActivity extends ActivityBase {

    private static final int LOW_SCREEN_HEIGHT = 800;
    private static final float KEY_VIEW_TEXT_SIZE_FOR_LOW_SCREEN_HEIGHT = 16;

    private KeySetupMode mode;
    private char[] password;
    private SecretsSetupService keySetupService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_key);

        mode = KeySetupMode.valueOf(requireNonNull(getIntent().getStringExtra("mode")));

        if (mode == KeySetupMode.REGENERATION) {
            disableBackButton(this);
        }

        try {
            password = bytesToChars(SecretCache.take("password"), StandardCharsets.UTF_8);
        } catch (CharacterCodingException e) {
            throw new RuntimeException(e);
        }

        Context context = getApplicationContext();
        CryptoManager cryptoManager = CryptoManagerProvider.getInstance(context);

        keySetupService = new SecretsSetupService(
                getApplicationContext(),
                cryptoManager,
                password
        );

        TextView keyView = findViewById(R.id.aesKeyHex);
        keyView.setText(KeyUtils.getKeyHexFromSecrets(keySetupService.getCryptoSecrets()));

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
                    .putExtra("mode", mode.toString());

            try {
                SecretCache.put("password", charsToBytes(password, StandardCharsets.UTF_8));
            } catch (CharacterCodingException e) {
                throw new RuntimeException(e);
            }

            startActivity(intent);
        };
    }

    private View.OnClickListener nextButtonOnClick() {
        return view ->
                new KeySetupCompletionHandler(this, keySetupService, mode).handle();
    }
}
