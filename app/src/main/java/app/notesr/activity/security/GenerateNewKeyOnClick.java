/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.security;

import static app.notesr.core.util.CharUtils.charsToBytes;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import app.notesr.activity.ActivityBase;
import app.notesr.R;
import app.notesr.activity.DialogFactory;
import app.notesr.core.security.SecretCache;
import app.notesr.core.security.crypto.CryptoManager;
import app.notesr.core.security.crypto.CryptoManagerProvider;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public final class GenerateNewKeyOnClick implements Consumer<ActivityBase> {
    @Override
    public void accept(ActivityBase activity) {
        DialogInterface.OnClickListener buttonHandler = regenerateKeyDialogOnClick(activity);
        new DialogFactory(activity)
                .getThemedAlertDialogBuilder(R.layout.dialog_re_encryption_warning)
                .setTitle(R.string.warning)
                .setPositiveButton(R.string.yes, buttonHandler)
                .setNegativeButton(R.string.no, buttonHandler)
                .create()
                .show();
    }

    private DialogInterface.OnClickListener regenerateKeyDialogOnClick(ActivityBase activity) {
        return (dialog, result) -> {
            if (result == DialogInterface.BUTTON_POSITIVE) {
                Context context = activity.getApplicationContext();
                Intent intent = new Intent(context, SetupKeyActivity.class);

                CryptoManager cryptoManager = CryptoManagerProvider.getInstance(context);
                char[] password = cryptoManager.getSecrets().getPassword();

                intent.putExtra(SetupKeyActivity.EXTRA_MODE, KeySetupMode.REGENERATION.toString());

                try {
                    byte[] passwordBytes = charsToBytes(password, StandardCharsets.UTF_8);
                    SecretCache.put(SetupKeyActivity.PASSWORD, passwordBytes);
                } catch (CharacterCodingException e) {
                    throw new RuntimeException(e);
                }

                activity.startActivity(intent);
            }
        };
    }
}
