/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.security;

import static androidx.core.view.inputmethod.EditorInfoCompat.IME_FLAG_NO_PERSONALIZED_LEARNING;

import static app.notesr.core.util.ActivityUtils.disableBackButton;
import static app.notesr.core.util.ActivityUtils.showToastMessage;
import static app.notesr.core.util.CharUtils.charsToBytes;
import static app.notesr.core.util.KeyUtils.getKeyBytesFromKeyHex;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;

import app.notesr.R;
import app.notesr.activity.ActivityBase;
import app.notesr.core.security.SecretCache;
import app.notesr.service.security.AppSecurityException;
import app.notesr.service.security.AppSecurityService;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

public final class KeyRecoveryActivity extends ActivityBase {
    private static final String TAG = KeyRecoveryActivity.class.getSimpleName();

    private AppSecurityService appSecurityService;
    private EditText hexKeyField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_key_recovery);
        applyInsets(findViewById(R.id.main));

        appSecurityService = new AppSecurityService(getApplicationContext());

        ActionBar actionBar = getSupportActionBar();
        Objects.requireNonNull(actionBar).setTitle(getString(R.string.key_recovery));

        hexKeyField = findViewById(R.id.importRecoveryKeyField);
        Button applyButton = findViewById(R.id.applyRecoveryKeyButton);

        disableBackButton(this);

        hexKeyField.setImeOptions(IME_FLAG_NO_PERSONALIZED_LEARNING);
        applyButton.setOnClickListener(getApplyButtonOnClickListener());
    }

    @Override
    protected boolean requiresSession() {
        return false;
    }

    @Override
    public void finish() {
        if (hexKeyField != null) {
            hexKeyField.getText().replace(0, hexKeyField.getText().length(), "");
            hexKeyField.setText("");
        }

        super.finish();
    }

    private View.OnClickListener getApplyButtonOnClickListener() {
        return view -> {
            Editable hexKeyEditable = hexKeyField.getText();
            int hexKeyLength = hexKeyEditable.length();

            if (hexKeyLength > 0) {
                char[] hexKey = new char[hexKeyLength];
                hexKeyEditable.getChars(0, hexKeyLength, hexKey, 0);

                try {
                    if (isMatch(hexKey)) {
                        proceedKeyMatch(hexKey);
                    } else {
                        proceedKeyMismatch();
                    }
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Invalid key", e);
                    showToastMessage(this, getString(R.string.invalid_key),
                            Toast.LENGTH_SHORT);
                } catch (AppSecurityException | CharacterCodingException e) {
                    Log.e(TAG, e.toString());
                    throw new RuntimeException(e);
                } finally {
                    Arrays.fill(hexKey, '\0');
                }
            }
        };
    }

    private boolean isMatch(char[] hexKey) {
        byte[] keyBytes = getKeyBytesFromKeyHex(Arrays.copyOf(hexKey, hexKey.length));
        boolean isMatch = appSecurityService.isKeyMatchingWithStored(keyBytes);

        Arrays.fill(keyBytes, (byte) 0);
        return isMatch;
    }

    private void proceedKeyMatch(char[] hexKey) throws CharacterCodingException {
        byte[] hexKeyBytes = charsToBytes(Arrays.copyOf(hexKey, hexKey.length),
                StandardCharsets.UTF_8);
        SecretCache.put(AuthActivity.CACHE_KEY_HEX_KEY, hexKeyBytes);

        var targetMode = AuthActivity.Mode.KEY_RECOVERY;
        var authActivityIntent = new Intent(getApplicationContext(), AuthActivity.class)
                .putExtra(AuthActivity.EXTRA_MODE, targetMode.toString());

        startActivity(authActivityIntent);
        finish();
    }

    private void proceedKeyMismatch() {
        showToastMessage(this,
                getString(R.string.wrong_key),
                Toast.LENGTH_SHORT);
    }
}
