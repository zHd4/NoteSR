/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.security.password;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import java.util.List;

import javax.inject.Inject;

import app.notesr.R;
import app.notesr.activity.ActivityBase;
import app.notesr.activity.FsaResolver;
import app.notesr.core.util.SecureStringBuilder;
import app.notesr.service.AndroidServiceBootstrapper;
import app.notesr.service.AndroidServiceRegistry;
import app.notesr.service.migration.DataVersionManager;
import app.notesr.service.security.AppSecurityService;
import app.notesr.service.security.rotation.SecretsRotationService;
import app.notesr.util.ActivityUtils;
import dagger.hilt.android.AndroidEntryPoint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AndroidEntryPoint
public final class AppPasswordActivity extends ActivityBase {

    private static final String TAG = AppPasswordActivity.class.getSimpleName();

    public static final String CACHE_KEY_HEX_KEY = "hexKey";
    public static final String EXTRA_MODE = "mode";

    private Mode currentMode;

    @Getter(AccessLevel.PROTECTED)
    private final SecureStringBuilder passwordBuilder = new SecureStringBuilder();

    @Getter(AccessLevel.PROTECTED)
    private boolean capsLockEnabled = false;

    @Getter(AccessLevel.PROTECTED)
    private boolean showingSymbols = false;

    private LinearLayout keyboardContainer;

    @Inject
    ActivityUtils activityUtils;

    @Inject
    AppSecurityService appSecurityService;

    @Inject
    AndroidServiceRegistry serviceRegistry;

    @Inject
    AndroidServiceBootstrapper serviceBootstrapper;

    @Inject
    FsaResolver fsaResolver;

    @Inject
    SecretsRotationService secretsRotationService;

    @Inject
    DataVersionManager dataVersionManager;

    private AuthenticationHandler authenticationHandler;
    private PasswordSetupHandler passwordSetupHandler;
    private KeyRecoveryHandler keyRecoveryHandler;
    private PasswordChangeHandler passwordChangeHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_password);
        applyInsets(findViewById(R.id.main));

        var passwordHelper = new PasswordHelper(
                this, passwordBuilder);

        var passwordFlowCompletionHelper = new PasswordFlowCompletionHelper(
                this, appSecurityService, fsaResolver, serviceBootstrapper,
                dataVersionManager);

        authenticationHandler = new AuthenticationHandler(
                this, appSecurityService, passwordBuilder, activityUtils,
                passwordFlowCompletionHelper, passwordHelper);

        passwordSetupHandler = new PasswordSetupHandler(this, passwordBuilder,
                activityUtils, passwordHelper, passwordFlowCompletionHelper);

        keyRecoveryHandler = new KeyRecoveryHandler(this, appSecurityService,
                passwordBuilder, activityUtils, passwordFlowCompletionHelper,
                passwordHelper);
        passwordChangeHandler = new PasswordChangeHandler(this, passwordBuilder,
                activityUtils, secretsRotationService, passwordFlowCompletionHelper,
                passwordHelper);

        currentMode = Mode.fromString(getIntent().getStringExtra(EXTRA_MODE));
        keyboardContainer = findViewById(R.id.keyboardContainer);

        validateModePermitted();
        configure();
        buildKeyboard();
    }

    void validateModePermitted() {
        if (currentMode == Mode.CREATE_PASSWORD && appSecurityService.isKeyExists()) {
            throw new IllegalStateException(
                    "Key already exists, so CREATE_PASSWORD mode is not permitted," +
                            " use CHANGE_PASSWORD instead");
        }
    }

    private void configure() {
        TextView topLabel = findViewById(R.id.authTopLabel);

        Button capsButton = findViewById(R.id.capsButton);
        Button backspaceButton = findViewById(R.id.pinBackspaceButton);
        Button okButton = findViewById(R.id.okButton);
        Button changeLayoutButton = findViewById(R.id.changeKeyboardLayoutButton);

        switch (currentMode) {
            case AUTHENTICATION -> {
                topLabel.setText(R.string.enter_access_code);
                activityUtils.disableBackButton();
            }
            case CHANGE_PASSWORD -> topLabel.setText(R.string.create_new_access_code);
            default -> topLabel.setText(R.string.create_access_code);
        }

        capsButton.setOnClickListener(getCapsButtonClickListener());

        backspaceButton.setOnClickListener(getBackspaceButtonClickListener());
        okButton.setOnClickListener(getOkButtonClickListener());
        changeLayoutButton.setOnClickListener(getChangeLayoutButtonClickListener());
    }

    private View.OnClickListener getCapsButtonClickListener() {
        return view -> {
            capsLockEnabled = !capsLockEnabled;

            int colorId = capsLockEnabled
                    ? R.color.caps_button_pressed
                    : R.color.caps_button_unpressed;

            Button capsButton = (Button) view;
            capsButton.setTextColor(ContextCompat.getColor(getApplicationContext(), colorId));

            if (!showingSymbols) {
                buildKeyboard();
            }
        };
    }

    private View.OnClickListener getBackspaceButtonClickListener() {
        return view -> {
            TextView censoredPasswordView = findViewById(R.id.censoredPasswordTextView);

            if (passwordBuilder.length() > 0) {
                String censoredPassword = censoredPasswordView.getText().toString();

                censoredPasswordView.setText(censoredPassword.substring(0,
                        censoredPassword.length() - 1));

                passwordBuilder.deleteCharAt(passwordBuilder.length() - 1);
            }
        };
    }

    private View.OnClickListener getOkButtonClickListener() {
        return view -> {
            switch (currentMode) {
                case AUTHENTICATION -> authenticate();
                case CREATE_PASSWORD -> createPassword();
                case CHANGE_PASSWORD -> changePassword();
                case KEY_RECOVERY -> recoverKey();
            }
        };
    }

    private View.OnClickListener getChangeLayoutButtonClickListener() {
        return view -> {
            showingSymbols = !showingSymbols;

            String buttonText = getString(showingSymbols
                    ? R.string.abc
                    : R.string.special_chars);

            Button changeLayoutButton = (Button) view;
            changeLayoutButton.setText(buttonText);

            buildKeyboard();
        };
    }

    private void authenticate() {
        try {
            authenticationHandler.authenticate();
        } catch (Exception e) {
            Log.e(TAG, "Error occurred while authenticating", e);
            activityUtils.showToastMessage(getString(R.string.authentication_error),
                    Toast.LENGTH_SHORT);
        }
    }

    private void createPassword() {
        try {
            passwordSetupHandler.createPassword();
        } catch (Exception e) {
            Log.e(TAG, "Error occurred while creating password", e);
            activityUtils.showToastMessage(getString(R.string.password_creation_error),
                    Toast.LENGTH_SHORT);
        }
    }

    private void changePassword() {
        try {
            passwordChangeHandler.changePassword();
        } catch (Exception e) {
            Log.e(TAG, "Error occurred while changing password", e);
            activityUtils.showToastMessage(getString(R.string.password_change_error),
                    Toast.LENGTH_SHORT);
        }
    }

    private void recoverKey() {
        try {
            keyRecoveryHandler.recoverKey();
        } catch (Exception e) {
            Log.e(TAG, "Error occurred while recovering key", e);
            activityUtils.showToastMessage(getString(R.string.key_recovery_error),
                    Toast.LENGTH_SHORT);
        }
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
            if (mode == null) {
                throw new IllegalArgumentException("Invalid auth activity mode: null");
            }

            for (Mode m : Mode.values()) {
                if (m.modeName.equals(mode)) {
                    return m;
                }
            }

            throw new IllegalArgumentException("Invalid auth activity mode: " + mode);
        }
    }
}
