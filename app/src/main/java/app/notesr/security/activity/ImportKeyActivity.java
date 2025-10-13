package app.notesr.security.activity;

import static androidx.core.view.inputmethod.EditorInfoCompat.IME_FLAG_NO_PERSONALIZED_LEARNING;

import static java.util.Objects.requireNonNull;

import static app.notesr.core.util.ActivityUtils.showToastMessage;
import static app.notesr.core.util.KeyUtils.getSecretsFromHex;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;

import app.notesr.R;
import app.notesr.ActivityBase;
import app.notesr.core.security.crypto.CryptoManager;
import app.notesr.core.security.crypto.CryptoManagerProvider;
import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.security.service.SecretsSetupService;

public final class ImportKeyActivity extends ActivityBase {

    private static final String TAG = ImportKeyActivity.class.getName();

    private KeySetupMode mode;
    private EditText keyField;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_key);

        ActionBar actionBar = requireNonNull(getSupportActionBar());

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(getResources().getString(R.string.import_key));

        mode = KeySetupMode.valueOf(requireNonNull(getIntent().getStringExtra("mode")));

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
            String hexKey = keyField.getText().toString();

            if (!hexKey.isBlank()) {
                try {
                    Context context = getApplicationContext();
                    String password = getIntent().getStringExtra("password");
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
                }
            }
        };
    }
}
