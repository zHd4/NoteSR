package com.notesr.controllers.activities;

import static android.view.inputmethod.EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Base64;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.notesr.R;
import com.notesr.controllers.ActivityHelper;
import com.notesr.controllers.crypto.CryptoController;
import com.notesr.controllers.StorageController;
import com.notesr.controllers.onclick.NextGenkeysButtonController;
import com.notesr.models.Config;

public class SetupActivity extends AppCompatActivity {
    public static final String regenerateKey = "regenerateKey";

    public static String keyToHex(String key) {
        StringBuilder result = new StringBuilder();
        byte[] keyBytes = key.getBytes();
        int buff = 4;

        for(int i=0; i<keyBytes.length; i++) {
            result.append(Integer.toHexString(keyBytes[i]));

            if(i != keyBytes.length - 1){
                result.append(" ");
            }

            buff--;

            if(buff == 0){
                result.append("\n");
                buff = 4;
            }
        }

        return result.toString();
    }

    private String visualKey = "";
    public String tempKey = "";

    @SuppressLint("InlinedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityHelper.context = getApplicationContext();

        setContentView(R.layout.setup_activity);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        if((Config.cryptoKey == null || Config.passwordCode == null) && StorageController.isFileExists(
                getApplicationContext(),
                Config.keyBinFileName)
        ) {
            startActivity(ActivityHelper.getIntent(getApplicationContext(), AccessActivity.class));
        }

        final TextView labelKeyView = findViewById(R.id.labelKeyView);
        final EditText keyField = findViewById(R.id.importKeyField);

        final Button copyToClipboardButton = findViewById(R.id.copyToClipboardButton);
        final Button nextGenkeysButton = findViewById(R.id.nextGenkeysButton);

        keyField.setImeOptions(IME_FLAG_NO_PERSONALIZED_LEARNING);

        copyToClipboardButton.setOnClickListener(view -> {
            ActivityHelper.clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

            ClipData clip = ClipData.newPlainText("", visualKey);

            ActivityHelper.clipboard.setPrimaryClip(clip);
            ActivityHelper.showTextMessage(getResources().getString(R.string.copied),
                    Toast.LENGTH_SHORT, getApplicationContext());
        });

        nextGenkeysButton.setOnClickListener(new NextGenkeysButtonController(this, keyField));

        try {
            this.tempKey = Base64.encodeToString(CryptoController.genKey(), Base64.DEFAULT);
            visualKey = keyToHex(this.tempKey);

            labelKeyView.setText(visualKey);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String checkKeyField(final String keyHex) {
        try {
            if(keyHex.equals("")) {
                return "";
            }

            final String key = ActivityHelper.hexToKey(keyHex);

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
            byte[] testData = CryptoController.genKey();
            byte[] key = Base64.decode(keyBase64, Base64.DEFAULT);

            String encrypted = Base64.encodeToString(
                    CryptoController.encrypt(
                            Base64.encodeToString(testData, Base64.DEFAULT).getBytes(),
                            ActivityHelper.sha256(keyBase64),
                            key),
                    Base64.DEFAULT);

            CryptoController.decrypt(encrypted.getBytes(), ActivityHelper.sha256(keyBase64), key);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void displayRegeneationFailedMessage() {
        ActivityHelper.showTextMessage(
                getResources().getString(R.string.regeneration_failed),
                Toast.LENGTH_SHORT,
                getApplicationContext()
        );
    }
}