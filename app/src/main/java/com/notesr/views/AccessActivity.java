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
import com.notesr.models.ActivityTools;
import com.notesr.models.Config;
import com.notesr.controllers.Storage;

public class AccessActivity extends AppCompatActivity {
    public static String enteredPin = "";

    public static int CREATE_PIN = 1;
    public static int REPEAT_PIN = 2;

    public static int operation = 0;
    private static int attempts = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.access_activity);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
        );

        ActivityTools.clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        final Button pinButtonBackspace = findViewById(R.id.pinButtonBackspace);

        final int[] sectors = {
                R.id.pinSector1,
                R.id.pinSector2,
                R.id.pinSector3,
                R.id.pinSector4
        };

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

        for (int i = 0; i < pinButtons.length; i++) {
            setOnClick((Button) findViewById(pinButtons[i]), sectors, i);
        }

        pinButtonBackspace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(enteredPin.length() == 3){
                    enteredPin = enteredPin.substring(0, 2);
                    ((TextView) findViewById(sectors[2])).setText(generateSector(false));
                } else if(enteredPin.length() == 2) {
                    enteredPin = enteredPin.substring(0, 1);
                    ((TextView) findViewById(sectors[1])).setText(generateSector(false));
                }  else if(enteredPin.length() == 1) {
                    resetEnteredPin();
                    ((TextView) findViewById(sectors[0])).setText(generateSector(false));
                }
            }
        });
    }

    private void setOnClick(final Button btn, final int[] sectors, final int num) {
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (enteredPin.length() == 0) {
                    ((TextView) findViewById(sectors[0])).setText(generateSector(true));
                    enteredPin += String.valueOf(num);

                } else if (enteredPin.length() == 1) {
                    ((TextView) findViewById(sectors[1])).setText(generateSector(true));
                    enteredPin += String.valueOf(num);

                } else if (enteredPin.length() == 2) {
                    ((TextView) findViewById(sectors[2])).setText(generateSector(true));
                    enteredPin += String.valueOf(num);

                } else if (enteredPin.length() == 3) {
                    ((TextView) findViewById(sectors[3])).setText(generateSector(true));
                    enteredPin += String.valueOf(num);

                    clearSectors(sectors);
                    AcceptPin(sectors);
                }
            }
        });
    }

    private void clearSectors(final int[] sectors) {
        for(int i = 0; i < 4; i++) {
            ((TextView) findViewById(sectors[i])).setText(generateSector(false));
        }
    }

    private String generateSector(boolean usingDot) {
        StringBuilder result = new StringBuilder();
        
        for(int i = 0; i < 5; i++) {
            if(i == 2 && usingDot) {
                result.append("•");
            } else {
                result.append(" ");
            }
        }
        
        return result.toString();
    }
    
    private void resetEnteredPin() {
        enteredPin = "";
    }

    public void AcceptPin(final int[] sectors) {
        if (operation == CREATE_PIN) {
            final TextView formLabel = findViewById(R.id.acTextView);

            Config.pinCode = enteredPin;
            formLabel.setText(R.string.repeatAccessCode);
            operation = REPEAT_PIN;

            resetEnteredPin();
        } else if(operation == REPEAT_PIN) {
            if(enteredPin.equals(Config.pinCode)){
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
        } else {
            boolean pinValid = ActivityTools.getKeys(enteredPin, getApplicationContext());
            ActivityTools.context = getApplicationContext();

            if (!pinValid) {
                if(attempts == 1) {
                    resetEnteredPin();
                    Storage.deleteFile(getApplicationContext(), Config.keyBinFileName);
                    startActivity(ActivityTools.getIntent(getApplicationContext(), RecoveryActivity.class));
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

        clearSectors(sectors);
    }
}