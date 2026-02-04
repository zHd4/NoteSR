/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.security;

import static app.notesr.core.util.ActivityUtils.disableBackButton;

import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.List;

import app.notesr.R;
import app.notesr.activity.ActivityBase;
import app.notesr.core.security.crypto.CryptoManager;
import app.notesr.core.security.crypto.CryptoManagerProvider;
import app.notesr.core.util.SecureStringBuilder;
import lombok.AllArgsConstructor;
import lombok.Getter;

public final class AuthActivity extends ActivityBase {

    public static final String HEX_KEY = "hex_key";
    public static final String EXTRA_MODE = "mode";

    @AllArgsConstructor
    @Getter
    public enum Mode {
        AUTHORIZATION("authorization"),
        CREATE_PASSWORD("create_password"),
        CHANGE_PASSWORD("change_password"),
        KEY_RECOVERY("key_recovery");

        private final String mode;
    }

    private AuthActivityExtension extension;
    private Mode currentMode;

    private final SecureStringBuilder passwordBuilder = new SecureStringBuilder();

    private boolean capsLockEnabled = false;
    private boolean showingSymbols = false;

    private LinearLayout keyboardContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);
        applyInsets(findViewById(R.id.main));

        String mode = getIntent().getStringExtra(EXTRA_MODE);
        CryptoManager cryptoManager = CryptoManagerProvider.getInstance(getApplicationContext());
        extension = new AuthActivityExtension(this, cryptoManager, passwordBuilder);

        try {
            currentMode = Mode.valueOf(mode);
        } catch (Exception e) {
            throw new RuntimeException("Invalid or missing mode: " + mode, e);
        }

        keyboardContainer = findViewById(R.id.keyboardContainer);

        configure();
        buildKeyboard();
    }

    private void configure() {
        TextView topLabel = findViewById(R.id.authTopLabel);

        Button capsButton = findViewById(R.id.capsButton);
        Button backspaceButton = findViewById(R.id.pinBackspaceButton);
        Button authButton = findViewById(R.id.authButton);
        Button changeLayoutButton = findViewById(R.id.changeKeyboardLayoutButton);

        switch (currentMode) {
            case AUTHORIZATION -> {
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
                case AUTHORIZATION -> extension.authorize();
                case CREATE_PASSWORD -> extension.createPassword();
                case KEY_RECOVERY -> extension.recoverKey();
                case CHANGE_PASSWORD -> extension.changePassword();
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
}
