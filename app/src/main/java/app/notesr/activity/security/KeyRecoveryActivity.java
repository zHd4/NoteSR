package app.notesr.activity.security;

import static androidx.core.view.inputmethod.EditorInfoCompat.IME_FLAG_NO_PERSONALIZED_LEARNING;

import static app.notesr.core.util.ActivityUtils.disableBackButton;
import static app.notesr.core.util.ActivityUtils.showToastMessage;
import static app.notesr.core.util.CharUtils.charsToBytes;
import static app.notesr.core.util.KeyUtils.getSecretsFromHex;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;

import app.notesr.R;
import app.notesr.ActivityBase;
import app.notesr.core.security.SecretCache;
import app.notesr.core.security.crypto.CryptoManager;
import app.notesr.core.security.crypto.CryptoManagerProvider;
import app.notesr.core.security.dto.CryptoSecrets;

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

public final class KeyRecoveryActivity extends ActivityBase {
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
            Editable hexKeyEditable = hexKeyField.getText();

            char[] hexKey = new char[hexKeyEditable.length()];
            hexKeyEditable.getChars(0, hexKeyEditable.length(), hexKey, 0);


            if (hexKey.length > 0) {
                try {
                    Context context = getApplicationContext();
                    CryptoManager cryptoManager = CryptoManagerProvider.getInstance(context);
                    CryptoSecrets cryptoSecrets = getSecretsFromHex(hexKey, null);

                    if (cryptoManager.verifyKey(context, cryptoSecrets.getKey())) {
                        SecretCache.put("hexKey", charsToBytes(hexKey, StandardCharsets.UTF_8));

                        startActivity(new Intent(context, AuthActivity.class)
                                .putExtra("mode", AuthActivity.Mode.KEY_RECOVERY.toString()));

                        finish();
                    } else {
                        Log.d(TAG, "Wrong key: " + hexKey);
                        showToastMessage(this,
                                getString(R.string.wrong_key),
                                Toast.LENGTH_SHORT);
                    }
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Invalid key", e);
                    showToastMessage(this, getString(R.string.invalid_key),
                            Toast.LENGTH_SHORT);
                } catch (CharacterCodingException e) {
                    throw new RuntimeException(e);
                } catch (IOException | NoSuchAlgorithmException e) {
                    Log.e(TAG, e.toString());
                    throw new RuntimeException(e);
                }
            }
        };
    }
}
