package app.notesr.activity.security;

import static androidx.core.view.inputmethod.EditorInfoCompat.IME_FLAG_NO_PERSONALIZED_LEARNING;

import static java.util.Objects.requireNonNull;

import static app.notesr.core.util.ActivityUtils.showToastMessage;
import static app.notesr.core.util.CharUtils.bytesToChars;
import static app.notesr.core.util.KeyUtils.getSecretsFromHex;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;

import app.notesr.R;
import app.notesr.activity.ActivityBase;
import app.notesr.core.security.SecretCache;
import app.notesr.core.security.crypto.CryptoManager;
import app.notesr.core.security.crypto.CryptoManagerProvider;
import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.service.security.SecretsSetupService;

public final class ImportKeyActivity extends ActivityBase {

    public static final String PASSWORD = "password";
    public static final String EXTRA_MODE = "mode";
    private static final String TAG = ImportKeyActivity.class.getCanonicalName();

    private KeySetupMode mode;
    private EditText keyField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_key);

        ActionBar actionBar = requireNonNull(getSupportActionBar());

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(getResources().getString(R.string.import_key));

        mode = KeySetupMode.valueOf(requireNonNull(getIntent().getStringExtra(EXTRA_MODE)));

        keyField = findViewById(R.id.importKeyField);
        keyField.setImeOptions(IME_FLAG_NO_PERSONALIZED_LEARNING);

        Button importKeyButton = findViewById(R.id.importKeyButton);
        importKeyButton.setOnClickListener(importKeyButtonOnClick());
    }

    @Override
    public boolean onSupportNavigateUp() {
        super.onBackPressed();
        return true;
    }

    private View.OnClickListener importKeyButtonOnClick() {
        return view -> {
            Editable hexKeyEditable = keyField.getText();

            char[] hexKey = new char[hexKeyEditable.length()];
            hexKeyEditable.getChars(0, hexKeyEditable.length(), hexKey, 0);
            hexKeyEditable.replace(0, hexKeyEditable.length(), "");
            keyField.setText("");

            if (hexKey.length > 0) {
                try {
                    Context context = getApplicationContext();

                    char[] password = bytesToChars(SecretCache.take(PASSWORD),
                            StandardCharsets.UTF_8);

                    CryptoSecrets cryptoSecrets = getSecretsFromHex(hexKey, password);
                    CryptoManager cryptoManager = CryptoManagerProvider.getInstance(context);

                    SecretsSetupService keySetupService = new SecretsSetupService(
                            getApplicationContext(),
                            cryptoManager,
                            cryptoSecrets
                    );

                    new KeySetupCompletionHandler(this, keySetupService, mode).handle();
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Invalid key", e);
                    showToastMessage(this, getString(R.string.invalid_key),
                            Toast.LENGTH_SHORT);
                } catch (CharacterCodingException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }
}
