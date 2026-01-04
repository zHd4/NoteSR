/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.security;

import static androidx.core.view.inputmethod.EditorInfoCompat.IME_FLAG_NO_PERSONALIZED_LEARNING;

import static app.notesr.core.util.ActivityUtils.disableBackButton;
import static app.notesr.core.util.ActivityUtils.showToastMessage;
import static app.notesr.core.util.CharUtils.charsToBytes;
import static app.notesr.core.util.KeyUtils.getKeyBytesFromHex;

import android.content.Context;
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
import app.notesr.core.security.crypto.CryptoManager;
import app.notesr.core.security.crypto.CryptoManagerProvider;

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Objects;

public final class KeyRecoveryActivity extends ActivityBase {
    private static final String TAG = KeyRecoveryActivity.class.toString();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_key_recovery);

        ActionBar actionBar = getSupportActionBar();
        Objects.requireNonNull(actionBar).setTitle(getString(R.string.key_recovery));

        EditText hexKeyField = findViewById(R.id.importRecoveryKeyField);
        Button applyButton = findViewById(R.id.applyRecoveryKeyButton);

        disableBackButton(this);

        hexKeyField.setImeOptions(IME_FLAG_NO_PERSONALIZED_LEARNING);
        applyButton.setOnClickListener(applyButtonOnClick(hexKeyField));
    }

    private View.OnClickListener applyButtonOnClick(EditText hexKeyField) {
        return view -> {
            Editable hexKeyEditable = hexKeyField.getText();
            int hexKeyLength = hexKeyEditable.length();

            if (hexKeyLength > 0) {
                char[] hexKey = new char[hexKeyLength];
                hexKeyEditable.getChars(0, hexKeyLength, hexKey, 0);

                try {
                    apply(hexKeyField, hexKey);
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Invalid key", e);
                    showToastMessage(this, getString(R.string.invalid_key),
                            Toast.LENGTH_SHORT);
                } catch (CharacterCodingException e) {
                    throw new RuntimeException(e);
                } catch (IOException | NoSuchAlgorithmException e) {
                    Log.e(TAG, e.toString());
                    throw new RuntimeException(e);
                }
            }
        };
    }

    private void apply(EditText hexKeyField, char[] hexKey)
            throws IOException, NoSuchAlgorithmException {

        byte[] keyBytes = getKeyBytesFromHex(Arrays.copyOf(hexKey, hexKey.length));

        Context context = getApplicationContext();
        CryptoManager cryptoManager = CryptoManagerProvider.getInstance(context);

        if (cryptoManager.verifyKey(context, keyBytes)) {
            SecretCache.put(AuthActivity.HEX_KEY, charsToBytes(hexKey, StandardCharsets.UTF_8));

            // The hex key has already been wiped by charsToBytes
            wipeSecretData(keyBytes, hexKeyField);

            startActivity(new Intent(context, AuthActivity.class)
                    .putExtra(AuthActivity.EXTRA_MODE, AuthActivity.Mode.KEY_RECOVERY.toString()));

            finish();
        } else {
            showToastMessage(this,
                    getString(R.string.wrong_key),
                    Toast.LENGTH_SHORT);
        }
    }

    private void wipeSecretData(byte[] keyBytes, EditText keyField) {
        Arrays.fill(keyBytes, (byte) 0);
        keyField.getText().replace(0, keyField.getText().length(), "");
        keyField.setText("");
    }
}
