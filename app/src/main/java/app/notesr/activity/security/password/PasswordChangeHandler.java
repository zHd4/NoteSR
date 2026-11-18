/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.security.password;

import android.widget.Toast;

import app.notesr.R;
import app.notesr.activity.note.list.NotesListActivity;
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

    private final AppPasswordActivity activity;
    private final ActivityUtils activityUtils;
    private final SecretsRotationService secretsRotationService;
    private final PasswordFlowCompletionHelper passwordFlowCompletionHelper;
    private final PasswordHelper passwordHelper;

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
        char[] password = passwordHelper.proceedPasswordSetting();

        if (password == null) {
            return;
        }

        secretsRotationService.updatePassword(password);
        activityUtils.showToastMessage(activity.getString(R.string.updated),
                Toast.LENGTH_SHORT);

        passwordFlowCompletionHelper.navigateNextActivity(
                passwordFlowCompletionHelper.getNewActivityIntent(NotesListActivity.class));
    }
}
