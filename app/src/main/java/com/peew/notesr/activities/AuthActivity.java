package com.peew.notesr.activities;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.peew.notesr.App;
import com.peew.notesr.R;

import java.util.Arrays;

public class AuthActivity extends AppCompatActivity {
    public static final int AUTHORIZATION_MODE = 0;
    public static final int PASSWORD_SETUP_MODE = 1;
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
    private final StringBuilder passwordBuilder = new StringBuilder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        currentMode = getIntent().getIntExtra("mode", -1);
        configure();
    }

    private void configure() {
        TextView topLabel = findViewById(R.id.auth_top_label);

        Button changeInputIndexButton = findViewById(R.id.change_input_index_button);
        Button capsButton = findViewById(R.id.caps_button);

        switch (currentMode) {
            case AUTHORIZATION_MODE:
                topLabel.setText(R.string.enter_access_code);
                break;
            case PASSWORD_SETUP_MODE:
                topLabel.setText(R.string.create_access_code);
                break;
        }

        Arrays.stream(PIN_BUTTONS_ID).forEach(id -> findViewById(id)
                .setOnClickListener(getPinButtonOnClickListener()));

        changeInputIndexButton.setOnClickListener(getChangeInputIndexButtonOnClickListener());
        capsButton.setOnClickListener(getCapsButtonOnClickListener());
    }

    private View.OnClickListener getChangeInputIndexButtonOnClickListener() {
        return view -> {
            Button self = ((Button)view);

            switch (inputIndex) {
                case 0:
                    self.setText("A");
                    inputIndex++;
                    break;
                case 1:
                    self.setText("B");
                    inputIndex++;
                    break;
                case 2:
                    self.setText("C");
                    inputIndex++;
                    break;
                case 3:
                    self.setText("1");
                    inputIndex = 0;
                    break;
            }
        };
    }

    @SuppressLint("SetTextI18n")
    private View.OnClickListener getPinButtonOnClickListener() {
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

    private View.OnClickListener getCapsButtonOnClickListener() {
        return view -> {
            Button self = ((Button)view);
            capsLockEnabled = !capsLockEnabled;

            int colorId = capsLockEnabled ?
                    R.color.caps_button_pressed :
                    R.color.caps_button_unpressed;

            self.setTextColor(ContextCompat.getColor(App.getContext(), colorId));
        };
    }
}