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
import app.notesr.service.security.AppSecurityService;
import app.notesr.util.ActivityUtils;
import lombok.RequiredArgsConstructor;

/**
 * Coordinates password confirmation and app-key recovery for the access code activity.
 */
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
     * Completes the key-recovery flow after the user enters and confirms a new
     * password.
     *
     * <p>If the password has not yet been confirmed, prompts for confirmation and
     * returns without recovering the key. Once confirmed, consumes the cached key,
     * unlocks the app, resets the password input, initializes post-authentication
     * services, and navigates to the next activity.
     *
     * @throws CharacterCodingException if the cached key cannot be decoded as UTF-8
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

    /**
     * Validates the entered password and, on the second entry, checks that it
     * matches the first entry.
     *
     * <p>On the first valid entry, stores the password for comparison and updates
     * the prompt to request confirmation. Input is reset after each attempt.
     *
     * @return the confirmed password if both entries match; {@code null} if the
     *         password is too short or the confirmation does not match
     */
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

    /**
     * Clears the current password input through the password helper.
     */
    protected void resetPassword() {
        passwordHelper.resetPassword();
    }
}
