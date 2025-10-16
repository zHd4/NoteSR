package app.notesr.activity.security;

import static app.notesr.core.util.CharUtils.bytesToChars;
import static app.notesr.core.util.CharUtils.charsToBytes;

import android.content.Context;
import android.content.Intent;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import app.notesr.BuildConfig;
import app.notesr.R;
import app.notesr.core.security.SecretCache;
import app.notesr.core.security.crypto.CryptoManager;
import app.notesr.activity.migration.MigrationActivity;
import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.activity.note.NotesListActivity;
import app.notesr.core.util.SecureStringBuilder;
import app.notesr.service.migration.DataVersionManager;
import app.notesr.core.util.ActivityUtils;
import app.notesr.core.util.KeyUtils;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class AuthActivityExtension {
    private static final int MAX_ATTEMPTS = 3;
    private static final int MIN_PASSWORD_LENGTH = 4;
    private static final int ON_WRONG_PASSWORD_DELAY_MS = 1500;

    private final AuthActivity activity;
    private final CryptoManager cryptoManager;
    private final SecureStringBuilder passwordBuilder;

    private int attempts = MAX_ATTEMPTS;
    private char[] createdPassword;

    public void authorize() {
        char[] password = passwordBuilder.toCharArray();

        if (password.length == 0) {
            String enterCodeMessage = activity.getString(R.string.enter_the_code);
            showToastMessage(enterCodeMessage);
            return;
        }

        if (cryptoManager.configure(activity.getApplicationContext(), password)) {
            onAuthorizationSuccessful();
        } else {
            onAuthorizationFailed();
        }
    }

    public void createPassword() {
        char[] password = proceedPasswordSetting();

        if (password != null) {
            Intent setupKeyActivityIntent = new Intent(activity.getApplicationContext(),
                    SetupKeyActivity.class);

            setupKeyActivityIntent.putExtra("mode", KeySetupMode.FIRST_RUN.toString());

            try {
                SecretCache.put("password", charsToBytes(password, StandardCharsets.UTF_8));
            } catch (CharacterCodingException e) {
                throw new RuntimeException(e);
            }

            activity.startActivity(getNextIntent(setupKeyActivityIntent, false));
        }
    }

    public void recoverKey() {
        char[] password = proceedPasswordSetting();

        if (password != null) {
            try {
                char[] hexKey = bytesToChars(SecretCache.take("hexKey"),
                        StandardCharsets.UTF_8);

                if (hexKey == null) {
                    throw new Exception("Missing hex key");
                }

                Context context = activity.getApplicationContext();

                cryptoManager.unblock(context);
                cryptoManager.setSecrets(context, KeyUtils.getSecretsFromHex(hexKey, password));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            Intent defaultIntent = new Intent(activity.getApplicationContext(),
                    NotesListActivity.class);

            activity.startActivity(getNextIntent(defaultIntent, true));
        }
    }

    public void changePassword() {
        char[] password = proceedPasswordSetting();

        if (password != null) {
            try {
                Context context = activity.getApplicationContext();
                CryptoSecrets secrets = cryptoManager.getSecrets();

                secrets.setPassword(password);
                cryptoManager.setSecrets(context, secrets);

                showToastMessage(R.string.updated);

                Intent defaultIntent = new Intent(activity.getApplicationContext(),
                        NotesListActivity.class);

                activity.startActivity(getNextIntent(defaultIntent, false));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private char[] proceedPasswordSetting() {
        char[] password = passwordBuilder.toCharArray();
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
            if (Arrays.equals(password, createdPassword)) {
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
                cryptoManager.block(activity.getApplicationContext());
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
        passwordBuilder.wipe();
    }

    private void showToastMessage(int stringResId) {
        showToastMessage(activity.getString(stringResId));
    }

    private void showToastMessage(String text) {
        ActivityUtils.showToastMessage(activity, text, Toast.LENGTH_SHORT);
    }
}
