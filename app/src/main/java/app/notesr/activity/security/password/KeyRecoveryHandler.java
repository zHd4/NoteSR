/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.security.password;

import static app.notesr.activity.security.password.AppPasswordActivity.CACHE_KEY_HEX_KEY;
import static app.notesr.core.util.CharUtils.bytesToChars;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;

import app.notesr.activity.note.list.NotesListActivity;
import app.notesr.core.security.SecretCache;
import app.notesr.core.security.dto.CryptoSecrets;
import app.notesr.core.util.KeyUtils;
import app.notesr.service.security.AppSecurityService;
import lombok.RequiredArgsConstructor;

/**
 * Coordinates password confirmation and app-key recovery for the {@link AppPasswordActivity}.
 */
@RequiredArgsConstructor
public class KeyRecoveryHandler {

    private final AppPasswordActivity activity;
    private final AppSecurityService appSecurityService;
    private final PasswordFlowCompletionHelper passwordFlowCompletionHelper;
    private final PasswordHelper passwordHelper;

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
        char[] password = passwordHelper.proceedPasswordSetting();

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
}
