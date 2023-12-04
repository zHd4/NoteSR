package com.peew.notesr.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.peew.notesr.R;
import com.peew.notesr.crypto.CryptoKey;
import com.peew.notesr.crypto.CryptoManager;
import com.peew.notesr.crypto.CryptoTools;

import java.security.NoSuchAlgorithmException;

public class SetupKeyActivity extends ExtendedAppCompatActivity {
    private String password;
    private CryptoKey key;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_key);

        TextView keyView = findViewById(R.id.aes_key_hex);
        Button copyToClipboardButton = findViewById(R.id.copy_aes_key_hex);

        password = getIntent().getStringExtra("password");

        try {
            key = CryptoManager.getInstance().generateNewKey(password);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        String keyHex = CryptoTools.keyBytesToHex(key.getKey().getEncoded());

        keyView.setText(keyHex);
        copyToClipboardButton.setOnClickListener(copyToClipboardOnClick(keyHex));
    }

    private View.OnClickListener copyToClipboardOnClick(String keyHex) {
        return view -> {
            copyToClipboard("", keyHex);
            showToastMessage(getString(R.string.copied), Toast.LENGTH_SHORT);
        };
    }
}