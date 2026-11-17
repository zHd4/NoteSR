/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.security;

import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import app.notesr.R;
import app.notesr.activity.note.list.NotesListActivity;
import app.notesr.core.util.SecureStringBuilder;
import app.notesr.service.security.AppSecurityException;
import app.notesr.service.security.AppSecurityService;
import app.notesr.service.security.AuthenticationFailedException;
import app.notesr.util.ActivityUtils;
import lombok.RequiredArgsConstructor;

/**
 * Coordinates password authentication and the resulting navigation for the access code
 * activity.
 *
 * <p>Failed attempts are counted and eventually block the app, directing the user to key
 * recovery.
 */
@RequiredArgsConstructor
public class AuthenticationHandler {

    private static final String TAG = AuthenticationHandler.class.getSimpleName();

    private static final int MAX_ATTEMPTS = 3;
    private static final int DELAY_AFTER_AUTH_FAILED = 1500;

    private final AuthActivity activity;
    private final AppSecurityService appSecurityService;
    private final SecureStringBuilder passwordBuilder;
    private final ActivityUtils activityUtils;
    private final PasswordFlowCompletionHelper passwordFlowCompletionHelper;
    private final PasswordHelper passwordHelper;

    private int authAttempts = MAX_ATTEMPTS;

    /**
     * Authenticates the password currently entered by the user.
     *
     * <p>An empty password prompts the user to enter a code. A failed authentication decrements
     * the remaining-attempt count; a successful authentication completes the password flow and
     * navigates onward.
     *
     * @throws AppSecurityException if an unexpected security error occurs during authentication
     */
    public void authenticate() {
        var password = passwordBuilder.toCharArray();

        if (password.length == 0) {
            activityUtils.showToastMessage(activity.getString(R.string.enter_the_code),
                    Toast.LENGTH_SHORT);
            return;
        }

        try {
            appSecurityService.authenticate(password);
        } catch (AuthenticationFailedException e) {
            Log.e(TAG, "Authentication failed", e);
            onAuthenticationFailed();
            return;
        }

        onAuthenticationSuccessful();
    }

    /**
     * Resets the entered password, initializes post-authentication services, and navigates to
     * the next activity in the password flow.
     */
    protected void onAuthenticationSuccessful() {
        passwordHelper.resetPassword();
        passwordFlowCompletionHelper.bootstrapPostAuthServices(activity.getApplicationContext());

        passwordFlowCompletionHelper.navigateNextOrIntermediateActivity(
                passwordFlowCompletionHelper.getNewActivityIntent(NotesListActivity.class));
    }

    /**
     * Records a failed authentication attempt and updates the password flow.
     *
     * <p>When no attempts remain, blocks the app and navigates to key recovery. Otherwise, waits
     * before allowing another attempt and displays the remaining-attempt count. The entered
     * password is reset in either case.
     */
    protected void onAuthenticationFailed() {
        authAttempts--;

        if (authAttempts == 0) {
            appSecurityService.blockApp();

            activityUtils.showToastMessage(activity.getString(R.string.blocked),
                    Toast.LENGTH_SHORT);

            passwordFlowCompletionHelper.navigateNextActivity(
                    passwordFlowCompletionHelper.getNewActivityIntent(KeyRecoveryActivity.class));
        } else {
            sleepBeforeRetry();

            activityUtils.showToastMessage(String.format(
                    activity.getString(R.string.wrong_code_you_have_n_attempts),
                    authAttempts), Toast.LENGTH_SHORT);
        }

        passwordHelper.resetPassword();
    }

    /**
     * Pauses the current thread for the retry delay after a failed authentication attempt.
     */
    protected void sleepBeforeRetry() {
        SystemClock.sleep(DELAY_AFTER_AUTH_FAILED);
    }
}
