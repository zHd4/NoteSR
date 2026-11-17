/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.security.password;

import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;

import app.notesr.R;
import app.notesr.activity.note.list.NotesListActivity;
import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.core.util.SecureStringBuilder;
import app.notesr.service.security.rotation.SecretsRotationFailedException;
import app.notesr.service.security.rotation.SecretsRotationService;
import app.notesr.util.ActivityUtils;
import lombok.RequiredArgsConstructor;

/**
 * Coordinates the password-change flow, including confirmation of the new password and
 * rotation of the stored secrets.
 */
@RequiredArgsConstructor
public class PasswordChangeHandler {

    private final AuthActivity activity;
    private final SecureStringBuilder passwordBuilder;
    private final ActivityUtils activityUtils;
    private final SecretsRotationService secretsRotationService;
    private final PasswordFlowCompletionHelper passwordFlowCompletionHelper;
    private final PasswordHelper passwordHelper;

    private char[] createdPassword;

    /**
     * Collects and confirms the new password, updates the password and stored secrets, then
     * navigates to the notes list.
     *
     * <p>If the password has not yet been entered and confirmed, this method only advances
     * the password-entry flow and returns without changing the password.
     *
     * @throws IllegalArgumentException if the new password is invalid
     * @throws SecretsRotationFailedException if rotating the stored secrets fails
     */
    public void changePassword() {
        char[] password = proceedPasswordSetting();

        if (password == null) {
            return;
        }

        secretsRotationService.updatePassword(password);
        activityUtils.showToastMessage(activity.getString(R.string.updated),
                Toast.LENGTH_SHORT);

        passwordFlowCompletionHelper.navigateNextActivity(
                passwordFlowCompletionHelper.getNewActivityIntent(NotesListActivity.class));
    }

    /**
     * Processes the password entered in the current step of the change flow.
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
