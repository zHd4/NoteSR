package app.notesr.activity.security;

import static androidx.core.view.inputmethod.EditorInfoCompat.IME_FLAG_NO_PERSONALIZED_LEARNING;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import app.notesr.App;
import app.notesr.R;
import app.notesr.activity.ExtendedAppCompatActivity;
import app.notesr.model.CryptoKey;
import app.notesr.crypto.CryptoTools;
import app.notesr.onclick.security.FinishKeySetupOnClick;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.security.NoSuchAlgorithmException;

@Getter
public class SetupKeyActivity extends ExtendedAppCompatActivity {

    @AllArgsConstructor
    public enum Mode {
        FIRST_RUN("first_run"),
        REGENERATION("regeneration");

        public final String mode;
    }

    private static final String TAG = SetupKeyActivity.class.getName();

    private Mode mode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_key);
        setMode();

        TextView keyView = findViewById(R.id.aesKeyHex);

        EditText importKeyField = findViewById(R.id.importKeyField);
        importKeyField.setImeOptions(IME_FLAG_NO_PERSONALIZED_LEARNING);

        Button copyToClipboardButton = findViewById(R.id.copyAesKeyHex);
        Button nextButton = findViewById(R.id.keySetupNextButton);

        String password = getPassword();
        CryptoKey key = getKey(password);
        String keyHex = CryptoTools.cryptoKeyToHex(key);

        keyView.setText(keyHex);
        copyToClipboardButton.setOnClickListener(getCopyKeyOnClick(keyHex));
        nextButton.setOnClickListener(new FinishKeySetupOnClick(this, password, key));
    }

    private void setMode() {
        String modeName = getIntent().getStringExtra("mode");

        try {
            mode = Mode.valueOf(modeName);

            if (mode == Mode.REGENERATION) {
                disableBackButton();
            }
        } catch (NullPointerException e) {
            Log.e(TAG, "Mode didn't provided", e);
            throw new RuntimeException(e);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid mode: " + modeName, e);
            throw new RuntimeException(e);
        }
    }

    private String getPassword() {
        String password = getIntent().getStringExtra("password");

        if (password == null) {
            throw new RuntimeException("Password didn't provided");
        }

        return password;
    }

    private CryptoKey getKey(String password) {
        try {
            return App.getAppContainer().getCryptoManager().generateNewKey(password);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private View.OnClickListener getCopyKeyOnClick(String keyHex) {
        return view -> {
            copyToClipboard("", keyHex);
            showToastMessage(getString(R.string.copied), Toast.LENGTH_SHORT);
        };
    }
}
