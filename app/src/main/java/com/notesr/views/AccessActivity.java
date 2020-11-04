package com.notesr.views;

import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.notesr.R;
import com.notesr.controllers.SecretPinController;
import com.notesr.models.ActivityTools;
import com.notesr.models.Config;
import com.notesr.controllers.StorageController;
import com.notesr.models.NumericKeyboardInputStates;

public class AccessActivity extends AppCompatActivity {
    public static String enteredPassword = "";

    public static int CREATE_PIN = 1;
    public static int REPEAT_PIN = 2;
    public static int SECRET_PIN = 3;

    public static int operation = 0;
    private static int attempts = 3;

    private boolean capsEnabled;
    private TextView passwordField;
    private NumericKeyboardInputStates inputState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.access_activity);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        this.capsEnabled = false;
        this.passwordField = findViewById(R.id.accessCodeSector);
        this.inputState = NumericKeyboardInputStates.NUMERIC;

        resetEnteredPin();

        if(operation == SECRET_PIN) {
            findViewById(R.id.infoTextView).setVisibility(View.VISIBLE);
        }

        if(operation == CREATE_PIN) {
            ((TextView)findViewById(R.id.acTextView)).setText(R.string.create_pin);
        }

        ActivityTools.clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        final Button capsButton = findViewById(R.id.capsButton);
        final Button pinButtonBackspace = findViewById(R.id.pinButtonBackspace);
        final Button changeInputTypeButton = findViewById(R.id.changeInputTypeButton);

        int[] pinButtons = {
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

        for (int buttonId : pinButtons) {
            setOnClick((Button) findViewById(buttonId));
        }

        capsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AccessActivity.this.capsEnabled = !capsEnabled;
                capsButton.setTextSize(capsEnabled ? 30 : 40);
            }
        });

        changeInputTypeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(AccessActivity.this.inputState == NumericKeyboardInputStates.NUMERIC) {
                    AccessActivity.this.inputState = NumericKeyboardInputStates.SYMBOL1;
                    changeInputTypeButton.setText("1");
                } else if(AccessActivity.this.inputState == NumericKeyboardInputStates.SYMBOL1) {
                    AccessActivity.this.inputState = NumericKeyboardInputStates.SYMBOL2;
                    changeInputTypeButton.setText("2");
                } else if(AccessActivity.this.inputState == NumericKeyboardInputStates.SYMBOL2) {
                    AccessActivity.this.inputState = NumericKeyboardInputStates.SYMBOL3;
                    changeInputTypeButton.setText("3");
                } else if(AccessActivity.this.inputState == NumericKeyboardInputStates.SYMBOL3) {
                    AccessActivity.this.inputState = NumericKeyboardInputStates.NUMERIC;
                    changeInputTypeButton.setText("0");
                }
            }
        });

        pinButtonBackspace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String currentPassword = AccessActivity.this.passwordField.getText().toString();

                if(currentPassword.length() > 0) {
                    AccessActivity.this.passwordField.setText(
                            currentPassword.substring(0, currentPassword.length() - 2)
                    );
                }
            }
        });
    }

    private void setOnClick(final Button btn) {
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (enteredPassword.length() <= 6) {
                    AccessActivity.this.passwordField.setText(
                            "•" + " " + AccessActivity.this.passwordField.getText().toString()
                    );
                }

                char character = btn
                        .getText()
                        .toString()
                        .replace("\n", "")
                        .toCharArray()
                        [AccessActivity.this.inputState.getState()];

                enteredPassword += capsEnabled ? String.valueOf(character).toUpperCase() : character;
                /* else {
                    ((TextView) findViewById(sectors[3])).setText(generateSector(true));
                    enteredPin += String.valueOf(num);

                    clearSectors(sectors);
                    acceptPin(sectors);
                } */
            }
        });
    }

    private void clearPasswordField() {
        this.passwordField.setText("");
    }
    
    private void resetEnteredPin() {
        enteredPassword = "";
    }

    public void acceptPin(final int[] sectors) {
        if (operation == CREATE_PIN) {
            final TextView formLabel = findViewById(R.id.acTextView);

            Config.pinCode = enteredPassword;
            formLabel.setText(R.string.repeatAccessCode);
            operation = REPEAT_PIN;

            resetEnteredPin();
        } else if(operation == REPEAT_PIN) {
            if(enteredPassword.equals(Config.pinCode)){
                ActivityTools.context = getApplicationContext();
                try {
                    ActivityTools.saveKey(ActivityTools.getAppContext());
                    startActivity(ActivityTools.getIntent(
                            getApplicationContext(),
                            MainActivity.class
                    ));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                resetEnteredPin();

                ActivityTools.showTextMessage("Try again",
                        Toast.LENGTH_SHORT,
                        getApplicationContext()
                );
            }
        } else if(operation == SECRET_PIN) {
            new SecretPinController(getApplicationContext(), Integer.parseInt(enteredPassword)).setPin();
            ActivityTools.showTextMessage(
                    getResources().getString(R.string.secret_pin_code_is_set),
                    Toast.LENGTH_SHORT,
                    getApplicationContext());

            finish();
        } else {
            SecretPinController secretPinController = new SecretPinController(
                    getApplicationContext(),
                    Integer.parseInt(enteredPassword)
                    );

            if(secretPinController.checkPin()) {
                StorageController.eraseFile(getApplicationContext(), Config.secretPinFileNameName);
                this.dropKeyFile();
            }

            boolean pinValid = ActivityTools.getKeys(enteredPassword, getApplicationContext());
            ActivityTools.context = getApplicationContext();

            if (!pinValid) {
                if(attempts == 1) {
                    resetEnteredPin();
                    this.dropKeyFile();
                } else {
                    resetEnteredPin();
                    attempts--;

                    ActivityTools.showTextMessage(
                            "Try again, you have " + attempts + " attempts",
                            Toast.LENGTH_SHORT,
                            getApplicationContext()
                    );
                }
            } else {
                startActivity(ActivityTools.getIntent(
                        getApplicationContext(),
                        MainActivity.class
                ));
            }
        }

        this.clearPasswordField();
    }

    private void dropKeyFile() {
        StorageController.eraseFile(getApplicationContext(), Config.keyBinFileName);
        startActivity(ActivityTools.getIntent(getApplicationContext(), RecoveryActivity.class));
    }
}