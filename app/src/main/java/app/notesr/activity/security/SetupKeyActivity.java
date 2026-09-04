/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.security;

import static java.util.Objects.requireNonNull;

import static app.notesr.core.util.KeyUtils.getKeyHexFromKeyBytes;

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

import app.notesr.R;
import app.notesr.activity.ActivityBase;
import app.notesr.core.security.SecretCache;
import app.notesr.service.migration.DataVersionManager;
import app.notesr.service.security.AppSecurityService;
import app.notesr.util.ActivityUtils;
import lombok.Getter;

@Getter
public final class SetupKeyActivity extends ActivityBase {

    public static final String CACHE_KEY_PASSWORD = "password";
    public static final String EXTRA_MODE = "mode";
    private static final int LOW_SCREEN_HEIGHT = 800;
    private static final float KEY_VIEW_TEXT_SIZE_FOR_LOW_SCREEN_HEIGHT = 16;

    private KeySetupMode mode;
    private ActivityResultLauncher<Intent> importKeyLauncher;
    private DataVersionManager dataVersionManager;
    private AppSecurityService appSecurityService;
    private ActivityUtils activityUtils;

    private byte[] newKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_key);
        applyInsets(findViewById(R.id.main));

        mode = KeySetupMode.fromValue(requireNonNull(getIntent().getStringExtra(EXTRA_MODE)));
        ActionBar actionBar = requireNonNull(getSupportActionBar());

        actionBar.setDisplayHomeAsUpEnabled(mode != KeySetupMode.FIRST_RUN);
        actionBar.setTitle(R.string.key_setup);

        importKeyLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), getImportKeyCallback());
        dataVersionManager = new DataVersionManager(getApplicationContext());
        appSecurityService = getAppSecurityService();
        activityUtils = new ActivityUtils(this);

        newKey = appSecurityService.generateMasterKey();
        showKeyHex(newKey);

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
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void finish() {
        wipeKeyView();

        if (mode == KeySetupMode.FIRST_RUN) {
            clearCache();
        }

        super.finish();
    }

    private OnBackPressedCallback getOnBackPressedCallback() {
        return new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        };
    }

    private void showKeyHex(byte[] keyBytes) {
        char[] newHexKey = getKeyHexFromKeyBytes(keyBytes);

        TextView keyView = findViewById(R.id.hexKey);
        keyView.setText(newHexKey, 0, newHexKey.length);

        if (getResources().getDisplayMetrics().heightPixels <= LOW_SCREEN_HEIGHT) {
            keyView.setTextSize(KEY_VIEW_TEXT_SIZE_FOR_LOW_SCREEN_HEIGHT);
        }
    }

    private View.OnClickListener copyKeyButtonOnClick() {
        return view -> {
            String keyHex = ((TextView) findViewById(R.id.hexKey)).getText().toString();

            activityUtils.copyToClipboard(keyHex);
            activityUtils.showToastMessage(getString(R.string.copied), Toast.LENGTH_SHORT);
        };
    }

    private View.OnClickListener importKeyButtonOnClick() {
        return view -> {
            Intent intent = new Intent(getApplicationContext(), ImportKeyActivity.class);
            importKeyLauncher.launch(intent);
        };
    }

    private ActivityResultCallback<ActivityResult> getImportKeyCallback() {
        return result -> {
            if (result.getResultCode() == RESULT_OK) {
                newKey = SecretCache.take(ImportKeyActivity.CACHE_KEY_HEX_KEY);
                handleKeyCompletion(getCompletionHandler(newKey));
            }
        };
    }

    private AppSecurityService getAppSecurityService() {
        return new AppSecurityService(getApplicationContext());
    }

    private View.OnClickListener nextButtonOnClick() {
        return view -> handleKeyCompletion(getCompletionHandler(newKey));
    }

    private KeySetupCompletionHandler getCompletionHandler(byte[] keyBytes) {
        return new KeySetupCompletionHandler(this, appSecurityService, keyBytes);
    }

    private void handleKeyCompletion(KeySetupCompletionHandler handler) {
        if (mode == KeySetupMode.FIRST_RUN) {
            handler.proceedFirstRun(getDataVersionManager());
        } else {
            handler.proceedRegeneration();
        }
    }

    private void clearCache() {
        SecretCache.removeIfExists(ImportKeyActivity.CACHE_KEY_HEX_KEY);
        SecretCache.removeIfExists(SetupKeyActivity.CACHE_KEY_PASSWORD);
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
