package com.peew.notesr.ui.auth;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.peew.notesr.App;
import com.peew.notesr.R;
import com.peew.notesr.ui.ExtendedAppCompatActivity;

import java.util.Arrays;

public class AuthActivity extends ExtendedAppCompatActivity {
    public static final int AUTHORIZATION_MODE = 0;
    public static final int CREATE_PASSWORD_MODE = 1;
    public static final int KEY_RECOVERY_MODE = 2;
    public static final int CHANGE_PASSWORD_MODE = 3;
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
    private final PasswordSetupProcessor passwordSetupProcessor=
            new PasswordSetupProcessor(this, passwordBuilder);

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
                case AUTHORIZATION_MODE -> authorize();
                case CREATE_PASSWORD_MODE -> passwordSetupProcessor.proceedPasswordCreation();
                case KEY_RECOVERY_MODE -> passwordSetupProcessor.proceedKeyRecovery();
                case CHANGE_PASSWORD_MODE -> passwordSetupProcessor.proceedPasswordChanging();
            }
        };
    }

    private void authorize() {
        new AuthorizationProcessor(this, passwordBuilder.toString()).proceed();
    }

    void resetPassword(String toastMessage) {
        TextView censoredPasswordView = findViewById(R.id.censored_password_text_view);
        passwordBuilder = new StringBuilder();

        censoredPasswordView.setText("");
        showToastMessage(toastMessage, Toast.LENGTH_SHORT);
    }
}
