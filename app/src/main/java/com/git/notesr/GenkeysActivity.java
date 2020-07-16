package com.git.notesr;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class GenkeysActivity extends AppCompatActivity {

    @SuppressLint("StaticFieldLeak")
    private static String visualKey = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityTools.context = getApplicationContext();

        setContentView(R.layout.genkeys_activity);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        TextView pKey = findViewById(R.id.pkeyView);
        Button copyTCButton = findViewById(R.id.copyToClipboardButton);
        Button nextGKButton = findViewById(R.id.nextGenkeysButton);

        copyTCButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityTools.clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("", visualKey);
                ActivityTools.clipboard.setPrimaryClip(clip);

                ActivityTools.showTextMessage("Copied!", Toast.LENGTH_SHORT,
                        getApplicationContext());
            }
        });

        nextGKButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AccessActivity.operation = AccessActivity.CREATE_PIN;
                startActivity(ActivityTools.getIntent(getApplicationContext(),
                        AccessActivity.class));
            }
        });

        try {
            String randKey = ActivityTools.randomString(2048);
            Config.aesKey = Base64.encodeToString(AES.genKey(randKey, ActivityTools.md5(randKey)),
                    Base64.DEFAULT);

            visualKey = ActivityTools.keyToHex(randKey);

            pKey.setText(String.format("%s...", visualKey.substring(0, 100)));
        } catch (Exception e) {
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        }
    }
}