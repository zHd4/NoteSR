/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.security;

import static app.notesr.activity.security.AuthActivity.CACHE_KEY_HEX_KEY;
import static app.notesr.core.util.CharUtils.bytesToChars;
import static app.notesr.core.util.CharUtils.charsToBytes;

import android.content.Intent;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import app.notesr.BuildConfig;
import app.notesr.R;
import app.notesr.activity.ActivityBase;
import app.notesr.activity.FsaResolver;
import app.notesr.core.security.SecretCache;
import app.notesr.activity.migration.MigrationActivity;
import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.activity.note.list.NotesListActivity;
import app.notesr.core.util.SecureStringBuilder;
import app.notesr.service.AndroidServiceBootstrapper;
import app.notesr.service.migration.DataVersionManager;
import app.notesr.core.util.ActivityUtils;
import app.notesr.core.util.KeyUtils;
import app.notesr.service.security.AppSecurityException;
import app.notesr.service.security.AppSecurityService;
import app.notesr.service.security.AuthenticationFailedException;
import app.notesr.service.security.rotation.SecretsRotationService;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
public class AuthHandler {
    private static final int MAX_ATTEMPTS = 3;
    private static final int DELAY_AFTER_AUTH_FAILED = 1500;

    private final AuthActivity activity;
    private final AppSecurityService appSecurityService;
    private final SecretsRotationService secretsRotationService;
    private final SecureStringBuilder passwordBuilder;
    private final FsaResolver fsaResolver;
    private final AndroidServiceBootstrapper androidServiceBootstrapper;
    private final DataVersionManager dataVersionManager;

    @Getter(AccessLevel.PROTECTED)
    @Setter(AccessLevel.PROTECTED)
    private int authAttempts = MAX_ATTEMPTS;

    @Getter(AccessLevel.PROTECTED)
    private char[] createdPassword;

    public void authenticate() {
        var password = passwordBuilder.toCharArray();

        if (password.length == 0) {
            showToastMessage(R.string.enter_the_code);
            return;
        }

        try {
            appSecurityService.authenticate(password);
        } catch (AuthenticationFailedException e) {
            onAuthenticationFailed();
            return;
        } catch (AppSecurityException e) {
            throw new RuntimeException(e);
        }

        onAuthenticationSuccessful();
    }

    public void createPassword() {
        char[] password = proceedPasswordSetting();

        if (password != null) {
            try {
                var passwordBytes = charsToBytes(password, StandardCharsets.UTF_8);
                SecretCache.put(SetupKeyActivity.CACHE_KEY_PASSWORD, passwordBytes);
            } catch (CharacterCodingException e) {
                throw new RuntimeException(e);
            }

            var setupKeyActivityIntent = getNewIntent(SetupKeyActivity.class)
                    .putExtra(SetupKeyActivity.EXTRA_MODE, KeySetupMode.FIRST_RUN.getMode());

            activity.startActivity(setupKeyActivityIntent);
            activity.finish();
        }
    }

    public void recoverKey() {
        char[] password = proceedPasswordSetting();

        if (password != null) {
            try {

                byte[] hexKeyBytes = SecretCache.take(CACHE_KEY_HEX_KEY);

                if (hexKeyBytes == null) {
                    throw new RuntimeException("Missing hex key");
                }

                char[] hexKey = bytesToChars(hexKeyBytes,
                        StandardCharsets.UTF_8);

                CryptoSecrets secrets = KeyUtils.getSecretsFromKeyHexAndPassword(hexKey, password);
                appSecurityService.unblockApp(secrets);
                secrets.destroy();
            } catch (AppSecurityException | CharacterCodingException e) {
                throw new RuntimeException(e);
            }

            activity.startActivity(getNewIntent(NotesListActivity.class));
            activity.finish();
        }
    }

    public void changePassword() {
        char[] password = proceedPasswordSetting();

        if (password == null) {
            // New password entered, but not confirmed (repeated by user)
            return;
        }

        try {
            secretsRotationService.updatePassword(password);

            showToastMessage(R.string.updated);
            activity.startActivity(getNewIntent(NotesListActivity.class));
            activity.finish();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected char[] proceedPasswordSetting() {
        char[] password = passwordBuilder.toCharArray();
        TextView topLabel = activity.findViewById(R.id.authTopLabel);

        if (createdPassword == null) {
            if (passwordBuilder.length() >= CryptoSecrets.PASSWORD_MIN_LENGTH) {
                createdPassword = password;
                topLabel.setText(activity.getString(R.string.repeat_access_code));
            } else {
                showToastMessage(String.format(
                        activity.getString(R.string.minimum_password_length_is_n),
                        CryptoSecrets.PASSWORD_MIN_LENGTH));
            }
        } else {
            if (Arrays.equals(password, createdPassword)) {
                resetPassword();
                return password;
            } else {
                showToastMessage(R.string.code_not_match);
            }
        }

        resetPassword();
        return null;
    }

    protected void onAuthenticationSuccessful() {
        TextView censoredPasswordView = activity.findViewById(R.id.censoredPasswordTextView);
        censoredPasswordView.setText("");

        var context = activity.getApplicationContext();

        androidServiceBootstrapper.startServicesPostAuth(
                context,
                appSecurityService.getActualSecrets()
        );

        activity.startActivity(getNextIntentAfterAuth());
        activity.finish();
    }

    protected void onAuthenticationFailed() {
        authAttempts--;

        if (authAttempts == 0) {
            try {
                appSecurityService.blockApp();
            } catch (AppSecurityException e) {
                throw new RuntimeException(e);
            }

            showToastMessage(R.string.blocked);
            activity.startActivity(getNewIntent(KeyRecoveryActivity.class));
            activity.finish();
        } else {
            try {
                sleepBeforeRetry();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            showToastMessage(String.format(
                    activity.getString(R.string.wrong_code_you_have_n_attempts),
                    authAttempts));
        }

        resetPassword();
    }

    protected Intent getNextIntentAfterAuth() {
        var fsaEntry = fsaResolver.getFsaEntryOfCurrentRunningFs();

        int lastMigrationVersion = dataVersionManager.getCurrentVersion();
        int currentDataSchemaVersion = BuildConfig.DATA_SCHEMA_VERSION;

        if (fsaEntry != null) {
            return getNewIntent(fsaEntry.getActivityClass());
        } else if (lastMigrationVersion < currentDataSchemaVersion) {
            return getNewIntent(MigrationActivity.class);
        }

        return getNewIntent(NotesListActivity.class);
    }

    protected Intent getNewIntent(Class<? extends ActivityBase> activityClass) {
        return new Intent(activity.getApplicationContext(), activityClass);
    }

    protected void resetPassword() {
        TextView censoredPasswordView = activity.findViewById(R.id.censoredPasswordTextView);

        censoredPasswordView.setText("");
        passwordBuilder.wipe();
    }

    protected void sleepBeforeRetry() throws InterruptedException {
        Thread.sleep(DELAY_AFTER_AUTH_FAILED);
    }

    protected void showToastMessage(int stringResId) {
        showToastMessage(activity.getString(stringResId));
    }

    protected void showToastMessage(String text) {
        ActivityUtils.showToastMessage(activity, text, Toast.LENGTH_SHORT);
    }
}
