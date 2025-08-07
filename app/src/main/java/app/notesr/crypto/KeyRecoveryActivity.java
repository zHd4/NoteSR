package app.notesr.crypto;

import static androidx.core.view.inputmethod.EditorInfoCompat.IME_FLAG_NO_PERSONALIZED_LEARNING;

import static app.notesr.util.ActivityUtils.disableBackButton;
import static app.notesr.util.ActivityUtils.showToastMessage;
import static app.notesr.util.KeyUtils.getSecretsFromHex;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;

import app.notesr.R;
import app.notesr.ActivityBase;
import app.notesr.dto.CryptoSecrets;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

public class KeyRecoveryActivity extends ActivityBase {
    private static final String TAG = KeyRecoveryActivity.class.toString();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_key_recovery);

        ActionBar actionBar = getSupportActionBar();
        Objects.requireNonNull(actionBar).setTitle(getString(R.string.key_recovery));

        EditText hexKeyField = findViewById(R.id.importRecoveryKeyField);
        Button applyButton = findViewById(R.id.applyRecoveryKeyButton);

        disableBackButton(this);

        hexKeyField.setImeOptions(IME_FLAG_NO_PERSONALIZED_LEARNING);
        applyButton.setOnClickListener(applyButtonOnClick(hexKeyField));
    }

    private View.OnClickListener applyButtonOnClick(EditText hexKeyField) {
        return view -> {
            String hexKey = hexKeyField.getText().toString();

            if (!hexKey.isBlank()) {
                try {
                    CryptoManager cryptoManager = CryptoManager.getInstance();
                    CryptoSecrets cryptoSecrets = getSecretsFromHex(hexKey, null);

                    if (cryptoManager.verifyKey(cryptoSecrets.getKey())) {
                        startActivity(new Intent(getApplicationContext(), AuthActivity.class)
                                .putExtra("mode", AuthActivity.Mode.KEY_RECOVERY.toString())
                                .putExtra("hexKey", hexKey));

                        finish();
                    } else {
                        Log.e(TAG, "Wrong key: " + hexKey);
                        showToastMessage(this,getString(R.string.wrong_key), Toast.LENGTH_SHORT);
                    }
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Invalid key", e);
                    showToastMessage(this,getString(R.string.invalid_key),
                            Toast.LENGTH_SHORT);
                } catch (IOException | NoSuchAlgorithmException e) {
                    Log.e(TAG, e.toString());
                    throw new RuntimeException(e);
                }
            }
        };
    }
}
