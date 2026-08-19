/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.security;

import android.content.Context;
import android.content.Intent;

import app.notesr.activity.ActivityBase;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class GenerateNewKeyAction {

    private final ActivityBase activity;

    public void startActivity() {
        Context context = activity.getApplicationContext();
        var intent = new Intent(context, SetupKeyActivity.class)
                .putExtra(SetupKeyActivity.EXTRA_MODE, KeySetupMode.REGENERATION.getModeName());
        activity.startActivity(intent);
    }
}
