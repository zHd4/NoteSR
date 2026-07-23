/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.security;

import static androidx.core.view.inputmethod.EditorInfoCompat.IME_FLAG_NO_PERSONALIZED_LEARNING;

import static java.util.Objects.requireNonNull;

import static app.notesr.core.util.ActivityUtils.showToastMessage;
import static app.notesr.core.util.CharUtils.bytesToChars;
import static app.notesr.core.util.CharUtils.charsToBytes;
import static app.notesr.core.util.KeyUtils.getSecretsFromHex;

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
import app.notesr.core.security.dto.CryptoSecrets;

public final class ImportKeyActivity extends ActivityBase {

    public static final String CACHE_KEY_HEX_KEY = "hexKey";
    public static final String CACHE_KEY_PASSWORD = "password";
    private static final String TAG = ImportKeyActivity.class.getCanonicalName();

    private int resultCode = RESULT_CANCELED;
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
        wipeUiFields();
        setResult(resultCode);
        super.finish();
    }

    private View.OnClickListener importKeyButtonOnClick() {
        return view -> {
            Editable hexKeyEditable = keyField.getText();

            hexKey = new char[hexKeyEditable.length()];
            hexKeyEditable.getChars(0, hexKeyEditable.length(), hexKey, 0);

            if (hexKey.length > 0) {
                try {
                    CryptoSecrets cryptoSecrets = getCryptoSecrets(hexKey, password);
                    cryptoSecrets.validate();
                    cryptoSecrets.destroy();
                } catch (IllegalArgumentException | IllegalStateException e) {
                    Log.e(TAG, "Invalid key", e);
                    showToastMessage(this, getString(R.string.invalid_key),
                            Toast.LENGTH_SHORT);

                    return;
                }

                putResultsToCache(hexKey);
                
                resultCode = RESULT_OK;
                finish();
            }
        };
    }
    
    private CryptoSecrets getCryptoSecrets(char[] hexKey, char[] password) {
        char[] hexKeyCopy = Arrays.copyOf(hexKey, hexKey.length);
        return getSecretsFromHex(hexKeyCopy, password);
    }
    
    private void putResultsToCache(char[] hexKey) {
        try {
            SecretCache.put(CACHE_KEY_HEX_KEY, charsToBytes(hexKey,
                    StandardCharsets.UTF_8));
        } catch (CharacterCodingException e) {
            throw new RuntimeException(e);
        }

        SecretCache.removeIfExists(CACHE_KEY_PASSWORD); // Should be already removed
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

    private void wipeUiFields() {
        Editable hexKeyEditable = keyField.getText();
        hexKeyEditable.replace(0, hexKeyEditable.length(), "");

        keyField.setText("");
    }
}
