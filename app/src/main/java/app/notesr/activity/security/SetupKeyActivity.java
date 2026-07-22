/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.security;

import static java.util.Objects.requireNonNull;

import static app.notesr.core.util.ActivityUtils.copyToClipboard;
import static app.notesr.core.util.ActivityUtils.showToastMessage;
import static app.notesr.core.util.CharUtils.bytesToChars;
import static app.notesr.core.util.CharUtils.charsToBytes;
import static app.notesr.core.util.KeyUtils.getKeyHexFromSecrets;
import static app.notesr.core.util.KeyUtils.getSecretsFromHex;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import app.notesr.R;
import app.notesr.activity.ActivityBase;
import app.notesr.core.security.SecretCache;
import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.service.security.AppSecurityService;
import lombok.Getter;

@Getter
public final class SetupKeyActivity extends ActivityBase {

    public static final String CACHE_KEY_PASSWORD = "password";
    public static final String EXTRA_MODE = "mode";
    private static final int LOW_SCREEN_HEIGHT = 800;
    private static final float KEY_VIEW_TEXT_SIZE_FOR_LOW_SCREEN_HEIGHT = 16;

    private KeySetupMode mode;
    private char[] password;
    private ActivityResultLauncher<Intent> importKeyLauncher;
    private AppSecurityService appSecurityService;
    private CryptoSecrets cryptoSecrets;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_key);
        applyInsets(findViewById(R.id.main));

        mode = KeySetupMode.valueOf(requireNonNull(getIntent().getStringExtra(EXTRA_MODE)));
        ActionBar actionBar = requireNonNull(getSupportActionBar());

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.key_setup);

        password = getPasswordFromCache();

        importKeyLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), getImportKeyCallback());
        appSecurityService = getAppSecurityService();
        cryptoSecrets = appSecurityService.getSecretsWithRandomKey(password);

        char[] hexKey = getKeyHexFromSecrets(cryptoSecrets);
        showHexKey(hexKey);

        Button copyToClipboardButton = findViewById(R.id.copyAesKeyHex);
        Button importButton = findViewById(R.id.importHexKeyButton);
        Button nextButton = findViewById(R.id.keySetupNextButton);

        copyToClipboardButton.setOnClickListener(copyKeyButtonOnClick());
        importButton.setOnClickListener(importKeyButtonOnClick());
        nextButton.setOnClickListener(nextButtonOnClick());

        getOnBackPressedDispatcher().addCallback(this, getOnBackPressedCallback());
    }

    @Override
    protected boolean requiresSession() {
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressedAction();
            finish();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void finish() {
        wipeKeyView();
        super.finish();
    }

    private OnBackPressedCallback getOnBackPressedCallback() {
        return new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                onBackPressedAction();
                finish();
            }
        };
    }

    private void onBackPressedAction() {
        SecretCache.removeIfExists(CACHE_KEY_PASSWORD);
    }

    private void showHexKey(char[] hexKey) {
        TextView keyView = findViewById(R.id.hexKey);
        keyView.setText(hexKey, 0, hexKey.length);

        if (getResources().getDisplayMetrics().heightPixels <= LOW_SCREEN_HEIGHT) {
            keyView.setTextSize(KEY_VIEW_TEXT_SIZE_FOR_LOW_SCREEN_HEIGHT);
        }
    }

    private View.OnClickListener copyKeyButtonOnClick() {
        return view -> {
            String keyHex = ((TextView) findViewById(R.id.hexKey)).getText().toString();

            copyToClipboard(this, keyHex);
            showToastMessage(this, getString(R.string.copied), Toast.LENGTH_SHORT);
        };
    }

    private View.OnClickListener importKeyButtonOnClick() {
        return view -> {
            char[] passwordCopy = Arrays.copyOf(password, password.length);

            try {
                byte[] passwordBytes = charsToBytes(passwordCopy, StandardCharsets.UTF_8);
                SecretCache.put(ImportKeyActivity.CACHE_KEY_PASSWORD, passwordBytes);
            } catch (CharacterCodingException e) {
                throw new RuntimeException(e);
            }

            Intent intent = new Intent(getApplicationContext(), ImportKeyActivity.class);
            importKeyLauncher.launch(intent);
        };
    }

    private ActivityResultCallback<ActivityResult> getImportKeyCallback() {
        return result -> {
            if (result.getResultCode() == RESULT_OK) {
                byte[] hexKeyBytes = SecretCache.take(ImportKeyActivity.CACHE_KEY_HEX_KEY);

                try {
                    char[] hexKey = bytesToChars(hexKeyBytes, StandardCharsets.UTF_8);
                    cryptoSecrets = getSecretsFromHex(hexKey, password);
                    getCompletionHandler(cryptoSecrets).handle();
                } catch (CharacterCodingException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    private char[] getPasswordFromCache() {
        try {
            byte[] passwordBytes = requireNonNull(SecretCache.take(CACHE_KEY_PASSWORD),
                    "Password missing in secret cache");
            return bytesToChars(passwordBytes, StandardCharsets.UTF_8);
        } catch (CharacterCodingException e) {
            throw new RuntimeException(e);
        }
    }

    private AppSecurityService getAppSecurityService() {
        return new AppSecurityService(getApplicationContext());
    }

    private View.OnClickListener nextButtonOnClick() {
        return view -> getCompletionHandler(cryptoSecrets).handle();
    }

    private KeySetupCompletionHandler getCompletionHandler(CryptoSecrets cryptoSecrets) {
        return new KeySetupCompletionHandler(this, appSecurityService, mode, cryptoSecrets);
    }

    private void wipeKeyView() {
        TextView keyView = findViewById(R.id.hexKey);
        CharSequence seq = keyView.getText();

        if (seq != null && seq.length() > 0) {
            try {
                if (seq instanceof Editable e) {
                    int len = e.length();

                    for (int i = 0; i < len; i++) {
                        e.replace(i, i + 1, "\u0000");
                    }

                    e.clear();
                }
            } finally {
                keyView.setText("");
            }
        }
    }
}
