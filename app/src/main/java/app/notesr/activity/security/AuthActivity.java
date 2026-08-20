/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.security;

import static app.notesr.util.ActivityUtils.disableBackButton;

import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.List;

import app.notesr.R;
import app.notesr.activity.ActivityBase;
import app.notesr.activity.FsaResolver;
import app.notesr.core.util.SecureStringBuilder;
import app.notesr.service.AndroidServiceBootstrapper;
import app.notesr.service.AndroidServiceRegistry;
import app.notesr.service.migration.DataVersionManager;
import app.notesr.service.security.AppSecurityService;
import app.notesr.service.security.rotation.SecretsRotationService;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

public final class AuthActivity extends ActivityBase {

    public static final String CACHE_KEY_HEX_KEY = "hexKey";
    public static final String EXTRA_MODE = "mode";

    private AuthHandler authHandler;
    private Mode currentMode;

    @Getter(AccessLevel.PROTECTED)
    private final SecureStringBuilder passwordBuilder = new SecureStringBuilder();

    @Getter(AccessLevel.PROTECTED)
    private boolean capsLockEnabled = false;

    @Getter(AccessLevel.PROTECTED)
    private boolean showingSymbols = false;

    private LinearLayout keyboardContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);
        applyInsets(findViewById(R.id.main));


        var appSecurityService = new AppSecurityService(getApplicationContext());
        var secretsRotationService = new SecretsRotationService(getApplicationContext(),
                appSecurityService);

        var serviceRegistry = AndroidServiceRegistry.getInstance(getApplicationContext());
        var serviceBootstrapper = new AndroidServiceBootstrapper(serviceRegistry);
        var fsaResolver = new FsaResolver(serviceRegistry);
        var dataVersionManager = new DataVersionManager(getApplicationContext());

        authHandler = new AuthHandler(this, appSecurityService, secretsRotationService,
                passwordBuilder, fsaResolver, serviceBootstrapper, dataVersionManager);

        currentMode = getModeFromIntent();
        keyboardContainer = findViewById(R.id.keyboardContainer);

        configure();
        buildKeyboard();
    }

    @Override
    protected boolean requiresSession() {
        return false;
    }

    private Mode getModeFromIntent() {
        String mode = getIntent().getStringExtra(EXTRA_MODE);

        try {
            return Mode.fromString(mode);
        } catch (Exception e) {
            throw new RuntimeException("Invalid or missing mode: " + mode, e);
        }
    }

    private void configure() {
        TextView topLabel = findViewById(R.id.authTopLabel);

        Button capsButton = findViewById(R.id.capsButton);
        Button backspaceButton = findViewById(R.id.pinBackspaceButton);
        Button authButton = findViewById(R.id.authButton);
        Button changeLayoutButton = findViewById(R.id.changeKeyboardLayoutButton);

        switch (currentMode) {
            case AUTHENTICATION -> {
                topLabel.setText(R.string.enter_access_code);
                disableBackButton(this);
            }
            case CHANGE_PASSWORD -> topLabel.setText(R.string.create_new_access_code);
            default -> topLabel.setText(R.string.create_access_code);
        }

        capsButton.setOnClickListener(view -> {
            capsLockEnabled = !capsLockEnabled;

            int colorId = capsLockEnabled
                    ? R.color.caps_button_pressed
                    : R.color.caps_button_unpressed;

            capsButton.setTextColor(ContextCompat.getColor(getApplicationContext(), colorId));

            if (!showingSymbols) {
                buildKeyboard();
            }
        });

        backspaceButton.setOnClickListener(view -> {
            TextView censoredPasswordView = findViewById(R.id.censoredPasswordTextView);

            if (passwordBuilder.length() > 0) {
                String censoredPassword = censoredPasswordView.getText().toString();

                censoredPasswordView.setText(censoredPassword.substring(0,
                        censoredPassword.length() - 1));

                passwordBuilder.deleteCharAt(passwordBuilder.length() - 1);
            }
        });

        authButton.setOnClickListener(view -> {
            switch (currentMode) {
                case AUTHENTICATION -> authHandler.authenticate();
                case CREATE_PASSWORD -> authHandler.createPassword();
                case KEY_RECOVERY -> authHandler.recoverKey();
                case CHANGE_PASSWORD -> authHandler.changePassword();
            }
        });

        changeLayoutButton.setOnClickListener(view -> {
            showingSymbols = !showingSymbols;

            String buttonText = getString(showingSymbols
                    ? R.string.abc
                    : R.string.special_chars);

            changeLayoutButton.setText(buttonText);

            buildKeyboard();
        });
    }

    private void buildKeyboard() {
        keyboardContainer.removeAllViews();

        if (showingSymbols) {
            buildSymbolKeyboard();
        } else {
            buildAlphaNumericKeyboard();
        }
    }

    private void buildAlphaNumericKeyboard() {
        addKeyboardRow(List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"));
        addKeyboardRow(List.of("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"));
        addKeyboardRow(List.of("a", "s", "d", "f", "g", "h", "j", "k", "l"));
        addKeyboardRow(List.of("z", "x", "c", "v", "b", "n", "m"));
    }

    private void buildSymbolKeyboard() {
        addKeyboardRow(List.of("!", "@", "#", "$", "%", "^", "&", "*", "(", ")"));
        addKeyboardRow(List.of("-", "_", "+", "=", "{", "}", "[", "]", "|", "\\"));
        addKeyboardRow(List.of(":", ";", "<", ">", "?", "/", "~", ".", ",", "'"));
    }

    private void addKeyboardRow(List<String> keys) {
        LinearLayout row = new LinearLayout(this);

        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        for (String key : keys) {
            Button button = new Button(this);
            button.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            String displayText;

            if (capsLockEnabled && !showingSymbols) {
                displayText = key.toUpperCase();
                button.setAllCaps(true);
            } else {
                displayText = key;
                button.setAllCaps(false);
            }

            button.setText(displayText);
            button.setBackgroundResource(R.drawable.pin_button);

            button.setOnClickListener(view -> {
                TextView censoredPasswordView = findViewById(R.id.censoredPasswordTextView);

                String charToAppend = capsLockEnabled && !showingSymbols
                        ? key.toUpperCase()
                        : key;

                passwordBuilder.append(charToAppend);

                String newCensoredPasswordText = censoredPasswordView.getText() + "•";
                censoredPasswordView.setText(newCensoredPasswordText);
            });

            row.addView(button);
        }

        keyboardContainer.addView(row);
    }

    @AllArgsConstructor
    @Getter
    public enum Mode {
        AUTHENTICATION("authentication"),
        CREATE_PASSWORD("create_password"),
        CHANGE_PASSWORD("change_password"),
        KEY_RECOVERY("key_recovery");

        private final String modeName;

        public static Mode fromString(String mode) {
            for (Mode m : Mode.values()) {
                if (m.modeName.equals(mode)) {
                    return m;
                }
            }

            throw new IllegalArgumentException("Invalid auth activity mode: " + mode);
        }
    }
}
