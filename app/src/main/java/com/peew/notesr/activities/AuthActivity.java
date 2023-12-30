package com.peew.notesr.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.peew.notesr.App;
import com.peew.notesr.R;
import com.peew.notesr.crypto.CryptoManager;
import com.peew.notesr.crypto.CryptoTools;

import java.util.Arrays;

public class AuthActivity extends ExtendedAppCompatActivity {
    public static final int AUTHORIZATION_MODE = 0;
    public static final int CREATE_PASSWORD_MODE = 1;
    public static final int KEY_RECOVERY_MODE = 2;
    public static final int CHANGE_PASSWORD_MODE = 3;
    private static final int MIN_PASSWORD_LENGTH = 4;
    private static final int MAX_ATTEMPTS = 3;
    private static final int ON_WRONG_PASSWORD_DELAY_MS = 1500;
    private static final Integer[] PIN_BUTTONS_ID = {
            R.id.pin_button_1,
            R.id.pin_button_2,
            R.id.pin_button_3,
            R.id.pin_button_4,
            R.id.pin_button_5,
            R.id.pin_button_6,
            R.id.pin_button_7,
            R.id.pin_button_8,
            R.id.pin_button_9,
            R.id.pin_button_0
    };

    private int currentMode;
    private int inputIndex = 0;
    private boolean capsLockEnabled = false;
    private StringBuilder passwordBuilder = new StringBuilder();
    private final CryptoManager cryptoManager = CryptoManager.getInstance();
    private String password;
    private int attempts = MAX_ATTEMPTS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        currentMode = getIntent().getIntExtra("mode", -1);

        if (currentMode == -1) {
            throw new RuntimeException("Authorization mode didn't provided");
        }

