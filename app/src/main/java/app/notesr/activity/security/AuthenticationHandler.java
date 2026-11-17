/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.security;

import android.os.SystemClock;
import android.widget.Toast;

import app.notesr.R;
import app.notesr.core.util.SecureStringBuilder;
import app.notesr.service.security.AppSecurityException;
import app.notesr.service.security.AppSecurityService;
import app.notesr.service.security.AuthenticationFailedException;
import app.notesr.util.ActivityUtils;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AuthenticationHandler {
    private static final int MAX_ATTEMPTS = 3;
    private static final int DELAY_AFTER_AUTH_FAILED = 1500;

    private final AuthActivity activity;
    private final AppSecurityService appSecurityService;
    private final SecureStringBuilder passwordBuilder;
    private final ActivityUtils activityUtils;
    private final PasswordFlowCompletionHelper passwordFlowCompletionHelper;
    private final PasswordConfirmationHelper passwordConfirmationHelper;

    private int authAttempts = MAX_ATTEMPTS;

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
            onAuthenticationFailed();
            return;
        } catch (AppSecurityException e) {
            throw new RuntimeException(e);
        }

        onAuthenticationSuccessful();
    }

    protected void onAuthenticationSuccessful() {
        passwordConfirmationHelper.resetPassword();
        passwordFlowCompletionHelper.bootstrapPostAuthServices(activity.getApplicationContext());
        passwordFlowCompletionHelper.navigateAfterAuth();
    }

    protected void onAuthenticationFailed() {
        authAttempts--;

        if (authAttempts == 0) {
            try {
                appSecurityService.blockApp();
            } catch (AppSecurityException e) {
                throw new RuntimeException(e);
            }

            activityUtils.showToastMessage(activity.getString(R.string.blocked),
                    Toast.LENGTH_SHORT);
            activity.startActivity(
                    passwordFlowCompletionHelper.getNewIntent(KeyRecoveryActivity.class));
            activity.finish();
        } else {
            try {
                sleepBeforeRetry();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            activityUtils.showToastMessage(String.format(
                    activity.getString(R.string.wrong_code_you_have_n_attempts),
                    authAttempts), Toast.LENGTH_SHORT);
        }

        passwordConfirmationHelper.resetPassword();
    }

    protected void sleepBeforeRetry() throws InterruptedException {
        SystemClock.sleep(DELAY_AFTER_AUTH_FAILED);
    }
}
