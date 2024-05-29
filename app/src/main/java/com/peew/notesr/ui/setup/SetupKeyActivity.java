package com.peew.notesr.ui.setup;

import static androidx.core.view.inputmethod.EditorInfoCompat.IME_FLAG_NO_PERSONALIZED_LEARNING;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.peew.notesr.App;
import com.peew.notesr.R;
import com.peew.notesr.crypto.CryptoKey;
import com.peew.notesr.crypto.CryptoTools;
import com.peew.notesr.ui.ExtendedAppCompatActivity;
import com.peew.notesr.ui.onclick.FinishKeySetupOnClick;

import java.security.NoSuchAlgorithmException;

public class SetupKeyActivity extends ExtendedAppCompatActivity {
    public static final int FIRST_RUN_MODE = 0;
    public static final int REGENERATION_MODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_key);

        TextView keyView = findViewById(R.id.aes_key_hex);

        EditText importKeyField = findViewById(R.id.import_key_field);
        importKeyField.setImeOptions(IME_FLAG_NO_PERSONALIZED_LEARNING);

        Button copyToClipboardButton = findViewById(R.id.copy_aes_key_hex);
        Button nextButton = findViewById(R.id.key_setup_next_button);

        int mode = getIntent().getIntExtra("mode", -1);
        String password = getIntent().getStringExtra("password");

        if (mode == -1) {
            throw new RuntimeException("Mode didn't provided");
        } else if (mode == REGENERATION_MODE) {
            disableBackButton();
        }

        if (password == null) {
            throw new RuntimeException("Password didn't provided");
        }

        CryptoKey key;

        try {
            key = App.getAppContainer().getCryptoManager().generateNewKey(password);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        String keyHex = CryptoTools.cryptoKeyToHex(key);

        keyView.setText(keyHex);
        copyToClipboardButton.setOnClickListener(getCopyKeyOnClick(keyHex));
        nextButton.setOnClickListener(new FinishKeySetupOnClick(this, mode, password, key));
    }

    private View.OnClickListener getCopyKeyOnClick(String keyHex) {
        return view -> {
            copyToClipboard("", keyHex);
            showToastMessage(getString(R.string.copied), Toast.LENGTH_SHORT);
        };
    }
}