        configure();
    }

    private void configure() {
        TextView topLabel = findViewById(R.id.auth_top_label);

        Button changeInputIndexButton = findViewById(R.id.change_input_index_button);
        Button capsButton = findViewById(R.id.caps_button);

        Button backspaceButton = findViewById(R.id.pin_backspace_button);
        Button authButton = findViewById(R.id.auth_button);

        switch (currentMode) {
            case AUTHORIZATION_MODE -> {
                topLabel.setText(R.string.enter_access_code);
                disableBackButton();
            }

            case CHANGE_PASSWORD_MODE -> topLabel.setText(R.string.create_new_access_code);
            default -> topLabel.setText(R.string.create_access_code);
        }

        Arrays.stream(PIN_BUTTONS_ID).forEach(id -> findViewById(id)
                .setOnClickListener(pinButtonOnClick()));

        changeInputIndexButton.setOnClickListener(changeInputIndexButtonOnClick());
        capsButton.setOnClickListener(capsButtonOnClick());

        backspaceButton.setOnClickListener(pinBackspaceButtonOnClick());
        authButton.setOnClickListener(authButtonOnClick());
    }

    private View.OnClickListener changeInputIndexButtonOnClick() {
        return view -> {
            Button self = ((Button)view);

            switch (inputIndex) {
                case 0 -> {
                    self.setText("A");
                    inputIndex++;
                }
                case 1 -> {
                    self.setText("B");
                    inputIndex++;
                }
                case 2 -> {
                    self.setText("C");
                    inputIndex++;
                }
                case 3 -> {
                    self.setText("1");
                    inputIndex = 0;
                }
            }
        };
    }

    @SuppressLint("SetTextI18n")
    private View.OnClickListener pinButtonOnClick() {
        return view -> {
            Button self = ((Button)view);
            TextView censoredPasswordView = findViewById(R.id.censored_password_text_view);

            char currentChar = self.getText()
                    .toString()
                    .replace("\n", "")
                    .charAt(inputIndex);

            if (capsLockEnabled) {
                passwordBuilder.append(Character.toString(currentChar).toUpperCase());
            } else {
                passwordBuilder.append(Character.toString(currentChar).toLowerCase());
            }

            censoredPasswordView.setText(censoredPasswordView.getText() + "•");
        };
    }

    private View.OnClickListener capsButtonOnClick() {
        return view -> {
            Button self = ((Button)view);
            capsLockEnabled = !capsLockEnabled;

            int colorId = capsLockEnabled ?
                    R.color.caps_button_pressed :
                    R.color.caps_button_unpressed;

            self.setTextColor(ContextCompat.getColor(App.getContext(), colorId));
        };
    }

    private View.OnClickListener pinBackspaceButtonOnClick() {
        return view -> {
            TextView censoredPasswordView = findViewById(R.id.censored_password_text_view);

            if (passwordBuilder.length() > 0) {
                passwordBuilder.deleteCharAt(passwordBuilder.length() - 1);

                String censoredPassword = censoredPasswordView.getText().toString();
                censoredPasswordView.setText(censoredPassword
                        .substring(0, censoredPassword.length() - 1));
            }
        };
    }

    private View.OnClickListener authButtonOnClick() {
        return view -> {
            switch (currentMode) {
                case AUTHORIZATION_MODE -> proceedAuthorization();
                case CREATE_PASSWORD_MODE -> proceedPasswordCreation();
                case KEY_RECOVERY_MODE -> proceedKeyRecovery();
                case CHANGE_PASSWORD_MODE -> proceedPasswordChanging();
            }
        };
    }

    private void proceedPasswordCreation() {
        if (setPassword()) {
            Intent setupKeyActivityIntent = new Intent(App.getContext(), SetupKeyActivity.class);
            setupKeyActivityIntent.putExtra("mode", SetupKeyActivity.FIRST_RUN_MODE);

            setupKeyActivityIntent.putExtra("password", password);
            startActivity(setupKeyActivityIntent);
        }
    }

    private void proceedKeyRecovery() {
        if (setPassword()) {
            String hexKey = getIntent().getStringExtra("hex-key");

            try {
                if (hexKey == null) throw new Exception("Missing hex-key");
                cryptoManager.applyNewKey(CryptoTools.hexToCryptoKey(hexKey, password));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            startActivity(new Intent(App.getContext(), MainActivity.class));
        }
    }

    private void proceedPasswordChanging() {
        if (setPassword()) {
            try {
                cryptoManager.changePassword(password);
                resetPassword(getString(R.string.updated));

                startActivity(new Intent(App.getContext(), MainActivity.class));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void proceedAuthorization() {
        String accessCode = passwordBuilder.toString();
        TextView censoredPasswordView = findViewById(R.id.censored_password_text_view);

        if (accessCode.isEmpty()) {
            showToastMessage(getString(R.string.enter_the_code), Toast.LENGTH_SHORT);
            return;
        }

        if (!cryptoManager.configure(accessCode)) {
            attempts--;

            if (attempts == 0) {
                cryptoManager.block();

                resetPassword(getString(R.string.blocked));
                startActivity(new Intent(App.getContext(), KeyRecoveryActivity.class));
            } else {
                try {
                    Thread.sleep(ON_WRONG_PASSWORD_DELAY_MS);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                String messageFormat = getString(R.string.wrong_code_you_have_n_attempts);
                resetPassword(String.format(messageFormat, attempts));
            }
        } else {
            censoredPasswordView.setText("");
            startActivity(new Intent(App.getContext(), MainActivity.class));
        }
    }

    private boolean setPassword() {
        TextView topLabel = findViewById(R.id.auth_top_label);
        TextView censoredPasswordView = findViewById(R.id.censored_password_text_view);

        String repeatCodeString = getString(R.string.repeat_access_code);

        if (password != null && topLabel.getText().equals(repeatCodeString)) {
            if (!passwordBuilder.toString().equals(password)) {
                resetPassword(getString(R.string.code_not_match));
                return false;
            }

            censoredPasswordView.setText("");
            return true;
        } else {
            if (passwordBuilder.length() >= MIN_PASSWORD_LENGTH) {
                password = passwordBuilder.toString();
                passwordBuilder = new StringBuilder();

                topLabel.setText(repeatCodeString);
                censoredPasswordView.setText("");
            } else {
                String messageFormat = getString(R.string.minimum_password_length_is_n);
                resetPassword(String.format(messageFormat, MIN_PASSWORD_LENGTH));
            }
        }

        return false;
    }

    private void resetPassword(String toastMessage) {
        TextView censoredPasswordView = findViewById(R.id.censored_password_text_view);
        passwordBuilder = new StringBuilder();

        censoredPasswordView.setText("");
        showToastMessage(toastMessage, Toast.LENGTH_SHORT);
    }
}
