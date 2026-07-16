/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.security;

import static app.notesr.core.util.CharUtils.charsToBytes;

import android.content.Context;
import android.content.Intent;

import app.notesr.activity.ActivityBase;
import app.notesr.core.security.SecretCache;
import app.notesr.core.security.crypto.CryptoManager;
import app.notesr.core.security.dto.CryptoSecrets;
import lombok.RequiredArgsConstructor;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@RequiredArgsConstructor
public final class GenerateNewKeyAction {

    private final ActivityBase activity;
    private final CryptoManager cryptoManager;

    public void startActivity() {
        CryptoSecrets secrets = cryptoManager.getSecrets();
        char[] passwordChars = Arrays.copyOf(secrets.getPassword(), secrets.getPassword().length);

        try {
            byte[] passwordBytes = charsToBytes(passwordChars, StandardCharsets.UTF_8);
            SecretCache.put(SetupKeyActivity.CACHE_KEY_PASSWORD, passwordBytes);
        } catch (CharacterCodingException e) {
            throw new RuntimeException(e);
        }

        secrets.destroy();

        Context context = activity.getApplicationContext();
        var intent = new Intent(context, SetupKeyActivity.class)
                .putExtra(SetupKeyActivity.EXTRA_MODE, KeySetupMode.REGENERATION.toString());
        activity.startActivity(intent);
    }
}
