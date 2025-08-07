package app.notesr.crypto;

import android.content.Context;
import android.content.Intent;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

import app.notesr.BuildConfig;
import app.notesr.R;
import app.notesr.data.MigrationActivity;
import app.notesr.dto.CryptoSecrets;
import app.notesr.note.NotesListActivity;
import app.notesr.service.migration.DataVersionManager;
import app.notesr.util.ActivityUtils;
import app.notesr.util.KeyUtils;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AuthActivityExtension {
    private static final int MAX_ATTEMPTS = 3;
    private static final int MIN_PASSWORD_LENGTH = 4;
    private static final int ON_WRONG_PASSWORD_DELAY_MS = 1500;

    private final AuthActivity activity;
    private final CryptoManager cryptoManager;
    private final StringBuilder passwordBuilder;

    private int attempts = MAX_ATTEMPTS;
    private String createdPassword;

    public void authorize() {
        String password = passwordBuilder.toString();

        if (password.isEmpty()) {
            String enterCodeMessage = activity.getString(R.string.enter_the_code);
            showToastMessage(enterCodeMessage);
            return;
        }

        if (cryptoManager.configure(password)) {
            onAuthorizationSuccessful();
        } else {
            onAuthorizationFailed();
        }
    }

    public void createPassword() {
        String password = proceedPasswordSetting();

        if (password != null) {
            Intent setupKeyActivityIntent = new Intent(activity.getApplicationContext(),
                    SetupKeyActivity.class);

            setupKeyActivityIntent.putExtra("mode", KeySetupMode.FIRST_RUN.toString());
            setupKeyActivityIntent.putExtra("password", password);

            activity.startActivity(getNextIntent(setupKeyActivityIntent, false));
        }
    }

    public void recoverKey() {
        String password = proceedPasswordSetting();

        if (password != null) {
            String hexKey = activity.getIntent().getStringExtra("hexKey");

            try {
                if (hexKey == null) throw new Exception("Missing hex key");

                cryptoManager.setSecrets(KeyUtils.getSecretsFromHex(hexKey, password));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            Intent defaultIntent = new Intent(activity.getApplicationContext(),
                    NotesListActivity.class);

            activity.startActivity(getNextIntent(defaultIntent, true));
        }
    }

    public void changePassword() {
        String password = proceedPasswordSetting();

        if (password != null) {
            try {
                CryptoSecrets secrets = cryptoManager.getSecrets();

                secrets.setPassword(password);
                cryptoManager.setSecrets(secrets);

                showToastMessage(R.string.updated);

                Intent defaultIntent = new Intent(activity.getApplicationContext(),
                        NotesListActivity.class);

                activity.startActivity(getNextIntent(defaultIntent, false));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private String proceedPasswordSetting() {
        String password = passwordBuilder.toString();
        TextView topLabel = activity.findViewById(R.id.authTopLabel);

        if (createdPassword == null) {
            if (passwordBuilder.length() >= MIN_PASSWORD_LENGTH) {
                createdPassword = password;
                topLabel.setText(activity.getString(R.string.repeat_access_code));
            } else {
                showToastMessage(String.format(
                        activity.getString(R.string.minimum_password_length_is_n),
                        MIN_PASSWORD_LENGTH));
            }
        } else {
            if (password.equals(createdPassword)) {
                return password;
            } else {
                showToastMessage(R.string.code_not_match);
            }
        }

        resetPassword();
        return null;
    }

    private void onAuthorizationSuccessful() {
        TextView censoredPasswordView = activity.findViewById(R.id.censoredPasswordTextView);
        censoredPasswordView.setText("");

        Intent defaultIntent = new Intent(activity.getApplicationContext(),
                NotesListActivity.class);

        activity.startActivity(getNextIntent(defaultIntent, true));
        activity.finish();
    }

    private void onAuthorizationFailed() {
        attempts--;

        if (attempts == 0) {
            try {
                cryptoManager.block();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            showToastMessage(R.string.blocked);
            activity.startActivity(new Intent(activity.getApplicationContext(),
                    KeyRecoveryActivity.class));
        } else {
            try {
                Thread.sleep(ON_WRONG_PASSWORD_DELAY_MS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            showToastMessage(String.format(
                    activity.getString(R.string.wrong_code_you_have_n_attempts),
                    attempts));
        }

        resetPassword();
    }

    private Intent getNextIntent(Intent defaultIntent, boolean checkForMigrations) {
        Context context = activity.getApplicationContext();

        if (checkForMigrations) {
            int lastMigrationVersion = new DataVersionManager(context).getCurrentVersion();
            int currentDataSchemaVersion = BuildConfig.DATA_SCHEMA_VERSION;

            return lastMigrationVersion < currentDataSchemaVersion
                    ? new Intent(context, MigrationActivity.class)
                    : defaultIntent;
        }

        return defaultIntent;
    }

    private void resetPassword() {
        TextView censoredPasswordView = activity.findViewById(R.id.censoredPasswordTextView);

        censoredPasswordView.setText("");
        passwordBuilder.setLength(0);
    }

    private void showToastMessage(int stringResId) {
        showToastMessage(activity.getString(stringResId));
    }

    private void showToastMessage(String text) {
        ActivityUtils.showToastMessage(activity, text, Toast.LENGTH_SHORT);
    }
}
