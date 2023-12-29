package com.peew.notesr.activities;

import static androidx.core.view.inputmethod.EditorInfoCompat.IME_FLAG_NO_PERSONALIZED_LEARNING;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.peew.notesr.App;
import com.peew.notesr.R;
import com.peew.notesr.crypto.CryptoTools;

public class KeyRecoveryActivity extends ExtendedAppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_key_recovery);

        EditText hexKeyField = findViewById(R.id.import_key_field);
        Button applyButton = findViewById(R.id.check_recovery_key_button);

        disableBackButton();

        hexKeyField.setImeOptions(IME_FLAG_NO_PERSONALIZED_LEARNING);
        applyButton.setOnClickListener(recoveryKeyButtonOnClick(hexKeyField));
    }

    private View.OnClickListener recoveryKeyButtonOnClick(EditText hexKeyField) {
        return view -> {
            String hexKey = hexKeyField.getText().toString();

            if (!hexKey.isBlank()) {
                try {
                    CryptoTools.hexToCryptoKey(hexKey, null);
                    Intent authActivityIntent = new Intent(App.getContext(), AuthActivity.class);

                    authActivityIntent.putExtra("mode", AuthActivity.KEY_RECOVERY_MODE);
                    authActivityIntent.putExtra("hex-key", hexKey);

                    startActivity(authActivityIntent);
                } catch (Exception e) {
                    Log.e("Key recovery error", e.toString());
                    showToastMessage(getString(R.string.wrong_key), Toast.LENGTH_SHORT);
                }
            }
        };
    }
}
