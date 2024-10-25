package app.notesr.activity.security;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import app.notesr.App;
import app.notesr.R;
import app.notesr.activity.ExtendedAppCompatActivity;

import java.util.Arrays;

public class AuthActivity extends ExtendedAppCompatActivity {
    public static final int AUTHORIZATION_MODE = 0;
    public static final int CREATE_PASSWORD_MODE = 1;
    public static final int KEY_RECOVERY_MODE = 2;
    public static final int CHANGE_PASSWORD_MODE = 3;
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
            R.id.pinButton0
    };

    private int currentMode;
    private int inputIndex = 0;
    private boolean capsLockEnabled = false;
    private final StringBuilder passwordBuilder = new StringBuilder();

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
        TextView topLabel = findViewById(R.id.authTopLabel);

        Button changeInputIndexButton = findViewById(R.id.changeInputIndexButton);
        Button capsButton = findViewById(R.id.capsButton);

        Button backspaceButton = findViewById(R.id.pinBackspaceButton);
        Button authButton = findViewById(R.id.authButton);

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

            if (passwordViewLength == 0 ||
                    screenWidth - passwordViewWidth > passwordViewWidth / passwordViewLength) {
                censoredPasswordView.setText(censoredPasswordView.getText() + "•");
            }
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
        AuthActivityExtension helper = new AuthActivityExtension(this, passwordBuilder);

        return view -> {
            switch (currentMode) {
                case AUTHORIZATION_MODE -> helper.authorize();
                case CREATE_PASSWORD_MODE -> helper.createPassword();
                case KEY_RECOVERY_MODE -> helper.recoverKey();
                case CHANGE_PASSWORD_MODE -> helper.changePassword();
            }
        };
    }

    private DisplayMetrics getDisplayMetrics() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        return displayMetrics;
    }
}
