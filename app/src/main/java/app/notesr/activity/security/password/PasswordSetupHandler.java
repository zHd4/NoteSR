/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.security.password;

import static app.notesr.core.util.CharUtils.charsToBytes;

import android.content.Intent;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import app.notesr.R;
import app.notesr.activity.security.KeySetupMode;
import app.notesr.activity.security.SetupKeyActivity;
import app.notesr.core.security.SecretCache;
import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.core.util.SecureStringBuilder;
import app.notesr.util.ActivityUtils;
import lombok.RequiredArgsConstructor;

/**
 * Coordinates first-run password setup, including confirmation of the password and transition
 * to key setup.
 */
@RequiredArgsConstructor
public class PasswordSetupHandler {

    private final AuthActivity activity;
    private final SecureStringBuilder passwordBuilder;
    private final ActivityUtils activityUtils;
    private final PasswordHelper passwordHelper;
    private final PasswordFlowCompletionHelper passwordFlowCompletionHelper;

    private char[] createdPassword;

    /**
     * Collects and confirms a new password, stores it for the key-setup flow, and navigates to
     * key setup.
     *
     * <p>If the password has not yet been entered and confirmed, this method advances the
     * password-entry flow and returns without navigating.
     *
     * @throws CharacterCodingException if the confirmed password cannot be encoded as UTF-8
     */
    public void createPassword() throws CharacterCodingException {
        char[] password = proceedPasswordSetting();

        if (password != null) {
            var passwordBytes = charsToBytes(password, StandardCharsets.UTF_8);
            SecretCache.put(SetupKeyActivity.CACHE_KEY_PASSWORD, passwordBytes);

            Intent setupKeyActivityIntent = passwordFlowCompletionHelper
                    .getNewActivityIntent(SetupKeyActivity.class)
                    .putExtra(SetupKeyActivity.EXTRA_MODE, KeySetupMode.FIRST_RUN.getModeName());

            passwordFlowCompletionHelper.navigateNextActivity(setupKeyActivityIntent);
        }
    }

    /**
     * Processes the password entered in the current step of the setup flow.
     *
     * <p>The first valid entry is retained for comparison and prompts the user to repeat it.
     * A matching second entry is returned; an insufficient-length or nonmatching entry is
     * rejected and the password input is reset.
     *
     * @return the confirmed password, or {@code null} if another entry is required
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
     * Clears the password currently entered in the password input.
     */
    protected void resetPassword() {
        passwordHelper.resetPassword();
    }
}
