/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.security.password;

import static app.notesr.core.util.CharUtils.charsToBytes;

import android.content.Intent;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;

import app.notesr.activity.security.KeySetupMode;
import app.notesr.activity.security.SetupKeyActivity;
import app.notesr.core.security.SecretCache;
import lombok.RequiredArgsConstructor;

/**
 * Coordinates first-run password setup, including confirmation of the password and transition
 * to key setup.
 */
@RequiredArgsConstructor
public class PasswordSetupHandler {

    private final PasswordHelper passwordHelper;
    private final PasswordFlowCompletionHelper passwordFlowCompletionHelper;

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
        char[] password = passwordHelper.proceedPasswordSetting();

        if (password != null) {
            var passwordBytes = charsToBytes(password, StandardCharsets.UTF_8);
            SecretCache.put(SetupKeyActivity.CACHE_KEY_PASSWORD, passwordBytes);

            Intent setupKeyActivityIntent = passwordFlowCompletionHelper
                    .getNewActivityIntent(SetupKeyActivity.class)
                    .putExtra(SetupKeyActivity.EXTRA_MODE, KeySetupMode.FIRST_RUN.getModeName());

            passwordFlowCompletionHelper.navigateNextActivity(setupKeyActivityIntent);
        }
    }
}
