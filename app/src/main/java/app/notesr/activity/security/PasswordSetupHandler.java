/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.security;

import static app.notesr.core.util.CharUtils.charsToBytes;

import android.content.Intent;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import app.notesr.R;
import app.notesr.core.security.SecretCache;
import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.core.util.SecureStringBuilder;
import app.notesr.util.ActivityUtils;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PasswordSetupHandler {

    private final AuthActivity activity;
    private final SecureStringBuilder passwordBuilder;
    private final ActivityUtils activityUtils;
    private final PasswordHelper passwordHelper;
    private final PasswordFlowCompletionHelper passwordFlowCompletionHelper;

    private char[] createdPassword;

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
