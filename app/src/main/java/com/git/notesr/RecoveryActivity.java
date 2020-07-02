package com.git.notesr;

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
                    String keyS = hexToKey(keyText.getText().toString());
                    byte[] key = AES.GenKey(keyS, GenkeysActivity.md5(keyS));
                    AES.Decrypt(Storage.ReadFile(getApplicationContext(), "notes.json"), key);

                    Config.aesKey = Base64.encodeToString(key, Base64.DEFAULT);

                    AccessActivity.operation = AccessActivity.CREATE_PIN;

                    startActivity(MainActivity.GetIntent(getApplicationContext(), AccessActivity.class));
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        });
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
