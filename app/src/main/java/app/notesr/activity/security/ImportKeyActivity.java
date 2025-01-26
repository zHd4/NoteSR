package app.notesr.activity.security;

import static androidx.core.view.inputmethod.EditorInfoCompat.IME_FLAG_NO_PERSONALIZED_LEARNING;

import static java.util.Objects.requireNonNull;

import android.content.Intent;
import android.graphics.BlendMode;
import android.graphics.BlendModeColorFilter;
import android.graphics.ColorFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import app.notesr.R;
import app.notesr.activity.notes.NotesListActivity;
import app.notesr.service.activity.security.KeySetupService;

public class ImportKeyActivity extends AppCompatActivity {

    private static final String TAG = ImportKeyActivity.class.getName();

    private EditText keyField;
    private KeySetupService keySetupService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_key);

        ActionBar actionBar = requireNonNull(getSupportActionBar());

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(getResources().getString(R.string.import_key));

        String password = requireNonNull(getIntent().getStringExtra("password"));
        keySetupService = new KeySetupService(password);

        keyField = findViewById(R.id.importKeyField);
        keyField.setImeOptions(IME_FLAG_NO_PERSONALIZED_LEARNING);

        Button importKeyButton = findViewById(R.id.importKeyButton);
        importKeyButton.setOnClickListener(importKeyButtonOnClick());
    }

    private View.OnClickListener importKeyButtonOnClick() {
        return view -> {
            String hexKey = keyField.getText().toString();

            if (!hexKey.isBlank()) {
                try {
                    keySetupService.setHexKey(hexKey);
                    keySetupService.apply();

                    startActivity(new Intent(getApplicationContext(), NotesListActivity.class));
                } catch (Exception e) {
                    Log.e(TAG, "Cannot parse or apply the key", e);

                    int fieldColor = ContextCompat.getColor(
                            getApplicationContext(),
                            R.color.key_import_failed_color);

                    ColorFilter colorFilter = new BlendModeColorFilter(
                            fieldColor,
                            BlendMode.SRC_ATOP);

                    keyField.getBackground().setColorFilter(colorFilter);
                }
            }
        };
    }
}