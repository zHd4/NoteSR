package app.notesr.activity.security;

import static androidx.core.view.inputmethod.EditorInfoCompat.IME_FLAG_NO_PERSONALIZED_LEARNING;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;

import app.notesr.App;
import app.notesr.R;
import app.notesr.activity.ExtendedAppCompatActivity;
import app.notesr.crypto.CryptoTools;

import java.util.Objects;

public class KeyRecoveryActivity extends ExtendedAppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_key_recovery);

        ActionBar actionBar = getSupportActionBar();
        Objects.requireNonNull(actionBar).setTitle(getString(R.string.key_recovery));

        EditText hexKeyField = findViewById(R.id.importKeyField);
        Button applyButton = findViewById(R.id.checkRecoveryKeyButton);

        disableBackButton();

        hexKeyField.setImeOptions(IME_FLAG_NO_PERSONALIZED_LEARNING);
        applyButton.setOnClickListener(applyButtonOnClick(hexKeyField));
    }

    private View.OnClickListener applyButtonOnClick(EditText hexKeyField) {
        return view -> {
            String hexKey = hexKeyField.getText().toString();

            if (!hexKey.isBlank()) {
                try {
                    CryptoTools.hexToCryptoKey(hexKey, null);

                    Intent authActivityIntent = new Intent(App.getContext(), AuthActivity.class);
                    String mode = AuthActivity.Mode.KEY_RECOVERY.toString();

                    authActivityIntent.putExtra("mode", mode);
                    authActivityIntent.putExtra("hexKey", hexKey);

                    startActivity(authActivityIntent);
                } catch (Exception e) {
                    Log.e("Key recovery error", e.toString());
                    showToastMessage(getString(R.string.wrong_key), Toast.LENGTH_SHORT);
                }
            }
        };
    }
}
