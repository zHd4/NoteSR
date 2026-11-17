/*
 * Copyright (c) 2026 zHd4
 * SPDX-License-Identifier: MIT
 */

package app.notesr.activity.security;

import android.content.Context;
import android.content.Intent;

import app.notesr.activity.ActivityBase;
import app.notesr.activity.security.password.AppPasswordActivity;
import app.notesr.service.security.AppSecurityService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class LockAction {

    private final ActivityBase activity;
    private final AppSecurityService appSecurityService;

    public void lock() {
        appSecurityService.logout();

        Context context = activity.getApplicationContext();
        Intent appPasswordActivityIntent = new Intent(context, AppPasswordActivity.class)
                .putExtra(AppPasswordActivity.EXTRA_MODE,
                        AppPasswordActivity.Mode.AUTHENTICATION.getModeName());

        activity.startActivity(appPasswordActivityIntent);
        activity.finish();
    }
}
