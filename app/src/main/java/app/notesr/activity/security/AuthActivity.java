package app.notesr.activity.security;

import static app.notesr.core.util.ActivityUtils.disableBackButton;

import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import app.notesr.R;
import app.notesr.ActivityBase;
import app.notesr.core.security.crypto.CryptoManager;
import app.notesr.core.security.crypto.CryptoManagerProvider;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

public final class AuthActivity extends ActivityBase {

    private static final String TAG = AuthActivity.class.getName();

    @AllArgsConstructor
    @Getter
    public enum Mode {
        AUTHORIZATION("authorization"),
        CREATE_PASSWORD("create_password"),
        CHANGE_PASSWORD("change_password"),
        KEY_RECOVERY("key_recovery");

        private final String mode;
    }

    private static final Integer[] PIN_BUTTONS_ID = {
        R.id.pinButton1,
        R.id.pinButton2,
        R.id.pinButton3,
        R.id.pinButton4,
        R.id.pinButton5,
        R.id.pinButton6,
        R.id.pinButton7,
        R.id.pinButton8,
        R.id.pinButton9,
        R.id.pinButton0,
        R.id.pinButtonSpecChars1,
        R.id.pinButtonSpecChars2
    };

    private AuthActivityExtension extension;
    private Mode currentMode;
    private int inputIndex = 0;
    private boolean capsLockEnabled = false;
    private final StringBuilder passwordBuilder = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        String mode = getIntent().getStringExtra("mode");

        CryptoManager cryptoManager = CryptoManagerProvider.getInstance(getApplicationContext());
        extension = new AuthActivityExtension(this, cryptoManager,
                passwordBuilder);

        try {
            currentMode = Mode.valueOf(mode);
        } catch (NullPointerException e) {
            Log.e(TAG, "Authorization mode didn't provided", e);
            throw new RuntimeException(e);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid mode: " + mode, e);
            throw new RuntimeException(e);
        }

        configure();
    }

    private void configure() {
        TextView topLabel = findViewById(R.id.authTopLabel);

        Button changeInputIndexButton = findViewById(R.id.changeInputIndexButton);
        Button capsButton = findViewById(R.id.capsButton);

        Button backspaceButton = findViewById(R.id.pinBackspaceButton);
        Button authButton = findViewById(R.id.authButton);

        switch (currentMode) {
            case AUTHORIZATION -> {
                topLabel.setText(R.string.enter_access_code);
                disableBackButton(this);
            }

            case CHANGE_PASSWORD -> topLabel.setText(R.string.create_new_access_code);
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
            Button self = ((Button) view);

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
                default -> throw new IllegalStateException("Unexpected value: " + inputIndex);
            }
        };
    }

    private View.OnClickListener pinButtonOnClick() {
        return view -> {
            Button self = ((Button) view);
            TextView censoredPasswordView = findViewById(R.id.censoredPasswordTextView);

            char currentChar = self.getText()
                    .toString()
                    .replace("\n", "")
                    .charAt(inputIndex);

            if (capsLockEnabled) {
                passwordBuilder.append(Character.toString(currentChar).toUpperCase());
            } else {
                passwordBuilder.append(Character.toString(currentChar).toLowerCase());
            }

            int passwordViewWidth = censoredPasswordView.getMeasuredWidth();
            int passwordViewLength = censoredPasswordView.getText().length();

            int screenWidth = getDisplayMetrics().widthPixels;

            if (passwordViewLength == 0
                    || screenWidth - passwordViewWidth > passwordViewWidth / passwordViewLength) {
                String censoredPassword = censoredPasswordView.getText() + "•";
                censoredPasswordView.setText(censoredPassword);
            }
        };
    }

    private View.OnClickListener capsButtonOnClick() {
        return view -> {
            Button self = ((Button) view);
            capsLockEnabled = !capsLockEnabled;

            int colorId = capsLockEnabled
                    ? R.color.caps_button_pressed
                    : R.color.caps_button_unpressed;

            self.setTextColor(ContextCompat.getColor(getApplicationContext(), colorId));
        };
    }

    private View.OnClickListener pinBackspaceButtonOnClick() {
        return view -> {
            TextView censoredPasswordView = findViewById(R.id.censoredPasswordTextView);

            if (passwordBuilder.length() > 0) {
                String censoredPassword = censoredPasswordView.getText().toString();

                if (censoredPassword.length() == passwordBuilder.length()) {
                    censoredPasswordView.setText(censoredPassword
                            .substring(0, censoredPassword.length() - 1));
                }

                passwordBuilder.deleteCharAt(passwordBuilder.length() - 1);
            }
        };
    }

    private View.OnClickListener authButtonOnClick() {
        return view -> {
            switch (currentMode) {
                case AUTHORIZATION -> extension.authorize();
                case CREATE_PASSWORD -> extension.createPassword();
                case KEY_RECOVERY -> extension.recoverKey();
                case CHANGE_PASSWORD -> extension.changePassword();
                default -> throw new IllegalStateException("Unexpected value: " + currentMode);
            }
        };
    }

    private DisplayMetrics getDisplayMetrics() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        return displayMetrics;
    }
}
