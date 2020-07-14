package com.git.notesr;

import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

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
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        ActivityTools.clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        final Button pinButtonBackspace = findViewById(R.id.pinButtonBackspace);

        final int[] sectors = { 0, R.id.pinSector1, R.id.pinSector2, R.id.pinSector3, R.id.pinSector4 };
        int[] pinButtons = { R.id.pinButton0, R.id.pinButton1, R.id.pinButton2, R.id.pinButton3,
                R.id.pinButton4, R.id.pinButton5, R.id.pinButton6, R.id.pinButton7,
                R.id.pinButton8, R.id.pinButton9};

        for (int i = 0; i < pinButtons.length; i++) {
            setOnClick((Button) findViewById(pinButtons[i]), sectors, i);
        }

        pinButtonBackspace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(enteredPin.length() == 3){
                    enteredPin = enteredPin.substring(0, 2);
                    ((TextView) findViewById(sectors[3])).setText("     ");
                } else if(enteredPin.length() == 2) {
                    enteredPin = enteredPin.substring(0, 1);
                    ((TextView) findViewById(sectors[2])).setText("     ");
                }  else if(enteredPin.length() == 1) {
                    enteredPin = "";
                    ((TextView) findViewById(sectors[1])).setText("     ");
                }
            }
        });
    }

    private void setOnClick(final Button btn, final int[] sectors, final int num) {
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (enteredPin.length() == 0) {
                    ((TextView) findViewById(sectors[1])).setText("  •  ");
                    enteredPin += String.valueOf(num);
                } else if (enteredPin.length() == 1) {
                    ((TextView) findViewById(sectors[2])).setText("  •  ");
                    enteredPin += String.valueOf(num);
                } else if (enteredPin.length() == 2) {
                    ((TextView) findViewById(sectors[3])).setText("  •  ");
                    enteredPin += String.valueOf(num);
                } else if (enteredPin.length() == 3) {
                    ((TextView) findViewById(sectors[4])).setText("  •  ");
                    enteredPin += String.valueOf(num);

                    ClearSectors();
                    AcceptPin();
                }
            }
        });
    }

    private void ClearSectors() {
        final TextView pinSector1 = findViewById(R.id.pinSector1);
        final TextView pinSector2 = findViewById(R.id.pinSector2);
        final TextView pinSector3 = findViewById(R.id.pinSector3);
        final TextView pinSector4 = findViewById(R.id.pinSector4);

        pinSector1.setText("     ");
        pinSector2.setText("     ");
        pinSector3.setText("     ");
        pinSector4.setText("     ");
    }

    public void AcceptPin() {
        if (operation == CREATE_PIN) {
            final TextView formLabel = findViewById(R.id.acTextView);

            Config.pinCode = enteredPin;
            formLabel.setText(R.string.repeat_access_code);
            operation = REPEAT_PIN;

            enteredPin = "";
        } else if(operation == REPEAT_PIN) {
            if(enteredPin.equals(Config.pinCode)){
                ActivityTools.context = getApplicationContext();
                try {
                    ActivityTools.saveKey(ActivityTools.getAppContext());
                    StartMainActivity();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                enteredPin = "";
                ShowTextMessage("Try again");
            }
        } else {
            if (attempts != 1) {
                ActivityTools.context = getApplicationContext();
                if (ActivityTools.getKeys(enteredPin, getApplicationContext())) {
                    Intent saIntent = new Intent(this, MainActivity.class);
                    startActivity(saIntent);
                } else {
                    enteredPin = "";
                    attempts--;

                    ShowTextMessage("Try again, you have " + attempts + " attempts");
                }
            } else {
                File dir = new File(getFilesDir(), "storage");
                File file = new File(dir, "key.bin");
                file.delete();

                android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(1);
            }
        }

        ClearSectors();
    }

    public void ShowTextMessage(String text) {
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }

    public void StartMainActivity() {
        Intent saIntent = new Intent(this, MainActivity.class);
        startActivity(saIntent);
    }
}