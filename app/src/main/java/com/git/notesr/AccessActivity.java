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
        GenkeysActivity.clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        final TextView pinSector1 = findViewById(R.id.pinSector1);
        final TextView pinSector2 = findViewById(R.id.pinSector2);
        final TextView pinSector3 = findViewById(R.id.pinSector3);
        final TextView pinSector4 = findViewById(R.id.pinSector4);

        final Button pinButton1 = findViewById(R.id.pinButton1);
        final Button pinButton2 = findViewById(R.id.pinButton2);
        final Button pinButton3 = findViewById(R.id.pinButton3);
        final Button pinButton4 = findViewById(R.id.pinButton4);
        final Button pinButton5 = findViewById(R.id.pinButton5);
        final Button pinButton6 = findViewById(R.id.pinButton6);
        final Button pinButton7 = findViewById(R.id.pinButton7);
        final Button pinButton8 = findViewById(R.id.pinButton8);
        final Button pinButton9 = findViewById(R.id.pinButton9);

        final Button pinButton0 = findViewById(R.id.pinButton0);
        final Button pinButtonBackspace = findViewById(R.id.pinButtonBackspace);

        pinButton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String bNum = "1";

                if (enteredPin.length() == 0) {
                    pinSector1.setText("  •  ");
                    enteredPin += bNum;
                } else if (enteredPin.length() == 1) {
                    pinSector2.setText("  •  ");
                    enteredPin += bNum;
                } else if (enteredPin.length() == 2) {
                    pinSector3.setText("  •  ");
                    enteredPin += bNum;
                } else if (enteredPin.length() == 3) {
                    pinSector4.setText("  •  ");
                    enteredPin += bNum;

                    AcceptPin();
                }
            }
        });

        pinButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String bNum = "2";

                if (enteredPin.length() == 0) {
                    pinSector1.setText("  •  ");
                    enteredPin += bNum;
                } else if (enteredPin.length() == 1) {
                    pinSector2.setText("  •  ");
                    enteredPin += bNum;
                } else if (enteredPin.length() == 2) {
                    pinSector3.setText("  •  ");
                    enteredPin += bNum;
                } else if (enteredPin.length() == 3) {
                    pinSector4.setText("  •  ");
                    enteredPin += bNum;

                    ClearSectors();
                    AcceptPin();
                }
            }
        });

        pinButton3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String bNum = "3";

                if (enteredPin.length() == 0) {
                    pinSector1.setText("  •  ");
                    enteredPin += bNum;
                } else if (enteredPin.length() == 1) {
                    pinSector2.setText("  •  ");
                    enteredPin += bNum;
                } else if (enteredPin.length() == 2) {
                    pinSector3.setText("  •  ");
                    enteredPin += bNum;
                } else if (enteredPin.length() == 3) {
                    pinSector4.setText("  •  ");
                    enteredPin += bNum;

                    ClearSectors();
                    AcceptPin();
                }
            }
        });

        pinButton4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String bNum = "4";

                if (enteredPin.length() == 0) {
                    pinSector1.setText("  •  ");
                    enteredPin += bNum;
                } else if (enteredPin.length() == 1) {
                    pinSector2.setText("  •  ");
                    enteredPin += bNum;
                } else if (enteredPin.length() == 2) {
                    pinSector3.setText("  •  ");
                    enteredPin += bNum;
                } else if (enteredPin.length() == 3) {
                    pinSector4.setText("  •  ");
                    enteredPin += bNum;

                    ClearSectors();
                    AcceptPin();
                }
            }
        });

        pinButton5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String bNum = "5";

                if (enteredPin.length() == 0) {
                    pinSector1.setText("  •  ");
                    enteredPin += bNum;
                } else if (enteredPin.length() == 1) {
                    pinSector2.setText("  •  ");
                    enteredPin += bNum;
                } else if (enteredPin.length() == 2) {
                    pinSector3.setText("  •  ");
                    enteredPin += bNum;
                } else if (enteredPin.length() == 3) {
                    pinSector4.setText("  •  ");
                    enteredPin += bNum;

                    ClearSectors();
                    AcceptPin();
                }
            }
        });

        pinButton6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String bNum = "6";

                if (enteredPin.length() == 0) {
                    pinSector1.setText("  •  ");
                    enteredPin += bNum;
                } else if (enteredPin.length() == 1) {
                    pinSector2.setText("  •  ");
                    enteredPin += bNum;
                } else if (enteredPin.length() == 2) {
                    pinSector3.setText("  •  ");
                    enteredPin += bNum;
                } else if (enteredPin.length() == 3) {
                    pinSector4.setText("  •  ");
                    enteredPin += bNum;

                    ClearSectors();
                    AcceptPin();
                }
            }
        });

        pinButton7.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String bNum = "7";

                if (enteredPin.length() == 0) {
                    pinSector1.setText("  •  ");
                    enteredPin += bNum;
                } else if (enteredPin.length() == 1) {
                    pinSector2.setText("  •  ");
                    enteredPin += bNum;
                } else if (enteredPin.length() == 2) {
                    pinSector3.setText("  •  ");
                    enteredPin += bNum;
                } else if (enteredPin.length() == 3) {
                    pinSector4.setText("  •  ");
                    enteredPin += bNum;

                    ClearSectors();
                    AcceptPin();
                }

            }
        });

        pinButton8.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String bNum = "8";

                if (enteredPin.length() == 0) {
                    pinSector1.setText("  •  ");
                    enteredPin += bNum;
                } else if (enteredPin.length() == 1) {
                    pinSector2.setText("  •  ");
                    enteredPin += bNum;
                } else if (enteredPin.length() == 2) {
                    pinSector3.setText("  •  ");
                    enteredPin += bNum;
                } else if (enteredPin.length() == 3) {
                    pinSector4.setText("  •  ");
                    enteredPin += bNum;

                    ClearSectors();
                    AcceptPin();
                }
            }
        });

        pinButton9.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String bNum = "9";

                if (enteredPin.length() == 0) {
                    pinSector1.setText("  •  ");
                    enteredPin += bNum;
                } else if (enteredPin.length() == 1) {
                    pinSector2.setText("  •  ");
                    enteredPin += bNum;
                } else if (enteredPin.length() == 2) {
                    pinSector3.setText("  •  ");
                    enteredPin += bNum;
                } else if (enteredPin.length() == 3) {
                    pinSector4.setText("  •  ");
                    enteredPin += bNum;

                    ClearSectors();
                    AcceptPin();
                }
            }
        });

        pinButton0.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String bNum = "0";

                if (enteredPin.length() == 0) {
                    pinSector1.setText("  •  ");
                    enteredPin += bNum;
                } else if (enteredPin.length() == 1) {
                    pinSector2.setText("  •  ");
                    enteredPin += bNum;
                } else if (enteredPin.length() == 2) {
                    pinSector3.setText("  •  ");
                    enteredPin += bNum;
                } else if (enteredPin.length() == 3) {
                    pinSector4.setText("  •  ");
                    enteredPin += bNum;

                    ClearSectors();
                    AcceptPin();
                }
            }
        });

        pinButtonBackspace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(enteredPin.length() == 3){
                    enteredPin = enteredPin.substring(0, 2);
                    pinSector3.setText("     ");
                } else if(enteredPin.length() == 2) {
                    enteredPin = enteredPin.substring(0, 1);
                    pinSector2.setText("     ");
                }  else if(enteredPin.length() == 1) {
                    enteredPin = "";
                    pinSector1.setText("     ");
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

    public void AcceptPin(){
        if (operation == CREATE_PIN) {
            final TextView formLabel = findViewById(R.id.acTextView);

            Config.pinCode = enteredPin;
            formLabel.setText("Repeat access code");
            operation = REPEAT_PIN;

            enteredPin = "";
        }else if(operation == REPEAT_PIN){
            if(enteredPin.equals(Config.pinCode)){
                GenkeysActivity.context = getApplicationContext();
                try {
                    GenkeysActivity.SaveKey();
                    StartMainActivity();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }else{
                enteredPin = "";
                ShowTextMessage("Try again");
            }
        }else{
            if (attempts != 1) {
                GenkeysActivity.context = getApplicationContext();
                if (GenkeysActivity.GetKeys(enteredPin)) {
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

    public void ShowTextMessage(String text)
    {
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }

    public void StartMainActivity()
    {
        Intent saIntent = new Intent(this, MainActivity.class);
        startActivity(saIntent);
    }
}