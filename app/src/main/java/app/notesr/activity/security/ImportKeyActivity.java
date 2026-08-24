/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.security;

import static androidx.core.view.inputmethod.EditorInfoCompat.IME_FLAG_NO_PERSONALIZED_LEARNING;

import static java.util.Objects.requireNonNull;

import static app.notesr.util.ActivityUtils.showToastMessage;
import static app.notesr.core.util.KeyUtils.getKeyBytesFromKeyHex;

import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;

import java.util.Arrays;

import app.notesr.R;
import app.notesr.activity.ActivityBase;
import app.notesr.core.security.SecretCache;
import app.notesr.core.util.CryptoSecretsValidator;

public final class ImportKeyActivity extends ActivityBase {

    public static final String CACHE_KEY_HEX_KEY = "hexKey";
    private static final String TAG = ImportKeyActivity.class.getSimpleName();

    private int resultCode = RESULT_CANCELED;
    private EditText keyField;
    private char[] hexKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_key);
        applyInsets(findViewById(R.id.main));

        ActionBar actionBar = requireNonNull(getSupportActionBar());

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(getResources().getString(R.string.import_key));

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
        wipeSensitiveClassFields();
        setResult(resultCode);
        super.finish();
    }

    private View.OnClickListener importKeyButtonOnClick() {
        return view -> {
            Editable hexKeyEditable = keyField.getText();

            hexKey = new char[hexKeyEditable.length()];
            hexKeyEditable.getChars(0, hexKeyEditable.length(), hexKey, 0);

            if (hexKey.length > 0) {
                byte[] keyBytes;

                try {
                    keyBytes = getKeyBytesFromKeyHex(hexKey);
                    CryptoSecretsValidator.validateKey(keyBytes);
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Invalid key", e);
                    showToastMessage(this, getString(R.string.invalid_key),
                            Toast.LENGTH_SHORT);

                    return;
                }

                SecretCache.put(CACHE_KEY_HEX_KEY, keyBytes);
                resultCode = RESULT_OK;
                finish();
            }
        };
    }


    private void wipeSensitiveClassFields() {
        if (hexKey != null) {
            Arrays.fill(hexKey, '\0');
        }
    }

    private void wipeUiFields() {
        Editable hexKeyEditable = keyField.getText();
        hexKeyEditable.replace(0, hexKeyEditable.length(), "");

        keyField.setText("");
    }
}
