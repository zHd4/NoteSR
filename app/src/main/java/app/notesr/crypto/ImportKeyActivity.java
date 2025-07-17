package app.notesr.crypto;

import static androidx.core.view.inputmethod.EditorInfoCompat.IME_FLAG_NO_PERSONALIZED_LEARNING;

import static java.util.Objects.requireNonNull;

import static app.notesr.util.ActivityUtils.showToastMessage;

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
import app.notesr.crypto.service.KeySetupService;
import app.notesr.data.ReEncryptionActivity;
import app.notesr.notes.NoteListActivity;

public class ImportKeyActivity extends ActivityBase {

    private static final String TAG = ImportKeyActivity.class.getName();

    private SetupKeyActivity.Mode mode;
    private EditText keyField;
    private KeySetupService keySetupService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_key);

        ActionBar actionBar = requireNonNull(getSupportActionBar());

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(getResources().getString(R.string.import_key));

        mode = SetupKeyActivity.Mode.valueOf(requireNonNull(getIntent().getStringExtra("mode")));

        String password = requireNonNull(getIntent().getStringExtra("password"));
        keySetupService = new KeySetupService(password);

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
                    keySetupService.setHexKey(hexKey);

                    if (mode == SetupKeyActivity.Mode.FIRST_RUN) {
                        keySetupService.apply();
                        startActivity(new Intent(getApplicationContext(), NoteListActivity.class));
                    } else if (mode == SetupKeyActivity.Mode.REGENERATION) {
                        Intent intent = new Intent(getApplicationContext(), ReEncryptionActivity.class)
                                .putExtra("newCryptoKey", keySetupService.getCryptoKey());

                        startActivity(intent);
                        finish();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Cannot parse or apply the key", e);
                    showToastMessage(this, getString(R.string.wrong_key), Toast.LENGTH_SHORT);
                }
            }
        };
    }
}