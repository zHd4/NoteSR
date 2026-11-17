/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.security;

import static app.notesr.activity.security.AuthActivity.CACHE_KEY_HEX_KEY;
import static app.notesr.core.util.CharUtils.bytesToChars;

import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import app.notesr.R;
import app.notesr.activity.note.list.NotesListActivity;
import app.notesr.core.security.SecretCache;
import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.core.util.KeyUtils;
import app.notesr.core.util.SecureStringBuilder;
import app.notesr.service.security.AppSecurityException;
import app.notesr.service.security.AppSecurityService;
import app.notesr.service.security.AuthenticationFailedException;
import app.notesr.util.ActivityUtils;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class KeyRecoveryHandler {

    private final AuthActivity activity;
    private final AppSecurityService appSecurityService;
    private final SecureStringBuilder passwordBuilder;
    private final ActivityUtils activityUtils;
    private final PasswordFlowCompletionHelper passwordFlowCompletionHelper;
    private final PasswordHelper passwordHelper;

    private char[] createdPassword;

    /**
     * Recovers the app key.
     *
     * @throws AppSecurityException          If the key recovery fails.
     * @throws AuthenticationFailedException if the key is invalid
     * @throws CharacterCodingException      If the key recovery fails due to character
     *                                       encoding issues.
     */
    public void recoverKey() throws CharacterCodingException {
        char[] password = proceedPasswordSetting();

        if (password != null) {
            byte[] hexKeyBytes = SecretCache.take(CACHE_KEY_HEX_KEY);

            if (hexKeyBytes == null) {
                throw new RuntimeException("Missing hex key");
            }

            char[] hexKey = bytesToChars(hexKeyBytes, StandardCharsets.UTF_8);
            CryptoSecrets secrets = KeyUtils.getSecretsFromKeyHexAndPassword(hexKey, password);

            appSecurityService.unblockApp(secrets);
            secrets.destroy();

            passwordHelper.resetPassword();
            passwordFlowCompletionHelper.bootstrapPostAuthServices(
                    activity.getApplicationContext());

            passwordFlowCompletionHelper.navigateNextOrIntermediateActivity(
                    passwordFlowCompletionHelper.getNewActivityIntent(NotesListActivity.class));
        }
    }

    protected char[] proceedPasswordSetting() {
        char[] password = passwordBuilder.toCharArray();

        if (createdPassword == null) {
            if (passwordBuilder.length() >= CryptoSecrets.PASSWORD_MIN_LENGTH) {
                createdPassword = password;

                TextView topLabel = activity.findViewById(R.id.authTopLabel);
                topLabel.setText(activity.getString(R.string.repeat_access_code));
            } else {
                activityUtils.showToastMessage(String.format(
                        activity.getString(R.string.minimum_password_length_is_n),
                        CryptoSecrets.PASSWORD_MIN_LENGTH), Toast.LENGTH_SHORT);
            }
        } else {
            if (Arrays.equals(password, createdPassword)) {
                resetPassword();
                return password;
            }

            activityUtils.showToastMessage(activity.getString(R.string.code_not_match),
                    Toast.LENGTH_SHORT);
        }

        resetPassword();
        return null;
    }

    protected void resetPassword() {
        passwordHelper.resetPassword();
    }
}
