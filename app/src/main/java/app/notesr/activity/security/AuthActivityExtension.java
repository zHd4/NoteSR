/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.security;

import static app.notesr.activity.security.AuthActivity.HEX_KEY;
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
import app.notesr.activity.FsaResolver;
import app.notesr.core.security.SecretCache;
import app.notesr.core.security.crypto.CryptoManager;
import app.notesr.activity.migration.MigrationActivity;
import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.activity.note.NotesListActivity;
import app.notesr.core.util.SecureStringBuilder;
import app.notesr.service.AndroidServiceBootstrapper;
import app.notesr.service.AndroidServiceRegistry;
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
        var password = passwordBuilder.toCharArray();

        if (password.length == 0) {
            showToastMessage(activity.getString(R.string.enter_the_code));
            return;
        }

        if (cryptoManager.configure(activity.getApplicationContext(), password)) {
            onAuthorizationSuccessful();
        } else {
            onAuthorizationFailed();
        }
    }

    public void createPassword() {
        var password = proceedPasswordSetting();

        if (password != null) {
            var context = activity.getApplicationContext();
            var setupKeyActivityIntent = new Intent(context, SetupKeyActivity.class)
                    .putExtra(SetupKeyActivity.EXTRA_MODE, KeySetupMode.FIRST_RUN.toString());

            try {
                var passwordBytes = charsToBytes(password, StandardCharsets.UTF_8);
                SecretCache.put(SetupKeyActivity.PASSWORD, passwordBytes);
            } catch (CharacterCodingException e) {
                throw new RuntimeException(e);
            }

            activity.startActivity(setupKeyActivityIntent);
        }
    }

    public void recoverKey() {
        var password = proceedPasswordSetting();

        if (password != null) {
            try {
                var hexKey = bytesToChars(SecretCache.take(HEX_KEY),
                        StandardCharsets.UTF_8);

                if (hexKey == null) {
                    throw new Exception("Missing hex key");
                }

                Context context = activity.getApplicationContext();
                CryptoSecrets secrets = KeyUtils.getSecretsFromHex(hexKey, password);

                cryptoManager.unblock(context);
                cryptoManager.setSecrets(context, secrets);

                secrets.destroy();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            activity.startActivity(new Intent(activity.getApplicationContext(),
                    NotesListActivity.class));
        }
    }

    public void changePassword() {
        var password = proceedPasswordSetting();

        if (password != null) {
            try {
                var context = activity.getApplicationContext();
                var secrets = cryptoManager.getSecrets();
                secrets.setPassword(password);

                cryptoManager.setSecrets(context, secrets);
                secrets.destroy();

                showToastMessage(R.string.updated);
                activity.startActivity(new Intent(context,  NotesListActivity.class));
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

        var context = activity.getApplicationContext();
        var servicesRegistry = getServicesRegistry();

        new AndroidServiceBootstrapper(servicesRegistry).startServicesPostAuth(
                context,
                cryptoManager.getSecrets()
        );

        activity.startActivity(getNextIntentAfterAuth(context, servicesRegistry));
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

    private Intent getNextIntentAfterAuth(
            Context context,
            AndroidServiceRegistry servicesRegistry
    ) {
        var fsaEntry = new FsaResolver(servicesRegistry).getFsaEntryOfCurrentRunningFs();

        int lastMigrationVersion = new DataVersionManager(context).getCurrentVersion();
        int currentDataSchemaVersion = BuildConfig.DATA_SCHEMA_VERSION;

        if (fsaEntry != null) {
            return new Intent(context, fsaEntry.getActivityClass());
        } else if (lastMigrationVersion < currentDataSchemaVersion) {
            return new Intent(context, MigrationActivity.class);
        }

        return new Intent(context, NotesListActivity.class);
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

    private AndroidServiceRegistry getServicesRegistry() {
        return AndroidServiceRegistry.getInstance(activity.getApplicationContext());
    }
}
