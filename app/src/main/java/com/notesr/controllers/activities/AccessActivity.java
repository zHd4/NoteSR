package com.notesr.controllers.activities;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.notesr.R;
import com.notesr.controllers.ActivityHelper;
import com.notesr.controllers.EmergencyPasswordController;
import com.notesr.controllers.StorageController;
import com.notesr.controllers.managers.KeyManager;
import com.notesr.models.Config;
import com.notesr.models.ManagePasswordOperation;
import com.notesr.models.NumericKeyboardInputStates;

import java.util.Arrays;

/** @noinspection ReassignedVariable*/
public class AccessActivity extends ActivityHelper {
    private static final int LATENCY = 1500;

    private static final int[] PIN_BUTTONS_ID = {
            R.id.pinButton0,
            R.id.pinButton1,
            R.id.pinButton2,
            R.id.pinButton3,
            R.id.pinButton4,
            R.id.pinButton5,
            R.id.pinButton6,
            R.id.pinButton7,
            R.id.pinButton8,
            R.id.pinButton9
    };

    private String enteredPassword = "";

    public ManagePasswordOperation operation = ManagePasswordOperation.DEFAULT;

    private int attempts = 3;

    private boolean capsEnabled;

    private TextView passwordField;

    private NumericKeyboardInputStates inputState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.access_activity);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        setup();
    }

    private void setup() {
        this.capsEnabled = false;
        this.passwordField = findViewById(R.id.accessCodeSector);
        this.inputState = NumericKeyboardInputStates.NUMERIC;

        if(operation == ManagePasswordOperation.SETUP_EMERGENCY_PASSWORD) {
            findViewById(R.id.infoTextView).setVisibility(View.VISIBLE);
        }

        if(operation == ManagePasswordOperation.CREATE_PASSWORD) {
            ((TextView)findViewById(R.id.acTextView)).setText(R.string.create_pin);
        }

        Arrays.stream(PIN_BUTTONS_ID)
                .forEach(buttonId -> setPinButtonOnClick(findViewById(buttonId)));

        final Button accessButton = findViewById(R.id.accessButton);
        accessButton.setOnClickListener(view -> acceptPassword());

        setCapsButtonOnClick(findViewById(R.id.capsButton));
        setChangeInputTypeButtonOnClick(findViewById(R.id.changeInputTypeButton));
        setPinButtonBackspaceOnClick(findViewById(R.id.pinButtonBackspace));
    }

    @SuppressLint("SetTextI18n")
    private void setPinButtonOnClick(final Button pinButton) {
        pinButton.setOnClickListener(view -> {
            char[] inputTypes = new char[] {
                NumericKeyboardInputStates.NUMERIC.getState(),
                NumericKeyboardInputStates.SYMBOL1.getState(),
                NumericKeyboardInputStates.SYMBOL2.getState(),
                NumericKeyboardInputStates.SYMBOL3.getState()
            };

            char character = pinButton
                    .getText()
                    .toString()
                    .replace("\n", "")
                    .toCharArray()
                    [new String(inputTypes).indexOf(AccessActivity.this.inputState.getState())];

            character = capsEnabled ?
                    String.valueOf(character).toUpperCase().toCharArray()[0] : character;

            String currentPassword = AccessActivity.this.passwordField.getText().toString();

            AccessActivity.this.passwordField.setText(currentPassword);
            AccessActivity.this.passwordField.setText(currentPassword + "•" + " ");

            enteredPassword += character;
        });
    }

    private void setCapsButtonOnClick(Button capsButton) {
        capsButton.setOnClickListener(view -> {
            AccessActivity.this.capsEnabled = !capsEnabled;
            capsButton.setTextSize(capsEnabled ? 40 : 30);
        });
    }

    private void setChangeInputTypeButtonOnClick(Button changeInputTypeButton) {
        changeInputTypeButton.setOnClickListener(view -> {
            switch (AccessActivity.this.inputState) {
                case NUMERIC:
                    inputState = NumericKeyboardInputStates.SYMBOL1;
                    break;
                case SYMBOL1:
                    inputState = NumericKeyboardInputStates.SYMBOL2;
                    break;
                case SYMBOL2:
                    inputState = NumericKeyboardInputStates.SYMBOL3;
                    break;
                case SYMBOL3:
                    inputState = NumericKeyboardInputStates.NUMERIC;
                    break;
            }

            changeInputTypeButton.setText(String.valueOf(AccessActivity.this.inputState.getState()));
        });
    }

    private void setPinButtonBackspaceOnClick(Button pinButtonBackspace) {
        pinButtonBackspace.setOnClickListener(view -> {
            String currentPassword = AccessActivity.this.passwordField.getText().toString();

            if(currentPassword.length() > 0) {
                enteredPassword = enteredPassword.substring(0, enteredPassword.length() - 1);

                AccessActivity.this.passwordField.setText(
                        currentPassword.substring(0, currentPassword.length() - 2)
                );
            }
        });
    }

    private void clearPasswordField() {
        this.passwordField.setText("");
    }

    private void resetEnteredCode() {
        enteredPassword = "";
    }

    public void acceptPassword() {
        switch (operation) {
            case CREATE_PASSWORD:
                onCreatePassword();
                break;
            case REPEAT_PASSWORD:
                onRepeatPassword();
                break;
            case SETUP_EMERGENCY_PASSWORD:
                onSetupEmergencyPassword();
                break;
            default:
                onCheckPassword();
                break;
        }

        this.clearPasswordField();
    }

    private void onCreatePassword() {
        if(enteredPassword.length() > 0) {
            final TextView formLabel = findViewById(R.id.acTextView);

            Config.passwordCode = enteredPassword;
            formLabel.setText(R.string.repeatAccessCode);
            operation = ManagePasswordOperation.REPEAT_PASSWORD;

            resetEnteredCode();
        }
    }

    private void onRepeatPassword() {
        if(enteredPassword.equals(Config.passwordCode)){
            try {
                KeyManager.saveKey(getApplicationContext());
                startActivity(getIntent(getApplicationContext(), MainActivity.class));
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            resetEnteredCode();
            showTextMessage(getResources().getString(R.string.try_again),
                    Toast.LENGTH_SHORT, getApplicationContext());
        }
    }

    private void onSetupEmergencyPassword() {
        new EmergencyPasswordController(getApplicationContext(), enteredPassword).setPin();
        showTextMessage(getResources().getString(R.string.secret_pin_code_is_set),
                Toast.LENGTH_SHORT, getApplicationContext());

        finish();
    }

    @SuppressLint("StringFormatMatches")
    private void onCheckPassword() {
        EmergencyPasswordController secretPinController = new EmergencyPasswordController(
                getApplicationContext(),
                enteredPassword
        );

        if(secretPinController.checkPin()) {
            StorageController.eraseFile(getApplicationContext(), Config.secretPinFileName);
            this.removeKeyFile();
        }

        boolean pinValid = KeyManager.getKeys(enteredPassword, getApplicationContext());

        if (pinValid) {
            startActivity(getIntent(
                    getApplicationContext(),
                    MainActivity.class
            ));
        } else {
            onFailedAuth();
        }
    }

    private void onFailedAuth() {
        try {
            Thread.sleep(LATENCY);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if(attempts == 1) {
            resetEnteredCode();
            this.removeKeyFile();
        } else {
            resetEnteredCode();
            attempts--;

            String messageFormat =
                    getResources().getString(R.string.try_again_you_have_n_attemps);

            showTextMessage(
                    String.format(messageFormat, attempts),
                    Toast.LENGTH_SHORT,
                    getApplicationContext()
            );
        }
    }

    private void removeKeyFile() {
        StorageController.eraseFile(getApplicationContext(), Config.keyBinFileName);
        startActivity(getIntent(getApplicationContext(), RecoveryActivity.class));
    }
}