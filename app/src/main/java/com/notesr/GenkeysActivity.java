package com.notesr;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.notesr.controllers.Crypto;
import com.notesr.models.ActivityTools;
import com.notesr.models.Config;

public class GenkeysActivity extends AppCompatActivity {
    private static final String EMPTY = "";

    private String visualKey = EMPTY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityTools.context = getApplicationContext();

        setContentView(R.layout.genkeys_activity);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        final TextView labelKeyView = findViewById(R.id.labelKeyView);
        final EditText keyField = findViewById(R.id.importKeyField);

        final Button copyToClipboardButton = findViewById(R.id.copyToClipboardButton);
        final Button nextGenkeysButton = findViewById(R.id.nextGenkeysButton);

        copyToClipboardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityTools.clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

                ClipData clip = ClipData.newPlainText(EMPTY, visualKey);

                ActivityTools.clipboard.setPrimaryClip(clip);
                ActivityTools.showTextMessage(getResources().getString(R.string.copied),
                        Toast.LENGTH_SHORT, getApplicationContext());
            }
        });

        nextGenkeysButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String impotedKey = checkKeyField(keyField.getText().toString());

                if(impotedKey != null) {
                    if(!impotedKey.equals(EMPTY)) {
                        Config.cryptoKey = impotedKey;
                    }

                    AccessActivity.operation = AccessActivity.CREATE_PIN;
                    startActivity(ActivityTools.getIntent(getApplicationContext(), AccessActivity.class));
                }
            }
        });

        try {
            Config.cryptoKey = Base64.encodeToString(Crypto.genKey(), Base64.DEFAULT);
            visualKey = ActivityTools.keyToHex(Config.cryptoKey);

            labelKeyView.setText(visualKey);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String checkKeyField(final String keyHex) {
        try {
            if(keyHex.equals(EMPTY)) {
                return EMPTY;
            }

            final String key = ActivityTools.hexToKey(keyHex);

            if(!testKey(key)) {
                return null;
            }

            return key;
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean testKey(final String keyBase64) {
        try {
            byte[] testData = Crypto.genKey();
            byte[] key = Base64.decode(keyBase64, Base64.DEFAULT);

            String encrypted = Crypto.encrypt(Base64.encodeToString(testData, Base64.DEFAULT),
                    ActivityTools.sha256(keyBase64), key);

            Crypto.decrypt(encrypted, ActivityTools.sha256(keyBase64), key);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}