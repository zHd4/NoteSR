/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.security.password;

import static java.util.Objects.requireNonNull;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.core.content.ContextCompat;

import java.util.List;

import javax.inject.Inject;

import app.notesr.R;
import app.notesr.activity.ActivityBase;
import app.notesr.activity.FsaResolver;
import app.notesr.activity.security.KeyRecoveryActivity;
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

/**
 * Displays the custom keyboard and coordinates the password-related activity flows.
 *
 * <p>The activity supports authentication, password creation, password change,
 * and key recovery completion. Its behavior is selected by the {@link #EXTRA_MODE} intent
 * extra.</p>
 *
 * <p>
 * TODO: {@link AppPasswordActivity} is currently handles key recovery completion.
 *  It must be refactored to move key recovery logic to the {@link KeyRecoveryActivity} class.
 */
@AndroidEntryPoint
public final class AppPasswordActivity extends ActivityBase {

    private static final String TAG = AppPasswordActivity.class.getSimpleName();

    /**
     * Intent extra containing the mode-specific hex key used by password flows.
     */
    public static final String CACHE_KEY_HEX_KEY = "hexKey";

    /**
     * Intent extra containing the {@link Mode} mode name.
     */
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

    /**
     * Initializes handlers, resolves the requested mode, and builds the keyboard.
     *
     * @param savedInstanceState previously saved activity state, or {@code null}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            currentMode = Mode.fromString(getIntent().getStringExtra(EXTRA_MODE));
        } catch (IllegalArgumentException e) {
            finishWithError(TAG, R.string.app_password_invalid_mode,
                    "Invalid AppPasswordActivity mode", e);
            return;
        }

        try {
            validateModePermitted();
        } catch (IllegalStateException e) {
            finishWithError(TAG, R.string.app_password_invalid_mode,
                    "The requested AppPasswordActivity mode is not permitted"
                            + " for the current state", e);
            return;
        }

        setContentView(R.layout.activity_app_password);
        applyInsets(findViewById(R.id.main));

        var passwordHelper = new PasswordHelper(
                this, passwordBuilder, activityUtils);

        var passwordFlowCompletionHelper = new PasswordFlowCompletionHelper(
                this, appSecurityService, fsaResolver, serviceBootstrapper,
                dataVersionManager);

        authenticationHandler = new AuthenticationHandler(
                this, appSecurityService, passwordBuilder, activityUtils,
                passwordFlowCompletionHelper, passwordHelper);

        passwordSetupHandler = new PasswordSetupHandler(passwordHelper,
                passwordFlowCompletionHelper);

        keyRecoveryHandler = new KeyRecoveryHandler(this, appSecurityService,
                passwordFlowCompletionHelper, passwordHelper);
        passwordChangeHandler = new PasswordChangeHandler(this, activityUtils,
                secretsRotationService, passwordFlowCompletionHelper, passwordHelper);

        keyboardContainer = findViewById(R.id.keyboardContainer);

        configurePasswordMode();
        setupHandlers();
        buildKeyboard();
    }

    /**
     * Checks whether the selected mode is permitted in the current security state.
     *
     * @throws IllegalStateException if the mode is not permitted for the current state
     */
    void validateModePermitted() {
        if (currentMode == Mode.CREATE_PASSWORD) {
            if (appSecurityService.isKeyExists()) {
                throw new IllegalStateException(
                        "Key already exists, so CREATE_PASSWORD mode is not permitted," +
                                " use CHANGE_PASSWORD instead");
            }

            if (appSecurityService.isAppBlocked()) {
                throw new IllegalStateException(
                        "App is blocked, so CREATE_PASSWORD mode is not permitted," +
                                " use KEY_RECOVERY instead");
            }
        }
    }

    /**
     * Applies the UI configuration for the current mode.
     */
    private void configurePasswordMode() {
        ActionBar actionBar = requireNonNull(getSupportActionBar());
        actionBar.setTitle("");

        TextView accessCodePrompt = findViewById(R.id.accessCodePrompt);

        switch (currentMode) {
            case AUTHENTICATION -> {
                accessCodePrompt.setText(R.string.enter_access_code);
                activityUtils.disableBackButton();
            }
            case CHANGE_PASSWORD -> {
                actionBar.setDisplayHomeAsUpEnabled(true);
                accessCodePrompt.setText(R.string.create_new_access_code);
            }
            default -> accessCodePrompt.setText(R.string.create_access_code);
        }
    }

