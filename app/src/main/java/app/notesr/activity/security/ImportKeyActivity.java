/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.security;

import static androidx.core.view.inputmethod.EditorInfoCompat.IME_FLAG_NO_PERSONALIZED_LEARNING;

import static java.util.Objects.requireNonNull;

import static app.notesr.core.util.ActivityUtils.showToastMessage;
import static app.notesr.core.util.CharUtils.bytesToChars;
import static app.notesr.core.util.KeyUtils.getSecretsFromHex;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import app.notesr.R;
import app.notesr.activity.ActivityBase;
import app.notesr.core.security.SecretCache;
import app.notesr.core.security.crypto.CryptoManager;
import app.notesr.core.security.crypto.CryptoManagerProvider;
import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.service.security.crypto.setup.SecretsSetupService;

public final class ImportKeyActivity extends ActivityBase {

    public static final String CACHE_KEY_PASSWORD = "password";
    public static final String EXTRA_MODE = "mode";
    private static final String TAG = ImportKeyActivity.class.getCanonicalName();

    private KeySetupMode mode;
    private EditText keyField;
    private char[] hexKey;
    private char[] password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_key);
        applyInsets(findViewById(R.id.main));

        ActionBar actionBar = requireNonNull(getSupportActionBar());

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(getResources().getString(R.string.import_key));

        mode = KeySetupMode.valueOf(requireNonNull(getIntent().getStringExtra(EXTRA_MODE)));
        password = getPasswordFromCache();

        keyField = findViewById(R.id.importKeyField);
        keyField.setImeOptions(IME_FLAG_NO_PERSONALIZED_LEARNING);

        Button importKeyButton = findViewById(R.id.importKeyButton);
        importKeyButton.setOnClickListener(importKeyButtonOnClick());
    }

    @Override
    protected boolean requiresSession() {
        return false;
    }

    @Override
    public void finish() {
        wipeSecrets();
        super.finish();
    }

    private View.OnClickListener importKeyButtonOnClick() {
        return view -> {
            Editable hexKeyEditable = keyField.getText();

            hexKey = new char[hexKeyEditable.length()];
            hexKeyEditable.getChars(0, hexKeyEditable.length(), hexKey, 0);

            if (hexKey.length > 0) {
                Context context = getApplicationContext();
                CryptoManager cryptoManager = CryptoManagerProvider.getInstance(context);

                CryptoSecrets cryptoSecrets;

                try {
                    cryptoSecrets = getSecretsFromHex(hexKey, password);
                    cryptoSecrets.validate();
                } catch (IllegalArgumentException | IllegalStateException e) {
                    Log.e(TAG, "Invalid key", e);
                    showToastMessage(this, getString(R.string.invalid_key),
                            Toast.LENGTH_SHORT);

                    return;
                }

                SecretsSetupService secretsSetupService = new SecretsSetupService(
                        getApplicationContext(),
                        cryptoManager,
                        cryptoSecrets
                );

                new KeySetupCompletionHandler(this, secretsSetupService, mode).handle();
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

    private void wipeSecrets() {
        // Clearing only key field, not password, because reference to password is stored in cache
        // and still could be used by SetupKeyActivity. SetupKeyActivity as a parent activity
        // is responsible to clear password from cache.
        if (hexKey != null && hexKey.length > 0) {
            Arrays.fill(hexKey, '\0');
        }

        Editable hexKeyEditable = keyField.getText();
        hexKeyEditable.replace(0, hexKeyEditable.length(), "");

        keyField.setText("");
    }
}
