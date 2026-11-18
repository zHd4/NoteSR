/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.security.password;

import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;

import app.notesr.R;
import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.core.util.SecureStringBuilder;
import app.notesr.util.ActivityUtils;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PasswordHelper {

    private final AppPasswordActivity activity;
    private final SecureStringBuilder passwordBuilder;
    private final ActivityUtils activityUtils;

    private char[] createdPassword;

    /**
     * Validates the entered password and, on the second entry, checks that it
     * matches the first entry.
     *
     * <p>On the first valid entry, stores the password for comparison and updates
     * the prompt to request confirmation. Input is reset after each attempt.
     *
     * @return the confirmed password if both entries match; {@code null} if the
     * password is too short or the confirmation does not match
     */
    public char[] proceedPasswordSetting() {
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
     * Resets the password input and clears the cached password.
     */
    public void resetPassword() {
        TextView censoredPasswordView = activity.findViewById(R.id.censoredPasswordTextView);
        censoredPasswordView.setText("");
        passwordBuilder.wipe();
    }
}