    /**
     * Configures the activity's control handlers.
     */
    private void setupHandlers() {
        Button capsButton = findViewById(R.id.capsButton);
        Button backspaceButton = findViewById(R.id.pinBackspaceButton);
        Button okButton = findViewById(R.id.okButton);
        Button changeLayoutButton = findViewById(R.id.changeKeyboardLayoutButton);

        capsButton.setOnClickListener(getCapsButtonClickListener());
        backspaceButton.setOnClickListener(getBackspaceButtonClickListener());
        okButton.setOnClickListener(getOkButtonClickListener());
        changeLayoutButton.setOnClickListener(getChangeLayoutButtonClickListener());
    }

    /**
     * Creates the click listener for toggling caps lock.
     *
     * @return the caps-lock click listener
     */
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

    /**
     * Creates the click listener for deleting the last entered password character.
     *
     * @return the backspace click listener
     */
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

    /**
     * Creates the click listener that dispatches to the handler for the current mode.
     *
     * @return the confirmation click listener
     */
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

    /**
     * Creates the click listener for switching between alphanumeric and symbol keyboards.
     *
     * @return the keyboard-layout click listener
     */
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

    /**
     * Attempts to authenticate the password entered in the custom keyboard.
     */
    private void authenticate() {
        try {
            authenticationHandler.authenticate();
        } catch (Exception e) {
            finishWithError(TAG, R.string.authentication_error,
                    "Error occurred while authenticating", e);
        }
    }

    /**
     * Attempts to create the app password with the entered characters.
     */
    private void createPassword() {
        try {
            passwordSetupHandler.createPassword();
        } catch (Exception e) {
            finishWithError(TAG, R.string.password_creation_error,
                    "Error occurred while creating password", e);
        }
    }

    /**
     * Attempts to change the existing app password.
     */
    private void changePassword() {
        try {
            passwordChangeHandler.changePassword();
        } catch (Exception e) {
            finishWithError(TAG, R.string.password_change_error,
                    "Error occurred while changing password", e);
        }
    }

    /**
     * Attempts to recover the encryption key using the entered password.
     */
    private void recoverKey() {
        try {
            keyRecoveryHandler.recoverKey();
        } catch (Exception e) {
            finishWithError(TAG, R.string.key_recovery_error,
                    "Error occurred while recovering key", e);
        }
    }

    /**
     * Rebuilds the keyboard using the currently selected layout.
     */
    private void buildKeyboard() {
        keyboardContainer.removeAllViews();

        if (showingSymbols) {
            buildSymbolKeyboard();
        } else {
            buildAlphaNumericKeyboard();
        }
    }

    /**
     * Builds the numeric and alphabetic keyboard layout.
     */
    private void buildAlphaNumericKeyboard() {
        addKeyboardRow(List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"));
        addKeyboardRow(List.of("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"));
        addKeyboardRow(List.of("a", "s", "d", "f", "g", "h", "j", "k", "l"));
        addKeyboardRow(List.of("z", "x", "c", "v", "b", "n", "m"));
    }

    /**
     * Builds the symbol keyboard layout.
     */
    private void buildSymbolKeyboard() {
        addKeyboardRow(List.of("!", "@", "#", "$", "%", "^", "&", "*", "(", ")"));
        addKeyboardRow(List.of("-", "_", "+", "=", "{", "}", "[", "]", "|", "\\"));
        addKeyboardRow(List.of(":", ";", "<", ">", "?", "/", "~", ".", ",", "'"));
    }

    /**
     * Adds one horizontally arranged row of keyboard keys.
     *
     * @param keys key labels to display in the row
     */
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

    /**
     * The password operation selected for this activity instance.
     */
    @AllArgsConstructor
    @Getter
    public enum Mode {
        AUTHENTICATION("authentication"),
        CREATE_PASSWORD("create_password"),
        CHANGE_PASSWORD("change_password"),
        KEY_RECOVERY("key_recovery");

        private final String modeName;

        /**
         * Resolves a mode name to its corresponding {@link Mode} value.
         *
         * @param mode serialized mode name
         * @return the matching mode
         * @throws IllegalArgumentException if the mode is {@code null} or unknown
         */
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
