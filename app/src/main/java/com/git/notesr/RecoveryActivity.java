package com.git.notesr;

import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

public class RecoveryActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recovery_activity);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        final EditText keyText = findViewById(R.id.keyText);
        final Button decryptButton =  findViewById(R.id.decryptButton);

        decryptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try{
                    String key = hexToKey(keyText.getText().toString());
                    byte[] keyB = Base64.decode(key, Base64.DEFAULT);
                    AES.Decrypt(Storage.ReadFile(getApplicationContext(), "notes.json"), keyB);

                    Config.aesKey = key;

                    AccessActivity.operation = AccessActivity.CREATE_PIN;
                    StartAccessActivity();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

    private void StartAccessActivity() {
        Intent saIntent = new Intent(this, AccessActivity.class);
        startActivity(saIntent);
    }

    public static String hexToKey(String hex) {
        String[] hexArr = hex.replace("\n", "").split(" ");
        byte[] keyBytes = new byte[hexArr.length];

        for (int i=0; i<hexArr.length; i++) {
            keyBytes[i] = (byte)Integer.parseInt(hexArr[i], 16);
        }

        return new String(keyBytes);
    }
}
