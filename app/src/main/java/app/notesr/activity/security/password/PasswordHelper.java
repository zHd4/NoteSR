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

/**
 * Validates access-code input and manages the two-entry confirmation required
 * when creating a password.
 *
 * <p>The helper retains the first valid entry while the user confirms it and
 * clears the visible and buffered input after each attempt.
 */
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
     * @return the entered password when the confirmation matches; {@code null}
     *         while awaiting confirmation, or if the password is too short or
     *         does not match the first entry
     */
    public char[] proceedPasswordSetting() {
        char[] password = passwordBuilder.toCharArray();

        if (createdPassword == null) {
            if (passwordBuilder.length() >= CryptoSecrets.PASSWORD_MIN_LENGTH) {
                createdPassword = password;

                TextView accessCodePrompt = activity.findViewById(R.id.accessCodePrompt);
                accessCodePrompt.setText(activity.getString(R.string.repeat_access_code));
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
     * Clears the visible password text and wipes the current input buffer.
     *
     * <p>If confirmation is in progress, the first entered password is retained
     * so that the user can try entering its confirmation again.
     */
    public void resetPassword() {
        TextView censoredPasswordView = activity.findViewById(R.id.censoredPasswordTextView);
        censoredPasswordView.setText("");
        passwordBuilder.wipe();
    }
}
