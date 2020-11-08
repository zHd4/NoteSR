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

    public static int CREATE_CODE = 1;
    public static int REPEAT_CODE = 2;
    public static int SECRET_CODE = 3;

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

        resetEnteredCode();

        if(operation == SECRET_CODE) {
            findViewById(R.id.infoTextView).setVisibility(View.VISIBLE);
        }

        if(operation == CREATE_CODE) {
            ((TextView)findViewById(R.id.acTextView)).setText(R.string.create_pin);
        }

        ActivityTools.clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        final Button capsButton = findViewById(R.id.capsButton);
        final Button accessButton = findViewById(R.id.accessButton);

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
                capsButton.setTextSize(capsEnabled ? 40 : 30);
            }
        });

        changeInputTypeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(AccessActivity.this.inputState == NumericKeyboardInputStates.NUMERIC) {
                    AccessActivity.this.inputState = NumericKeyboardInputStates.SYMBOL1;
                } else if(AccessActivity.this.inputState == NumericKeyboardInputStates.SYMBOL1) {
                    AccessActivity.this.inputState = NumericKeyboardInputStates.SYMBOL2;
                } else if(AccessActivity.this.inputState == NumericKeyboardInputStates.SYMBOL2) {
                    AccessActivity.this.inputState = NumericKeyboardInputStates.SYMBOL3;
                } else if(AccessActivity.this.inputState == NumericKeyboardInputStates.SYMBOL3) {
                    AccessActivity.this.inputState = NumericKeyboardInputStates.NUMERIC;
                }

                changeInputTypeButton.setText(String.valueOf(AccessActivity.this.inputState.getState()));
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

        accessButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                acceptPassword();
            }
        });
    }

    private void setOnClick(final Button btn) {
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                char[] inputTypes = new char[] {
                    NumericKeyboardInputStates.NUMERIC.getState(),
                    NumericKeyboardInputStates.SYMBOL1.getState(),
                    NumericKeyboardInputStates.SYMBOL2.getState(),
                    NumericKeyboardInputStates.SYMBOL3.getState()
                };

                char character = btn
                        .getText()
                        .toString()
                        .replace("\n", "")
                        .toCharArray()
                        [new String(inputTypes).indexOf(AccessActivity.this.inputState.getState())];

                character = capsEnabled ? String.valueOf(character).toUpperCase().toCharArray()[0] : character;

                String currentPassword = AccessActivity.this.passwordField.getText().toString();

                AccessActivity.this.passwordField.setText(currentPassword);
                AccessActivity.this.passwordField.setText(currentPassword + "•" + " ");

                enteredPassword += character;
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
        if (operation == CREATE_CODE) {
            final TextView formLabel = findViewById(R.id.acTextView);

            Config.passwordCode = enteredPassword;
            formLabel.setText(R.string.repeatAccessCode);
            operation = REPEAT_CODE;

            resetEnteredCode();
        } else if(operation == REPEAT_CODE) {
            if(enteredPassword.equals(Config.passwordCode)){
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
                resetEnteredCode();

                ActivityTools.showTextMessage("Try again",
                        Toast.LENGTH_SHORT,
                        getApplicationContext()
                );
            }
        } else if(operation == SECRET_CODE) {
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
                    resetEnteredCode();
                    this.dropKeyFile();
                } else {
                    resetEnteredCode();
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